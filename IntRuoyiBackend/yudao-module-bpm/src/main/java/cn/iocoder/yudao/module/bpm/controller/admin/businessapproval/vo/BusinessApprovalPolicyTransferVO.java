package cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.model.BpmModelMetaInfoVO;
import cn.iocoder.yudao.module.bpm.controller.admin.definition.vo.category.BpmCategorySaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.rolecategory.RoleCategorySaveReqVO;

import java.util.List;

/** Cross-tenant business configuration only; database identities and audit records are not portable. */
@Data
public class BusinessApprovalPolicyTransferVO {
    @NotNull @Min(2) @Max(2)
    private Integer schemaVersion;
    @NotBlank @Pattern(regexp = "BUSINESS_APPROVAL_POLICIES")
    private String format;
    @NotNull @Valid
    private List<@NotNull Item> policies;
    @NotNull @Valid
    private List<@NotNull Process> processes;

    /** Deployed snapshot. Role candidates use stable role codes, never source tenant IDs. */
    @Data
    public static class Process {
        @NotBlank private String key;
        @NotBlank private String name;
        @NotNull @Valid private BpmCategorySaveReqVO category;
        @NotBlank private String bpmnXml;
        @NotNull private BpmModelMetaInfoVO metaInfo;
        @NotNull @Valid private List<@NotNull Role> roles;
        @NotNull private List<@NotBlank String> startUsernames;

        @JsonAnySetter
        public void rejectUnknown(String name, Object value) {
            throw new IllegalArgumentException("审批流程包含不支持的字段：" + name);
        }
    }

    @Data
    public static class Role {
        @NotBlank @Size(max = 100) private String code;
        @NotBlank @Size(max = 30) private String name;
        @NotNull @Valid private RoleCategorySaveReqVO category;

        @JsonAnySetter
        public void rejectUnknown(String name, Object value) {
            throw new IllegalArgumentException("审批角色包含不支持的字段：" + name);
        }
    }

    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
        throw new IllegalArgumentException("审批策略文件包含不支持的字段：" + name);
    }

    @Data
    public static class Item {
        @NotBlank @Size(max = 64)
        private String dataDomain;
        @NotBlank @Size(max = 64)
        private String systemCode;
        @NotBlank @Size(max = 64)
        private String objectType;
        @NotBlank @Size(max = 64)
        private String actionCode;
        @NotBlank @Size(max = 64)
        private String objectState;
        @NotBlank @Pattern(regexp = "BPM_REQUIRED|SIGNATURE_REQUIRED|DIRECT")
        private String policyMode;
        @Size(max = 128)
        private String processDefinitionKey;
        @NotBlank @Size(max = 128)
        private String effectExecutorCode;
        @Size(max = 32)
        private String formPolicyType;
        private String formSlotsJson;
        @NotBlank @Pattern(regexp = "DRAFT|PUBLISHED|DISABLED")
        private String status;
        @Size(max = 500)
        private String remark;

        @JsonAnySetter
        public void rejectUnknown(String name, Object value) {
            throw new IllegalArgumentException("审批策略包含不支持的字段：" + name);
        }
    }
}
