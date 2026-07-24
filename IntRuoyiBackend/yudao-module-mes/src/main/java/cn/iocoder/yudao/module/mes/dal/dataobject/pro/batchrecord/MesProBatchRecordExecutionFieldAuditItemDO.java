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

@TableName("mes_pro_batch_record_execution_field_audit_item")
@KeySequence("mes_pro_batch_record_execution_field_audit_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long auditBatchId;

    private Long executionId;

    private Long fieldAuditRevision;

    private Integer batchItemIndex;

    private String fieldPath;

    private String fieldKey;

    private String fieldLabel;

    private Integer rowIndex;

    private Integer columnIndex;

    private String component;

    private String valueType;

    private String oldValueJson;

    private String oldValueDisplay;

    private String oldValueHash;

    private String newValueJson;

    private String newValueDisplay;

    private String newValueHash;

    private String reasonCategory;

    private String reasonText;

    private Long actorId;

    private String actorName;

    private Long signatureId;

    private String signatureProjectionHash;

    private String previousHash;

    private String auditHash;

    private String beforeCellValuesHash;

    private String afterCellValuesHash;

    private String executionSnapshotHash;

    private LocalDateTime changedAt;

    private Long tenantId;
}
