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

@TableName("mes_pro_edhr_batch_execution_trace_link")
@KeySequence("mes_pro_edhr_batch_execution_trace_link_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProEdhrBatchExecutionTraceLinkDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long batchExecutionId;
    private Long originId;
    private String linkType;
    private String sourceObjectType;
    private Long sourceObjectId;
    private Long sourceLineId;
    private Long sourceEventId;
    private Integer sourceVersion;
    private String sourceIdentityKey;
    private String idempotencyKey;
    private String snapshotJson;
    private String snapshotHash;
    private String relationStatus;
    private String relationReason;
    private Long capturedBy;
    private LocalDateTime capturedAt;
}
