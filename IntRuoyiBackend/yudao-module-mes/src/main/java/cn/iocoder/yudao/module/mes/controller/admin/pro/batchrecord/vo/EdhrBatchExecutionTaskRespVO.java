package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class EdhrBatchExecutionTaskRespVO {

    private Long id;

    private String nodeType;

    private Long routeProcessId;

    private Integer routeProcessSort;

    private Long processId;

    private String processCode;

    private String processName;

    private String batchRecordReportId;

    private String batchRecordReportName;

    private Long batchRecordDefinitionId;

    private Long batchRecordVersionId;

    private String batchRecordVersionNo;

    private Integer batchRecordSort;

    private String instanceScope;

    private String sharedFormKey;

    private String fillableScopeJson;

    private String executionMode;

    private String formSlotType;

    private String formBindingKey;

    private Long formTemplateId;

    private String formTemplateName;

    private Long formTemplateVersionId;

    private String formTemplateVersionNo;

    private Long formCenterInstanceId;

    private String recordCategory;

    private String validationProfile;

    private Boolean recordbookEnabled;

    private Long permissionScopeId;

    private Long routeBindingId;

    private String routeBindingSnapshotHash;

    private String requiredPolicy;

    private String requiredConditionJson;

    private String ownerRoleKey;

    private String archiveVisibility;

    private String slotConfigSnapshotHash;

    private Boolean available;

    private String gateMessage;

    private String currentUserRole;

    private List<String> allowedActions;

    private String disabledReason;

    private Long activeWorkTaskId;

    private String activeWorkTaskType;

    private String activeWorkTaskActionUrl;

    private Long executionId;

    private Integer status;

    private Boolean requiredFlag;

    private String blockerCode;

    private String blockerMessage;

    private LocalDateTime openedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime approvedAt;

    private Long skippedBy;

    private LocalDateTime skippedAt;

    private String specialPayloadJson;

    private List<EdhrBatchExecutionSpecialNodeAttachmentVO> pendingSpecialNodeAttachments;

    private List<FillableUser> fillableUsers;

    @Data
    @Accessors(chain = true)
    public static class FillableUser {

        private Long userId;

        private String displayName;
    }
}
