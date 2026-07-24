package cn.iocoder.yudao.module.showroom.content.model;

import java.util.Map;

public record ShowroomCompanyRevision(Long companyId, Long revisionId, int revisionNo, String status,
                                      Map<String, String> fields) {
}
