package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DccNasPermissionRestoreApplyReqVO {

    @NotBlank
    private String idempotencyKey;

    @NotBlank
    private String planHash;

    @NotBlank
    private String restoreMode;

    private String changeReason;
}
