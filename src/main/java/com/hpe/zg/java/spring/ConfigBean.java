package com.hpe.zg.java.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "my")
public class ConfigBean {
    private String name;
    private int age;
    @Override public String toString(){
        return "name=" + name + "; age=" + age;
    }
}
