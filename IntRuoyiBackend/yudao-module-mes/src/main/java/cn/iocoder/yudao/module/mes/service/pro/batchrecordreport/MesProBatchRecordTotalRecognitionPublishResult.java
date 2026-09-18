package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MesProBatchRecordTotalRecognitionPublishResult(
        String action,
        Long dccProjectCodeId,
        String projectCode,
        String projectName,
        Long routeId,
        String routeCode,
        String routeName,
        Long routeVersionId,
        String routeVersionNo,
        Long routeCandidateVersionId,
        String routeCandidateVersionNo,
        Integer processCount,
        Integer updatedProcessCount,
        String recognitionJsonSha256,
        LocalDateTime updateTime
) {
}
