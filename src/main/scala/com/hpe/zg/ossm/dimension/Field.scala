package com.hpe.zg.ossm.dimension

import java.text.SimpleDateFormat

import org.json.JSONObject
import org.slf4j.LoggerFactory

object Field {
    private val logger = LoggerFactory.getLogger(classOf[Field])

    def fromMap(map: java.util.HashMap[String, java.io.Serializable]): Option[Field] = {
        try {
            Some(Field(
                name = map.get("name").toString,
                `type` = map.get("type").toString,
                unique = map.getOrDefault("unique", false).asInstanceOf[Boolean],
                enum = try {
                    val s = map.get("values").toString
                    Some(s.split(","))
                } catch {
                    case _: Throwable => None
                },
                format = try Some(map.get("format").toString) catch {
                    case _: Throwable => None
                })
            )
        } catch {
            case e: Throwable =>
                logger.error(s"Wrong in field definition $map, $e")
                None
        }
    }

   /* def fromJson(str: String): Option[Field] = {
        try {
            val ob = new JSONObject(str)
            Some(Field(
                name = ob.getString("name"),
                `type` = ob.getString("type"),
                unique = try ob.getBoolean("unique") catch {
                    case _: Throwable => false
                },
                enum = try {
                    val s = ob.getString("enum")
                    Some(s.split(","))
                } catch {
                    case _: Throwable => None
                },
                format = try Some(ob.getString("format")) catch {
                    case _: Throwable => None
                })
            )
        } catch {
            case e: Throwable =>
                logger.error(s"Wrong in field definition $str, $e")
                None
        }
    }*/
}

case class Field(name: String, `type`: String, unique: Boolean, enum: Option[Array[String]], format: Option[String]) {
    private val logger = LoggerFactory.getLogger(classOf[Field])

    private def randomValue[T](list: Array[T]): T = list((math.random() * list.length).round.toInt)

    def createValue(): java.io.Serializable = {
        `type` match {
            case "string" =>
                if (unique)  name + System.currentTimeMillis
                else if (enum.isDefined) randomValue(enum.get).trim
                else  name
            case "long" =>  long2Long(System.currentTimeMillis)
            case "int" => int2Integer((math.random * 100.0).round.toInt)
            case "double" =>  double2Double(math.random * 1000.0)
            case "float" =>float2Float((math.random * 1000.0).toFloat)
            case "date" =>
                val d: SimpleDateFormat = try {
                    new SimpleDateFormat(format.getOrElse("yyyy-MM-dd HH:mm:ss"))
                } catch {
                    case e: IllegalArgumentException =>
                        logger.error(s"Wrong format ${format.get} in field $name. $e\n use default 'yyyy-MM-dd HH:mm:ss'")
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                }
                d.format(System.currentTimeMillis)
            case "boolean" => boolean2Boolean(math.random > 0.5)
            case _ =>throw new IllegalArgumentException(s"not support the type ${`type`}")
        }
    }

    override def toString: String = s"name = $name, type = ${`type`}"
}

