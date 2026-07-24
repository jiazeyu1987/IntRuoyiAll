package cn.iocoder.yudao.module.showroom.controller.admin.vo.award;

import cn.iocoder.yudao.module.showroom.controller.admin.vo.product.ShowroomProductImportExtra;

public record ShowroomAwardExcelImportRow(int rowNo, String awardCode, String nameCn, String issuer,
                                          String awardDateText,
                                          ShowroomProductImportExtra.ImportedCoverImage coverImage,
                                          int extraImageCount) {
}
