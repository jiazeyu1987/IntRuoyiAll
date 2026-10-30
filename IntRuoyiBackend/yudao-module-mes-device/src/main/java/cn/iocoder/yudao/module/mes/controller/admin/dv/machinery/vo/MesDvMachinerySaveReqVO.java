package cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "\u7BA1\u7406\u540E\u53F0 - MES \u8BBE\u5907\u53F0\u8D26\u65B0\u589E/\u4FEE\u6539 Request VO")
@Data
public class MesDvMachinerySaveReqVO {

    @Schema(description = "\u7F16\u53F7", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "\u8BBE\u5907\u7F16\u7801", requiredMode = Schema.RequiredMode.REQUIRED, example = "EQ-001")
    @NotEmpty(message = "\u8BBE\u5907\u7F16\u7801\u4E0D\u80FD\u4E3A\u7A7A")
    private String code;

    @Schema(description = "\u8BBE\u5907\u540D\u79F0", requiredMode = Schema.RequiredMode.REQUIRED, example = "CNC \u52A0\u5DE5\u4E2D\u5FC3")
    @NotEmpty(message = "\u8BBE\u5907\u540D\u79F0\u4E0D\u80FD\u4E3A\u7A7A")
    private String name;

    @Schema(description = "\u54C1\u724C", example = "\u897F\u95E8\u5B50")
    private String brand;

    @Schema(description = "\u89C4\u683C\u578B\u53F7", example = "S7-300")
    private String specification;

    @Schema(description = "\u8BBE\u5907\u7C7B\u578B\u7F16\u53F7", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "\u8BBE\u5907\u7C7B\u578B\u4E0D\u80FD\u4E3A\u7A7A")
    private Long machineryTypeId;

    @Schema(description = "\u6240\u5C5E\u8F66\u95F4\u7F16\u53F7", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    @NotNull(message = "\u6240\u5C5E\u8F66\u95F4\u4E0D\u80FD\u4E3A\u7A7A")
    private Long workshopId;

    @Schema(description = "\u5DE5\u5E8F\u540D\u79F0", example = "\u9020\u5F71\u5BFC\u7BA1\u78E8\u524A")
    private String processName;

    @Schema(description = "\u8BBE\u5907\u6807\u51C6\u5C0F\u65F6\u4EA7\u80FD", example = "180")
    private BigDecimal standardHourlyCapacity;

    @Schema(description = "\u8BBE\u5907\u72B6\u6001", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "\u8BBE\u5907\u72B6\u6001\u4E0D\u80FD\u4E3A\u7A7A")
    private Integer status;

    @Schema(description = "\u6700\u8FD1\u4FDD\u517B\u65F6\u95F4")
    private LocalDateTime lastMaintenTime;

    @Schema(description = "\u6700\u8FD1\u70B9\u68C0\u65F6\u95F4")
    private LocalDateTime lastCheckTime;

    @Schema(description = "\u5907\u6CE8", example = "\u5907\u6CE8")
    private String remark;
}
