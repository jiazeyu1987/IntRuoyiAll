package cn.iocoder.yudao.module.mes.service.pro.route;

import lombok.Builder;

@Builder
public record MesProRouteVersionApprovalResult(
        Long routeVersionId,
        String lifecycleStatus,
        String approvalProcessInstanceId,
        String approvalEventId,
        String approvalResult,
        String processedResult
) {
}
