package com.sapwbd.kenbot;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.rest.RestClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@SpringBootApplication
public class KenBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(KenBotApplication.class, args);
    }

    @Bean
    public GatewayDiscordClient discordClient() {
        return DiscordClientBuilder.create(System.getenv("BOT_TOKEN")).build().gateway().login().block();
    }

    @Bean
    public RestClient restClient(GatewayDiscordClient discordClient) {
        return discordClient.getRestClient();
    }

    @Bean
    public PathMatchingResourcePatternResolver resourceResolver() {
        return new PathMatchingResourcePatternResolver();
    }
}
