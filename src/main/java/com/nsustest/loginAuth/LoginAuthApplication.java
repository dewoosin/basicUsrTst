package com.nsustest.loginAuth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 로그인 인증 애플리케이션 메인 클래스
 * 
 * @author nsustest
 */
@SpringBootApplication
@MapperScan("com.nsustest.loginAuth.dao")
@EnableScheduling
public class LoginAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoginAuthApplication.class, args);

		System.out.println("start test");
	}

}
