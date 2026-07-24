package cn.iocoder.yudao.module.showroom.controller.admin.vo.award;

public record AwardPageRespVO(Long awardId, String awardCode, Long currentRevisionId, boolean incomplete,
                              boolean live, AwardDetailRespVO revision, AwardDetailRespVO displayRevision) {
}
