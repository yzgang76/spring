package com.hpe.zg.akka.system

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{Behavior, PostStop}
import akka.util.Timeout
import com.hpe.zg.akka.message.OssmBeMessage
import com.hpe.zg.ossm.dimension.DimensionRepository

import scala.collection.mutable

object AkkaSystem {

    def apply(): Behavior[OssmBeMessage] = Behaviors.setup { context =>
        context.log.info(s"AkkaSystem starting")

        //setup Dimension repository
        val dimensionRepository = context.spawn(DimensionRepository(mutable.HashMap.empty), "dimensionRepository")

        import DimensionRepository._
        Behaviors.receiveMessage[OssmBeMessage] {
            case r@Register(_) =>
                dimensionRepository ! r
                Behaviors.same
            case r@GetDimension(_, _) =>
                dimensionRepository ! r
                Behaviors.same
            case _ => Behaviors.same
        }.receiveSignal({
            case (_, signal) if signal == PostStop =>
                context.log.info(s"AkkaSystem is stopped")
                Behaviors.same
        })
    }
}

object AkkaSystemTest extends App {

    import akka.actor.typed._
    import scala.concurrent.duration._
    import com.typesafe.config.ConfigFactory
    import DimensionRepository._
    import akka.actor.typed.scaladsl.AskPattern._
    import scala.concurrent.{ExecutionContextExecutor, Future}
    import scala.util.{Failure, Success}

    implicit val system: ActorSystem[OssmBeMessage] = ActorSystem(AkkaSystem(), "akkaSystem", ConfigFactory.load("clustermanager"))
    implicit val timeout: Timeout = 60.seconds
    implicit val ec: ExecutionContextExecutor = system.executionContext

    val f: Future[GetDimensionResponse] = system.ask[GetDimensionResponse](ref => {
        GetDimension(
            name = "temip_raw",
            replyTo = ref
        )
    })(timeout = timeout, scheduler = system.scheduler)

    f.onComplete {
        case Success(r) => println(s"_________________\n${r.dimension}\n${r.dimension.generateValue()}")
        case Failure(e) => e.printStackTrace()
    }
}

