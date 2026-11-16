package cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.process;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "\u7BA1\u7406\u540E\u53F0 - MES \u8BBE\u5907\u5DE5\u5E8F\u660E\u7EC6 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesDvMachineryProcessRespVO {

    @Schema(description = "\u7F16\u53F7", example = "1")
    private Long id;

    @Schema(description = "\u8BBE\u5907\u7F16\u53F7", example = "1")
    private Long machineryId;

    @Schema(description = "\u8BBE\u5907\u7F16\u7801", example = "A03196")
    @ExcelProperty("\u8BBE\u5907\u7F16\u7801")
    private String machineryCode;

    @Schema(description = "\u4EA7\u7EBF\u540D\u79F0", example = "\u7403\u56CA\u6269\u5F20\u5BFC\u7BA1")
    @ExcelProperty("\u4EA7\u7EBF\u540D\u79F0")
    private String lineName;

    @Schema(description = "\u5DE5\u5E8F\u540D\u79F0", example = "\u5916\u7BA1\u4E0E\u7403\u56CA\u710A\u63A5")
    @ExcelProperty("\u5DE5\u5E8F\u540D\u79F0")
    private String processName;

    @Schema(description = "\u8BBE\u5907\u540D\u79F0", example = "\u6FC0\u5149\u710A\u63A5\u673A")
    @ExcelProperty("\u8BBE\u5907\u540D\u79F0")
    private String deviceName;

    @Schema(description = "\u8BBE\u5907\u6570\u91CF", example = "1")
    @ExcelProperty("\u8BBE\u5907\u6570\u91CF")
    private BigDecimal deviceQuantity;

    @Schema(description = "10.5\u5C0F\u65F6\u65E5\u4EA7\u80FD", example = "585")
    @ExcelProperty("10.5\u5C0F\u65F6\u65E5\u4EA7\u80FD")
    private BigDecimal tenHalfHourDailyCapacity;

    @Schema(description = "\u8BBE\u5907\u6807\u51C6\u5C0F\u65F6\u4EA7\u80FD", example = "55.714286")
    @ExcelProperty("\u8BBE\u5907\u6807\u51C6\u5C0F\u65F6\u4EA7\u80FD")
    private BigDecimal standardHourlyCapacity;

    @Schema(description = "Excel \u6E90\u884C\u53F7", example = "10")
    @ExcelProperty("Excel\u6E90\u884C\u53F7")
    private Integer sourceRowNo;

    @Schema(description = "\u5907\u6CE8")
    @ExcelProperty("\u5907\u6CE8")
    private String remark;

    @Schema(description = "\u521B\u5EFA\u65F6\u95F4")
    private LocalDateTime createTime;
}
