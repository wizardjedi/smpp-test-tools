package com.a1systems.logwatcher;

import org.springframework.context.support.GenericXmlApplicationContext;

public class App {
    public static void main(String []args) {
        GenericXmlApplicationContext genericXmlApplicationContext = new GenericXmlApplicationContext("spring-context.xml");
    }
}
