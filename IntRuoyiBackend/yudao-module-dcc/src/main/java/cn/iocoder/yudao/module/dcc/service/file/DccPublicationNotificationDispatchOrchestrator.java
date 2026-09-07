package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationFollowupBatchDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationCandidateReasonDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccPublicationNotificationDeliveryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationFollowupBatchMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationCandidateReasonMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccPublicationNotificationDeliveryMapper;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PUBLICATION_NOTIFICATION_NOT_EXISTS;

@Service
@Slf4j
public class DccPublicationNotificationDispatchOrchestrator {
    @Resource private DccPublicationNotificationDeliveryMapper deliveryMapper;
    @Resource private DccPublicationNotificationCandidateReasonMapper reasonMapper;
    @Resource private DccPublicationFollowupBatchMapper batchMapper;
    @Resource private NotifyMessageSendApi notifyMessageSendApi;
    @Resource private DccPublicationNotificationTransactionService transactionService;

    public void dispatchBatch(Long tenantId, Long batchId) {
        List<DccPublicationNotificationDeliveryDO> rows =
                deliveryMapper.selectDispatchableByBatchId(tenantId, batchId);
        if (rows == null) {
            throw new IllegalStateException("dispatchable publication notifications must not be null");
        }
        for (DccPublicationNotificationDeliveryDO row : rows) {
            try {
                dispatchOne(tenantId, row, null, "发布后自动发送", false);
            } catch (RuntimeException ex) {
                log.error("[dispatchBatch][batchId({}) deliveryId({}) failed, failureType={}]",
                        batchId, row.getId(), ex.getClass().getSimpleName());
            }
        }
    }

    public void retryDelivery(Long tenantId, Long actorId, Long deliveryId, Integer expectedVersion, String reason) {
        DccPublicationNotificationDeliveryDO current = deliveryMapper.selectById(deliveryId);
        if (current == null || !tenantId.equals(current.getTenantId())) {
            throw exception(PUBLICATION_NOTIFICATION_NOT_EXISTS);
        }
        dispatchOne(tenantId, current, actorId, reason, true, expectedVersion);
    }

    private void dispatchOne(Long tenantId, DccPublicationNotificationDeliveryDO row, Long actorId,
                             String reason, boolean retry) {
        dispatchOne(tenantId, row, actorId, reason, retry, row.getRowVersion());
    }

    private void dispatchOne(Long tenantId, DccPublicationNotificationDeliveryDO row, Long actorId,
                             String reason, boolean retry, Integer expectedVersion) {
        DccPublicationNotificationDeliveryDO attempt = transactionService.beginAttempt(
                tenantId, row.getId(), expectedVersion, actorId, reason, retry);
        try {
            NotifySendSingleToUserIdempotentReqDTO request = new NotifySendSingleToUserIdempotentReqDTO();
            request.setUserId(attempt.getUserId());
            request.setTemplateCode(attempt.getTemplateCode());
            request.setBusinessKey(attempt.getBusinessKey());
            request.setTemplateParams(templateParams(tenantId, attempt));
            Long messageId = notifyMessageSendApi.sendSingleMessageIdempotentlyToAdmin(request);
            if (messageId == null) throw new IllegalStateException("platform message id is empty");
            transactionService.markSent(tenantId, attempt.getId(), attempt.getRowVersion(), messageId, actorId, reason);
        } catch (RuntimeException ex) {
            transactionService.markFailed(tenantId, attempt.getId(), attempt.getRowVersion(), actorId,
                    reason, sanitize(ex));
            if (retry) throw ex;
        }
    }

    private Map<String, Object> templateParams(Long tenantId, DccPublicationNotificationDeliveryDO row) {
        DccPublicationFollowupBatchDO batch = batchMapper.selectById(row.getBatchId());
        if (batch == null || !tenantId.equals(batch.getTenantId())) throw new IllegalStateException("publication batch not found");
        List<DccPublicationNotificationCandidateReasonDO> reasons = reasonMapper.selectListByCandidateId(
                tenantId, row.getCandidateId());
        if (reasons == null || reasons.isEmpty()) {
            throw new IllegalStateException("publication notification context invalid");
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("fileNumber", batch.getFileNumberSnapshot());
        params.put("versionNo", batch.getVersionNoSnapshot());
        params.put("followupUrl", "/dcc/controlled-file/detail/" + batch.getPublishedControlledFileId()
                + "?viewer=1&from=notification");
        params.put("reasonSummaries", reasons.stream()
                .map(DccPublicationNotificationCandidateReasonDO::getReasonSummary).toList());
        return params;
    }

    private String sanitize(RuntimeException ex) {
        String type = ex.getClass().getSimpleName();
        String value = ex.getMessage();
        if ("publication notification context invalid".equals(value)) {
            return type + ": " + value;
        }
        return type + ": publication notification delivery failed";
    }
}
