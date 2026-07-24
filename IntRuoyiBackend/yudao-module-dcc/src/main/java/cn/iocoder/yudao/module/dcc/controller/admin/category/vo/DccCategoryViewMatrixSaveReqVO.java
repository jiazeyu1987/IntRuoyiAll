package cn.iocoder.yudao.module.dcc.controller.admin.category.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DccCategoryViewMatrixSaveReqVO {

    @Valid
    @NotNull(message = "查看矩阵规则不能为空")
    private List<Rule> rules;

    @Data
    public static class Rule {
        private Long id;
        private String excelFileName;
        private Integer excelRowNo;
        private String excelColumnLetter;
        private String subjectLabel;
        private String subjectTopHeader;
        private String subjectSubHeader;
        private String marker;
        private String scopeType;
        private String subjectType;
        private Long subjectId;
        private Boolean active;
        private String remark;
    }
}
