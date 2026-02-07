package com.yourname.aibot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = AIBotMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    private static final ForgeConfigSpec.ConfigValue<String> OLLAMA_BASE_URL = BUILDER
            .comment("Base URL for the Ollama server.")
            .define("ollamaBaseUrl", "http://localhost:11434");

    private static final ForgeConfigSpec.ConfigValue<String> OLLAMA_MODEL = BUILDER
            .comment("Model name used for Ollama generation.")
            .define("ollamaModel", "llama3");

    private static final ForgeConfigSpec.IntValue OLLAMA_REQUEST_TIMEOUT_MS = BUILDER
            .comment("Timeout (ms) for Ollama HTTP requests.")
            .defineInRange("requestTimeoutMs", 15_000, 1_000, 120_000);

    private static final ForgeConfigSpec.IntValue OLLAMA_MAX_RETRIES = BUILDER
            .comment("Retry attempts for Ollama requests.")
            .defineInRange("ollamaMaxRetries", 2, 0, 10);

    private static final ForgeConfigSpec.IntValue OLLAMA_RETRY_DELAY_MS = BUILDER
            .comment("Delay (ms) between Ollama request retries.")
            .defineInRange("ollamaRetryDelayMs", 500, 0, 10_000);

    private static final ForgeConfigSpec.ConfigValue<String> OLLAMA_FALLBACK_MESSAGE = BUILDER
            .comment("Fallback message when Ollama fails to respond.")
            .define("ollamaFallbackMessage", "いまはうまく答えられないみたい。少し待ってからもう一度話しかけてみて。");

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;
    public static String ollamaBaseUrl;
    public static String ollamaModel;
    public static int requestTimeoutMs;
    public static int ollamaMaxRetries;
    public static int ollamaRetryDelayMs;
    public static String ollamaFallbackMessage;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(ResourceLocation.tryParse(itemName));
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();
        ollamaBaseUrl = OLLAMA_BASE_URL.get();
        ollamaModel = OLLAMA_MODEL.get();
        requestTimeoutMs = OLLAMA_REQUEST_TIMEOUT_MS.get();
        ollamaMaxRetries = OLLAMA_MAX_RETRIES.get();
        ollamaRetryDelayMs = OLLAMA_RETRY_DELAY_MS.get();
        ollamaFallbackMessage = OLLAMA_FALLBACK_MESSAGE.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(itemName)))
                .collect(Collectors.toSet());
    }
}
