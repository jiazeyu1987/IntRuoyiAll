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

@TableName("mes_pro_batch_record_execution_archive")
@KeySequence("mes_pro_batch_record_execution_archive_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProBatchRecordExecutionArchiveDO extends BaseDO {

    @TableId
    private Long id;

    private Long executionId;

    private String archiveCode;

    private Integer archiveVersion;

    private String artifactType;

    private String archiveStatus;

    private Long fileId;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private String sha256;

    private String renderSourceVersion;

    private String executionSnapshotHash;

    private String cellValuesHash;

    private Long fieldAuditRevision;

    private String fieldAuditHeadHash;

    private String signatureHash;

    private Long approvalSnapshotId;

    private String approvalSnapshotHash;

    private Long sealSignatureId;

    private Long generatedBy;

    private LocalDateTime generatedAt;

    private Long sealedBy;

    private LocalDateTime sealedAt;

    private String failureReason;

    private String remark;

    private Long supersededByArchiveId;

    private Long invalidatedByChangeEventId;

    private Boolean archiveValidFlag;

    private String archiveValidStatus;
}
