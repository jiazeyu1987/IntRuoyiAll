package cn.iocoder.yudao.module.ai.controller.admin.tts.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - AI TTS 阿里云 NLS 默认配置 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTtsAliyunNlsDefaultsRespVO {

    @Schema(description = "当前默认音色", example = "ruoxi")
    private String defaultVoice;

    @Schema(description = "默认音色是否已保存", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean voiceSaved;

    @Schema(description = "默认音色是否已配置", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean voiceConfigured;

    @Schema(description = "默认音色来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "saved")
    private String voiceSource;

    @Schema(description = "AppKey 是否已保存", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean appKeySaved;

    @Schema(description = "AppKey 是否已配置", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean appKeyConfigured;

    @Schema(description = "AppKey 来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "runtime")
    private String appKeySource;

    @Schema(description = "脱敏 AppKey", example = "i0nm****UXM9")
    private String maskedAppKey;

    @Schema(description = "Token 是否已保存", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean tokenSaved;

    @Schema(description = "Token 是否已配置", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean tokenConfigured;

    @Schema(description = "Token 来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "runtime")
    private String tokenSource;

    @Schema(description = "脱敏 AccessToken", example = "test****abcd")
    private String maskedAccessToken;
}
