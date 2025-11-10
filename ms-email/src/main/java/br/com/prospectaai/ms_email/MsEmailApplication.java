package br.com.prospectaai.ms_email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsEmailApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsEmailApplication.class, args);
    }
}
