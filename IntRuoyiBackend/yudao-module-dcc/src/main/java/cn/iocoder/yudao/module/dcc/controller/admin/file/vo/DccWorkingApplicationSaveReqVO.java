package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/** Current values of an existing authorized draft. Null is an explicit unfilled choice. */
@Data
public class DccWorkingApplicationSaveReqVO {
    private cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrix applicationCustomMatrix;
    private String expectedApplicationCustomMatrixDigest;
    private String applicationCustomMatrixToken;
    private String applicationCustomMatrixPhase;
    @NotNull @Valid private DccProjectAttributes actual;
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

    @com.fasterxml.jackson.annotation.JsonIgnore
    @jakarta.validation.constraints.AssertTrue(message="主管来源期待摘要和凭证须完整同给，或全部不提供")
    public boolean isSupervisorSourceExpectationComplete() {
        if (expectedSupervisorSourceDigest == null && supervisorSourceToken == null) return true;
        return expectedSupervisorSourceDigest != null && expectedSupervisorSourceDigest.matches("[a-f0-9]{64}")
                && supervisorSourceToken != null && !supervisorSourceToken.isEmpty() && supervisorSourceToken.length() <= 131072
                && supervisorSourceToken.chars().noneMatch(value -> value <= 0x20 || value == 0x7f);
    }
    private Boolean prepareNewActionBaseline;
    private LocalDate effectiveDate;
    private List<@NotNull @Positive Long> selectedSignoffDepartmentIds;
    private List<@NotNull @Positive Long> selectedDistributionDepartmentIds;
    private Boolean needTraining;
    @Size(max=1000) private String changeDescription;
}
