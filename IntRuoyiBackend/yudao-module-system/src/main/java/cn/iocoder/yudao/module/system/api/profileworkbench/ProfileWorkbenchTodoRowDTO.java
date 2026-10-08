package cn.iocoder.yudao.module.system.api.profileworkbench;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;
@Data
public class ProfileWorkbenchTodoRowDTO {
    private ProfileWorkbenchTodoSourceId sourceId;
    private String taskKey;
    private String businessId;
    private String taskType;
    private String source;
    private String detail;
    private String statusLabel;
    private LocalDateTime createdAt;
    private LocalDateTime dueAt;
    private Map<String, String> navigation;
    @JsonIgnore private Long numericId;
    @JsonIgnore private String controlledFileId;
    @JsonIgnore private String distributionId;
    @JsonIgnore private String code;
    @JsonIgnore private String domainTaskType;
    @JsonIgnore private String actionUrl;
    @JsonIgnore private String batchExecutionId;
    @JsonIgnore private String batchTaskId;
    @JsonIgnore private String executionId;
    @JsonIgnore private String businessScopeType;
    @JsonIgnore private String businessScopeId;
}
