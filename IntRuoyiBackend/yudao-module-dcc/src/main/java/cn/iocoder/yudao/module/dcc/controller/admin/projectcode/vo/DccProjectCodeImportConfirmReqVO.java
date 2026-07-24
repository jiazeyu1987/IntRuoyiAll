package cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccProjectCodeImportConfirmReqVO {

    @NotNull(message = "batchId is required")
    private Long batchId;
}
