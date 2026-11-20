package cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

/** One portable configuration unit: role grants and the policies that reference those roles. */
@Data
public class RoleApprovalConfigPackageVO {
    @NotNull @Min(1) @Max(1)
    private Integer schemaVersion;
    @NotBlank @Pattern(regexp = "ROLE_APPROVAL_CONFIGURATION")
    private String format;
    @NotNull
    private JsonNode roleConfig;
    @NotNull @Valid
    private BusinessApprovalPolicyTransferVO approvalPolicies;

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("权限角色与审批策略配置包包含不支持的字段：" + name);
    }
}
