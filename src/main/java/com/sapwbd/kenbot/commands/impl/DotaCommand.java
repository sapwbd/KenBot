package com.sapwbd.kenbot.commands.impl;

import com.sapwbd.kenbot.store.impl.UserAccountStoreService;
import com.sapwbd.kenbot.util.OpenDotaUtility;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.function.Consumer;

@Component
public class DotaCommand {

    final UserAccountStoreService userAccountStoreService;

    public DotaCommand(UserAccountStoreService userAccountStoreService) {
        this.userAccountStoreService = userAccountStoreService;
    }

    public Mono<String> getPlayerId(ChatInputInteractionEvent event) {
        String playerId;
        if (event.getOption("account_id").isPresent()) {
            playerId = event.getOption("account_id").flatMap(ApplicationCommandInteractionOption::getValue)
                            .map(ApplicationCommandInteractionOptionValue::asLong).map(String::valueOf).get();
        } else {
            String username = event.getInteraction().getUser().getUsername();
            playerId = userAccountStoreService.get(username);
        }
        if (playerId == null) {
            throw new IllegalArgumentException("Pass a non empty account_id or register your user with a Dota account");
        }
        return Mono.just(playerId);
    }

    protected Mono<String> getPlayerEntity(String playerId) {
        HttpClient httpClient = HttpClient.create();
        return httpClient.get()
                         .uri(OpenDotaUtility.getOpenDotaURL(String.format(OpenDotaUtility.RESOURCE_PLAYER, playerId)))
                         .responseSingle((httpClientResponse, byteBufFlux) -> {
                             if (httpClientResponse.status() == HttpResponseStatus.OK) {
                                 return byteBufFlux.asString();
                             }
                             if (httpClientResponse.status() == HttpResponseStatus.TOO_MANY_REQUESTS) {
                                 throw new RuntimeException("The bot is currently rate limited! Try again later");
                             }
                             throw new RuntimeException("Internal server error!");
                         });
    }

    protected Consumer<? super Throwable> commandProcessingExceptionHandler(ChatInputInteractionEvent event) {
        return e -> event.editReply(e.getMessage()).subscribe();
    }
}
