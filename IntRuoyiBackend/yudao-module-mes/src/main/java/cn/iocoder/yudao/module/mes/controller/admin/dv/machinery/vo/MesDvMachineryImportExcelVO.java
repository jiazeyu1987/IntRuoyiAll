package cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.mes.enums.DictTypeConstants;
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
    private String code;

    @ExcelProperty("\u8BBE\u5907\u540D\u79F0")
    private String name;

    @ExcelProperty("\u54C1\u724C")
    private String brand;

    @ExcelProperty("\u89C4\u683C\u578B\u53F7")
    private String specification;

    @ExcelProperty("\u8BBE\u5907\u7C7B\u578B\u7F16\u7801")
    private String machineryTypeCode;

    @ExcelProperty("\u6240\u5C5E\u8F66\u95F4\u7F16\u7801")
    private String workshopCode;

    @ExcelProperty("\u5DE5\u5E8F\u540D\u79F0")
    private String processName;

    @ExcelProperty("\u8BBE\u5907\u6807\u51C6\u5C0F\u65F6\u4EA7\u80FD")
    private BigDecimal standardHourlyCapacity;

    @ExcelProperty(value = "\u8BBE\u5907\u72B6\u6001", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.MES_DV_MACHINERY_STATUS)
    private Integer status;

    @ExcelProperty("\u5907\u6CE8")
    private String remark;
}
