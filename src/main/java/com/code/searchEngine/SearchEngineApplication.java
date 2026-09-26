package com.code.searchEngine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SearchEngineApplication {

	public static void main(String[] args) {
		ApplicationContext context =  SpringApplication.run(SearchEngineApplication.class, args);

	}

}
