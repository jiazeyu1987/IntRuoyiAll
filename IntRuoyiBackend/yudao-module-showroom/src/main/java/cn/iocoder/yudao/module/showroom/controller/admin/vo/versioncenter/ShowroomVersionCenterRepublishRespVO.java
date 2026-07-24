package cn.iocoder.yudao.module.showroom.controller.admin.vo.versioncenter;

public record ShowroomVersionCenterRepublishRespVO(String targetType,
                                                   Long targetId,
                                                   Long sourceRevisionId,
                                                   Long newRevisionId,
                                                   Integer newRevisionNo,
                                                   String releaseId,
                                                   String manifestHash,
                                                   String publishedAt) {
}
