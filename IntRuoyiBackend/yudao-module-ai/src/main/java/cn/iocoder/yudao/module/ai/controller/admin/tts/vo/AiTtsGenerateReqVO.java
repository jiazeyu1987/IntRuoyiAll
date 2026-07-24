package cn.iocoder.yudao.module.ai.controller.admin.tts.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - AI TTS 测试生成 Request VO")
@Data
public class AiTtsGenerateReqVO {

    @Schema(description = "待合成文本", requiredMode = Schema.RequiredMode.REQUIRED, example = "你好，这是一条 TTS 测试语音。")
    @NotBlank(message = "待合成文本不能为空")
    private String text;

    @Schema(description = "TTS 提供方", example = "windows")
    private String provider;

    @Schema(description = "指定音色", example = "longyang")
    private String voice;

}
