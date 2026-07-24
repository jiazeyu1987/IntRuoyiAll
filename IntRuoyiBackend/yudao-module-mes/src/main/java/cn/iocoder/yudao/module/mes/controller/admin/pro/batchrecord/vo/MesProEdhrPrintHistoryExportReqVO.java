package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MesProEdhrPrintHistoryExportReqVO {

    @NotBlank(message = "筛选快照不能为空")
    private String filterSnapshotJson;

    @NotBlank(message = "幂等键不能为空")
    private String idempotencyKey;
}
