package com.hpe.zg.ossm.graph

import java.util

import akka.NotUsed
import akka.stream.UniformFanOutShape
import akka.stream.scaladsl._

trait OssmShape extends OssmGraphNode

case class ShapeBroadcast(map: util.Map[String, AnyRef], builder: GraphDSL.Builder[NotUsed]) extends OssmShape {
    def get: UniformFanOutShape[Serializable, Serializable] = builder.add(Broadcast[Serializable](map.get("out").asInstanceOf[Int]))
}

case class ShapeMerge(map: util.Map[String, AnyRef], builder: GraphDSL.Builder[NotUsed]) extends OssmShape {
    def get: MergePreferred.MergePreferredShape[Serializable] = builder.add(MergePreferred[Serializable](map.get("in").asInstanceOf[Int] - 1))
}