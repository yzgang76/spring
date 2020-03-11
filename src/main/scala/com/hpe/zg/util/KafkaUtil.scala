package com.hpe.zg.util

import java.time.Duration
import java.util
import java.util.Properties

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.TopicPartition
import org.slf4j.{Logger, LoggerFactory}

object KafkaUtil {
    val logger: Logger = LoggerFactory.getLogger(KafkaUtil.getClass)
    logger.info(s"Init KafkaUtil")
    private def createProperties(config: Config, group: String, isConsumer: Boolean): Properties = {
        val props = new Properties()
        props.put("bootstrap.servers", "localhost:9092" /*config.getString("kafka.bootstrap-server") + ":" + config.getInt("kafka.port")*/)
        props.put("retries", 0.asInstanceOf[Object])
        props.put("batch.size", 16384.asInstanceOf[Object])
        props.put("buffer.memory", 33554432.asInstanceOf[Object])
        //        props.put("linger.ms", 1)
        if (isConsumer) {
            props.put("group.id", "OSSM_BE_CLUSTER" + group)
            props.put("enable.auto.commit", "true")
            props.put("auto.commit.interval.ms", "1000")
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest") //for test
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        } else {
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        }
        props
    }

    def createProcedure(conf: Config): KafkaProducer[String, java.io.Serializable] = new KafkaProducer[String, java.io.Serializable](createProperties(conf, null, isConsumer = false))

    def createConsumer(conf: Config, group: String): KafkaConsumer[String, java.io.Serializable] = new KafkaConsumer[String, java.io.Serializable](createProperties(conf, group, isConsumer = true))


    /*  import akka.kafka.scaladsl.Consumer
        import akka.kafka.{ConsumerSettings, Subscriptions}
        import akka.stream.scaladsl.{RunnableGraph, Sink}
        import org.apache.kafka.common.serialization.StringDeserializer
        import org.apache.kafka.clients.consumer.ConsumerRecord
        import org.apache.kafka.clients.producer.ProducerRecord

        def createAkkaConsumerWithHistoryData(conf: Config, group: String, topic: String, callback: ConsumerRecord[String, String] => Unit): RunnableGraph[Consumer.Control] =
           _createAkkaConsumer(conf, group, topic, fetchHistory = true, callback)

       def createAkkaConsumer(conf: Config, group: String, topic: String, callback: ConsumerRecord[String, String] => Unit): RunnableGraph[Consumer.Control] =
           _createAkkaConsumer(conf, group, topic, fetchHistory = false, callback)

       private def _createAkkaConsumer(conf: Config, group: String, topic: String, fetchHistory: Boolean, callback: ConsumerRecord[String, String] => Unit): RunnableGraph[Consumer.Control] = {
           val consumerSettings =
               ConsumerSettings(conf.getConfig("akka.kafka.consumer"), new StringDeserializer, new StringDeserializer).withBootstrapServers(conf.getString("kafka.bootstrap-server") + ":" + conf.getInt("kafka.port"))
                   .withGroupId(group)
                   .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, if (fetchHistory) "earliest" else "latest")
                   .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
                   .withProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "5000")
           Consumer
               .plainSource(
                   consumerSettings,
                   if (fetchHistory) Subscriptions.assignmentWithOffset(new TopicPartition(topic, 0), 0)
                   else Subscriptions.topics(topic)
               )
               .to(Sink.foreach(msg => callback(msg))).async
       }
    */
    def main(args: Array[String]): Unit = {
        val conf = ConfigFactory.load("management")
        val consumer = createConsumer(conf, "g3")
        val producer = createProcedure(conf)

        val topic = "OSSM_BE_METRICS"
//        val key_record = conf.getString("ossm.monitor.keys.record")

        //        consumer.subscribe(util.Arrays.asList(topic))

        //        for (c <- 1 to 10) producer.send(new ProducerRecord[String, java.io.Serializable](topic, key_record, c.toString))
        import scala.jdk.CollectionConverters._

        val m = consumer.partitionsFor(topic).asScala
        m.foreach(println)

        val p = new TopicPartition(topic, 0)
        consumer.assign(util.Arrays.asList(p))
        consumer.seek(p, 0)


        while (true) {
            val records = consumer.poll(Duration.ofMillis(100)).asScala
            for (record <- records) {
                println(s"${record.partition}-${record.offset()} : ${record.key}->${record.value}")
            }
            Thread.sleep(1000)
        }

    }
}
