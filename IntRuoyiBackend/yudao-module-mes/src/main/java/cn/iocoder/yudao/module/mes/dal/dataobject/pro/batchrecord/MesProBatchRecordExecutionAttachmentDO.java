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

@TableName("mes_pro_batch_record_execution_attachment")
@KeySequence("mes_pro_batch_record_execution_attachment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordExecutionAttachmentDO extends BaseDO {

    @TableId
    private Long id;

    private Long executionId;

    private Long batchExecutionId;

    private Long batchTaskId;

    private Long workTaskId;

    private Integer rowIndex;

    private Integer columnIndex;

    private String fieldKey;

    private String fieldPath;

    private String fieldLabel;

    private String attachmentType;

    private String attachmentGroupKey;

    private String attachmentAction;

    private Integer versionNo;

    private Long fileId;

    private String fileUrl;

    private Long storageConfigId;

    private String storagePath;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private String sha256;

    private String storageRetentionJson;

    private String storageRetentionHash;

    private Long auditBatchId;

    private Long signatureId;

    private String previousAttachmentHash;

    private String attachmentHash;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime operatedAt;

    private String reasonCategory;

    private String reasonText;

    private Long tenantId;
}
