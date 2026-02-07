package com.yourname.aibot.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class BotMemory {
    private final String botName;
    private final int maxEntries;
    private final Deque<String> entries = new ArrayDeque<>();

    public BotMemory(String botName, int maxEntries) {
        this.botName = botName;
        this.maxEntries = maxEntries;
    }

    public void addEntry(String entry) {
        if (entries.size() >= maxEntries) {
            entries.removeFirst();
        }
        entries.addLast(entry);
    }

    public String botName() {
        return botName;
    }

    public Optional<String> latestEntry() {
        return Optional.ofNullable(entries.peekLast());
    }
}
