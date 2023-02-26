package com.sapwbd.kenbot.commands.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapwbd.kenbot.commands.SlashCommand;
import com.sapwbd.kenbot.store.impl.UserAccountStoreService;
import com.sapwbd.kenbot.util.OpenDotaUtility;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.function.Tuple3;

import java.util.Map;

@Component
public class LastMatchCommand extends DotaCommand implements SlashCommand {

    public static final String SUMMARY_TEXT = "%s / %s %s their last match playing as %s";
    public static final String KDA_TEXT = "KDA: %s/%s/%s";
    CloseableHttpClient httpClient;

    ObjectMapper mapper;

    public LastMatchCommand(UserAccountStoreService userAccountStoreService) {
        super(userAccountStoreService);
        this.httpClient = HttpClients.createDefault();
        this.mapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "lastmatch";
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        return event.deferReply().then(processEvent(event));
    }

    public Mono<Void> processEvent(ChatInputInteractionEvent event) {
        Mono<String> playerIdMono = Mono.just(event).flatMap(this::getPlayerId)
                                        .doOnError(commandProcessingExceptionHandler(event)).cache();
        Mono<String> playerPersonaNameMono = playerIdMono.flatMap(this::getPlayerEntity)
                                                         .doOnError(commandProcessingExceptionHandler(event))
                                                         .flatMap(this::getPlayerPersonaName)
                                                         .doOnError(commandProcessingExceptionHandler(event));
        Mono<JsonNode> playerLastMatchMono = playerIdMono.flatMap(playerId -> getPlayerMatches(playerId).map(playerMatches -> Map.entry(playerId, playerMatches)))
                                                         .doOnError(commandProcessingExceptionHandler(event))
                                                         .flatMap(this::getLastMatchDetails)
                                                         .doOnError(commandProcessingExceptionHandler(event));
        return Mono.zip(playerIdMono, playerPersonaNameMono, playerLastMatchMono)
                   .flatMap(tuple -> publishLastMatchCommandResponse(event, tuple));
    }

    private Mono<String> getPlayerPersonaName(String playerEntity) {
        try {
            JsonNode playerRootNode = mapper.readTree(playerEntity);
            if (playerRootNode.path("profile").isMissingNode()) {
                throw new RuntimeException("account_id does not match any known records in the API database");
            }
            return Mono.just(playerRootNode.path("profile").path("personaname").asText());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Exception while parsing OpenDota payload");
        }
    }

    private Mono<String> getPlayerMatches(String playerId) {
        HttpClient httpClient = HttpClient.create();
        String querySegment = "limit=1";
        return httpClient.get()
                         .uri(OpenDotaUtility.getOpenDotaURL(String.format(OpenDotaUtility.RESOURCE_PLAYER, playerId), OpenDotaUtility.RESOURCE_MATCHES)
                                             .concat("?").concat(querySegment))
                         .responseSingle((httpClientResponse, byteBufMono) -> {
                             if (httpClientResponse.status() == HttpResponseStatus.OK) {
                                 return byteBufMono.asString();
                             }
                             if (httpClientResponse.status() == HttpResponseStatus.TOO_MANY_REQUESTS) {
                                 throw new RuntimeException("The bot is currently rate limited! Try again later");
                             }
                             throw new RuntimeException("Internal server error!");
                         });
    }

    private Mono<JsonNode> getLastMatchDetails(Map.Entry<String, String> playerIdMatches) {
        try {
            String playerId = playerIdMatches.getKey();
            String matchesEntity = playerIdMatches.getValue();
            JsonNode matchesNode = mapper.readTree(matchesEntity);
            if (!matchesNode.isArray() || matchesNode.path(0).isMissingNode()) {
                throw new RuntimeException(String.format("Last match data not found for user %s", playerId));
            }
            return Mono.just(matchesNode.get(0));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Exception while parsing OpenDota payload");
        }
    }

    private Mono<Void> publishLastMatchCommandResponse(ChatInputInteractionEvent event, Tuple3<String, String, JsonNode> tuple) {
        String playerId = tuple.getT1();
        String playerName = tuple.getT2();
        JsonNode matchEntityNode = tuple.getT3();
        StringBuilder botReplyOnSuccess = new StringBuilder();
        boolean playerWin = matchEntityNode.get("radiant_win").asBoolean() ^ ((matchEntityNode.get("player_slot")
                                                                                              .asInt() & 1 << 7) != 0);
        String heroName = OpenDotaUtility.getHeroName(matchEntityNode.get("hero_id").asText());
        botReplyOnSuccess.append(String.format(SUMMARY_TEXT, playerName, playerId, playerWin ? "won" : "lost", heroName));
        botReplyOnSuccess.append("\n");
        botReplyOnSuccess.append(String.format(KDA_TEXT, matchEntityNode.get("kills")
                                                                        .asText(), matchEntityNode.get("deaths")
                                                                                                  .asText(), matchEntityNode.get("assists")
                                                                                                                            .asText()));
        return event.editReply(botReplyOnSuccess.toString()).then();
    }

}
