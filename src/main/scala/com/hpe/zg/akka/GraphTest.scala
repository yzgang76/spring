package com.hpe.zg.akka

import java.util
import java.util.Optional

import akka.{Done, NotUsed}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.{ClosedShape, DelayOverflowStrategy, FlowShape, Graph, KillSwitches, Materializer, Outlet, Shape, SinkShape, SourceShape, UniformFanInShape, UniformFanOutShape}
import akka.stream.scaladsl._
import akka.stream.stage.GraphStage
import com.alibaba.fastjson.JSON
import com.hpe.zg.ossm.graph.{FlowMap, FlowMap2Json, Node_Type, ShapeBroadcast, ShapeMerge, SinkLast, SinkPrint, SourceSimulator}
import org.json.JSONObject
import org.mvel2.MVEL
import org.mvel2.integration.impl.MapVariableResolverFactory

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Random, Success}

object GraphTest extends App {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "test")
    //  implicit val mat: Materializer = Materializer(system)
    implicit val ec: ExecutionContext = system.executionContext

    /*def test(): Unit = println(s"r test")

    val source: Source[Int, Int] = Source(1 to 10)
      .mapMaterializedValue(m => println(m.getClass))
      .watchTermination()((_, t) => {
        t.onComplete(_ => println("source completed"))
        1
      })
    val sink: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0)(_ + _).mapMaterializedValue(m => {
      m.onComplete {
        case Success(value) => println(s"1111111111111 $value")
        case Failure(e) => println(s"failed $e ")
      }
      m
    })

  */
    // connect the Source to the Sink, obtaining a RunnableGraph
    //  val runnable: RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)
    //  val (a, b) = source.toMat(sink)(Keep.both).run()

    // materialize the flow and get the value of the FoldSink
    //    val sum: Future[Int] = runnable.run()

    //    sum.onComplete {
    //        case Success(value) => println(s"$value")
    //        case Failure(e) => println(s"failed $e ")
    //    }

    //kill graph
    /*
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
    */

    //    system.scheduler.scheduleOnce(5.second, Kill(k))
    /* val in = Source(1 to 10).watchTermination()((_, d) => d)
     val out = Sink.head[Int]
     val g = RunnableGraph.fromGraph(GraphDSL.create(in) { implicit builder /*: GraphDSL.Builder[Future[Int]]*/ =>
       i =>
         import GraphDSL.Implicits._
         val in = Source(1 to 10)
         val out = Sink.head[Int]

         val bcast = builder.add(Broadcast[Int](2))
         val merge = builder.add(Merge[Int](2))

         val f1, f2, f3, f4 = Flow[Int].map(_ + 10)

         i ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
         bcast ~> f4 ~> merge

         out.mapMaterializedValue(f => f onComplete(_ => println(s"ccccccccccccccccccccccccccc")))
         ClosedShape
     })
     implicit val r: Future[Done] = g.run()
     r.onComplete {
       case Success(_) => println(s"doneeeeeeeeeeeeeeeeeee")
       case Failure(exception) => println(s"2doneeeeeeeeeeeeeeeeeee")
     }

   */

    import java.util
    import scala.jdk.CollectionConverters._
    import scala.io.{Codec, Source => ioSource}
    import com.hpe.zg.util.Utils

    /* val map = {
       val path = GraphTest.getClass.getResource("/").getPath + "graphs/scenario1.json"

       implicit val codec: Codec = Codec.UTF8

       Utils.using(ioSource.fromFile(path))(buff => {
         new JSONObject(buff.mkString).toMap
       })(e => {
         println(s"Error during create JSONObject $e")
         null
       })
     }*/
    /*  val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>

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

  */
    /*  @scala.annotation.tailrec
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
                  println(s"from broadcast out $port")
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
                  println(s"to merge in $port")
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
  })*/

    //    graph.run
    /*
      val c = Class.forName("com.hpe.zg.akka.GraphTest")
      val m = c.getMethod("test")
      m.invoke(this)

      val k = Future {
        1 / 0
      }
      k.onComplete {
        case Success(value) => println(s"kkkkkkkkkkkk $value")
        case Failure(e) => println(e)
      }

      val shapes = mutable.HashMap[String, AnyRef]()
      shapes("in") = "1"
      shapes.+=("in" -> "2")
      println(shapes)*/
    ////////////////////////////////////////////////////////////////////////////////////////////
    /* val pickMaxOfThree = GraphDSL.create() { implicit b =>
         import GraphDSL.Implicits._

         val zip1 = b.add(ZipWith[Int, Int, Int](math.max))
         val zip2 = b.add(ZipWith[Int, Int, Int](math.max))
         zip1.out ~> zip2.in0

         UniformFanInShape(zip2.out, zip1.in0, zip1.in1, zip2.in1)
     }

     val resultSink = Sink.head[Int]

     val gr = RunnableGraph.fromGraph(GraphDSL.create(resultSink) { implicit b =>
         sink =>
             import GraphDSL.Implicits._

             // importing the partial graph will return its shape (inlets & outlets)
             val pm3 = b.add(pickMaxOfThree)

             Source.single(1) ~> pm3.in(0)
             Source.single(2) ~> pm3.in(1)
             Source.single(3) ~> pm3.in(2)
             pm3.out ~> sink.in
             ClosedShape
     })

     val max: Future[Int] = gr.run()*/

    /* val Futures = mutable.ListBuffer[Future[Int]]()
     Futures += Future(1)
     Futures += Future {
         Thread.sleep(5000)
         2
     }
  isC*/


    /*  @scala.annotation.tailrec
      def isAllDone(futures: mutable.ListBuffer[Future[Done]], interval: Long): Unit = {
          if (futures.filter(_ != null).forall(_.isCompleted)) println(s"cccccccccc")
          else {
              println(s"nnnnnnnnnnnnnnnn")
              Thread.sleep(interval)
              isAllDone(futures, interval)
          }
      }

      val ff = mutable.ListBuffer[Future[Done]]()
      ff += null
      println(ff)

      def i(n: Int) = Iterator.fill[Int](n)({
          Thread.sleep(1000)
          99
      })*/

    /* val s1 = Source(1 to 15).watchTermination()((_, d) => ff += d)
     val s2 = Source.fromIterator(() => i(10)).watchTermination()((_, d) => ff += d)
     RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
         import GraphDSL.Implicits._

         val merge = b.add(Merge[Int](2))
         val sink = Sink.foreach(println)
         s1 ~> merge ~> sink
         s2 ~> merge
         ClosedShape
     }).run

     isAllDone(ff, 1000)

     val h1: String = "a"
     val h2: String = null
     val h3: String="De"
     println(s"null = ${Optional.ofNullable(h1).orElseGet(() => h3)}")*/

    val datamap = new java.util.HashMap[String, Object]()
    datamap.put("b", "b")


    var switch: Boolean = false
    var equals: Boolean = false
    val switchFlow = Flow[List[Object]].filter(l => {
        l.last match {
            case nObject: JSONObject =>
                if (nObject.isEmpty) {
                    true
                } else {
                    switch = if (!switch) equals else true
                    val params = new util.HashMap[String, Object]
                    params.put("head", l.head)
                    params.put("last", l.last)
                    val functionFactory = new MapVariableResolverFactory(params)
                    equals = MVEL.eval("head==last.get('id')", functionFactory, classOf[Boolean])
                    switch
                }
            case _ => switch
        }

    }).map(_.head)
    val latestElement = new JSONObject()
    latestElement.put("id", 2)
    val ggg = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
        import GraphDSL.Implicits._
        var i = new java.lang.Integer(0)
        val in = Source.fromIterator(() => Iterator.fill(5)({
            i = i + 1
            i
        })).watchTermination()((_, f) => f.onComplete(_ => println("done")))
        val in2 = Source.single(latestElement)

        val merge = b.add(MergeLatest[Object](2))
        val sink = Sink.foreach(println)

        in ~> merge ~> switchFlow ~> sink
        in2 ~> merge


        ClosedShape
    })
    try {
        ggg.run()
    } catch {
        case e: Throwable => println(s"11 $e")
    }

}