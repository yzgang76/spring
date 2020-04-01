package com.hpe.zg.ossm.graph

import java.util

import akka.Done
import akka.stream.scaladsl.Sink

import scala.concurrent.Future

trait OssmSink extends OssmGraphNode

case class SinkLast(map: util.Map[String, AnyRef]) extends OssmSink {
    def get: Sink[Serializable, Future[Serializable]] = Sink.last
}

case class SinkPrint(map: util.Map[String, AnyRef]) extends OssmSink {
    def get: Sink[Serializable, Future[Done]] = Sink.foreach(println)
}

