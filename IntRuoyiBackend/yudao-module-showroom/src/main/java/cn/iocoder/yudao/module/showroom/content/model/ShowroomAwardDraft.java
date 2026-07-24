package cn.iocoder.yudao.module.showroom.content.model;

public record ShowroomAwardDraft(Long awardId, String awardCode, String nameCn, String nameEn,
                                 String descriptionZh, String descriptionEn, String issuer,
                                 String awardDateText, String coverImage) {
}
