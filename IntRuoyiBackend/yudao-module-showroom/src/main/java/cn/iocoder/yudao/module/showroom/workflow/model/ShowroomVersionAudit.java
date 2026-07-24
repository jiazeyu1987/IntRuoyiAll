package cn.iocoder.yudao.module.showroom.workflow.model;

import java.time.Instant;

public record ShowroomVersionAudit(String targetType, Long targetId, Long revisionId, String fieldCode,
                                   String oldValueJson, String newValueJson, Long operatorId,
                                   String operatorAction, Instant createdAt) {
}
