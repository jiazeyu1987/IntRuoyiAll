package cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class MesDvMachineryTypeExcelVO {
    @ExcelProperty("设备类型编码")
    private String code;
    @ExcelProperty("设备类型名称")
    private String name;
    @ExcelProperty("父类型编码（顶级留空）")
    private String parentCode;
    @ExcelProperty("状态（0启用1停用）")
    private Integer status;
    @ExcelProperty("排序")
    private Integer sort;
    @ExcelProperty("备注")
    private String remark;
}
