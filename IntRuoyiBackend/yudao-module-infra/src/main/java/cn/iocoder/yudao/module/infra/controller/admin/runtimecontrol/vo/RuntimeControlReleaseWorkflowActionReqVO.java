package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - 程序发布工作流动作请求")
@Data
public class RuntimeControlReleaseWorkflowActionReqVO {

    @NotBlank(message = "操作原因不能为空")
    private String reason;

    @JsonAnySetter
    public void rejectClientOwnedField(String fieldName, Object ignoredValue) {
        throw new IllegalArgumentException("RELEASE_WORKFLOW_FIELD_NOT_ALLOWED: " + fieldName);
    }
}
