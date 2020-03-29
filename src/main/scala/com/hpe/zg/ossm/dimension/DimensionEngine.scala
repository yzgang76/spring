package com.hpe.zg.ossm.dimension

import java.util

import com.hpe.zg.util.Utils
import org.json.JSONObject
import org.slf4j.LoggerFactory

import scala.io.{Codec, Source}

object DimensionFactory {
    private val logger = LoggerFactory.getLogger(DimensionFactory.getClass)

    def createDimension(definition: String): Option[Dimension] = {
        implicit val codec: Codec = Codec.UTF8
        val jsonObj = Utils.using(Source.fromFile(definition))(buff => {
            new JSONObject(buff.mkString)
        })(e => {
            logger.error(s"Error during create Dimension $e")
            null
        })

        if (null != jsonObj) {
            Some(Dimension.fromJsonObject(jsonObj))
        } else None
    }
}

case class DimensionData(name: String,
                         ts: Long,
                         data: util.HashMap[String, java.io.Serializable]) extends java.io.Serializable {
}

object Dimension {
    def fromJsonObject(obj: JSONObject): Dimension = {
        new Dimension(obj.toMap)
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

    def formatPrint(): Unit = formatPrint(map, 0)

    private def formatPrint(ob: AnyRef, intend: Int): Unit =
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
        }

    override def toString: String =
        s"name= $name\nField:\n" + (for (field <- fields) yield field.toString).mkString("\n")
}

object Test extends App {
    val path = Test.getClass.getResource("/").toString + "graph.json"
    val path2 = """C:\mycode\spring\target\classes\temip_raw.json"""
    val d = DimensionFactory.createDimension(path2)
    d match {
        case Some(v) =>
            println(v)
            println(v.generateValue())
        case None => println(s"Failed to create Dimension")
    }
}
