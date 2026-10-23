package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class DccControlledFileDelayedEffectiveConfirmReqVO {
    @NotBlank private String taskId;
    @NotBlank private String processInstanceId;
    @NotNull @Positive private Integer attributeRound;
    @NotBlank private String expectedProcessDefinitionId;
    @NotBlank @Pattern(regexp="[a-f0-9]{64}") private String expectedDefinitionDigest;
    @NotNull @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss",timezone="Asia/Shanghai") private LocalDateTime selectedEffectiveTime;
    @NotBlank private String password;
    @NotBlank @Size(max=500) private String reason;
    @NotBlank @Size(max=128) private String idempotencyKey;
}
