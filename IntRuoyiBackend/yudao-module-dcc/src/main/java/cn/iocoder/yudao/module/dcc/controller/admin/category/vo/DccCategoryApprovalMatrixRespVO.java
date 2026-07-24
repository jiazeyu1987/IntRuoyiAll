package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class DccCategoryApprovalMatrixRespVO {

    private Long categoryId;
    private Integer routeVersionNo;
    private LocalDateTime effectiveTime;
    private String remark;
    private List<Long> signoffPositionIds;
    private List<Long> approvalPositionIds;
    private List<Rule> rules;

    @Data
    @Accessors(chain = true)
    public static class Rule {
        private String stageType;
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
