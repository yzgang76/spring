package com.hpe.zg.util

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.stream.Materializer
import com.alibaba.fastjson.{JSON, JSONObject}
import com.hpe.zg.spring.MyApplication
import org.springframework.web.context.request.async.DeferredResult

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Utils {
    def httpGet(uri: String): DeferredResult[JSONObject] = {
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
            Http()(MyApplication.system.toClassic).singleRequest(HttpRequest(uri = uri, method = HttpMethods.GET))

        implicit val ec: ExecutionContextExecutor = MyApplication.system.executionContext
        implicit val mat: Materializer = Materializer(MyApplication.system)

        import akka.http.scaladsl.unmarshalling.Unmarshal
        responseFuture.onComplete {
            case Success(res) => Unmarshal(res.entity).to[String].onComplete {
                case Success(str) =>
                    result.setResult(JSON.parseObject(str))
                case Failure(e) => result.setErrorResult(e.toString)
            }

            case Failure(e) => result.setErrorResult(e.toString)
        }
        result
    }

    def using[A <: {def close(): Unit}, B](resource: A)(f: A => B)(l: Throwable => B): B =
        try {
            f(resource)
        } catch {
            case e: Throwable => l(e)
        } finally {
            if (resource != null) resource.close()
        }
}
