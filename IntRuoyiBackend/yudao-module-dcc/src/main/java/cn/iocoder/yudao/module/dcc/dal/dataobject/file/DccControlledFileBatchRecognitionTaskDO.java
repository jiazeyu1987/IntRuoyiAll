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

@TableName("dcc_controlled_file_batch_recognition_task")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DccControlledFileBatchRecognitionTaskDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private Long operatorUserId;

    private String recognitionType;

    private String scopeType;

    private String recognitionVersionSnapshot;

    private Long directoryId;

    private String directoryPathSnapshot;

    private String keyword;

    private String statusFilter;

    private Long categoryId;

    private Boolean overwriteExisting;

    private String existingRecordPolicy;

    private Boolean syncFileNameTitle;

    private Integer workerCount;

    private String candidateIdsJson;

    private String status;

    private Long totalCount;

    private Long processedCount;

    private Long successCount;

    private Long failedCount;

    private Long skippedExistingCount;

    private Long unclassifiedCount;

    private Long ambiguousCount;

    private Long conflictCount;

    private Long remainingCount;

    private String lastFailureMessage;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;
}
