package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "管理后台 - 确认清理 ERP 不存在的生产工单")
public class MesProWorkOrderErpCleanupReqVO {
    @NotBlank
    private String environmentKey;
    @Valid
    @NotEmpty
    private List<@NotNull Item> items;

    @Data
    public static class Item {
        @NotNull
        private Long workOrderId;
        @NotNull
        private Long syncRecordId;
        @NotBlank
        private String sourceFid;
        @NotBlank
        private String sourceBillNo;
        @NotBlank
        private String sourceMaterialNumber;
    }
}
