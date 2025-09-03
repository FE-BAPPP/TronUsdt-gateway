package com.UsdtWallet.UsdtWallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks
@EnableAsync       // Enable async processing
public class UsdtWalletApplication {

	public static void main(String[] args) {
		SpringApplication.run(UsdtWalletApplication.class, args);
	}

}
