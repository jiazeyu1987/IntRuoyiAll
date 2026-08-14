package cn.iocoder.yudao.module.dcc.dal.dataobject.file;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("dcc_nas_control_audit_file")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccNasControlAuditFileDO extends BaseDO {

    @TableId
    private Long id;

    private Long taskId;

    private String nasShareName;

    private String rootPath;

    private String normalizedRelativePath;

    private String pathHash;

    private String fileName;

    private Long fileSize;

    private LocalDateTime modifiedAt;

    private String sourceSignature;

    private String controlStatus;

    private String classificationStatus;

    private Long matchedProjectCodeId;

    private Long matchedFileTypeTaxonomyId;

    private String matchedFileTypeLevel1;

    private String matchedFileTypeLevel2;

    private String matchedFileTypeLevel3;

    private String matchedFileTypeLevel4;

    private String matchedFileTypeLevel5;

    private String classificationReason;

    private String classificationCandidatesJson;

    private String expectedLocalRelativePath;

    private String downloadStatus;

    private String archiveStatus;

    private Long selectedImportTaskId;

    private Long selectedImportTaskItemId;

    private String localRelativePath;

    private String localWriteErrorCode;

    private String localWriteError;

    private String archiveErrorCode;

    private String archiveError;

    private Long controlledFileId;

    private Long tenantId;
}
