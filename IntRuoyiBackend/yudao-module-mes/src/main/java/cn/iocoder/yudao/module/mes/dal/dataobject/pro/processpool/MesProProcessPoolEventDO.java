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

    @TableId
    private Long id;

    private Long poolId;
    private String eventType;
    private String eventIdempotencyKey;
    private Long workOrderId;
    private Long routeId;
    private Long routeProcessId;
    private Long processId;
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
}
