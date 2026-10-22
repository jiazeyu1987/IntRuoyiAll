package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - MES QA 通用检验规程套检验项目升版 Request VO")
@Data
public class MesQaCommonRegulationSetItemsUpgradeReqVO {

    @Schema(description = "通用检验规程套 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "通用检验规程套不能为空")
    private Long setId;

    @Schema(description = "来源套版本 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "来源通用检验规程套版本不能为空")
    private Long sourceSetVersionId;

    @Schema(description = "新套版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新通用检验规程套版本不能为空")
    private String versionNo;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "备注")
    private String remark;

    @Valid
    @NotEmpty(message = "通用检验规程成员不能为空")
    private List<Member> members;

    @Schema(description = "通用检验规程成员升版内容")
    @Data
    public static class Member {
        @NotNull(message = "通用检验规程 ID 不能为空")
        private Long commonRegulationId;
        @NotNull(message = "来源通用检验规程版本不能为空")
        private Long sourceCommonRegulationVersionId;
        @NotNull(message = "通用 DCC 项目代码不能为空")
        private Long commonDccProjectCodeId;
        @NotBlank(message = "通用检验规程编码不能为空")
        private String commonRegulationCode;
        @NotBlank(message = "通用检验规程名称不能为空")
        private String commonRegulationName;
        @NotBlank(message = "通用检验规程新版本不能为空")
        private String versionNo;
        private Integer sort;
        private String memberRole;
        private String remark;
        @NotNull(message = "末检适用性不能为空")
        private Boolean finalInspectionApplicable;
        private String finalInspectionNotApplicableReason;
        @Valid
        @NotEmpty(message = "检验类型规则不能为空")
        private List<MesQaInspectionRegulationSaveReqVO.InspectionTypeRule> inspectionTypeRules;
        @Valid
        @NotEmpty(message = "通用检验规程工序不能为空")
        private List<MesQaInspectionRegulationSaveReqVO.InspectionProcess> processes;
    }
}
