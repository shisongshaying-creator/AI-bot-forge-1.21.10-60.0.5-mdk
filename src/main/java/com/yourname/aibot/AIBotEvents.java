package com.yourname.aibot;

import com.yourname.aibot.ai.AIBotManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AIBotMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AIBotEvents {
    private AIBotEvents() {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        String message = event.getMessage().getString();
        AIBotManager.handlePlayerMessage(player, message);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("aibot")
                .then(Commands.literal("say")
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String message = StringArgumentType.getString(context, "message");
                            AIBotManager.handlePlayerMessage(player, message);
                            player.sendSystemMessage(Component.literal("AI BOTにメッセージを送信しました。"));
                            return 1;
                        })
                    )
                )
        );
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        AIBotManager.onServerTick(event.getServer());
    }
}
