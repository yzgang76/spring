package com.hpe.zg.akka

import akka.{Done, NotUsed}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.{DelayOverflowStrategy, KillSwitches, Materializer}
import akka.stream.scaladsl._

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
    println(s"aaaaaaaaaaaaa $a")

    // materialize the flow and get the value of the FoldSink
    val sum: Future[Int] = runnable.run()

    //    sum.onComplete {
    //        case Success(value) => println(s"$value")
    //        case Failure(e) => println(s"failed $e ")
    //    }

    //kill graph
    val sink2: Sink[Int, Future[Done]] = Sink.foreach(println)
    val source2 = Source.
        fromIterator(() => Iterator.continually(Random.nextInt(100))).delay(500.millis, DelayOverflowStrategy.dropHead)
    val (k, d) = source2.map(0 - _).viaMat(KillSwitches.single)(Keep.right).toMat(sink2)(Keep.both).run()

    import akka.stream.KillSwitch

    case class Kill(k: KillSwitch) extends Runnable {
        override def run(): Unit = {
            k.shutdown()
            //            system.terminate()
        }
    }

    system.scheduler.scheduleOnce(5.second, Kill(k))


}
