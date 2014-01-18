package com.a1systems.http.adapter;

import org.springframework.context.support.GenericXmlApplicationContext;

/**
 * Hello world!
 *
 */
public class App
{
    public static void main( String[] args )
    {
        GenericXmlApplicationContext genericXmlApplicationContext = new GenericXmlApplicationContext("spring-context.xml");
    }
}
