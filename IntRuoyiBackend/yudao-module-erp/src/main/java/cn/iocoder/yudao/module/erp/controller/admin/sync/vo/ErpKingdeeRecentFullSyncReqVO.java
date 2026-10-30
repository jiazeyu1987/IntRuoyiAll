package cn.iocoder.yudao.module.erp.controller.admin.sync.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ErpKingdeeRecentFullSyncReqVO {
    @NotBlank
    private String syncType;
    @NotNull
    @Min(1)
    private Integer days = 7;
    @NotNull
    private Long clickedAt;
}
