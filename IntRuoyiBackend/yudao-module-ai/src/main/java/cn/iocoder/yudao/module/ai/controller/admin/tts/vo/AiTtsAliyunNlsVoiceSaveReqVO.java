package cn.iocoder.yudao.module.ai.controller.admin.tts.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - AI TTS 阿里云 NLS 默认音色保存 Request VO")
@Data
public class AiTtsAliyunNlsVoiceSaveReqVO {

    @Schema(description = "阿里云 NLS 默认音色", requiredMode = Schema.RequiredMode.REQUIRED, example = "ruoxi")
    @NotBlank(message = "默认音色不能为空")
    @Size(max = 64, message = "默认音色长度不能超过 64 个字符")
    private String voice;
}
