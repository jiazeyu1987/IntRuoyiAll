package cn.iocoder.yudao.module.dcc.service.filepreview;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.filepreview.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFilePreviewViewSessionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileAccessEventDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFilePreviewViewSessionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileAccessEventMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileWatermarkTraceMapper;
import cn.iocoder.yudao.module.dcc.service.file.*;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenExpectedContext;
import cn.iocoder.yudao.module.dcc.service.token.DccViewerTokenService;
import cn.iocoder.yudao.module.infra.service.file.access.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.time.*;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.*;

/** Server-confirmed viewer intervals, separate from metadata issuance and training qualification. */
@Service
public class DccOnlineFilePreviewReadingSessionService {
    public static final int HEARTBEAT_INTERVAL_SECONDS = 10;
    public static final int MAX_HEARTBEAT_GAP_SECONDS = 15;
    private static final String ONLINE = "ONLINE_FILE";
    private static final String CONTROLLED = "CONTROLLED_FILE";
    private Clock clock = Clock.systemUTC();

    @Resource private DccControlledFilePreviewViewSessionMapper sessionMapper;
    @Resource private DccControlledFileAccessEventMapper accessEventMapper;
    @Resource private DccControlledFileWatermarkTraceMapper watermarkTraceMapper;
    @Resource private DccViewerTokenService viewerTokenService;
    @Resource private BusinessFileAccessService businessFileAccessService;
    @Resource private DccControlledFileQueryService controlledFileQueryService;

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public DccPreviewReadingSessionRespVO start(Long user, Long file, DccPreviewReadingSessionStartReqVO req, DccRequestAuditContext audit) {
        return startInternal(user, file, req, audit, null);
    }
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public DccPreviewReadingSessionRespVO startControlled(Long user, Long file, DccControlledFileBodyReadContext context,
            DccPreviewReadingSessionStartReqVO req, DccRequestAuditContext audit) {
        return startInternal(user, file, req, audit, Objects.requireNonNull(context, "body context"));
    }
    @Transactional(rollbackFor = Exception.class)
    public DccPreviewReadingSessionRespVO heartbeat(Long user, Long file, DccPreviewReadingSessionHeartbeatReqVO req, DccRequestAuditContext audit) {
        return advance(user, file, req.sessionId(), req, audit, null, false);
    }
    @Transactional(rollbackFor = Exception.class)
    public DccPreviewReadingSessionRespVO heartbeatControlled(Long user, Long file, DccControlledFileBodyReadContext context,
            DccPreviewReadingSessionHeartbeatReqVO req, DccRequestAuditContext audit) {
        return advance(user, file, req.sessionId(), req, audit, Objects.requireNonNull(context, "body context"), false);
    }
    @Transactional(rollbackFor = Exception.class)
    public DccPreviewReadingSessionRespVO stop(Long user, Long file, DccPreviewReadingSessionStopReqVO req, DccRequestAuditContext audit) {
        return advance(user, file, req.sessionId(), req, audit, null, true);
    }
    @Transactional(rollbackFor = Exception.class)
    public DccPreviewReadingSessionRespVO stopControlled(Long user, Long file, DccControlledFileBodyReadContext context,
            DccPreviewReadingSessionStopReqVO req, DccRequestAuditContext audit) {
        return advance(user, file, req.sessionId(), req, audit, Objects.requireNonNull(context, "body context"), true);
    }

    @Transactional(readOnly = true)
    public DccPreviewReadingSummaryRespVO summary(Long user, Long file, DccPreviewReadingSessionEvidence req, DccRequestAuditContext audit) {
        return summaryInternal(user, file, req, audit, null);
    }
    @Transactional(readOnly = true)
    public DccPreviewReadingSummaryRespVO summaryControlled(Long user, Long file, DccControlledFileBodyReadContext context,
            DccPreviewReadingSessionEvidence req, DccRequestAuditContext audit) {
        return summaryInternal(user, file, req, audit, Objects.requireNonNull(context, "body context"));
    }
    private DccPreviewReadingSummaryRespVO summaryInternal(Long user, Long file, DccPreviewReadingSessionEvidence req,
            DccRequestAuditContext audit, DccControlledFileBodyReadContext context) {
        var event = authorize(user, file, req, audit, context);
        var sum = sessionMapper.selectSummary(event.getTenantId(), resource(context), file, event.getFileVersionNo(), user);
        if (sum == null || sum.getAccumulatedSeconds() == null || sum.getOpenedSessionCount() == null
                || sum.getAccumulatedSeconds() < 0 || sum.getOpenedSessionCount() < 0)
            throw new IllegalStateException("Reading summary did not return valid server facts");
        return new DccPreviewReadingSummaryRespVO(iso(sum.getFirstOpenedAt()), iso(sum.getLastOpenedAt()),
                sum.getAccumulatedSeconds(), sum.getOpenedSessionCount());
    }

    private DccPreviewReadingSessionRespVO startInternal(Long user, Long file, DccPreviewReadingSessionStartReqVO req,
            DccRequestAuditContext audit, DccControlledFileBodyReadContext context) {
        text(req.clientSessionId(), 64);
        Long tenant = validateEvidence(user, file, req, audit);
        // Establish the new transaction's RR snapshot only after the resource lock is acquired.
        lock(file, tenant, context);
        var event = authorize(user, file, req, audit, context);
        var existing = sessionMapper.selectClientAfterResourceLock(event.getId(), user, event.getTenantId(), req.clientSessionId());
        if (existing != null) {
            requireSession(existing, event, req, context);
            return response(existing); // Retry must never reopen a stopped client generation.
        }
        var now = now();
        for (var previous : sessionMapper.selectActiveAfterResourceLock(file, resource(context), user, event.getTenantId())) {
            // A lost stop/unload cannot justify adding unconfirmed tail seconds.
            previous.setEndedAt(previous.getLastHeartbeatAt());
            previous.setEndedReason("REPLACED");
            persist(previous);
        }
        var session = DccControlledFilePreviewViewSessionDO.builder()
                .tenantId(event.getTenantId()).resourceType(resource(context)).controlledFileId(file)
                .fileVersionNo(event.getFileVersionNo()).accessEventId(event.getId())
                .accessEventCode(event.getAccessEventCode()).watermarkTraceCode(req.watermarkTraceCode())
                .userId(user).viewerTokenId(req.viewerTokenId()).viewerTokenNonce(req.viewerTokenNonce())
                .clientSessionId(req.clientSessionId()).bodyReadContext(wire(context))
                .openedAt(event.getOccurredAt()).startedAt(now).lastHeartbeatAt(now).accumulatedSeconds(0)
                .requestId(audit.requireRequestId("reading start")).sourceIp(audit.sourceIp()).userAgent(audit.userAgent()).build();
        if (sessionMapper.insert(session) != 1 || session.getId() == null || session.getId() <= 0)
            throw new IllegalStateException("Reading session was not persisted");
        return response(session);
    }

    private DccPreviewReadingSessionRespVO advance(Long user, Long file, Long id, DccPreviewReadingSessionEvidence req,
            DccRequestAuditContext audit, DccControlledFileBodyReadContext context, boolean stop) {
        if (id == null || id <= 0) throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        var event = authorize(user, file, req, audit, context, stop && context != null);
        lock(file, event.getTenantId(), context);
        event = authorize(user, file, req, audit, context, stop && context != null);
        var session = sessionMapper.selectOwnedByIdForUpdate(id, event.getTenantId());
        requireSession(session, event, req, context);
        if (session.getEndedAt() != null) {
            if (stop) return response(session);
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        }
        var now = now();
        if (now.isBefore(session.getLastHeartbeatAt())) throw new IllegalStateException("Reading server clock moved backwards");
        Duration gap = Duration.between(session.getLastHeartbeatAt(), now);
        // Gaps beyond the liveness window are unconfirmed, including hidden/suspended browsers.
        long increment = gap.compareTo(Duration.ofSeconds(MAX_HEARTBEAT_GAP_SECONDS)) <= 0 ? gap.getSeconds() : 0;
        session.setAccumulatedSeconds(Math.toIntExact(Math.addExact(session.getAccumulatedSeconds().longValue(), increment)));
        session.setLastHeartbeatAt(now);
        if (stop) {
            session.setEndedAt(now);
            session.setEndedReason("STOPPED");
        }
        persist(session);
        return response(session);
    }

    private DccControlledFileAccessEventDO authorize(Long user, Long file, DccPreviewReadingSessionEvidence req,
            DccRequestAuditContext audit, DccControlledFileBodyReadContext context) {
        return authorize(user, file, req, audit, context, false);
    }

    private DccControlledFileAccessEventDO authorize(Long user, Long file, DccPreviewReadingSessionEvidence req,
            DccRequestAuditContext audit, DccControlledFileBodyReadContext context, boolean closing) {
        Long tenant = validateEvidence(user, file, req, audit);
        var event = accessEventMapper.selectOne(new LambdaQueryWrapper<DccControlledFileAccessEventDO>()
                .eq(DccControlledFileAccessEventDO::getTenantId, tenant)
                .eq(DccControlledFileAccessEventDO::getAccessEventCode, req.accessEventCode()));
        if (event == null || !Objects.equals(event.getTenantId(), tenant)
                || !Objects.equals(event.getControlledFileId(), file) || !Objects.equals(event.getUserId(), user)
                || !Objects.equals(event.getAccessEventCode(), req.accessEventCode())
                || !"PREVIEW".equals(event.getAccessType()) || !"SUCCESS".equals(event.getResult())
                || event.getId() == null || event.getOccurredAt() == null)
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
        if (context != null) {
            context.requireFile(file, event.getFileVersionNo());
            if (!Objects.equals(context.purpose(), event.getPurpose()))
                throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
            if (closing) {
                if (!Objects.equals(context.bpmRound(), event.getProcessInstanceId()))
                    throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
                requireWatermarkEvidence(user, file, req, event);
                viewerTokenService.verifyReadingSessionClose(req.viewerToken(), new DccViewerTokenExpectedContext(tenant, user, file,
                        event.getFileVersionNo(), event.getId(), event.getPurpose(), 900L, req.viewerTokenNonce(), req.viewerTokenId()));
            } else {
                controlledFileQueryService.assertPreviewReadingAccess(user, file, context, req.viewerToken(),
                        req.accessEventCode(), req.watermarkTraceCode(), req.viewerTokenId(), req.viewerTokenNonce(), audit);
            }
        } else {
            if (!("INFRA_FILE:" + file).equals(event.getFileVersionNo()) || !"ONLINE_FILE_PREVIEW".equals(event.getPurpose()))
                throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
            requireWatermarkEvidence(user, file, req, event);
            viewerTokenService.verify(req.viewerToken(), new DccViewerTokenExpectedContext(tenant, user, file,
                    event.getFileVersionNo(), event.getId(), event.getPurpose(), 900L, req.viewerTokenNonce(), req.viewerTokenId()));
            try {
                businessFileAccessService.assertAllowed(new BusinessFileAccessRequest(BusinessFileAccessOperation.PREVIEW,
                        file, tenant, user, null, audit.requireRequestId("reading lifecycle"), null, audit.sourceIp(), audit.userAgent()));
            } catch (BusinessFileAccessDeniedException denied) {
                throw exception(CONTROLLED_FILE_ACCESS_DENIED);
            }
        }
        return event;
    }

    private Long validateEvidence(Long user, Long file, DccPreviewReadingSessionEvidence req, DccRequestAuditContext audit) {
        if (TenantContextHolder.isIgnore() || user == null || user <= 0 || file == null || file <= 0)
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        Long tenant = TenantContextHolder.getRequiredTenantId();
        Objects.requireNonNull(audit, "audit context").requireRequestId("reading lifecycle");
        Objects.requireNonNull(req, "preview evidence");
        text(req.viewerToken(), 8192); text(req.accessEventCode(), 64); text(req.watermarkTraceCode(), 64);
        text(req.viewerTokenId(), 64); text(req.viewerTokenNonce(), 64);
        return tenant;
    }

    private void requireWatermarkEvidence(Long user, Long file, DccPreviewReadingSessionEvidence req,
            DccControlledFileAccessEventDO event) {
        var trace = watermarkTraceMapper.selectOne(new LambdaQueryWrapper<cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO>()
                .eq(cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileWatermarkTraceDO::getTraceCode, req.watermarkTraceCode()));
        if (trace == null || !Objects.equals(trace.getTraceCode(), req.watermarkTraceCode())
                || !Objects.equals(trace.getAccessEventId(), event.getId())
                || !Objects.equals(trace.getAccessEventCode(), event.getAccessEventCode())
                || !Objects.equals(trace.getControlledFileId(), file) || !Objects.equals(trace.getUserId(), user)
                || !Objects.equals(trace.getFileVersionNo(), event.getFileVersionNo()))
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
    }

    private void requireSession(DccControlledFilePreviewViewSessionDO session, DccControlledFileAccessEventDO event,
            DccPreviewReadingSessionEvidence req, DccControlledFileBodyReadContext context) {
        if (session == null || !Objects.equals(session.getTenantId(), event.getTenantId())
                || !Objects.equals(session.getControlledFileId(), event.getControlledFileId())
                || !Objects.equals(session.getFileVersionNo(), event.getFileVersionNo())
                || !Objects.equals(session.getUserId(), event.getUserId())
                || !Objects.equals(session.getAccessEventId(), event.getId())
                || !Objects.equals(session.getAccessEventCode(), req.accessEventCode())
                || !Objects.equals(session.getWatermarkTraceCode(), req.watermarkTraceCode())
                || !Objects.equals(session.getViewerTokenId(), req.viewerTokenId())
                || !Objects.equals(session.getViewerTokenNonce(), req.viewerTokenNonce())
                || !Objects.equals(session.getResourceType(), resource(context))
                || !Objects.equals(session.getBodyReadContext(), wire(context))
                || session.getLastHeartbeatAt() == null || session.getAccumulatedSeconds() == null
                || session.getAccumulatedSeconds() < 0)
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_CONTEXT_MISMATCH);
    }

    private void lock(Long file, Long tenant, DccControlledFileBodyReadContext context) {
        Long locked = context == null ? sessionMapper.lockInfraFile(file) : sessionMapper.lockControlledFile(file, tenant);
        if (!Objects.equals(file, locked)) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
    }
    private void persist(DccControlledFilePreviewViewSessionDO session) {
        if (sessionMapper.updateById(session) != 1) throw new IllegalStateException("Reading session update did not persist one row");
    }
    private void text(String value, int max) {
        if (StrUtil.isBlank(value) || value.length() > max || !value.equals(value.trim()))
            throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
    }
    private String resource(DccControlledFileBodyReadContext context) { return context == null ? ONLINE : CONTROLLED; }
    private String wire(DccControlledFileBodyReadContext context) { return context == null ? null : context.wire(); }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private String iso(LocalDateTime time) { return time == null ? null : time.atOffset(ZoneOffset.UTC).toString(); }
    private DccPreviewReadingSessionRespVO response(DccControlledFilePreviewViewSessionDO row) {
        return new DccPreviewReadingSessionRespVO(row.getId(), iso(row.getOpenedAt()), iso(row.getStartedAt()),
                iso(row.getLastHeartbeatAt()), iso(row.getEndedAt()), row.getAccumulatedSeconds(),
                HEARTBEAT_INTERVAL_SECONDS, MAX_HEARTBEAT_GAP_SECONDS);
    }
}
