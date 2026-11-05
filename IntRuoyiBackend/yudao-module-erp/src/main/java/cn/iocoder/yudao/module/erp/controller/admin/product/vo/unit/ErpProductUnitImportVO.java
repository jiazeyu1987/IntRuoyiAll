package cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

/** 产品单位导入模板，列名与导出文件一致。 */
@Data
@ExcelIgnoreUnannotated
public class ErpProductUnitImportVO {

    @ExcelProperty("单位名字")
    private String name;

    @ExcelProperty("单位状态")
    private String status;

}
