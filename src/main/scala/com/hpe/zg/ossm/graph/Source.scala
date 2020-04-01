package com.hpe.zg.ossm.graph

import java.util

import akka.NotUsed
import akka.stream.{DelayOverflowStrategy, Graph, Shape, SourceShape}
import akka.stream.scaladsl.Source
import com.hpe.zg.ossm.dimension.Dimension
import com.hpe.zg.util.Utils

import scala.concurrent.duration._
import scala.io.{Codec, Source => ioSource}

trait OssmSource extends OssmGraphNode

case class SourceSimulator(map: util.Map[String, AnyRef]) extends OssmSource {
    val dimension: Option[Dimension] = {
        val path = Graph.getClass.getResource("/").getPath + s"dimensions/${map.get("dimension")}.json"
        implicit val codec: Codec = Codec.UTF8
        Utils.using(ioSource.fromFile(path))(buff => Dimension(buff.mkString)
        )(e => throw e)
    }
    if (dimension.isEmpty) throw new ExceptionInInitializerError(s"Failed to create dimension ${map.get("dimension")}")
    val interval: Int = map.get("interval").asInstanceOf[Int]

    def get: Source[util.HashMap[String, Serializable], NotUsed] = {
        if (interval < 0) Source.single(dimension.get.generateValue().data)
        else Source.fromIterator(() => Iterator.continually(dimension.get.generateValue().data)).delay(interval.millis, DelayOverflowStrategy.dropHead)
    }


}
