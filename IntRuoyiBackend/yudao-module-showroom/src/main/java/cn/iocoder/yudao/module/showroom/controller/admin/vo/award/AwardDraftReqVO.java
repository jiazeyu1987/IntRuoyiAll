package cn.iocoder.yudao.module.showroom.controller.admin.vo.award;

public record AwardDraftReqVO(Long awardId, String awardCode, String nameCn, String nameEn,
                              String descriptionZh, String descriptionEn, String issuer,
                              String awardDateText, String coverImage) {
}
