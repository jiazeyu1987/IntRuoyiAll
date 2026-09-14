package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
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

/** Append-only Flow 7 event record consumed by Flow 6. */
@TableName("mes_pro_edhr_batch_trace_outbox_event")
@KeySequence("mes_pro_edhr_batch_trace_outbox_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrBatchTraceOutboxEventDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private String eventId;
    private String idempotencyKey;
    private Long batchExecutionId;
    private Long originId;
    private Long originLinkId;
    private String eventType;
    private String mappingStatus;
    private String errorCode;
    private String reason;
    private String traceLinkHash;
    private String sourceSnapshotHash;
    private String sourceBundleHash;
    private Integer manifestVersion;
    private String payloadJson;
    private String payloadHash;
    private Boolean retryable;
    private LocalDateTime occurredAt;
}
