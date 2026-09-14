package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DccNasControlAuditTaskRespVO {

    private Long taskId;
    private String status;
    private String nasShareName;
    private List<String> scanRoots = new ArrayList<>();
    private String currentPath;
    private Long scannedFileCount = 0L;
    private Long controlledFileCount = 0L;
    private Long notControlledFileCount = 0L;
    private Long ambiguousFileCount = 0L;
    private Long sourceMissingCount = 0L;
    private Long skippedDirectoryCount = 0L;
    private String unscannedFileCountLabel = "未知";
    private String reportFileName;
    private String startedAt;
    private String completedAt;
    private String failureReason;
}
