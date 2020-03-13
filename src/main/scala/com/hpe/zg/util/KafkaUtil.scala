package com.hpe.zg.util

import java.time.Duration
import java.util
import java.util.Properties

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.admin.{AdminClient, CreateTopicsResult, DeleteTopicsResult, KafkaAdminClient, NewTopic, TopicListing}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.TopicPartition
import org.slf4j.{Logger, LoggerFactory}

object KafkaUtil {
    val logger: Logger = LoggerFactory.getLogger(KafkaUtil.getClass)
    logger.info(s"Init KafkaUtil")

    val bootstrapServer = "localhost:9092"


    private def createCommonProperties: Properties = {
        val props = new Properties()
        props.put("bootstrap.servers", bootstrapServer)
        props.put("retries", 0.asInstanceOf[Object])
        props
    }

    private def createProperties(config: Config, group: String, isConsumer: Boolean): Properties = {
        val props = createCommonProperties
        props.put("batch.size", 16384)
        props.put("buffer.memory", 33554432)
        if (isConsumer) {
            props.put("group.id", group)
            props.put("enable.auto.commit", "true")
            props.put("auto.commit.interval.ms", "1000")
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest") //for test
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        } else {
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        }
        props
    }

    def getAdmin: AdminClient = {
        val props = createCommonProperties
        //        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        //        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
        AdminClient.create(props) //need to be closed by caller
    }

    def createTopic(admin: AdminClient, topic: String, numPartitions: Int, replicationFactor: Short): CreateTopicsResult = {
        logger.debug(s"create new topic $topic, $numPartitions, $replicationFactor")
        admin.createTopics(util.Arrays.asList(new NewTopic(topic, numPartitions, replicationFactor)))
    }

    def createTopic(topic: String, numPartitions: Int, replicationFactor: Short): CreateTopicsResult = {
        logger.debug(s"create new topic $topic, $numPartitions, $replicationFactor")
        val create: AdminClient = getAdmin
        val result = create.createTopics(util.Arrays.asList(new NewTopic(topic, numPartitions, replicationFactor)))
        create.close()
        result
    }

    /*  def deleteTopic(admin: AdminClient, topic: String): DeleteTopicsResult = {
          admin.deleteTopics(util.Arrays.asList(topic))
      }

      def deleteTopic(topic: String): DeleteTopicsResult = {
          val delete: AdminClient = getAdmin
          val result = delete.deleteTopics(util.Arrays.asList(topic))
          delete.close()
          result
      }*/

    def getTopics(admin: AdminClient): util.Collection[TopicListing] = admin.listTopics().listings().get()


    def listTopic(): Unit = {
        val admin: AdminClient = getAdmin
        admin.listTopics().listings().get().forEach(println)
        admin.close()
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
        val admin = getAdmin
        //        createTopic(admin, "p1", 2, 1)
        getTopics(admin).forEach(println)
        admin.close()

        val conf = ConfigFactory.load("management")
        val consumer = createConsumer(conf, "group13332")
        val producer = createProcedure(conf)

        val topic = "mytest"
        val topic2 = "p1"
        val key1 = "key1"
        val key2 = "key2"

//                consumer.subscribe(util.Arrays.asList(topic))

        //        for (c <- 11 to 15) producer.send(new ProducerRecord[String, java.io.Serializable](topic, key1, c.toString))
        //        for (c <- 15 to 20) producer.send(new ProducerRecord[String, java.io.Serializable](topic, key2, c.toString))

//        for (c <- 1 to 5) {
//            val v = c * 2 + 30
//            producer.send(new ProducerRecord[String, java.io.Serializable](topic, key1, v.toString))
//            producer.send(new ProducerRecord[String, java.io.Serializable](topic, key2, (v+1).toString))
//        }
        import scala.jdk.CollectionConverters._

        val m = consumer.partitionsFor(topic).asScala
        println(s"********************  partitions")
        m.foreach(println)
        val m1 = consumer.partitionsFor(topic2).asScala
        println(s"********************  partitions")
        m1.foreach(println)
        //

        val p0 = new TopicPartition(topic, 0)

        val p20 = new TopicPartition(topic2, 0)
        val p21 = new TopicPartition(topic2, 1)

        consumer.assign(util.Arrays.asList(p0, p20,p21))

        consumer.seek(p0, 10)
        consumer.seek(p20, 74)
        consumer.seek(p21, 95)



        println(s"data")
        while (true) {
            val records = consumer.poll(Duration.ofMillis(100)).asScala
            for (record <- records) {
                println(s"[${record.topic()}] ${record.partition}-${record.offset()} : ${record.key}->${record.value}")
            }
            Thread.sleep(1000)
        }

    }
}
