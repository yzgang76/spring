package com.hpe.zg.spring

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout

import scala.concurrent.duration._
import akka.actor.typed.scaladsl.AskPattern._
import com.alibaba.fastjson.JSONObject
import com.typesafe.config.ConfigFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.context.request.async.DeferredResult

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

@SpringBootApplication
class AppConfig {
    println(s"000000000000000000000000")
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

    def apply(): Behavior[MyAkkaSystem.MyCommand] = Behaviors.setup {
        context =>
            context.log.info(s"MyAkkaSystem starting")
            println(s"MyAkkaSystem starting")
            Behaviors.receiveMessage {
                case Request(replyTo) =>
                    replyTo ! Response("hello")
                    Behaviors.same
                case _ => Behaviors.same
            }

    }
}

object myApplication {
    val system: ActorSystem[MyAkkaSystem.MyCommand] = ActorSystem[MyAkkaSystem.MyCommand](MyAkkaSystem(),
        "myakkasystem", ConfigFactory.load("clustermanaer"))

    def main(args: Array[String]): Unit = {
        new SpringApplicationBuilder(classOf[AppConfig]).run(args: _*)
    }
}

import com.alibaba.fastjson.JSONObject
import org.springframework.web.bind.annotation.{RequestMapping, RestController}

@RestController
class indexController {
    println(s"11111111111111111")

    @RequestMapping(value = Array("/index"))
    def index(): JSONObject = {
        val json = new JSONObject
        json.put("code", 0)
        json.put("data", "success")
        json
    }
}

@RestController
class indexController2 {
    println(s"111111111111111112")

    @RequestMapping(value = Array("/akka"))
    @ResponseBody
    def akka(): DeferredResult[JSONObject] = {
        import myApplication.system
        import com.hpe.zg.spring.MyAkkaSystem._

        implicit val timeout: Timeout = 59.seconds
        implicit val ec: ExecutionContextExecutor = system.executionContext

        import org.springframework.web.context.request.async.DeferredResult
        val result = new DeferredResult[JSONObject](60 * 1000L)
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
                println(s"_________________ $r")
                r match {
                    case res@MyAkkaSystem.Response(_) => result.setResult(res.toJson)
                    case res: MyAkkaSystem.MyCommand => println(s"unknown response ${res} ")
                }
            case Failure(e) => e.printStackTrace()
        }
        result
    }
}