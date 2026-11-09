package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileLifecycleEvent;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Objects;

/** A references B: B's completed revision informs A's signed file compiler. */
@Service
public class DccRelatedFileRevisionNotifier {
    public static final String TEMPLATE_CODE = "dcc_related_file_revision";
    private final DccRelationStore store;
    private final DccControlledFileMapper files;
    private final NotifyMessageSendApi messages;

    public DccRelatedFileRevisionNotifier(DccRelationStore store, DccControlledFileMapper files,
                                          NotifyMessageSendApi messages) {
        this.store = store; this.files = files; this.messages = messages;
    }

    @EventListener
    @Order(Ordered.LOWEST_PRECEDENCE)
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void onLifecycleEvent(DccControlledFileLifecycleEvent event) {
        if (event == null || !"CONTROLLED".equals(event.eventType())) return;
        Long tenant = TenantContextHolder.getRequiredTenantId();
        var revised = files.selectById(event.controlledFileId());
        if (revised == null || !Objects.equals(tenant, event.tenantId())
                || !Objects.equals(tenant, revised.getTenantId())
                || !Objects.equals(event.masterId(), revised.getMasterId())
                || revised.getControlledTime() == null || !Objects.equals(event.versionNo(), revised.getVersionNo()))
            throw new DccRelationFailure("DCC_REVISION_NOTIFICATION_EVENT_INVALID");
        if (!"REVISION".equals(revised.getChangeType())) return;
        if (revised.getRevisionSourceControlledFileId() == null)
            throw new DccRelationFailure("DCC_REVISION_NOTIFICATION_BASELINE_MISSING");
        // Only the authoritative current relation set counts; historical snapshots cannot revive removed links.
        var sources = store.jdbc().queryForList("SELECT source.id FROM dcc_current_file_relation relation "
                + "INNER JOIN dcc_current_file_relation_set current_set ON current_set.tenant_id=relation.tenant_id "
                + "AND current_set.source_master_id=relation.source_master_id "
                + "INNER JOIN dcc_controlled_file source ON source.tenant_id=current_set.tenant_id "
                + "AND source.id=current_set.controlled_file_id AND source.master_id=relation.source_master_id AND source.deleted=0 "
                + "WHERE relation.tenant_id=? AND relation.related_master_id=? AND relation.source_master_id<>? "
                + "AND source.controlled_time IS NOT NULL AND source.status IN ('ACTIVE','CONTROLLED_PENDING_EFFECTIVE') "
                + "ORDER BY source.id", Long.class, tenant, event.masterId(), event.masterId());
        for (Long sourceId : sources) {
            var source = files.selectById(sourceId);
            if (source == null || !Objects.equals(tenant, source.getTenantId())
                    || source.getFileOwnerUserId() == null || source.getFileOwnerUserId() <= 0
                    || source.getFileOwnerSignatureId() == null)
                throw new DccRelationFailure("DCC_RELATED_FILE_SIGNED_COMPILER_MISSING");
            var request = new NotifySendSingleToUserIdempotentReqDTO();
            request.setUserId(source.getFileOwnerUserId());
            request.setTemplateCode(TEMPLATE_CODE);
            request.setBusinessKey("dcc-related-revision:" + tenant + ":" + event.controlledFileId()
                    + ":" + sourceId + ":" + source.getFileOwnerUserId());
            request.setTemplateParams(Map.of("fileNumber", revised.getFileNumber(), "versionNo", revised.getVersionNo(),
                    "relatedFileNumber", source.getFileNumber(), "sourceControlledFileId", String.valueOf(sourceId),
                    "revisedControlledFileId", String.valueOf(revised.getId()),
                    "detailUrl", "/dcc/controlled-file/detail/" + sourceId + "?viewer=1&from=notification"));
            // The platform uses REQUIRED propagation: the message becomes visible with this control commit.
            Long messageId = messages.sendSingleMessageIdempotentlyToAdmin(request);
            if (messageId == null || messageId <= 0)
                throw new DccRelationFailure("DCC_PLATFORM_MESSAGE_ID_MISSING");
        }
    }
}
