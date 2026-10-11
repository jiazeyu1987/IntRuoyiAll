package cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "管理后台 - ERP 不存在生产工单清理分析")
public class MesProWorkOrderErpCleanupPreviewRespVO {
    private String environmentKey;
    private int analyzedCount;
    private int unverifiableCount;
    private List<Item> missingWorkOrders;

    @Data
    public static class Item {
        private Long workOrderId;
        private String workOrderCode;
        private Long syncRecordId;
        private String sourceFid;
        private String sourceBillNo;
        private String sourceMaterialNumber;
        private boolean deletable;
        private String retainReason;
    }
}
