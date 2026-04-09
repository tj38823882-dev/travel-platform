package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // 啟用非同步執行
public class Springbootdemo1Application {

	public static void main(String[] args) {
		SpringApplication.run(Springbootdemo1Application.class, args);
	}

}
