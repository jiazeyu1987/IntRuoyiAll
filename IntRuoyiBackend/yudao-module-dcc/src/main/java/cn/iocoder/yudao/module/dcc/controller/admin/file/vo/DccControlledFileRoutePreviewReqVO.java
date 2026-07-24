package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccControlledFileRoutePreviewReqVO {

    @NotNull(message = "categoryId is required")
    private Long categoryId;
}
