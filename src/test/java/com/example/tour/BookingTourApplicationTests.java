package com.example.tour;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest
class BookingTourApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void generate() {
		System.out.println(new BCryptPasswordEncoder().encode("123456"));
	}

}
