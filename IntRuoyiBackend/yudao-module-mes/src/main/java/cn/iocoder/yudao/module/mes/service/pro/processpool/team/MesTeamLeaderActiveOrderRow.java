package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 生产组长活跃订单列表读模型。
 */
@Data
@Accessors(chain = true)
public class MesTeamLeaderActiveOrderRow {

    private Long id;
    private Long leaderUserId;
    private Long workOrderId;
    private String workOrderCode;
    private Long productId;
    private String productName;
    private String productCode;
    private String batchCode;
    private BigDecimal quantity;
    private Long routeId;
    private String routeName;
    private Long routeVersionId;
    private String routeVersionNo;
    private BigDecimal erpFixedQuantitySnapshot;
    private List<ProcessRemainingQuantity> processRemainingQuantities = List.of();
    private BigDecimal productionProgressPercent;
    private BigDecimal inspectionProgressPercent;
    private String activeStatus;
    private String businessStatus;
    private LocalDateTime joinedAt;
    private LocalDateTime removedAt;
    private Integer version;
    private Boolean abnormal;
    private String abnormalReason;
    private LocalDateTime abnormalReportedAt;
    private Long releaseApplicationId;
    private Long pqcReleaseWorkTaskId;
    private String releaseApplicationStatus;
    private String releaseSourceSnapshotHash;
    private Integer releaseApplicationVersion;
    private Boolean quantityConflict;
    private Boolean hasQuantityConflict;
    private Integer quantityConflictProcessCount;
    private BigDecimal overageQuantity;
    private Boolean simulated;
    private String simulationStage;
    private String simulationRunId;

    @Data
    @Accessors(chain = true)
    public static class ProcessRemainingQuantity {

        private Long routeProcessId;
        private Long processId;
        private BigDecimal plannedQuantity;
        private BigDecimal allocatedQuantity;
        private BigDecimal remainingQuantity;
        private Boolean quantityConflict;
        private BigDecimal overageQuantity;
    }
}
