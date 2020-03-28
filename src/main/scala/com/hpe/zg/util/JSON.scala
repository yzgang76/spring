package com.hpe.zg.util

import org.json.{JSONArray, JSONException, JSONObject}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.Iterable
import scala.io.{Codec, Source}

object JSON {
    val logger: Logger = LoggerFactory.getLogger(JSON.getClass)

    def isJsonString(str: String): Byte = {
        str match {
            case null => -1
            case "" => -1
            case s: String if s.startsWith("{") && s.endsWith("}") => 1
            case s: String if s.startsWith("[") && s.endsWith("]") => 2
            case _ => -1
        }
    }

    def parse(str: String): Option[AnyRef] = {
        try {
            isJsonString(str) match {
                case 1 => parseObject(str)
                case 2 => parseArray(str)
                case -1 =>
                    logger.warn(s"Not a json string")
                    None
            }
        } catch {
            case e: JSONException => logger.error(s"Failed to parse the string \n$str\n $e")
                None
        }
    }

    def parseObject(str: String): Option[mutable.Map[String, AnyRef]] = {
        import scala.jdk.CollectionConverters._
        try {
            logger.info(s"parse json object\n $str")
            val obj = new JSONObject(str).toMap.asScala
//            val map = mutable.HashMap[String, AnyRef]()
//            obj.foreach(entity => {
//                println(s"entity ${entity._1} ${entity._2.getClass}")
//                isJsonString(entity._2.toString) match {
//                    case 1 => map.+=(entity._1 -> parseObject(entity._2.toString).getOrElse(entity._2))
//                    case 2 => map.+=(entity._1 -> parseArray(entity._2.toString).getOrElse(entity._2))
//                    case -1 => map.+=(entity._1 -> entity._2)
//                }
//            })
            Some(obj)
        } catch {
            case e: Throwable =>
                logger.error(s"Failed to parse the string \n$str\n$e")
                None
        }
    }

    def parseArray(str: String): Option[mutable.Buffer[AnyRef]] = {
        import scala.jdk.CollectionConverters._
        try {
            logger.info(s"parse json array\n $str")
            val obj = new JSONArray(str).toList.asScala
//            val list = mutable.ListBuffer[AnyRef]()
//            obj.foreach(entity => {
//                isJsonString(entity.toString) match {
//                    case 1 => list.+=(parseObject(entity.toString).getOrElse(entity))
//                    case 2 => list.+=(parseArray(entity.toString).getOrElse(entity))
//                    case -1 => list.+=(entity)
//                }
//            })
            Some(obj)
        } catch {
            case e: Throwable =>
                logger.error(s"Failed to parse the string $e")
                None
        }
    }
}

object JsonTest extends App {
    val logger: Logger = LoggerFactory.getLogger(JsonTest.getClass)
    val path = JsonTest.getClass.getResource("/").toString + "graph.json"
    val path2 = """C:\mycode\spring\target\classes\graph.json"""
    implicit val codec: Codec = Codec.UTF8
    val res = Utils.using(Source.fromFile(path2))(buff => {
        JSON.parse(buff.mkString)
    })(e => {
        logger.error(s"Error during create Dimension $e")
        None
    }).orNull

    res match {
        case mutable.HashMap => println(s"MMMMMMMM")
        case mutable.Buffer => println(s"AAAAAAAAAA")
        case _ => println(s"eeeeee ${res.getClass}")
    }

}
