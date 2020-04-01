package com.hpe.zg.ossm.graph

import java.util
import java.util.Optional
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import com.hpe.zg.akka.message.OssmBeMessage
import com.hpe.zg.ossm.dimension.DimensionRepository.DimensionRepositoryMessage
import com.hpe.zg.util.Utils
import org.json.JSONObject
import scala.collection.mutable
import scala.io.{Codec, Source}
import scala.jdk.CollectionConverters._

object GraphEngine {

    trait GraphMessage extends OssmBeMessage

    final case class GetGraphs(replyTo: ActorRef[GraphList]) extends GraphMessage

    final case class GraphList(l: util.List[java.lang.String]) extends GraphMessage

    def apply(graphs: mutable.HashMap[String, ActorRef[GraphMessage]], dimensions: ActorRef[DimensionRepositoryMessage]): Behavior[GraphMessage] =
        Behaviors.setup { context =>
            context.log.debug(s"GraphEngine Starting")

            def getName(fullName: String): String = fullName.substring(0, fullName.length - 4)

            def loadGraphs(): Unit = {
                val path = GraphEngine.getClass.getResource("/").getPath + "graphs"
                implicit val codec: Codec = Codec.UTF8

                val dir = new java.io.File(path)
                for (file <- dir.listFiles.filter(f => f.getName.endsWith(".json"))) {
                    Utils.using(Source.fromFile(file.getPath))(buff => {
                        val graphName = getName(file.getName)
                        graphs(graphName) = context.spawn(Graph(graphName, new JSONObject(buff.mkString).toMap, dimensions), graphName)
                    })(e => {
                        context.log.error(s"Error during create JSONObject $e")
                    })
                }
            }

            loadGraphs()
            Behaviors.receiveMessage[GraphMessage] {
                case GetGraphs(replyTo) =>
                    Optional.ofNullable(replyTo).map(r => r ! GraphList(graphs.keys.toList.asJava))
                    Behaviors.same
                case msg: GraphMessage =>
                    context.log.warn(s"GraphEngine received unhandled message $msg")
                    Behaviors.same
            }.receiveSignal {
                case (_, signal) if signal == PostStop =>
                    context.log.info(s"GraphEngine is stopped")
                    Behaviors.same
            }
        }
}


//            implicit val mat: Materializer = Materializer(context.system)
//            implicit val ec: ExecutionContext = context.system.executionContext
//
//            import java.util
//
//                import scala.jdk.CollectionConverters._
//
//                def parseDefinition(jsonString: String): Unit = {
//                    val map = try {
//                        new JSONObject(jsonString).toMap.asScala
//                    } catch {
//                        case e: JSONException =>
//                            context.log.error(s"Failed to parse definition object.\n$e\n$jsonString")
//                            null
//                    }
//
//                    val nodes = map.getOrElse("nodes", null)
//                    val nodelist: List[GraphNode] = nodes match {
//                        case list: util.ArrayList[util.Map[String, AnyRef]] =>
//                            list.asScala.map(GraphNode(_)).toList
//                        case _ =>
//                            context.log.error(s"ignore illegal node definition $nodes")
//                            List.empty
//                    }
//            }
//
//            Behaviors.receiveMessage[GraphEngineMessage] {
//                case _ =>
//                    Behaviors.same
//                case msg: GraphEngineMessage =>
//                    context.log.warn(s"GraphEngineMessage received unhandled message $msg")
//                    Behaviors.same
//            }.receiveSignal {
//                case (_, signal) if signal == PostStop =>
//                    context.log.info(s"GraphEngineMessage is stopped")
//                    Behaviors.same
//            }
//        }


//}

//object GraphEngineTest extends App {
//
//    import scala.io.Source
//    import akka.actor.typed.scaladsl.Behaviors
//
//    private val logger = LoggerFactory.getLogger(GraphEngineTest.getClass)
//
//    val path = """C:\mycode\spring\target\classes\scenario1.json"""
//    val system: ActorSystem[OssmBeMessage] = ActorSystem(Behaviors.empty, "test")
//    val engine = new GraphEngine(system)
//    Utils.using(Source.fromFile(path))(buff => {
//        engine.parseDefinition(buff.mkString)
//    })(e => {
//        logger.error(s"Error $e")
//        null
//    })
//}
