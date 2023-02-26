package com.sapwbd.kenbot.listeners;

import com.sapwbd.kenbot.commands.SlashCommand;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SlashCommandListener {

    final GatewayDiscordClient discordClient;
    final List<SlashCommand> slashCommands;

    public SlashCommandListener(GatewayDiscordClient discordClient, List<SlashCommand> slashCommands) {
        this.discordClient = discordClient;
        this.slashCommands = slashCommands;
    }

    @PostConstruct
    private void registerSlashCommandListenerForGlobalCommands() {
        Map<String, SlashCommand> nameSlashCommandMap = new HashMap<>();
        for (SlashCommand slashCommand : slashCommands) {
            nameSlashCommandMap.put(slashCommand.getName(), slashCommand);
        }

        discordClient.on(ChatInputInteractionEvent.class, event -> {
            String commandName = event.getCommandName();
            Optional<SlashCommand> slashCommand = Optional.ofNullable(nameSlashCommandMap.get(commandName));
            if (slashCommand.isPresent()) {
                return slashCommand.get().executeCommand(event);
            }
            return Mono.empty();
        }).subscribe();
    }
}
