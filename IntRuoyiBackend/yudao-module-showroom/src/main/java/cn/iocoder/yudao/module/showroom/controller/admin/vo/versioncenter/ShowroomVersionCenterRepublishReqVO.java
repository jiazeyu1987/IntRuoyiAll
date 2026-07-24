package cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter;

public record ShowroomVersionCenterRepublishReqVO(String targetType,
                                                  Long targetId,
                                                  Long sourceRevisionId,
                                                  String siteKey,
                                                  String stage) {
}
