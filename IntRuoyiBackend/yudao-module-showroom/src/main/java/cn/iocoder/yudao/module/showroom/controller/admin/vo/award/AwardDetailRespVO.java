package cn.iocoder.yudao.module.showroom.controller.admin.vo.award;

import cn.iocoder.yudao.module.showroom.controller.admin.ShowroomAdminController;

import java.util.List;

public record AwardDetailRespVO(Long awardId, String awardCode, Long currentRevisionId, boolean incomplete,
                                boolean live, Long revisionId, int revisionNo, String status, String nameCn,
                                String nameEn, String descriptionZh, String descriptionEn, String issuer,
                                String awardDateText, String coverImage,
                                List<ShowroomAdminController.NarrationAvailabilityRespVO> narrations,
                                boolean editable,
                                List<ShowroomAdminController.MaterialBlockerRespVO> materialBlockers) {
}
