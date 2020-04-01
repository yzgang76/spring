package com.hpe.zg.ossm.graph


import java.util

import akka.NotUsed
import akka.stream.scaladsl.Flow
import org.json.JSONObject
import org.slf4j.LoggerFactory

trait OssmFlow extends OssmGraphNode

case class FlowMap2Json() extends OssmFlow {
    def get: Flow[Serializable, String, NotUsed] =
        Flow[Serializable].map(e => new JSONObject(e.asInstanceOf[util.Map[String, Serializable]]).toString)
}

case class FlowMap(map: util.Map[String, AnyRef]) extends OssmFlow {
    private val logger = LoggerFactory.getLogger(classOf[FlowMap])

    def get: Flow[Serializable, Serializable, NotUsed] =
        Flow[Serializable].map {
            case m: util.Map[String, Serializable] =>
                try {
                    val fun = map.get("function").asInstanceOf[util.Map[String, Serializable]]
                    fun.forEach((k, v) => {
                        m.put(k, v)
                    })
                    m
                } catch {
                    case e: Throwable => logger.error(s"error in mapping $e")
                        m
                }
            case e: Serializable => e
        }
}