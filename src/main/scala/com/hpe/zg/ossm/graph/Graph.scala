package com.hpe.zg.ossm.graph

import java.util

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.stream.scaladsl._
import akka.stream.{ClosedShape, Graph, Shape, UniformFanOutShape}
import com.hpe.zg.ossm.dimension.DimensionRepository.{DimensionRepositoryMessage, GetDimensionResponse}
import com.hpe.zg.ossm.graph.GraphEngine.GraphMessage

import scala.collection.mutable
import scala.jdk.CollectionConverters._

object Node_Type {
    val SOURCE_SIMULATOR = "source_simulator"
    val SINK_LAST = "sink_last"
    val SINK_PRINT = "sink_print"
    val SHAPE_BROADCAST = "shape_broadcast"
    val SHAPE_MERGE = "shape_merge"
    val FLOW_MAP2JSON = "flow_map2json"
    val FLOW_MAP = "flow_map"
    //TODO: more
}

trait OssmGraphNode

object Graph {

    private final case class AdaptGetDimensionResponse(res: GetDimensionResponse) extends GraphMessage


    def apply(name: String, map: util.Map[String, AnyRef], dimensionRepository: ActorRef[DimensionRepositoryMessage]): Behavior[GraphMessage] =
        Behaviors.setup { context =>
            context.log.debug(s"Graph $name Starting")
            val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

                import GraphDSL.Implicits._

                val sources = mutable.HashMap[String, Source[Serializable, AnyRef]]()
                val sinks = mutable.HashMap[String, Sink[Serializable, AnyRef]]()
                val shapeBroadcasts = mutable.HashMap[String, UniformFanOutShape[Serializable, Serializable]]()
                val shapeMerges = mutable.HashMap[String, MergePreferred.MergePreferredShape[Serializable]]()
                val flows = mutable.HashMap[String, Flow[Serializable, Serializable, AnyRef]]()

                val nodes = map.get("nodes").asInstanceOf[util.ArrayList[util.Map[String, AnyRef]]]
                nodes.asScala.collect {
                    case node: util.Map[String, AnyRef]
                        if Node_Type.SOURCE_SIMULATOR.equals(node.get("type").toString) =>
                        sources(node.get("id").toString) = SourceSimulator(node).get
                    case node: util.Map[String, AnyRef]
                        if Node_Type.SINK_LAST.equals(node.get("type").toString) =>
                        sinks(node.get("id").toString) = SinkLast(node).get
                    case node: util.Map[String, AnyRef]
                        if Node_Type.SHAPE_BROADCAST.equals(node.get("type").toString) =>
                        shapeBroadcasts(node.get("id").toString) = ShapeBroadcast(node, builder).get
                    case node: util.Map[String, AnyRef]
                        if Node_Type.SHAPE_MERGE.equals(node.get("type").toString) =>
                        shapeMerges(node.get("id").toString) = ShapeMerge(node, builder).get
                    case _ =>
                }

                val flows2 = map.get("flows").asInstanceOf[util.ArrayList[util.Map[String, AnyRef]]]
                flows2.asScala.collect {
                    case node: util.Map[String, AnyRef]
                        if Node_Type.FLOW_MAP2JSON.equals(node.get("type").toString) =>
                        flows(node.get("id").toString) = FlowMap2Json().get
                    case node: util.Map[String, AnyRef]
                        if Node_Type.FLOW_MAP.equals(node.get("type").toString) =>
                        flows(node.get("id").toString) = FlowMap(map).get
                    case _ =>
                }
                val connections = map.get("connections").asInstanceOf[util.ArrayList[util.Map[String, AnyRef]]]
                connections.forEach(conn => {
                    val fromId = conn.get("from").toString
                    val toId = conn.get("to").toString
                    val viaIds = conn.get("via").asInstanceOf[util.ArrayList[String]]
                    val from: Graph[Shape,Object] = sources.getOrElse(fromId, null)
                    val to = sinks.getOrElse(toId, shapeBroadcasts.getOrElse(toId, shapeMerges.getOrElse(toId, null)))

                    builder.add(from).outlets(0) ~> Sink.ignore
                })

//                Source.single(1) ~> Sink.ignore
                ClosedShape
            })

            Behaviors.receiveMessage[GraphMessage] {
                case msg: GraphMessage =>
                    context.log.warn(s"Graph received unhandled message $msg")
                    Behaviors.same
            }.receiveSignal {
                case (_, signal) if signal == PostStop =>
                    context.log.info(s"Graph is stopped")
                    Behaviors.same
            }
        }
}

/*
 {
      "from": "in",
      "to": "broadcast",
      "via": []
    },
    {
      "from": "broadcast",
      "to": "merge",
      "via": []
    },
    {
      "from": "broadcast",
      "to": "merge",
      "via": ["mkDelete"]
    },
    {
      "from": "merge",
      "to": "out",
      "via": ["toJson"]
    }
 */