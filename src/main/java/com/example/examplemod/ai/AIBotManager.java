package com.example.examplemod.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AIBotManager {
    private static final int MEMORY_LIMIT = 12;
    private static final int CHAT_INTERVAL_TICKS = 200;
    private static final List<String> BOT_NAMES = List.of("AI-Alpha", "AI-Beta");
    private static final Map<String, BotMemory> BOT_MEMORIES = new HashMap<>();
    private static int tickCounter = 0;
    private static int conversationIndex = 0;

    static {
        for (String name : BOT_NAMES) {
            BOT_MEMORIES.put(name, new BotMemory(name, MEMORY_LIMIT));
        }
    }

    private AIBotManager() {
    }

    public static void handlePlayerMessage(ServerPlayer player, String message) {
        String entry = player.getName().getString() + ": " + message;
        for (BotMemory memory : BOT_MEMORIES.values()) {
            memory.addEntry(entry);
        }
    }

    public static void onServerTick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter < CHAT_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        sendBotConversation(server);
    }

    private static void sendBotConversation(MinecraftServer server) {
        if (BOT_NAMES.isEmpty()) {
            return;
        }
        String speaker = BOT_NAMES.get(conversationIndex % BOT_NAMES.size());
        String listener = BOT_NAMES.get((conversationIndex + 1) % BOT_NAMES.size());
        BotMemory speakerMemory = BOT_MEMORIES.get(speaker);
        String topic = speakerMemory.latestEntry().orElse("最近は静かな時間が続いているね。");
        String message = listener + "、" + topic + "についてどう思う？";

        speakerMemory.addEntry(speaker + ": " + message);
        BOT_MEMORIES.get(listener).addEntry(speaker + ": " + message);

        Component broadcast = Component.literal("[" + speaker + "] " + message);
        server.getPlayerList().broadcastSystemMessage(broadcast, false);
        conversationIndex++;
    }
}
