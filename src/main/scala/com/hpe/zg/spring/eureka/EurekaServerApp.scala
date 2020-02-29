package com.hpe.zg.spring.eureka

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
//import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer

@SpringBootApplication
//@EnableEurekaServer
class EurekaServerApp {
    println(s"eeeeeeeeeeee")
}


object EurekaServerApp {
    def main(args: Array[String]): Unit= {
        SpringApplication.run(classOf[EurekaServerApp])
    }

}
