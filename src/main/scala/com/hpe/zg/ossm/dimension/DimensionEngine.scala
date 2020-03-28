package com.hpe.zg.ossm.dimension

import java.text.SimpleDateFormat
import java.util

import com.hpe.zg.util.Utils
import org.json.JSONObject
import org.slf4j.{Logger, LoggerFactory}

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

case class Field(name: String, `type`: String, unique: Boolean, enum: Option[util.ArrayList[AnyRef]], format: Option[String]) {
    val logger: Logger = LoggerFactory.getLogger(classOf[Field])

    def randomValue[T](list: util.ArrayList[T]): T = list.get((math.random() * list.size).round.toInt)


    def createValue(): (String, AnyRef) = {
        `type` match {
            case "String" =>
                if (unique) (name, name + System.currentTimeMillis)
                else if (enum.isDefined) (name, randomValue(enum.get))
                else (name, name)
            case "long" => (name, long2Long(System.currentTimeMillis))
            case "int" => (name, int2Integer((math.random * 100.0).round.toInt))
            case "double" => (name, double2Double(math.random * 1000.0))
            case "float" => (name, float2Float((math.random * 1000.0).toFloat))
            case "date" =>
                val d: SimpleDateFormat = try {
                    new SimpleDateFormat(format.getOrElse("yyyy-MM-dd HH:mm:ss"))
                } catch {
                    case e: IllegalArgumentException =>
                        logger.error(s"Wrong format ${format.get} in field $name. $e\n use default 'yyyy-MM-dd HH:mm:ss'")
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                }
                (name, d.format(System.currentTimeMillis))
            case "boolean" => (name, boolean2Boolean(math.random > 0.5))
        }
    }
}

object Dimension {
    def fromJsonObject(obj: JSONObject): Dimension = {
        new Dimension(obj.toMap)
    }

    def simData(): util.Map[java.lang.String, AnyRef] = {
        null
    }
}

class Dimension(map: util.Map[java.lang.String, AnyRef]) {

    import scala.jdk.CollectionConverters._

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
}

object Test extends App {
    val dd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    println(dd.format(System.currentTimeMillis))

    val path = Test.getClass.getResource("/").toString + "graph.json"
    val path2 = """C:\mycode\spring\target\classes\graph.json"""
    val d = DimensionFactory.createDimension(path2)
    d match {
        case Some(v) => v.formatPrint()
        case None => println(s"Failed to create Dimension")
    }
}
