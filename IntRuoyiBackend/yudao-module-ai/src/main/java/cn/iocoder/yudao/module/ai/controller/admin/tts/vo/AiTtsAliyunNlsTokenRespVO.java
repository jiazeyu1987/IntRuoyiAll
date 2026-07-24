package cn.iocoder.yudao.module.ai.controller.admin.tts.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - AI TTS 阿里云 NLS Token Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiTtsAliyunNlsTokenRespVO {

    @Schema(description = "是否已经保存到系统配置", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean saved;

    @Schema(description = "当前是否存在可用配置", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean configured;

    @Schema(description = "配置来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "saved")
    private String source;

    @Schema(description = "脱敏 AccessToken", example = "a5a7****a69e")
    private String maskedAccessToken;

}
