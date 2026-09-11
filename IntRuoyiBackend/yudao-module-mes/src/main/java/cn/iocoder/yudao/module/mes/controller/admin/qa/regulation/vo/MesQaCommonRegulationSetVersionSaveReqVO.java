package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - MES QA 通用检验规程套版本保存 Request VO")
@Data
public class MesQaCommonRegulationSetVersionSaveReqVO {

    @Schema(description = "套版本 ID")
    private Long id;

    @Schema(description = "通用检验规程套 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "通用检验规程套不能为空")
    private Long setId;

    @Schema(description = "套版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "通用检验规程套版本不能为空")
    private String versionNo;

    @Schema(description = "版本状态：DRAFT/PUBLISHED")
    private String lifecycleStatus;

    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "备注")
    private String remark;

    @Valid
    @NotEmpty(message = "通用检验规程套成员不能为空")
    private List<Member> members;

    @Schema(description = "通用检验规程套成员")
    @Data
    public static class Member {
        @NotNull(message = "通用检验规程版本不能为空")
        private Long commonRegulationVersionId;
        private Integer sort;
        private String memberRole;
        private String remark;
    }
}
