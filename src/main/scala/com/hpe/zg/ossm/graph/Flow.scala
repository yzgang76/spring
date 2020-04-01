package com.hpe.zg.ossm.graph


import java.util

import akka.NotUsed
import akka.stream.scaladsl.Flow
import org.json.JSONObject

trait OssmFlow extends OssmGraphNode

case class FlowMap2Json() extends OssmFlow {
    def get: Flow[Serializable, String, NotUsed] =
        Flow[Serializable].map(e => new JSONObject(e.asInstanceOf[util.Map[String, Serializable]]).toString)
}

case class FlowMap(map: util.Map[String, AnyRef]) extends OssmFlow {
    def get: Flow[Serializable, Serializable, NotUsed] =
        Flow[Serializable].map {
            case map: util.Map[String, Serializable] =>
                val fun: util.Map[String, AnyRef] = new JSONObject(map.get("function").toString).toMap
                fun.forEach((k, v) => {
                    map.putIfAbsent(k, v.asInstanceOf[Serializable])
                })
                map
            case e => e
        }
}