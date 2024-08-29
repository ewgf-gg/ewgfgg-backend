package org.tekkenstats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@SpringBootApplication
public class Main {

    @Autowired
    private FighterService fighterService;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            System.out.println("Fetching fighters from database...");
            Map<String, String> fighters = fighterService.getAllFighters();
            if (fighters != null && !fighters.isEmpty()) {
                System.out.println("Fighters found:");
                fighters.forEach((key, value) -> System.out.println(key + ": " + value));
            } else {
                System.out.println("No fighters found in the database.");
            }
        };
    }
}
