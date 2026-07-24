package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - DCC 受控浏览批量识别任务 Response VO")
@Data
public class DccControlledFileBatchRecognitionTaskRespVO {

    private Long taskId;
    private String status;
    private String recognitionType;
    private String scope;
    private String recognitionVersionSnapshot;
    private Long directoryId;
    private String directoryPath;
    private String keyword;
    private String statusFilter;
    private Long categoryId;
    private Boolean overwriteExisting;
    private String existingRecordPolicy;
    private Boolean syncFileNameTitle;
    private Integer workerCount;
    private Integer activeWorkerCount;
    private Long recordedFileCount;
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
    private List<FailureSummary> failureSummaries = List.of();
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Data
    public static class FailureSummary {

        private String stage;
        private String code;
        private String reason;
        private Long count;
    }
}
