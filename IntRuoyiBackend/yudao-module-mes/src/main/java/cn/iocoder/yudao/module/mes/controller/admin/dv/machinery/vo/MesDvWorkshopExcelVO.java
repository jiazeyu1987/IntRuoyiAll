package cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MesDvWorkshopExcelVO {
    @ExcelProperty("车间编码")
    private String code;
    @ExcelProperty("车间名称")
    private String name;
    @ExcelProperty("面积")
    private BigDecimal area;
    @ExcelProperty("状态（0启用1停用）")
    private Integer status;
    @ExcelProperty("备注")
    private String remark;
}
