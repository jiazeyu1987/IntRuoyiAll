package cn.iocoder.yudao.module.ai.enums.model;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * AI model platform enum.
 */
@Getter
@AllArgsConstructor
public enum AiPlatformEnum implements ArrayValuable<String> {

    // ========== Domestic platforms ==========

    TONG_YI("TongYi", "通义千问"),
    YI_YAN("YiYan", "文心一言"),
    DEEP_SEEK("DeepSeek", "DeepSeek"),
    ZHI_PU("ZhiPu", "智谱"),
    XING_HUO("XingHuo", "星火"),
    DOU_BAO("DouBao", "豆包"),
    HUN_YUAN("HunYuan", "混元"),
    SILICON_FLOW("SiliconFlow", "硅基流动"),
    MINI_MAX("MiniMax", "MiniMax"),
    MOONSHOT("Moonshot", "月之暗面"),
    BAI_CHUAN("BaiChuan", "百川智能"),

    // ========== External / local platforms ==========

    CODEX_CLI("CodexCli", "Codex CLI"),
    OPENAI("OpenAI", "OpenAI"),
    AZURE_OPENAI("AzureOpenAI", "AzureOpenAI"),
    ANTHROPIC("Anthropic", "Anthropic"),
    GEMINI("Gemini", "Gemini"),
    OLLAMA("Ollama", "Ollama"),

    STABLE_DIFFUSION("StableDiffusion", "StableDiffusion"),
    MIDJOURNEY("Midjourney", "Midjourney"),
    SUNO("Suno", "Suno"),
    GROK("Grok", "Grok");

    private final String platform;
    private final String name;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(AiPlatformEnum::getPlatform)
            .toArray(String[]::new);

    public static AiPlatformEnum validatePlatform(String platform) {
        for (AiPlatformEnum platformEnum : AiPlatformEnum.values()) {
            if (platformEnum.getPlatform().equals(platform)) {
                return platformEnum;
            }
        }
        throw new IllegalArgumentException("非法平台：" + platform);
    }

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
