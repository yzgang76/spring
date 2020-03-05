package com.hpe.zg.spring

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.stream.Materializer

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}
import com.alibaba.fastjson.{JSON, JSONObject}
import com.typesafe.config.ConfigFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.context.request.async.DeferredResult

@SpringBootApplication
class AppConfig {
    println(s"init AppConfig")
}

@Service("BeanService")
class BeanService {
    val id: Double = Math.random()

    override def toString: String = s"BeanService - $id"
}


object MyAkkaSystem {

    sealed trait MyCommand

    final case class Request(ref: ActorRef[MyAkkaSystem.MyCommand]) extends MyAkkaSystem.MyCommand

    final case class Response(msg: String) extends MyAkkaSystem.MyCommand {
        def toJson: JSONObject = {
            val json = new JSONObject
            json.put("code", 0)
            json.put("msg", msg)
            json
        }
    }

    val context: ApplicationContext = new ClassPathXmlApplicationContext("cp_context.xml")
    val beanService: AnyRef = context.getBean("beanService")

    def apply(): Behavior[MyAkkaSystem.MyCommand] = Behaviors.setup {
        context =>
            context.log.info(s"MyAkkaSystem starting")
            println(s"MyAkkaSystem starting")
            Behaviors.receiveMessage {
                case Request(replyTo) =>
                    replyTo ! Response(s"hello, I am $beanService")
                    Behaviors.same
                case _ => Behaviors.same
            }
    }
}

object MyApplication {
    implicit val system: ActorSystem[MyAkkaSystem.MyCommand] =
        ActorSystem[MyAkkaSystem.MyCommand](MyAkkaSystem(), "myakkasystem", ConfigFactory.load("clustermanager"))

    def main(args: Array[String]): Unit = {
        new SpringApplicationBuilder(classOf[AppConfig]).run(args: _*)
    }
}

import com.alibaba.fastjson.JSONObject
import org.springframework.web.bind.annotation.{RequestMapping, RestController}

@RestController
class indexController {
    @RequestMapping(value = Array("/index"))
    def index(): JSONObject = {
        val json = new JSONObject
        json.put("code", 0)
        json.put("data", "success")
        json
    }

    @RequestMapping(value = Array("/info"))
    @ResponseBody
    def info(): DeferredResult[JSONObject] = {
        import akka.actor.typed.scaladsl.adapter._
        val result = new DeferredResult[JSONObject](6 * 1000L)
        result.onTimeout(() => {
            println(s"DeferredResult overtime")
            val json = new JSONObject
            json.put("code", 500)
            json.put("error", "timeout")
            result.setResult(json)
        })
        val responseFuture: Future[HttpResponse] =
            Http()(MyApplication.system.toClassic).singleRequest(HttpRequest(uri = "http://localhost:8082/info", method = HttpMethods.GET))

        implicit val ec: ExecutionContextExecutor = MyApplication.system.executionContext
        implicit val mat: Materializer = Materializer(MyApplication.system)

        import akka.http.scaladsl.unmarshalling.Unmarshal
        responseFuture.onComplete{
            case Success(res)=> Unmarshal(res.entity).to[String].onComplete{
                case Success(str)=>
                    result.setResult(JSON.parseObject(str))
                case Failure(e)=>result.setErrorResult(e.toString)
            }

            case Failure(e)=>result.setErrorResult(e.toString)
        }
        result
    }
}

@RestController
class indexController2 {
    @RequestMapping(value = Array("/akka"))
    @ResponseBody
    def akka(): DeferredResult[JSONObject] = {
        import MyApplication.system
        import com.hpe.zg.spring.MyAkkaSystem._

        implicit val timeout: Timeout = 5.seconds
        implicit val ec: ExecutionContextExecutor = system.executionContext

        val result = new DeferredResult[JSONObject](6 * 1000L)
        result.onTimeout(() => {
            println(s"DeferredResult overtime")
            val json = new JSONObject
            json.put("code", 500)
            json.put("error", "timeout")
            result.setResult(json)
        })

        val f: Future[MyAkkaSystem.MyCommand] = system.ask[MyAkkaSystem.MyCommand](Request)(timeout = timeout, scheduler = system.scheduler)
        f.onComplete {
            case Success(r) =>
                r match {
                    case res@MyAkkaSystem.Response(_) => result.setResult(res.toJson)
                    case res: MyAkkaSystem.MyCommand => println(s"unknown response $res ")
                }
            case Failure(e) =>
                e.printStackTrace()
                result.setErrorResult(e.toString)
        }
        result
    }
}

