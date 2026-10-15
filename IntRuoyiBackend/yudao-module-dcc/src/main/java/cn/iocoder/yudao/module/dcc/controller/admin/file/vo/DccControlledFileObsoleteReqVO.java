package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DccControlledFileObsoleteReqVO {
    @NotBlank(message="obsolete application phase is required")
    @jakarta.validation.constraints.Pattern(regexp="OPEN|PREVIEW|SAVE|SUBMIT",message="obsolete application phase is invalid")
    private String phase;
    @jakarta.validation.constraints.Positive
    private Long formInstanceId;
    private String formInstanceCode;
    @jakarta.validation.constraints.Positive
    private Integer attributeRound;
    private String applicationContextKey;
    private Long expectedApprovalRouteId;
    private Integer expectedApprovalRouteVersionNo;
    private String expectedApprovalMatrixDigest;
    private String approvalMatrixBaselineToken;
    private String approvalMatrixConfirmationToken;
    private Boolean matrixChangeConfirmed;
    private Boolean prepareExecution;
    @jakarta.validation.constraints.Positive
    private Long reworkSourceFormInstanceId;
    private String reworkSourceFormInstanceCode;
    private String reworkSourceBpmProcessInstanceId;
    @jakarta.validation.constraints.Positive
    private Integer reworkSourceAttributeRound;
    @jakarta.validation.constraints.Pattern(regexp="[a-f0-9]{64}")
    private String reworkSourceDigest;
    @jakarta.validation.constraints.AssertTrue(message="rework source identity and digest must be supplied as one exact group")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isReworkSourceComplete(){
        if(reworkSourceFormInstanceId==null&&reworkSourceFormInstanceCode==null&&reworkSourceBpmProcessInstanceId==null&&reworkSourceAttributeRound==null&&reworkSourceDigest==null)return true;
        return reworkSourceFormInstanceId!=null&&reworkSourceFormInstanceId>0&&reworkSourceAttributeRound!=null&&reworkSourceAttributeRound>0
                &&reworkSourceFormInstanceCode!=null&&!reworkSourceFormInstanceCode.isBlank()&&reworkSourceFormInstanceCode.equals(reworkSourceFormInstanceCode.strip())
                &&reworkSourceBpmProcessInstanceId!=null&&!reworkSourceBpmProcessInstanceId.isBlank()&&reworkSourceBpmProcessInstanceId.equals(reworkSourceBpmProcessInstanceId.strip())
                &&reworkSourceDigest!=null&&reworkSourceDigest.matches("[a-f0-9]{64}");
    }

    @jakarta.validation.constraints.AssertTrue(message="non-OPEN phase requires a real form identity and reserved numeric round")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isExactFormContextComplete() {
        if("OPEN".equals(phase))return true;
        return formInstanceId!=null&&formInstanceId>0&&formInstanceCode!=null&&!formInstanceCode.isBlank()
                &&formInstanceCode.equals(formInstanceCode.strip())&&attributeRound!=null&&attributeRound>0;
    }
    @jakarta.validation.constraints.AssertTrue(message="SAVE must explicitly choose draft saving or execution preparation")
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isExplicitSaveIntent(){return !"SAVE".equals(phase)||prepareExecution!=null;}
    private java.util.List<Long> selectedSignoffDepartmentIds;
    private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes projectAttributes;

    @NotBlank(message = "reason is required")
    private String reason;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String approvalProcessInstanceId;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String approvedVersionNo;

    private Map<String, List<Long>> startUserSelectAssignees;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Boolean needTraining;

    public void setNeedTraining(Boolean needTraining) {
        throw new IllegalArgumentException("obsolete request does not accept needTraining");
    }
}
