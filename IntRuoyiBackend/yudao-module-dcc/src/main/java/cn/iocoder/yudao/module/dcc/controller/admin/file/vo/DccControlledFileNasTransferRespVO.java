package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DccControlledFileNasTransferRespVO {

    private Long taskId;
    private String status;
    private String sourceType;
    private List<String> selectedNasPaths = new ArrayList<>();
    private Long expectedFileCount = 0L;
    private Long expectedTotalBytes = 0L;
    private Long uploadedFileCount = 0L;
    private Long uploadedTotalBytes = 0L;
    private String uploadCompletedAt;
    private Integer createdDirectoryCount = 0;
    private Integer reusedDirectoryCount = 0;
    private Integer createdCategoryCount = 0;
    private Integer reusedCategoryCount = 0;
    private Integer createdFileCount = 0;
    private Integer failedFileCount = 0;
    private Integer skippedPreviewOnlyCount = 0;
    private Integer remainingPendingCount = 0;
    private String lastFailureMessage;
    private String completedAt;
    private String failureReportPath;
    private String failureReportGeneratedAt;
    private String failureReportError;
    private List<FailureItem> failures = new ArrayList<>();

    @Data
    public static class FailureItem {
        private String nasPath;
        private String stage;
        private String reason;
    }
}
