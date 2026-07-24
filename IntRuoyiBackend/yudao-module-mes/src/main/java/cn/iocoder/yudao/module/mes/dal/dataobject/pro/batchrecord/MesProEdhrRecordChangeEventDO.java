package cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("mes_pro_edhr_record_change_event")
@KeySequence("mes_pro_edhr_record_change_event_seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class MesProEdhrRecordChangeEventDO extends BaseDO {

    @TableId
    private Long id;
    private String changeCode;
    private String changeType;
    private String targetScope;
    private Long batchExecutionId;
    private Long executionId;
    private Long sourceExecutionId;
    private Long newExecutionId;
    private Long sourceArchiveId;
    private Long newArchiveId;
    private String changeStatus;
    private String reasonCategory;
    private String reasonText;
    private Long requestedBy;
    private LocalDateTime requestedAt;
    private Long requestSignatureId;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private Long approvalSignatureId;
    private LocalDateTime effectiveAt;
    private String previousStatus;
    private String newStatus;
    private String previousHeadHash;
    private String newHeadHash;
    private String previousArchiveHash;
    private String newArchiveHash;
    private String bpmProcessInstanceId;
    private String bpmTaskId;
    private String remark;

}
