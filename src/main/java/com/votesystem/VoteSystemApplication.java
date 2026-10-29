package com.votesystem;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class VoteSystemApplication {

    @PostConstruct
    public void init() {
        // Set server timezone to Indian Standard Time (IST) so all schedules,
        // election closing times, and audit timestamps match local voter/admin clocks
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        SpringApplication.run(VoteSystemApplication.class, args);
    }
}
