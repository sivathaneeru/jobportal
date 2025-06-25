package com.govjobtrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing // To enable automatic population of fields like createdDate, lastModifiedDate
public class GovjobtrackApplication {

	public static void main(String[] args) {
		SpringApplication.run(GovjobtrackApplication.class, args);
	}

}
