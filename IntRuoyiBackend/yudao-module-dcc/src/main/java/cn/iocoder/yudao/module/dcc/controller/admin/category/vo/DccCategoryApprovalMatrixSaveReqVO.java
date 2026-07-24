package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class DccCategoryApprovalMatrixSaveReqVO {

    @NotNull(message = "生效时间不能为空")
    private LocalDateTime effectiveTime;

    private String remark;
    private List<Long> signoffPositionIds;
    private List<Long> approvalPositionIds;

    @NotEmpty(message = "审阅矩阵规则不能为空")
    private List<Rule> rules;

    @Data
    @Accessors(chain = true)
    public static class Rule {
        @NotNull(message = "阶段类型不能为空")
        private String stageType;
        @NotNull(message = "启用状态不能为空")
        private Boolean active;
        private String subjectLabel;
        private String marker;
        private String subjectType;
        private Long subjectId;
        private String subjectName;
        private String subjectDepartmentPath;
        private String remark;
    }
}
