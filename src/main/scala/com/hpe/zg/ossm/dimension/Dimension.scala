package com.hpe.zg.ossm.dimension

import java.util
import com.hpe.zg.util.Utils
import org.json.{JSONException, JSONObject}
import org.slf4j.LoggerFactory
import scala.io.{Codec, Source}

case class DimensionData(name: String,
                         ts: Long,
                         data: util.HashMap[String, java.io.Serializable]) extends java.io.Serializable {

    override def toString: String = {
        val json = new JSONObject()
        json.put("name", name)
        json.put("ts", ts)
        json.put("data", new JSONObject(data))
        json.toString
    }
}

object Dimension {
    private val logger = LoggerFactory.getLogger(classOf[Dimension])

    def apply(definition: String): Option[Dimension] = {
        try {
            Some(new Dimension(new JSONObject(definition).toMap))
        } catch {
            case e: JSONException => logger.error(s"Failed to create Dimension.\n$e\n$definition")
                None
        }
    }
}

class Dimension(map: util.Map[java.lang.String, AnyRef]) {
    private val logger = LoggerFactory.getLogger(classOf[Dimension])

    import scala.jdk.CollectionConverters._

    val name: String = map.get("name").toString
    val version: String = map.get("version").toString
    val fields: List[Field] = {
        map.get("fields") match {
            case list: util.ArrayList[AnyRef] => list.asScala
                .collect({
                    case m: util.HashMap[String, java.io.Serializable] => Field.fromMap(m).orNull
                    case o: Any =>
                        logger.warn(s"Invalid definition of field $o")
                        null
                })
                .filter(f => f != null)
                .toList
            case _ => List.empty
        }
    }

    def generateValue(): DimensionData = {
        val map = new util.HashMap[String, java.io.Serializable]()
        fields.foreach(f => map.put(f.name, f.createValue()))
        DimensionData(name, System.currentTimeMillis(), map)
    }

    //    def formatPrint(): Unit = formatPrint(map, 0)

    /* private def formatPrint(ob: AnyRef, intend: Int): Unit =
         ob match {
             case list: util.ArrayList[AnyRef] =>
                 for (_ <- 1 to intend) print("\t")
                 println(s"[")
                 list.asScala.foreach(formatPrint(_, intend + 1))
                 for (_ <- 1 to intend) print("\t")
                 println(s"]")
             case map1: util.HashMap[String, AnyRef] =>
                 for (_ <- 1 to intend) print("\t")
                 println(s"{")
                 map1.asScala.foreach(formatPrint(_, intend + 1))
                 for (_ <- 1 to intend) print("\t")
                 println(s"}")
             case e: (String, AnyRef) =>
                 for (_ <- 1 to intend) print("\t")
                 print(s"${e._1} : ")
                 formatPrint(e._2, intend)
             case _ =>
                 println(s"$ob")
         }*/

    override def toString: String =
        s"name= $name\nField:\n" + (for (field <- fields) yield field.toString).mkString("\n")
}

object DimensionTest extends App {
    private val logger = LoggerFactory.getLogger(DimensionTest.getClass)
    val path = DimensionTest.getClass.getResource("/").getPath + "dimensions"

    implicit val codec: Codec = Codec.UTF8

    val dir = new java.io.File(path)
    for (file <- dir.listFiles.filter(f => f.getName.endsWith(".json"))) {
        Utils.using(Source.fromFile(file.getPath))(buff => {
            Dimension(buff.mkString) match {
                case Some(v) =>
                    println(v)
                    println(v.generateValue())
                case None => println(s"Failed to create Dimension")
            }
        })(e => {
            logger.error(s"Error during create JSONObject $e")
            null
        })
    }



}
