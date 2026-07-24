package cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.mes.enums.DictTypeConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "\u7BA1\u7406\u540E\u53F0 - MES \u8BBE\u5907\u53F0\u8D26 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesDvMachineryRespVO {

    @Schema(description = "\u7F16\u53F7", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("\u7F16\u53F7")
    private Long id;

    @Schema(description = "\u8BBE\u5907\u7F16\u7801", requiredMode = Schema.RequiredMode.REQUIRED, example = "EQ-001")
    @ExcelProperty("\u8BBE\u5907\u7F16\u7801")
    private String code;

    @Schema(description = "\u8BBE\u5907\u540D\u79F0", requiredMode = Schema.RequiredMode.REQUIRED, example = "CNC \u52A0\u5DE5\u4E2D\u5FC3")
    @ExcelProperty("\u8BBE\u5907\u540D\u79F0")
    private String name;

    @Schema(description = "\u54C1\u724C", example = "\u897F\u95E8\u5B50")
    @ExcelProperty("\u54C1\u724C")
    private String brand;

    @Schema(description = "\u89C4\u683C\u578B\u53F7", example = "S7-300")
    @ExcelProperty("\u89C4\u683C\u578B\u53F7")
    private String specification;

    @Schema(description = "\u8BBE\u5907\u7C7B\u578B\u7F16\u53F7", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long machineryTypeId;

    @Schema(description = "\u8BBE\u5907\u7C7B\u578B\u540D\u79F0", example = "\u6570\u63A7\u673A\u5E8A")
    @ExcelProperty("\u8BBE\u5907\u7C7B\u578B")
    private String machineryTypeName;

    @Schema(description = "\u6240\u5C5E\u8F66\u95F4\u7F16\u53F7", requiredMode = Schema.RequiredMode.REQUIRED, example = "200")
    private Long workshopId;

    @Schema(description = "\u6240\u5C5E\u8F66\u95F4\u540D\u79F0", example = "\u4E00\u53F7\u8F66\u95F4")
    @ExcelProperty("\u6240\u5C5E\u8F66\u95F4")
    private String workshopName;

    @Schema(description = "\u5DE5\u5E8F\u540D\u79F0", example = "\u9020\u5F71\u5BFC\u7BA1\u78E8\u524A")
    @ExcelProperty("\u5DE5\u5E8F\u540D\u79F0")
    private String processName;

    @Schema(description = "\u8BBE\u5907\u6807\u51C6\u5C0F\u65F6\u4EA7\u80FD", example = "180")
    @ExcelProperty("\u8BBE\u5907\u6807\u51C6\u5C0F\u65F6\u4EA7\u80FD")
    private BigDecimal standardHourlyCapacity;

    @Schema(description = "\u8BBE\u5907\u72B6\u6001", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "\u8BBE\u5907\u72B6\u6001", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.MES_DV_MACHINERY_STATUS)
    private Integer status;

    @Schema(description = "\u6700\u8FD1\u4FDD\u517B\u65F6\u95F4")
    @ExcelProperty("\u6700\u8FD1\u4FDD\u517B\u65F6\u95F4")
    private LocalDateTime lastMaintenTime;

    @Schema(description = "\u6700\u8FD1\u70B9\u68C0\u65F6\u95F4")
    @ExcelProperty("\u6700\u8FD1\u70B9\u68C0\u65F6\u95F4")
    private LocalDateTime lastCheckTime;

    @Schema(description = "\u5907\u6CE8", example = "\u5907\u6CE8")
    @ExcelProperty("\u5907\u6CE8")
    private String remark;

    @Schema(description = "\u521B\u5EFA\u65F6\u95F4", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("\u521B\u5EFA\u65F6\u95F4")
    private LocalDateTime createTime;
}
