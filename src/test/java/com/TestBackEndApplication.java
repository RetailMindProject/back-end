package com;

import com.example.back_end.BackEndApplication;
import org.springframework.boot.SpringApplication;

public class TestBackEndApplication {

	public static void main(String[] args) {
		SpringApplication.from(BackEndApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
