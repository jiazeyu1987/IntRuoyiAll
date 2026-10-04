package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class DccWorkflowDistributionReqVO {
    @NotEmpty @Valid private List<Scope> scopes;
    @Data public static class Scope {
        @NotNull private Long departmentId;
        @NotBlank private String distributionMedium;
        private List<Long> recipientUserIds;
    }
}
