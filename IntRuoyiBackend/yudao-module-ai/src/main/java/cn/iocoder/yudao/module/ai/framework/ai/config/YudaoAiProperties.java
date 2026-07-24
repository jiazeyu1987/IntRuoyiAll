package cn.iocoder.yudao.module.ai.framework.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Yudao AI properties.
 */
@ConfigurationProperties(prefix = "yudao.ai")
@Data
public class YudaoAiProperties {

    private CodexCli codexCli;
    private Gemini gemini;
    private DouBao doubao;
    private HunYuan hunyuan;
    private SiliconFlow siliconflow;
    private XingHuo xinghuo;
    private BaiChuan baichuan;
    private Midjourney midjourney;
    private Suno suno;
    private Grok grok;
    private WebSearch webSearch;
    private Tts tts = new Tts();

    @Data
    public static class CodexCli {

        private boolean enable = true;
        private String command;
        private Long timeoutMs = 240000L;
        private Integer parallelism = 8;
        private String workingDirectory;
        private String model;
        private String openAiApiKey;
        private String openAiBaseUrl;

    }

    @Data
    public static class Gemini {

        private String enable;
        private String apiKey;
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;

    }

    @Data
    public static class DouBao {

        private String enable;
        private String apiKey;
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;

    }

    @Data
    public static class HunYuan {

        private String enable;
        private String baseUrl;
        private String apiKey;
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;

    }

    @Data
    public static class SiliconFlow {

        private String enable;
        private String apiKey;
        private String model;
        private String imageModel;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;

    }

    @Data
    public static class XingHuo {

        private String enable;
        private String appId;
        private String appKey;
        private String secretKey;
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;

    }

    @Data
    public static class BaiChuan {

        private String enable;
        private String apiKey;
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;

    }

    @Data
    public static class Midjourney {

        private String enable;
        private String baseUrl;
        private String apiKey;
        private String notifyUrl;

    }

    @Data
    public static class Suno {

        private boolean enable;
        private String baseUrl;

    }

    @Data
    public static class Grok {

        private String enable;
        private String apiKey;
        private String baseUrl;
        private String model;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;

    }

    @Data
    public static class WebSearch {

        private boolean enable;
        private String apiKey;

    }

    @Data
    public static class Tts {

        private boolean enable = true;
        private String provider = "windows";
        private String voice = "";
        private Integer rate = 0;
        private Integer volume = 100;
        private Long timeoutMs = 30000L;
        private Dashscope dashscope = new Dashscope();
        private AliyunNls aliyunNls = new AliyunNls();

    }

    @Data
    public static class Dashscope {

        private String model = "cosyvoice-v3-plus";
        private String voice = "longyang";
        private Integer speechRate;
        private Integer volume = 50;
        private Long timeoutMs = 30000L;
        private Integer pitchRate;

    }

    @Data
    public static class AliyunNls {

        private String url = "";
        private String region = "cn-shanghai";
        private String appkey = "";
        private String accessToken = "";
        private String voice = "xiaoyun";
        private String format = "wav";
        private Integer sampleRate = 16000;
        private Integer speechRate;
        private Integer pitchRate;
        private Integer volume = 50;
        private Long timeoutMs = 30000L;

    }

}
