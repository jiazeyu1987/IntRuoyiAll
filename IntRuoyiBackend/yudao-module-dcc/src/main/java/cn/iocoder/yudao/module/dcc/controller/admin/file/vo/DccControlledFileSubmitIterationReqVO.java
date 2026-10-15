package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;
import java.time.LocalDate;

/** Request to submit an existing latest WORKING iteration for approval. */
@Data
public class DccControlledFileSubmitIterationReqVO {
    @jakarta.validation.constraints.NotNull
    private List<@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long> expectedOrdinaryAttachmentIds;
    @jakarta.validation.constraints.NotBlank
    @jakarta.validation.constraints.Pattern(regexp="[a-f0-9]{64}")
    private String expectedOrdinaryAttachmentManifestHash;
    private cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrix applicationCustomMatrix;
    private String expectedApplicationCustomMatrixDigest;
    private String applicationCustomMatrixToken;
    private String applicationCustomMatrixPhase;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;
    /** Required actual formal intent: PARTIAL or REPLACEMENT. */
    private String revisionChangeType;
    private String changeDescription;

    private Boolean needTraining;

    /** CC-2: this application's explicitly planned date, not the selected body's previous date. */
    private LocalDate effectiveDate;
    private String effectiveMode;
    private Long expectedApprovalRouteId;
    private Integer expectedApprovalRouteVersionNo;
    private String expectedApprovalMatrixDigest;
    private String approvalMatrixBaselineToken;
    private String approvalMatrixConfirmationToken;
    private Boolean matrixChangeConfirmed;
    private String applicationContextKey;
    private String executionContract;
    private String expectedProcessDefinitionId;
    private String expectedDefinitionDigest;
    private String expectedSupervisorSourceDigest;
    private String supervisorSourceToken;

    @JsonIgnore
    @AssertTrue(message="主管来源期待摘要和凭证须完整同给，或全部不提供")
    public boolean isSupervisorSourceExpectationComplete() {
        if (expectedSupervisorSourceDigest == null && supervisorSourceToken == null) return true;
        return expectedSupervisorSourceDigest != null && expectedSupervisorSourceDigest.matches("[a-f0-9]{64}")
                && supervisorSourceToken != null && !supervisorSourceToken.isEmpty() && supervisorSourceToken.length() <= 131072
                && supervisorSourceToken.chars().noneMatch(value -> value <= 0x20 || value == 0x7f);
    }

    @JsonIgnore
    @AssertTrue(message="请明确生效方式；立即或延迟方式申请时不填日期，历史预设日期方式须保留合法日期")
    public boolean isEffectiveSelectionValid() {
        return cn.iocoder.yudao.module.dcc.service.file.DccApplicationEffectiveSelection.isComplete(effectiveMode,effectiveDate);
    }

    private List<Long> selectedSignoffUserIds;
    private List<Long> selectedSignoffDepartmentIds;
    private List<Long> selectedDistributionDepartmentIds;
    private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes projectAttributes;
}
