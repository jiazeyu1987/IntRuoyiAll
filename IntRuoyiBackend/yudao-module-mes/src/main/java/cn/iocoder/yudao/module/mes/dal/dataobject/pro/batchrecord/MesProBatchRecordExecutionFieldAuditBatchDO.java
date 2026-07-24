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

@TableName("mes_pro_batch_record_execution_field_audit_batch")
@KeySequence("mes_pro_batch_record_execution_field_audit_batch_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditBatchDO extends BaseDO {

    @TableId
    private Long id;

    private Long executionId;

    private String idempotencyKey;

    private String requestHash;

    private String actionType;

    private String reasonCategory;

    private String reasonText;

    private Integer fieldCount;

    private Long actorId;

    private String actorName;

    private Long signatureId;

    private String signatureChallengeHash;

    private String signatureProjectionHash;

    private String baseCellValuesHash;

    private String beforeCellValuesHash;

    private String afterCellValuesHash;

    private Long baseFieldAuditRevision;

    private Long beforeFieldAuditRevision;

    private Long afterFieldAuditRevision;

    private String baseFieldAuditHeadHash;

    private String previousHeadHash;

    private String newHeadHash;

    private String hashVerificationJson;

    private LocalDateTime changedAt;

    private String clientIp;

    private String userAgent;

    private Long tenantId;
}
