package com.hpe.zg.ossm.etl

import akka.actor.typed.ActorSystem
import akka.stream.Materializer
import com.hpe.zg.akka.message.OssmBeMessage
import com.hpe.zg.ossm.node.GraphNode
import com.hpe.zg.util.Utils
import org.json.{JSONException, JSONObject}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext


class GraphEngine(akkaSystem: ActorSystem[OssmBeMessage]) {
    private val logger = LoggerFactory.getLogger(classOf[GraphEngine])
    implicit val system: ActorSystem[OssmBeMessage] = akkaSystem
    implicit val mat: Materializer = Materializer(system)
    implicit val ec: ExecutionContext = system.executionContext


    import scala.jdk.CollectionConverters._
    import java.util

    def parseDefinition(jsonString: String): Unit = {
        val map = try {
            new JSONObject(jsonString).toMap.asScala
        } catch {
            case e: JSONException =>
                logger.error(s"Failed to parse definition object.\n$e\n$jsonString")
                null
        }

        val nodes = map.getOrElse("nodes", null)
        val nodelist: List[GraphNode] = nodes match {
            case list: util.ArrayList[util.Map[String, AnyRef]] =>
                list.asScala.map(GraphNode(_)).toList
            case _ =>
                logger.error(s"ignore illegal node definition $nodes")
                List.empty
        }
    }
}

object GraphEngineTest extends App {

    import scala.io.Source
    import akka.actor.typed.scaladsl.Behaviors

    private val logger = LoggerFactory.getLogger(GraphEngineTest.getClass)

    val path = """C:\mycode\spring\target\classes\scenario1.json"""
    val system: ActorSystem[OssmBeMessage] = ActorSystem(Behaviors.empty, "test")
    val engine = new GraphEngine(system)
    Utils.using(Source.fromFile(path))(buff => {
        engine.parseDefinition(buff.mkString)
    })(e => {
        logger.error(s"Error $e")
        null
    })
}
