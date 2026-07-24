package cn.iocoder.yudao.module.showroom.controller.admin.vo.award;

public record ShowroomAwardExcelExportRow(String awardCode, String sequenceText, String nameCn,
                                          String awardDateText, String issuer, String coverImage,
                                          byte[] coverImageContent) {
}
