package cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 新增生产工序；编码由系统按租户统一分配，忽略传入编码")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MesProProcessCreateReqVO {

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "工序名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "工序名称不能为空")
    private String name;

    @Schema(description = "工艺要求")
    private String attention;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "人工班次产能")
    private BigDecimal manualShiftCapacity;

    @Schema(description = "备注")
    private String remark;
}
