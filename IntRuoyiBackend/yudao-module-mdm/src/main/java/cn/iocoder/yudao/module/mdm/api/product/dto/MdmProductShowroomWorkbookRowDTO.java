package cn.iocoder.yudao.module.mdm.api.product.dto;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmProductShowroomWorkbookRowDTO {

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("DCC产品编号")
    private String dccProductCode;

    @ExcelProperty("中文名称")
    private String nameCn;

    @ExcelProperty("英文名称")
    private String nameEn;

    @ExcelProperty("型号规格")
    private String modelSpecification;

    @ExcelProperty("产品分类")
    private String category;

}
