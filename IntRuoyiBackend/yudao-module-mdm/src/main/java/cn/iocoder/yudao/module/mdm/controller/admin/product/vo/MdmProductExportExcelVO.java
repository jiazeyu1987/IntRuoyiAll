package cn.iocoder.yudao.module.mdm.controller.admin.product.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.module.mdm.dal.dataobject.product.MdmProductDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MdmProductExportExcelVO {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("更新时间")
    private String updateDate;

    public static MdmProductExportExcelVO from(MdmProductDO product) {
        return MdmProductExportExcelVO.builder()
                .productCode(product.getProductCode())
                .dccProductCode(product.getDccProductCode())
                .nameCn(product.getNameCn())
                .nameEn(product.getNameEn())
                .modelSpecification(product.getModelSpecification())
                .category(product.getCategory())
                .status(product.getStatus())
                .updateDate(product.getUpdateTime() == null ? null : DATE_FORMATTER.format(product.getUpdateTime()))
                .build();
    }

}
