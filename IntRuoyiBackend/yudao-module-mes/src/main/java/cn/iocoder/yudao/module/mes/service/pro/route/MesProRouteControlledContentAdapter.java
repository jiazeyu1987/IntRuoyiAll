package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.controlledcontent.ControlledContentVersionRefDO;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentKey;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentLifecycleCoreService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * MES route-version adapter for the platform controlled-content lifecycle.
 */
@Service
public class MesProRouteControlledContentAdapter {

    @Resource
    private ControlledContentLifecycleCoreService lifecycleCoreService;

    public void recordActiveRegistered(MesProRouteVersionDO active, Long actorId, String reason) {
        requireVersion(active, "active");
        if (!Boolean.TRUE.equals(active.getActive())) {
            throw new IllegalArgumentException("active route version must be active");
        }
        lifecycleCoreService.registerActiveRef(routeKey(active), active.getRouteId(), active.getId(),
                active.getVersionNo(), active.getLifecycleStatus(), actorId, reason);
    }

    public void recordCandidateCreated(MesProRouteVersionDO active, MesProRouteVersionDO candidate,
                                       Long actorId, String reason) {
        requireVersion(active, "active");
        requireVersion(candidate, "candidate");
        ControlledContentKey key = routeKey(candidate);
        ControlledContentVersionRefDO activeRef = lifecycleCoreService.getActiveRef(key);
        if (activeRef == null) {
            throw new IllegalStateException("controlled content active ref does not exist for route: "
                    + candidate.getRouteId());
        }
        lifecycleCoreService.createCandidateRef(key, candidate.getRouteId(), candidate.getId(),
                candidate.getVersionNo(), candidate.getLifecycleStatus(), activeRef.getId(), active.getId(),
                actorId, reason);
    }

    public void recordSubmitted(MesProRouteVersionDO candidate, Long actorId, String approvalProcessInstanceId) {
        requireVersion(candidate, "candidate");
        ControlledContentKey key = routeKey(candidate);
        ensureHistoricalDraftCandidateRef(candidate, actorId, key,
                "route version historical draft registered before submission");
        lifecycleCoreService.transitionVersionRef(key, candidate.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW, MesProRouteVersionLifecycleServiceImpl.STATUS_PENDING_APPROVAL,
                ControlledContentTransitionAction.SUBMIT, actorId, "route version submitted",
                approvalProcessInstanceId);
    }

    public void recordApproved(MesProRouteVersionDO candidate, Long actorId, String approvalEventId) {
        requireVersion(candidate, "candidate");
        lifecycleCoreService.transitionVersionRefByDomainEvent(routeKey(candidate), candidate.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW, ControlledContentCanonicalStatus.READY_TO_PUBLISH,
                MesProRouteVersionLifecycleServiceImpl.STATUS_READY_TO_PUBLISH,
                ControlledContentTransitionAction.APPROVE, actorId, "route version approved",
                candidate.getApprovalProcessInstanceId(), approvalEventId);
    }

    public void recordWithdrawn(MesProRouteVersionDO candidate, Long actorId) {
        requireVersion(candidate, "candidate");
        lifecycleCoreService.transitionVersionRef(routeKey(candidate), candidate.getId(),
                ControlledContentCanonicalStatus.DRAFT, MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT,
                ControlledContentTransitionAction.WITHDRAW, actorId, "route version approval withdrawn",
                null);
    }

    public void recordRejected(MesProRouteVersionDO candidate, Long actorId, String reason, String approvalEventId) {
        requireVersion(candidate, "candidate");
        lifecycleCoreService.transitionVersionRefByDomainEvent(routeKey(candidate), candidate.getId(),
                ControlledContentCanonicalStatus.IN_REVIEW, ControlledContentCanonicalStatus.REJECTED,
                MesProRouteVersionLifecycleServiceImpl.STATUS_REJECTED,
                ControlledContentTransitionAction.REJECT, actorId, reason, candidate.getApprovalProcessInstanceId(),
                approvalEventId);
    }

    public void recordCancelled(MesProRouteVersionDO candidate, Long actorId) {
        requireVersion(candidate, "candidate");
        ControlledContentKey key = routeKey(candidate);
        ensureHistoricalDraftCandidateRef(candidate, actorId, key);
        lifecycleCoreService.transitionVersionRef(key, candidate.getId(),
                ControlledContentCanonicalStatus.CANCELLED, MesProRouteVersionLifecycleServiceImpl.STATUS_CANCELLED,
                ControlledContentTransitionAction.CANCEL, actorId, "route version candidate cancelled",
                null);
    }

    public void recordPublished(MesProRouteVersionDO active, MesProRouteVersionDO candidate, Long actorId) {
        requireVersion(active, "active");
        requireVersion(candidate, "candidate");
        lifecycleCoreService.publishVersionRefs(routeKey(candidate), active.getId(), candidate.getId(),
                MesProRouteVersionLifecycleServiceImpl.STATUS_SUPERSEDED,
                MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE, actorId, "route version published");
    }

    private ControlledContentKey routeKey(MesProRouteVersionDO version) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("tenant is required for route controlled content lifecycle");
        }
        if (version.getRouteId() == null) {
            throw new IllegalArgumentException("routeId is required for route controlled content lifecycle");
        }
        return ControlledContentKey.of(tenantId, ControlledContentType.MES_ROUTE, String.valueOf(version.getRouteId()));
    }

    private void ensureHistoricalDraftCandidateRef(MesProRouteVersionDO candidate, Long actorId,
                                                   ControlledContentKey key) {
        ensureHistoricalDraftCandidateRef(candidate, actorId, key,
                "route version historical draft registered before cancellation");
    }

    private void ensureHistoricalDraftCandidateRef(MesProRouteVersionDO candidate, Long actorId,
                                                   ControlledContentKey key, String reason) {
        ControlledContentVersionRefDO existingRef = lifecycleCoreService.getVersionRef(key, candidate.getId());
        if (existingRef != null) {
            return;
        }
        if (!MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT.equals(candidate.getLifecycleStatus())) {
            throw new IllegalStateException("controlled content ref does not exist for non-draft route candidate: "
                    + candidate.getRouteId() + "/" + candidate.getId() + "/" + candidate.getLifecycleStatus());
        }
        ControlledContentVersionRefDO activeRef = lifecycleCoreService.getActiveRef(key);
        if (activeRef == null) {
            throw new IllegalStateException("controlled content active ref does not exist for route: "
                    + candidate.getRouteId());
        }
        lifecycleCoreService.createCandidateRef(key, candidate.getRouteId(), candidate.getId(),
                candidate.getVersionNo(), candidate.getLifecycleStatus(), activeRef.getId(),
                candidate.getSourceRouteVersionId(), actorId, reason);
    }

    private void requireVersion(MesProRouteVersionDO version, String role) {
        if (version == null || version.getId() == null) {
            throw new IllegalArgumentException(role + " route version is required");
        }
    }
}
