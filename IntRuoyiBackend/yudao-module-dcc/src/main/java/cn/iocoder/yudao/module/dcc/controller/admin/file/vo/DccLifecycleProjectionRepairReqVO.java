package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class DccLifecycleProjectionRepairReqVO {
    @NotNull @Positive private Long masterId;
    @NotNull @Positive private Long versionRefId;
    @NotBlank @Size(max=128) private String processInstanceId;
    @NotBlank @Pattern(regexp="FINALIZING") private String expectedCanonicalStatus;
    @NotBlank @Pattern(regexp="[a-f0-9]{64}") private String sourceFactsHash;
    @NotBlank @Pattern(regexp="[a-f0-9]{64}") private String preimageHash;
    @NotBlank @Size(max=500) private String reason;
    @NotBlank @Size(max=128) private String idempotencyKey;
}
