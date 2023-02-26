package com.sapwbd.kenbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class GlobalCommandRegistrar implements ApplicationRunner {

    final RestClient restClient;

    final PathMatchingResourcePatternResolver resourceResolver;


    public GlobalCommandRegistrar(RestClient restClient, PathMatchingResourcePatternResolver resourceResolver) {
        this.restClient = restClient;
        this.resourceResolver = resourceResolver;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ObjectMapper mapper = JacksonResources.create().getObjectMapper(); // Use discord4j's custom mapper
        Long applicationId = restClient.getApplicationId().block();
        List<InputStream> isList = new ArrayList<>();
        for (Resource resource : resourceResolver.getResources("static/commands/*.json")) {
            InputStream inputStream = resource.getInputStream();
            isList.add(inputStream);
        }
        List<ApplicationCommandRequest> globalCommandRequests = new ArrayList<>();
        for (InputStream x : isList) {
            ApplicationCommandRequest applicationCommandRequest = mapper.readValue(x, ApplicationCommandRequest.class);
            globalCommandRequests.add(applicationCommandRequest);
        }
        restClient.getApplicationService().bulkOverwriteGlobalApplicationCommand(applicationId, globalCommandRequests)
                  .flatMap(commandData -> {
                      System.out.printf("Command %s added successfully%n", commandData.name());
                      return Mono.empty();
                  }).onErrorResume(e -> {
                      System.out.printf("Failed to register command - %s%n", e.getMessage());
                      return Mono.empty();
                  }).subscribe();
    }
}
