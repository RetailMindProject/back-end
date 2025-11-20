package com.example.back_end;

import org.springframework.boot.SpringApplication;
import org.testcontainers.utility.TestcontainersConfiguration;

public class TestBackEndApplication {

	public static void main(String[] args) {
		SpringApplication.from(BackEndApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
