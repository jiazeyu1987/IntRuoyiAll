package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchActiveOrderDetailService;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc.MesPqcReleaseOrderDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Uses the existing business-detail authorization before reading any correction source. */
@Service
@RequiredArgsConstructor
public class MesActiveOrderCorrectionEvidenceService {
    private final MesTeamLeaderActiveOrderDetailService team;
    private final MesPqcReleaseOrderDetailService pqc;
    private final MesProEdhrBatchActiveOrderDetailService batch;
    private final MesProProcessPoolEventMapper events;
    private final MesProProcessPoolEventRevisionMapper revisions;
    private final MesPqcInspectionTaskMapper pqcTasks;
    private final MesProductionSubmissionReadBinding productionBinding;
    private final MesCorrectionSignatureEvidenceReader reader;

    @Transactional(readOnly = true)
    public Timeline getTeam(Long actor, Long activeOrderId) {
        positive(actor); positive(activeOrderId);
        return read(team.getDetail(actor, activeOrderId));
    }
    @Transactional(readOnly = true)
    public Timeline getPqc(Long actor, Long applicationId) {
        positive(actor); positive(applicationId);
        return read(pqc.get(actor, applicationId).detail());
    }
    @Transactional(readOnly = true)
    public Timeline getBatch(Long batchExecutionId, Long activeOrderId) {
        check((batchExecutionId == null) != (activeOrderId == null), "批次补正历史必须指定唯一业务查询身份");
        if (activeOrderId != null) { positive(activeOrderId); return read(batch.getDetailByActiveOrderId(activeOrderId)); }
        positive(batchExecutionId); return read(batch.getDetail(batchExecutionId));
    }
    private Timeline read(MesTeamLeaderActiveOrderDetail detail) {
        var sources = eventNames(detail);
        List<Correction> rows = new ArrayList<>();
        for (var source : sources.entrySet()) {
            for (var revision : revisions.selectListByEventId(source.getKey())) {
                var verified = verifyRelated(detail, revision);
                var event = events.selectById(source.getKey());
                rows.add(new Correction(revision.getId(), revision.getEventId(), event.getEventType(), source.getValue(),
                        revision.getServerRevisionTime(), revision.getChangeReason(), verified.signerName(),
                        verified.evidence().id(), verified.evidence().signedAt(), verified.verification().verificationStatus(),
                        verified.changes().stream().map(d -> new Change(d.getFieldCode(), d.getFieldName(), d.getBeforeValue(), d.getAfterValue())).toList()));
            }
        }
        rows.sort(Comparator.comparing(Correction::revisedAt).thenComparing(Correction::revisionId).reversed());
        return new Timeline(detail.getActiveOrderId(), List.copyOf(rows));
    }
    public MesCorrectionSignatureEvidenceReader.Verified verifyRelated(MesTeamLeaderActiveOrderDetail detail,
                                                                       MesProProcessPoolEventRevisionDO revision) {
        check(revision != null && eventNames(detail).containsKey(revision.getEventId()), "补正修订不属于当前正式详情");
        var event = events.selectById(revision.getEventId());
        check(event != null && Objects.equals(TenantContextHolder.getRequiredTenantId(), event.getTenantId()), "补正原事件正式来源缺失");
        if ("PRODUCTION_SUBMIT".equals(event.getEventType())) {
            productionBinding.require(event, detail.getActiveOrderId());
        } else {
            check("PQC_INSPECTION".equals(event.getEventType()), "补正原事件业务类型无效");
            var task = pqcTasks.selectById(event.getFeedbackSourceId());
            check(task != null && Objects.equals(TenantContextHolder.getRequiredTenantId(), task.getTenantId())
                    && Objects.equals(task.getActiveOrderId(), detail.getActiveOrderId())
                    && Objects.equals(task.getSubmittedEventId(), event.getId())
                    && Objects.equals(task.getWorkOrderId(), revision.getWorkOrderId())
                    && Objects.equals(task.getRouteId(), revision.getRouteId())
                    && Objects.equals(task.getRouteProcessId(), revision.getRouteProcessId())
                    && Objects.equals(task.getProcessId(), revision.getProcessId()), "PQC补正任务与当前正式详情不一致");
        }
        return reader.read(event, revision);
    }
    static Map<Long, String> eventNames(MesTeamLeaderActiveOrderDetail detail) {
        check(detail != null && detail.getActiveOrderId() != null && detail.getProcesses() != null, "补正历史详情身份缺失");
        Map<Long, String> ids = new LinkedHashMap<>();
        for (var process : detail.getProcesses()) {
            for (var submission : process.getSubmissions()) ids.put(submission.getEventId(), process.getProcessName());
            for (var submission : process.getPqcSubmissions()) {
                submission.getSubmittedEventIds().forEach(id -> ids.put(id, process.getProcessName()));
                submission.getProductionEventIds().forEach(id -> ids.put(id, process.getProcessName()));
            }
        }
        check(ids.keySet().stream().allMatch(id -> id != null && id > 0), "补正历史原事件身份缺失");
        return ids;
    }
    private static void positive(Long id) { check(id != null && id > 0, "补正历史查询编号无效"); }
    private static void check(boolean valid, String reason) { if (!valid) throw new IllegalStateException("CORRECTION_HISTORY_INVALID: " + reason); }
    public record Timeline(Long activeOrderId, List<Correction> corrections) {}
    public record Correction(Long revisionId, Long eventId, String eventType, String processName, LocalDateTime revisedAt,
                             String reason, String signerName, Long signatureId, LocalDateTime signedAt,
                             String verificationStatus, List<Change> changes) {}
    public record Change(String fieldCode, String fieldName, String beforeValue, String afterValue) {}
}
