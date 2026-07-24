package cn.iocoder.yudao.module.showroom.content.model;

import java.util.Optional;

public record ShowroomCompanySnapshot(Long companyId, String companyType, String displayName, String displayNameEn,
                                      Optional<Long> currentRevisionId, boolean live) {
}
