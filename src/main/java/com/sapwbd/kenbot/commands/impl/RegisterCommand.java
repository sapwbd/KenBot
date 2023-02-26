package com.sapwbd.kenbot.commands.impl;

import com.sapwbd.kenbot.commands.SlashCommand;
import com.sapwbd.kenbot.store.impl.UserAccountStoreService;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RegisterCommand extends DotaCommand implements SlashCommand {


    public RegisterCommand(UserAccountStoreService userAccountStoreService) {
        super(userAccountStoreService);
    }

    @Override
    public String getName() {
        return "register";
    }

    @Override
    public Mono<Void> executeCommand(ChatInputInteractionEvent event) {
        // Extract userName && account_id option
        String invokerName = event.getInteraction().getUser().getUsername();
        // account_id always present for register
        String accountId = event.getOption("account_id").flatMap(ApplicationCommandInteractionOption::getValue)
                                .map(ApplicationCommandInteractionOptionValue::asLong).map(String::valueOf).get();
        userAccountStoreService.insert(invokerName, accountId);
        return event.reply().withContent("Dota account registered successfully");
    }
}
