package cn.iocoder.yudao.module.showroom.content.model;

import java.util.Map;

public record ShowroomCompanyDraft(Long companyId, String companyType, String displayName, String displayNameEn,
                                   Map<String, String> fields) {
}
