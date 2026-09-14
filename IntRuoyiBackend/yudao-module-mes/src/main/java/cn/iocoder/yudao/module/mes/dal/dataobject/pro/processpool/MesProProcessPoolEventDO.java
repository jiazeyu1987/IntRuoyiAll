package cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("mes_pro_process_pool_event")
@KeySequence("mes_pro_process_pool_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProProcessPoolEventDO extends TenantBaseDO {

    public static final String EVENT_TYPE_PRODUCTION_SUBMIT = "PRODUCTION_SUBMIT";
    public static final String EVENT_TYPE_PQC_INSPECTION = "PQC_INSPECTION";
    public static final String REPORT_MANAGEMENT_STATUS_NOT_APPLICABLE = "NOT_APPLICABLE";
    public static final String REPORT_MANAGEMENT_STATUS_UNALLOCATED = "UNALLOCATED";
    public static final String REPORT_MANAGEMENT_STATUS_PARTIALLY_ALLOCATED = "PARTIALLY_ALLOCATED";
    public static final String REPORT_MANAGEMENT_STATUS_PENDING_RELEASE = "PENDING_RELEASE";
    public static final String REPORT_MANAGEMENT_STATUS_ARCHIVED = "ARCHIVED";
    public static final String REPORT_RELEASE_STATUS_NOT_APPLICABLE = "NOT_APPLICABLE";
    public static final String REPORT_RELEASE_STATUS_NOT_ALLOCATED = "NOT_ALLOCATED";
    public static final String REPORT_RELEASE_STATUS_NOT_RELEASED = "NOT_RELEASED";
    public static final String REPORT_RELEASE_STATUS_PARTIALLY_RELEASED = "PARTIALLY_RELEASED";
    public static final String REPORT_RELEASE_STATUS_RELEASED = "RELEASED";

    @TableId
    private Long id;

    private Long poolId;
    private String eventType;
    private String eventIdempotencyKey;
    private Long workOrderId;
    private Long routeId;
    private Long routeProcessId;
    private Long processId;
    private Long qaProcessId;
    private Long actualEmployeeId;
    private Long deviceAccountId;
    private Long deviceId;
    private Long workstationId;
    private String templateType;
    private String feedbackSourceType;
    private Long feedbackSourceId;
    private Long recordbookEntryId;
    private String recordbookSourceType;
    private Long recordbookSourceId;
    private String rawPayload;
    private LocalDateTime serverSubmitTime;
    private Long signatureId;
    private Long signatureUserId;
    private String signatureSnapshot;
    private Boolean simulated;
    private String simulationStage;
    private String simulationRunId;
    private String reportManagementStatus;
    private BigDecimal reportOutputQuantity;
    private BigDecimal reportAllocatedQuantity;
    private BigDecimal reportUnallocatedQuantity;
    private String reportReleaseStatus;
}
