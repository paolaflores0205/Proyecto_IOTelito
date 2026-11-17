package com.iotelito.taxiweb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class TaxiwebApplication {

	public static void main(String[] args) {
		// Las fechas se guardan en hora de Lima aunque el servidor (EC2) esté en UTC.
		TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
		SpringApplication.run(TaxiwebApplication.class, args);
	}
}
