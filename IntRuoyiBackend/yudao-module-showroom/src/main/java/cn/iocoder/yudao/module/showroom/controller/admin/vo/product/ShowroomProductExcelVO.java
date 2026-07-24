package cn.iocoder.yudao.module.showroom.controller.admin.vo.product;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowroomProductExcelVO {

    @ExcelProperty(value = "展品编码", index = 0)
    private String productCode;

    @ExcelProperty(value = "旧产品编号", index = 1)
    private String legacyProductCode;

    @ExcelProperty(value = "产品名-中文", index = 2)
    private String nameCn;

    @ExcelProperty(value = "产品名-英文", index = 3)
    private String nameEn;

    @ExcelProperty(value = "展柜名称", index = 4)
    private String hallName;

    @ExcelProperty(value = "持证公司", index = 5)
    private String ownerCompanyName;

    @ExcelProperty(value = "在售/在研", index = 6)
    private String lifecycleStage;

    @ExcelProperty(value = "BU", index = 7)
    private String pipelineLayout;

    @ExcelProperty(value = "在售国家", index = 8)
    private String coreSellingPoints;

    @ExcelProperty(value = "适应症", index = 9)
    private String indicationContent;

    @ExcelProperty(value = "型号规格", index = 10)
    private String modelSpecification;

    @ExcelProperty(value = "注册证信息", index = 11)
    private String registrationCertificate;

    @ExcelIgnore
    private String productName;

    @ExcelProperty(value = "卖点文案", index = 12)
    private String sellingPointsCopy;

    @ExcelProperty(value = "产品图", index = 13)
    private String productImage;

    @ExcelProperty(value = "奖项", index = 14)
    private String awards;

    @ExcelProperty(value = "原材料表单", index = 15)
    private String rawMaterialSheet;

    @ExcelIgnore
    private String coverImage;
}
