package com.hpe.zg.ossm.graph

import java.util

import akka.NotUsed
import akka.stream.{FanOutShape, UniformFanInShape, UniformFanOutShape}
import akka.stream.scaladsl._
import akka.stream.stage.GraphStage

trait OssmShape extends OssmGraphNode

case class ShapeBroadcast(map: util.Map[String, AnyRef]) extends OssmShape {
    println(s"bbbbbbbbbb $map")
    def get: GraphStage[UniformFanOutShape[Serializable, Serializable]] = Broadcast[Serializable](2)
}

case class ShapeMerge(map: util.Map[String, AnyRef]) extends OssmShape {
    def get: GraphStage[MergePreferred.MergePreferredShape[Serializable]] = MergePreferred[Serializable](map.get("in").asInstanceOf[Int] - 1)
}