package com.plugpass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class PlugPassApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(PlugPassApplication.class, args);
		if (context.getEnvironment().getProperty("plugpass.ingestion.command.enabled", Boolean.class, false)) {
			System.exit(SpringApplication.exit(context));
		}
	}

}
