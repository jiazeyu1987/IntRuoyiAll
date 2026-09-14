package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DccPublicationImpactVersionReqVO {

    @NotNull(message = "expectedVersion is required")
    private Integer expectedVersion;
}
