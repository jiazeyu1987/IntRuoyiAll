package cn.iocoder.yudao.module.ai.controller.admin.tts.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - AI TTS 阿里云 NLS Token 保存 Request VO")
@Data
public class AiTtsAliyunNlsTokenSaveReqVO {

    @Schema(description = "阿里云 NLS AccessToken", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "AccessToken 不能为空")
    @Size(max = 500, message = "AccessToken 长度不能超过 500 个字符")
    private String accessToken;

}
