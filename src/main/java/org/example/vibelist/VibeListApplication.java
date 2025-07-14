package org.example.vibelist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@Slf4j
public class VibeListApplication {

    public static void main(String[] args) {
        log.info("🔥 Application is starting...");
        SpringApplication.run(VibeListApplication.class, args);
    }

}
