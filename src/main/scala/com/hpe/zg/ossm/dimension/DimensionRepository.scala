package com.hpe.zg.ossm.dimension

import java.util.Optional

import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.hpe.zg.akka.message.OssmBeMessage
import com.hpe.zg.util.Utils

import scala.collection.mutable
import scala.io.{Codec, Source}

object DimensionRepository {

    sealed trait DimensionRepositoryMessage extends OssmBeMessage

    final case class Register(definition: String) extends DimensionRepositoryMessage

    final case class GetDimension(name: String, replyTo: ActorRef[GetDimensionResponse]) extends DimensionRepositoryMessage

    final case class GetDimensionResponse(dimension: Dimension, msg: String) extends DimensionRepositoryMessage

    private def setupDimensionRepository(repository: mutable.HashMap[String, Dimension], context: ActorContext[DimensionRepositoryMessage]): Unit = {
        val path = DimensionTest.getClass.getResource("/").getPath + "dimensions"
        implicit val codec: Codec = Codec.UTF8

        val dir = new java.io.File(path)
        for (file <- dir.listFiles.filter(f => f.getName.endsWith(".json"))) {
            Utils.using(Source.fromFile(file.getPath))(buff => {
                Dimension(buff.mkString) match {
                    case Some(d) => repository(d.name) = d
                    case None => println(s"Failed to create Dimension")
                }
            })(e => {
                context.log.error(s"Error during create JSONObject $e")
                null
            })
        }
    }

    def apply(repository: mutable.HashMap[String, Dimension]): Behavior[DimensionRepositoryMessage] = {
        Behaviors.setup { context =>
            context.log.debug(s"DimensionRepository Starting")
            setupDimensionRepository(repository, context)
            Behaviors.receiveMessage[DimensionRepositoryMessage] {
                case Register(d) =>
                    Optional.ofNullable(Dimension(d).orNull).map(dim => repository(dim.name) = dim)
                    Behaviors.same
                case GetDimension(name, replyTo) =>
                    val dim = repository.get(name).orNull
                    val msg: String = Optional.ofNullable(dim).map(_ => "Success").orElseGet(() => s"Error:Dimension $name not exists ")
                    replyTo ! GetDimensionResponse(dimension = dim, msg = msg)
                    Behaviors.same
                case msg: DimensionRepositoryMessage =>
                    context.log.warn(s"GetDimensionResponse received unhandled message $msg")
                    Behaviors.same
            }.receiveSignal {
                case (_, signal) if signal == PostStop =>
                    context.log.info(s"DimensionRepositoryMessage is stopped")
                    Behaviors.same
            }
        }
    }
}
