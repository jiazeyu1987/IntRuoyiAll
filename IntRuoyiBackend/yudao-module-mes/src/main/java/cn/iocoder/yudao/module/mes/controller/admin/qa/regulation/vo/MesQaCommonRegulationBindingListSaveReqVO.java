package cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - MES QA 通用检验规程绑定列表保存 Request VO")
@Data
public class MesQaCommonRegulationBindingListSaveReqVO {

    @NotNull(message = "DCC 项目代码不能为空")
    @Positive
    private Long dccProjectCodeId;

    @Schema(description = "按执行顺序排列的通用规程版本 ID，空列表表示解除全部绑定")
    @NotNull(message = "通用规程绑定列表不能为空")
    private List<@NotNull @Positive Long> commonRegulationVersionIds;

    private String changeReason;
}
