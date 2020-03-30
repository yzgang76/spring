package com.hpe.zg.ossm.node

import java.util

import org.slf4j.LoggerFactory

object GraphNode {
    private val logger = LoggerFactory.getLogger(classOf[GraphNode])

    object TYPES {
        val TYPE_SOURCE = "source"
        val TYPE_SINK = "sink"
        val TYPE_BROADCAST = "broadcast"
        val TYPE_BALANCE = "balance"
        //TODO: more
    }


    def apply(map: util.Map[String, AnyRef]): GraphNode = {
        logger.debug(s"Create Graph Node $map")
        new GraphNode(map.get("type").toString)
    }
}

class GraphNode(nodeType: String) {
    private val logger = LoggerFactory.getLogger(classOf[GraphNode])

    nodeType match {
        case GraphNode.TYPES.TYPE_SOURCE =>
        case GraphNode.TYPES.TYPE_SINK =>
        case _ => logger.error(s"Not support the type $nodeType")
    }
}
