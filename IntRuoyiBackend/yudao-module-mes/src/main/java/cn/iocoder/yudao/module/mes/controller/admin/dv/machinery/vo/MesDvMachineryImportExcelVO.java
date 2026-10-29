package cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.ExcelIgnore;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 设备台账 Excel 导入 VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MesDvMachineryImportExcelVO {

    @ExcelProperty("\u8BBE\u5907\u7F16\u7801")
    @NotBlank(message = "设备编码不能为空")
    private String code;

    @ExcelProperty("\u8BBE\u5907\u540D\u79F0")
    @NotBlank(message = "设备名称不能为空")
    private String name;

    @ExcelProperty("\u54C1\u724C")
    private String brand;

    @ExcelProperty("\u89C4\u683C\u578B\u53F7")
    private String specification;

    @ExcelProperty("\u8BBE\u5907\u7C7B\u578B\u7F16\u7801")
    @NotBlank(message = "设备类型编码不能为空")
    private String machineryTypeCode;

    @ExcelProperty("\u6240\u5C5E\u8F66\u95F4\u7F16\u7801")
    private String workshopCode;

    @ExcelProperty("所属车间")
    private String workshopName;

    @ExcelIgnore
    private String processName;

    @ExcelProperty("\u8BBE\u5907\u6807\u51C6\u5C0F\u65F6\u4EA7\u80FD")
    @DecimalMin(value = "0.000000001", message = "设备标准小时产能必须大于 0")
    private BigDecimal standardHourlyCapacity;

    @ExcelIgnore
    private Integer status;

    @ExcelIgnore
    private String remark;
}
