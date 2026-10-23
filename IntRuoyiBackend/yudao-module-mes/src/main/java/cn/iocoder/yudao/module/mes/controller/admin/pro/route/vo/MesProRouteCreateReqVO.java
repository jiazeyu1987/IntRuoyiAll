package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "管理后台 - 新增工艺流程，编码由服务端生成")
public class MesProRouteCreateReqVO {

    @NotBlank(message = "名称不能为空")
    @Size(max = 128, message = "名称不能超过128个字符")
    private String name;

    @Size(max = 512, message = "备注不能超过512个字符")
    private String remark;

    @Schema(description = "按选择顺序排列的工序编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请至少选择一个工序")
    private List<@NotNull(message = "工序不能为空") @Positive(message = "工序编号无效") Long> processIds;
}
