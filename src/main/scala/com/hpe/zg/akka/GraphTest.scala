package com.hpe.zg.akka

import java.util

import akka.{Done, NotUsed}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.{ClosedShape, DelayOverflowStrategy, FlowShape, Graph, KillSwitches, Materializer, Outlet, Shape, SinkShape, SourceShape, UniformFanInShape, UniformFanOutShape}
import akka.stream.scaladsl._
import akka.stream.stage.GraphStage
import com.hpe.zg.ossm.graph.{FlowMap, FlowMap2Json, Node_Type, ShapeBroadcast, ShapeMerge, SinkLast, SinkPrint, SourceSimulator}
import org.json.JSONObject

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Random, Success}

object GraphTest extends App {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "test")
    implicit val mat: Materializer = Materializer(system)
    implicit val ec: ExecutionContext = system.executionContext

    val source: Source[Int, Int] = Source(1 to 10)
        .mapMaterializedValue(m => println(m.getClass))
        .watchTermination()((_, t) => {
            t.onComplete(_ => println("graph1 completed"))
            1
        })
    val sink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _).mapMaterializedValue(m => {
        m.onComplete {
            case Success(value) => println(s"1111111111111 $value")
            case Failure(e) => println(s"failed $e ")
        }
        m
    })


    // connect the Source to the Sink, obtaining a RunnableGraph
    val runnable: RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)
    val (a, b) = source.toMat(sink)(Keep.both).run()

    // materialize the flow and get the value of the FoldSink
    //    val sum: Future[Int] = runnable.run()

    //    sum.onComplete {
    //        case Success(value) => println(s"$value")
    //        case Failure(e) => println(s"failed $e ")
    //    }

    //kill graph
    val sink2: Sink[Int, Future[Done]] = Sink.foreach(println)
    val source2 = Source.
        fromIterator(() => Iterator.continually(Random.nextInt(100))).delay(500.millis, DelayOverflowStrategy.dropHead)
    //    val (k, d) = source2.map(0 - _).viaMat(KillSwitches.single)(Keep.right).toMat(sink2)(Keep.both).run()

    import akka.stream.KillSwitch

    case class Kill(k: KillSwitch) extends Runnable {
        override def run(): Unit = {
            k.shutdown()
            //            system.terminate()
        }
    }

    //    system.scheduler.scheduleOnce(5.second, Kill(k))


    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._
        val in = Source(1 to 10)
        val in2: Option[Source[Int, NotUsed]] = None
        val out = Sink.foreach(println)

        val bcast = builder.add(Broadcast[Int](2))
        val merge = builder.add(Merge[Int](2))

        val f1, f2, f3, f4 = Flow[Int].map(_ + 10)

        val step1 = in ~> f1
        step1 match {
            case s: Graph[SourceShape[Int], NotUsed] => s ~> bcast ~> f2 ~> merge ~> f3 ~> out
            case s: PortOps[Int] => s ~> bcast ~> f2 ~> merge ~> f3 ~> out
            case _ =>
        }

        bcast ~> f4 ~> merge
        ClosedShape
    })
    //    g.run()


    import java.util
    import scala.jdk.CollectionConverters._
    import scala.io.{Codec, Source => ioSource}
    import com.hpe.zg.util.Utils

    val map = {
        val path = GraphTest.getClass.getResource("/").getPath + "graphs/scenario1.json"

        implicit val codec: Codec = Codec.UTF8

        Utils.using(ioSource.fromFile(path))(buff => {
            new JSONObject(buff.mkString).toMap
        })(e => {
            println(s"Error during create JSONObject $e")
            null
        })
    }
    val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

        import GraphDSL.Implicits._

        val shapes = mutable.HashMap[String, AnyRef]()
        val flows = mutable.HashMap[String, Flow[Serializable, Serializable, AnyRef]]()

        val ports = mutable.HashMap[(String, String), Int]()

        def getPort(id: String, tp: String): Int = {
            val index: Int = ports.get(id, tp).getOrElse(0)
            ports((id, tp)) = index + 1
            index
        }

        val nodes = map.get("nodes").asInstanceOf[util.ArrayList[util.Map[String, AnyRef]]]
        nodes.asScala.collect {
            case node: util.Map[String, AnyRef]
                if Node_Type.SOURCE_SIMULATOR.equals(node.get("type").toString) =>
                shapes(node.get("id").toString) = SourceSimulator(node).get
            case node: util.Map[String, AnyRef]
                if Node_Type.SINK_LAST.equals(node.get("type").toString) =>
                shapes(node.get("id").toString) = SinkLast(node).get
            case node: util.Map[String, AnyRef]
                if Node_Type.SINK_PRINT.equals(node.get("type").toString) =>
                shapes(node.get("id").toString) = SinkPrint(node).get
            case node: util.Map[String, AnyRef]
                if Node_Type.SHAPE_BROADCAST.equals(node.get("type").toString) =>
                shapes(node.get("id").toString) = builder.add(ShapeBroadcast(node).get)
            case node: util.Map[String, AnyRef]
                if Node_Type.SHAPE_MERGE.equals(node.get("type").toString) =>
                shapes(node.get("id").toString) = builder.add(ShapeMerge(node).get)
            case _ =>
        }

        val flows2 = map.get("flows").asInstanceOf[util.ArrayList[util.Map[String, AnyRef]]]
        flows2.asScala.collect {
            case node: util.Map[String, AnyRef]
                if Node_Type.FLOW_MAP2JSON.equals(node.get("type").toString) =>
                flows(node.get("id").toString) = FlowMap2Json().get
            case node: util.Map[String, AnyRef]
                if Node_Type.FLOW_MAP.equals(node.get("type").toString) =>
                flows(node.get("id").toString) = FlowMap(node).get
            case _ =>
        }


        @scala.annotation.tailrec
        def connectToFlows(out: PortOps[Serializable], flows: List[Flow[Serializable, Serializable, AnyRef]]): PortOps[Serializable] =
            if (flows.isEmpty) out
            else connectToFlows(out ~> flows.head, flows.slice(1, flows.length))

        val connections = map.get("connections").asInstanceOf[util.ArrayList[util.Map[String, AnyRef]]]
        connections.forEach(conn => {
            val fromId = conn.get("from").toString
            //            println(s"from $fromId")
            val from = shapes(fromId) match {
                case s: Graph[SourceShape[Serializable], Any] =>
                    println(s"from source out")
                    builder.add(s).out
                case s: UniformFanOutShape[Serializable, Serializable] =>
                    val port = getPort(fromId, "out")
                    println(s"from broadcast out $port");
                    s.out(port)
                case s: MergePreferred.MergePreferredShape[Serializable] =>
                    println(s"from merge out");
                    s.out

            }

            val toId = conn.get("to").toString
            val to = shapes(toId) match {
                case s: UniformFanOutShape[Serializable, Serializable] =>
                    println(s"to broadcast in ")
                    s.in
                case s: MergePreferred.MergePreferredShape[Serializable] =>
                    val port = getPort(toId, "in")
                    println(s"to merge in $port");
                    s.in(port)
                case s: Graph[SinkShape[Serializable], Any] =>
                    println(s"to sink in")
                    builder.add(s).in
            }

            val viaIds = conn.get("via").asInstanceOf[util.ArrayList[String]]
            println(s"via $viaIds")
            println(s"---------------------")
            connectToFlows(from, viaIds.asScala.map(id => flows(id)).toList) ~> to

        })
        ClosedShape
    })

    graph.run
}
