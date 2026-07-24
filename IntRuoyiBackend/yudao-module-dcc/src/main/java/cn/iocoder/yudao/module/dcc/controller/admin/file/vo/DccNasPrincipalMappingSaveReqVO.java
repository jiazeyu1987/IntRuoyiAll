package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccNasPrincipalMappingSaveReqVO {

    private String sourceAuthority;

    @NotBlank
    private String sourceSid;

    private String sourceName;

    private String accountName;

    @NotBlank
    private String accountType;

    @NotBlank
    private String targetSubjectType;

    @NotNull
    private Long targetSubjectId;

    @NotNull
    private Boolean active;

    private String changeReason;
}
