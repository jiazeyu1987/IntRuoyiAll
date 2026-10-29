package cn.iocoder.yudao.module.mes.controller.admin.pro.process.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @Schema(description = "SOP 文件地址，支持 PDF 和图片；空字符串表示解除绑定")
    @Size(max = 2048, message = "SOP 文件地址不能超过 2048 个字符")
    @Pattern(regexp = "(?i)^(?:|(?:https?://[^\\s]+|/admin-api/infra/file/[0-9]+/get/[^\\s]+)\\.(?:pdf|png|jpe?g|webp)(?:\\?[^\\s]*)?)$",
            message = "SOP 文件必须是系统文件地址或 HTTP(S) 地址的 PDF、PNG、JPG、JPEG 或 WEBP 文件")
    private String sopFileUrl;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "人工班次产能")
    private BigDecimal manualShiftCapacity;

    @Schema(description = "备注")
    private String remark;
}
