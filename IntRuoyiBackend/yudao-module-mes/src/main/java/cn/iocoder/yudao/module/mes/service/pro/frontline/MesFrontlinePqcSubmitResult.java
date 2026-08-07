package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.time.LocalDateTime;

public record MesFrontlinePqcSubmitResult(Long pqcTaskId,
                                         Long pqcEventId,
                                         Long pqcRecordId,
                                         Long signatureId,
                                         String inspectionResult,
                                         LocalDateTime serverSubmitTime) {
}
