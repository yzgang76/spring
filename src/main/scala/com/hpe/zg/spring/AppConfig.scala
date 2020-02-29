package com.hpe.zg.spring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.PropertySource

@PropertySource(value = Array{"classpath:myservice.yml"},ignoreResourceNotFound = false, encoding = "UTF-8", name = "myservice.yml")
@SpringBootApplication
class AppConfig {
    println(s"000000000000000000000000")
}

object myApplication {
    def main(args: Array[String]): Unit= {
        new SpringApplicationBuilder(classOf[AppConfig]).run(args:_*)
    }
}

import com.alibaba.fastjson.JSONObject
import org.springframework.web.bind.annotation.{RequestMapping, RestController}

@RestController
class indexController {
    println(s"11111111111111111")
    @RequestMapping(value = Array("/index"))
    def index(): JSONObject = {
        val json = new JSONObject
        json.put("code", 0)
        json.put("data", "success")
        json
    }
}

@RestController
class indexController2 {
    println(s"111111111111111112")
    @RequestMapping(value = Array("/index2"))
    def index(): JSONObject = {
        val json = new JSONObject
        json.put("code", 0)
        json.put("data", "failed")
        json
    }
}