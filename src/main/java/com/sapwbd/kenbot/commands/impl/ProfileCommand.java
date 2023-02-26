package com.sapwbd.kenbot.commands.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapwbd.kenbot.commands.SlashCommand;
import com.sapwbd.kenbot.store.impl.UserAccountStoreService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.possible.Possible;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Component
public class ProfileCommand extends DotaCommand implements SlashCommand {

    public static final String PROFILE_IDENTIFIER_TEXT = "%s / %s";
    public static final String PROFILE_URL_TEXT = "%s";
    public static final String LAST_LOGIN_TEXT = "Last recorded login on %s";
    public static final String LAST_SOLO_MMR_TEXT = "Last recorded solo MMR was %s";
    public static final String LAST_PARTY_MMR_TEXT = "Last recorded party MMR was %s";

    CloseableHttpClient httpClient;

    ObjectMapper mapper;

    public ProfileCommand(UserAccountStoreService userAccountStoreService) {
        super(userAccountStoreService);
        this.httpClient = HttpClients.createDefault();
        this.mapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "profile";
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        return event.deferReply().then(processEvent(event));
    }

    private Mono<Void> processEvent(ChatInputInteractionEvent event) {
        return Mono.just(event).flatMap(this::getPlayerId).doOnError(commandProcessingExceptionHandler(event))
                   .flatMap(this::getPlayerEntity).doOnError(commandProcessingExceptionHandler(event))
                   .flatMap(playerEntity -> publishProfileCommandResponse(event, playerEntity))
                   .doOnError(commandProcessingExceptionHandler(event));
    }

    private Mono<Void> publishProfileCommandResponse(ChatInputInteractionEvent event, String playerEntity) {
        try {
            JsonNode playerRootNode = mapper.readTree(playerEntity);
            if (playerRootNode.path("profile").isMissingNode()) {
                throw new RuntimeException("account_id does not match any known records in the API database");
            }
            // A StringBuilder instance to hold the response as it gets appended
            StringBuilder botReplyOnSuccess = new StringBuilder();
            // Mark / 12349999
            botReplyOnSuccess.append(String.format(PROFILE_IDENTIFIER_TEXT, playerRootNode.get("profile")
                                                                                          .get("personaname")
                                                                                          .asText(), playerRootNode.get("profile")
                                                                                                                   .get("account_id")
                                                                                                                   .asText()));
            botReplyOnSuccess.append("\n");
            // https://steamcommunity.com/id/mark/
            // Wrap within <> to prevent link embed
            botReplyOnSuccess.append("<")
                             .append(String.format(PROFILE_URL_TEXT, playerRootNode.get("profile").get("profileurl")
                                                                                   .asText())).append(">");
            botReplyOnSuccess.append("\n");
            // Last recorded login on 2000-01-01T00:00:00.000Z
            String locale = "UTC";
            Possible<String> possibleLocale = event.getInteraction().getUser().getUserData().locale();
            if (!possibleLocale.isAbsent()) {
                locale = possibleLocale.get();
            }
            SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            Date lastLoginDate = sourceFormat.parse(playerRootNode.get("profile").get("last_login").asText());
            SimpleDateFormat destFormat = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss:SSS");
            destFormat.setTimeZone(TimeZone.getTimeZone(locale));
            botReplyOnSuccess.append(String.format(LAST_LOGIN_TEXT, destFormat.format(lastLoginDate)));
            botReplyOnSuccess.append("\n");
            // Last recorded solo MMR was 10000
            botReplyOnSuccess.append(String.format(LAST_SOLO_MMR_TEXT, playerRootNode.get("solo_competitive_rank")
                                                                                     .asText()));
            botReplyOnSuccess.append("\n");
            // Last recorded party MMR was 10000
            botReplyOnSuccess.append(String.format(LAST_PARTY_MMR_TEXT, playerRootNode.get("competitive_rank")
                                                                                      .asText()));
            return event.editReply(botReplyOnSuccess.toString()).then();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Exception while parsing OpenDota payload");
        } catch (ParseException e) {
            throw new RuntimeException("Issue while parsing datetime. Reach out to the developer to fix the code");
        }
    }

}
