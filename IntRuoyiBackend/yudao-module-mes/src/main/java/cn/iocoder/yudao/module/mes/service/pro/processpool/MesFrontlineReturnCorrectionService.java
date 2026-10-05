package cn.iocoder.yudao.module.mes.service.pro.processpool;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation.MesQaInspectionRegulationProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSignatureEvidenceService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewService;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesReportAllocationReleaseStateService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesSubmissionSignatureIdentityReader;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;

/** A returned form remains owned by its original formal SYSTEM_USER signer, not a leader scope. */
@Service
@RequiredArgsConstructor
public class MesFrontlineReturnCorrectionService {
    private final MesProProcessPoolEventMapper events;
    private final MesProcessPoolActiveOrderMapper activeOrders;
    private final MesProcessPoolSubmissionReviewMapper reviews;
    private final MesProProcessPoolEventRevisionMapper revisions;
    private final MesPqcInspectionTaskMapper tasks;
    private final MesPqcInspectionPieceDetailMapper pieces;
    private final ElectronicSignatureQueryService signatures;
    private final MesProductionSignatureEvidenceService productionEvidence;
    private final MesSubmissionSignatureIdentityReader pqcReader;
    private final MesProEdhrNonconformanceReviewService nonconformance;
    private final MesReportAllocationReleaseStateService releaseState;
    private final MesActiveOrderHandoffService handoff;
    @Resource private MesProWorkOrderMapper workOrders;
    @Resource private MesProProcessPoolEventRevisionDiffMapper revisionDiffs;
    @Resource private MesProProcessMapper processes;
    @Resource private MesQaInspectionRegulationProcessMapper qaProcesses;
    @Resource private GxpAuditService gxpAuditService;

    public record ReturnContext(MesProProcessPoolEventDO event, MesProcessPoolSubmissionReviewDO review,
                                Long activeOrderId, Long expectedRevisionId, String returnTaskId) { }
    public record ReturnRow(String returnTaskId, Long eventId, Long activeOrderId, Long rejectedReviewId, Long expectedRevisionId,
                            String workOrderCode, String formName, String rejectionReason, LocalDateTime rejectedAt) { }
    public record ProductionFields(BigDecimal outputQuantity,
            List<MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand> materialDetails,
            List<MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand> lossDetails,
            List<MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand> deviceParameterReadings) { }
    public record PqcItem(String itemCode, String itemName, String resultType, String standardText,
                          BigDecimal lowerLimit, BigDecimal upperLimit, String unit, Integer precision,
                          Long selectedEquipmentId, String selectedEquipmentNumber, List<String> sampleValues) { }
    public record PqcFields(Integer actualInspectionQuantity, Integer scrapQuantity, List<PqcItem> items) { }
    public record ReturnDetail(ReturnRow row, ProductionFields production, PqcFields pqc) { }
    public record ChangedField(String fieldName,String beforeValue,String afterValue) { }
    public record CorrectionResult(Long eventId,Long revisionId,List<ChangedField> changes) { }

    @Transactional(propagation = Propagation.MANDATORY)
    public CorrectionResult complete(ReturnContext context,Long revisionId,Long actor) {
        handoff.completeReturnAndScheduleReview(context.event().getId(),context.review().getId(),revisionId,actor);
        var saved=revisions.selectById(revisionId);
        require(saved != null && Objects.equals(saved.getTenantId(),context.event().getTenantId())
                && Objects.equals(saved.getEventId(),context.event().getId())
                && Objects.equals(saved.getModifiedByUserId(),actor)
                && Objects.equals(saved.getRevisionSignatureUserId(),actor),"ownReturn.savedRevision");
        var diff=revisionDiffs.selectListByRevisionIds(List.of(revisionId));
        require(diff != null && !diff.isEmpty() && diff.stream().allMatch(d->
                Objects.equals(d.getTenantId(),saved.getTenantId()) && Objects.equals(d.getEventId(),saved.getEventId())
                && Objects.equals(d.getRevisionId(),revisionId)),"ownReturn.savedDiff");
        return new CorrectionResult(saved.getEventId(),revisionId,
                diff.stream().map(d->new ChangedField(d.getFieldName(),d.getBeforeValue(),d.getAfterValue())).toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public List<ReturnRow> listOwnReturned(Long actor, String leaderType) {
        requireLeaderType(leaderType);
        // Review hooks may already hold event before active; serialize reads with the same ledger first.
        gxpAuditService.acquireLedgerLock();
        List<ReturnRow> result = new ArrayList<>();
        for (var pending : handoff.listOwnReturnTasks(actor, leaderType)) {
            require("PROCESS_POOL_EVENT".equals(pending.getSourceType()), "returnTask.source");
            Long revision = latestRevisionId(pending.getSourceId());
            ReturnContext context = requireOwnReturned(pending.getSourceId(), pending.getActiveOrderId(),
                    pending.getRoundId(), revision, actor, leaderType);
            result.add(toRow(context));
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public ReturnDetail getOwnReturned(Long eventId, Long activeOrderId, Long rejectedReviewId,
                                       Long expectedRevisionId, Long actor, String leaderType) {
        gxpAuditService.acquireLedgerLock();
        ReturnContext context=requireOwnReturned(eventId,activeOrderId,rejectedReviewId,expectedRevisionId,actor,leaderType);
        return new ReturnDetail(toRow(context), "PRODUCTION".equals(leaderType) ? productionFields(context.event()) : null,
                "PQC".equals(leaderType) ? pqcFields(context.event()) : null);
    }

    /** Called under the correction transaction; all ownership/round facts are locked before signing. */
    @Transactional(propagation = Propagation.MANDATORY)
    public ReturnContext requireOwnReturned(Long eventId, Long requestedCycle, Long rejectedReviewId,
                                           Long expectedRevisionId, Long actor, String leaderType) {
        requireLeaderType(leaderType);
        require(positive(actor) && positive(eventId) && positive(requestedCycle) && positive(rejectedReviewId)
                && expectedRevisionId != null && expectedRevisionId >= 0, "ownReturn.request");
        Long tenant=TenantContextHolder.getRequiredTenantId();
        // Use the same active -> event order as formal PQC correction, including detail reads.
        var active=activeOrders.selectByIdForUpdate(requestedCycle);
        MesProProcessPoolEventDO event=events.selectByIdForUpdate(eventId);
        require(event != null && Objects.equals(event.getId(),eventId) && Objects.equals(event.getTenantId(),tenant)
                && Objects.equals(event.getSignatureUserId(),actor)
                && !Boolean.TRUE.equals(event.getSimulated()) && event.getSimulationStage()==null
                && event.getSimulationRunId()==null
                && ("PRODUCTION".equals(leaderType) ? "PRODUCTION_SUBMIT" : "PQC_INSPECTION").equals(event.getEventType()),
                "ownReturn.originalSigner");
        JsonNode payload=object(event.getRawPayload(),"ownReturn.payload");
        require(!payload.path("simulated").asBoolean(false), "ownReturn.formalSource");
        Long cycle=longValue(payload.get("activeOrderId"),"ownReturn.activeOrderId");
        require(Objects.equals(cycle,requestedCycle),"ownReturn.cycleChanged");
        require(active != null && Objects.equals(active.getId(),cycle) && Objects.equals(active.getTenantId(),tenant)
                && Objects.equals(active.getWorkOrderId(),event.getWorkOrderId())
                && Objects.equals(active.getRouteId(),event.getRouteId())
                && "ACTIVE".equals(active.getActiveStatus()) && "ACTIVE".equals(active.getBusinessStatus())
                && active.getReleaseDecisionId()==null && active.getReleasedAt()==null, "ownReturn.activeCycle");
        verifyOriginalSigner(event,payload,actor,cycle,leaderType);
        var review=reviews.selectLatestByEventIdForUpdate(eventId);
        require(review != null && Objects.equals(review.getId(),rejectedReviewId)
                && Objects.equals(review.getTenantId(),tenant) && Objects.equals(review.getEventId(),eventId)
                && leaderType.equals(review.getLeaderType()) && "REJECTED".equals(review.getReviewStatus()),
                "ownReturn.latestRejectedRound");
        var lockedRevisions=revisions.selectListByEventIdForUpdate(eventId);
        require(lockedRevisions != null && Objects.equals(
                lockedRevisions.isEmpty() ? 0L : lockedRevisions.get(0).getId(),expectedRevisionId),"ownReturn.staleRevision");
        nonconformance.ensureWorkOrderNotFrozen(event.getWorkOrderId(),"本人退回更正");
        require(!releaseState.findReleasedActiveOrderIdsForUpdate(List.of(cycle)).contains(cycle)
                && !releaseState.isReleaseApplicationLockedForUpdate(cycle),"ownReturn.releaseLocked");
        var pending=handoff.findOwnReturn(eventId,rejectedReviewId,actor);
        require(pending != null && positive(pending.getId()) && Objects.equals(pending.getTenantId(),tenant)
                && Objects.equals(pending.getActiveOrderId(),cycle)
                && Objects.equals(pending.getWorkOrderId(),event.getWorkOrderId()),"ownReturn.pendingTask");
        return new ReturnContext(event,review,cycle,expectedRevisionId,pending.getId().toString());
    }

    private void verifyOriginalSigner(MesProProcessPoolEventDO event,JsonNode payload,Long actor,Long cycle,String type) {
        var evidence=signatures.getById(event.getSignatureId());
        require(evidence != null && Objects.equals(evidence.id(),event.getSignatureId())
                && Objects.equals(evidence.actorId(),actor) && "MES".equals(evidence.moduleCode())
                && "MES_BATCH_RECORD".equals(evidence.subjectType()) && evidence.signedAt()!=null
                && "VALID".equals(evidence.verificationStatus()), "ownReturn.signatureSource");
        JsonNode identity=object(evidence.canonicalContentJson(),"ownReturn.signatureContent").get("signatureIdentity");
        require(identity != null && identity.isObject() && "SYSTEM_USER".equals(identity.path("domain").asText())
                && Objects.equals(longValue(identity.get("tenantId"),"ownReturn.identityTenant"),event.getTenantId())
                && Objects.equals(longValue(identity.get("signerId"),"ownReturn.identitySigner"),actor)
                && Objects.equals(longValue(identity.get("operatorId"),"ownReturn.identityOperator"),event.getDeviceAccountId()),
                "ownReturn.systemUserIdentity");
        if ("PRODUCTION".equals(type)) {
            require("SYSTEM_USER".equals(payload.path("signatureIdentityDomain").asText())
                    && "PRODUCTION_SUBMIT".equals(evidence.actionCode())
                    && productionEvidence.isValidForEvent(event), "ownReturn.productionSignature");
        } else {
            require("PQC_SUBMIT".equals(evidence.actionCode()),"ownReturn.pqcSignature");
            var task=tasks.selectByIdForUpdate(event.getFeedbackSourceId());
            require(task != null && "MES_PQC_INSPECTION_TASK".equals(event.getFeedbackSourceType())
                    && Objects.equals(task.getTenantId(),event.getTenantId())
                    && Objects.equals(task.getActiveOrderId(),cycle) && Objects.equals(task.getWorkOrderId(),event.getWorkOrderId())
                    && Objects.equals(task.getRouteId(),event.getRouteId())
                    && Objects.equals(task.getSubmittedEventId(),event.getId())
                    && Objects.equals(task.getQaProcessId(),event.getQaProcessId()),"ownReturn.pqcTaskIdentity");
            require(pqcReader.read(event.getSignatureId(),event.getId(),cycle,"PQC_SUBMIT") != null,
                    "ownReturn.pqcVerifiedSignature");
        }
    }

    private Long latestRevisionId(Long eventId) {
        var current=revisions.selectListByEventId(eventId);
        require(current != null,"ownReturn.revisions");
        return current.isEmpty() ? 0L : current.get(0).getId();
    }

    private ReturnRow toRow(ReturnContext c) {
        var order=workOrders.selectById(c.event().getWorkOrderId());
        require(order != null && Objects.equals(order.getTenantId(),c.event().getTenantId())
                && order.getCode()!=null && !order.getCode().isBlank(),"ownReturn.workOrderCode");
        String name;
        if("PRODUCTION_SUBMIT".equals(c.event().getEventType())) {
            var process=processes.selectById(c.event().getProcessId());
            require(process!=null,"ownReturn.productionProcess");name=process.getName();
        } else {
            var process=qaProcesses.selectById(c.event().getQaProcessId());
            require(process!=null && Objects.equals(process.getTenantId(),c.event().getTenantId()),"ownReturn.qaProcess");
            name=process.getProcessName();
        }
        require(name!=null&&!name.isBlank(),"ownReturn.formName");
        return new ReturnRow(c.returnTaskId(),c.event().getId(),c.activeOrderId(),c.review().getId(),c.expectedRevisionId(),
                order.getCode(),name,c.review().getReviewRemark(),c.review().getReviewedAt());
    }

    private ProductionFields productionFields(MesProProcessPoolEventDO event) {
        JsonNode p=object(event.getRawPayload(),"ownReturn.payload");
        require(p.path("outputQuantity").isNumber() && p.path("lossDetails").isArray(),"ownReturn.productionFields");
        return new ProductionFields(p.get("outputQuantity").decimalValue(),
                array(p,"materialDetails",MesProcessPoolProductionReportCorrectionCommand.MaterialDetailCommand.class,false),
                array(p,"lossDetails",MesProcessPoolProductionReportCorrectionCommand.LossDetailCommand.class,true),
                array(p,"deviceParameterReadings",MesProcessPoolProductionReportCorrectionCommand.DeviceParameterReadingCommand.class,false));
    }

    private PqcFields pqcFields(MesProProcessPoolEventDO event) {
        var task=tasks.selectByIdForUpdate(event.getFeedbackSourceId());
        JsonNode p=object(event.getRawPayload(),"ownReturn.payload");
        require(task != null && task.getActualInspectionQuantity()!=null && p.path("scrapQuantity").isIntegralNumber(),
                "ownReturn.pqcFields");
        var detail=pieces.selectListByTaskIdForUpdate(task.getId());
        require(detail != null && !detail.isEmpty(),"ownReturn.pqcPieces");
        Map<String,List<cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO>> byItem=new LinkedHashMap<>();
        for(var d:detail){
            require(Objects.equals(d.getTenantId(),event.getTenantId()) && Objects.equals(d.getTaskId(),task.getId())
                    && d.getItemCode()!=null && d.getSampleNo()!=null && d.getMeasuredValue()!=null,"ownReturn.pqcPieces");
            byItem.computeIfAbsent(d.getItemCode(),k->new ArrayList<>()).add(d);
        }
        List<PqcItem> result=new ArrayList<>();
        for(var entry:byItem.entrySet()){
            var item=entry.getValue();item.sort(Comparator.comparing(d->d.getSampleNo()));
            var first=item.get(0);
            require(item.size()==task.getActualInspectionQuantity(),"ownReturn.pqcPieceCount");
            for(int i=0;i<item.size();i++)require(item.get(i).getSampleNo()==i+1,"ownReturn.pqcPieceOrder");
            result.add(new PqcItem(first.getItemCode(),first.getItemName(),first.getResultType(),first.getStandardText(),
                    first.getStandardLowerLimit(),first.getStandardUpperLimit(),first.getStandardUnit(),first.getStandardPrecision(),
                    first.getSelectedEquipmentId(),first.getSelectedEquipmentNumber(),item.stream().map(d->d.getMeasuredValue()).toList()));
        }
        return new PqcFields(task.getActualInspectionQuantity(),p.get("scrapQuantity").intValue(),result);
    }

    private static <T> List<T> array(JsonNode p,String key,Class<T> type,boolean required) {
        JsonNode value=p.get(key);
        if(value==null && !required)return List.of();
        require(value != null && value.isArray(),"ownReturn."+key);
        return JsonUtils.parseArray(value.toString(),type);
    }
    private static JsonNode object(String raw,String field) {
        require(raw!=null&&!raw.isBlank(),field);JsonNode p=JsonUtils.parseTree(raw);require(p!=null&&p.isObject(),field);return p;
    }
    private static Long longValue(JsonNode value,String field) {
        require(value!=null&&value.isIntegralNumber()&&value.canConvertToLong()&&value.longValue()>0,field);return value.longValue();
    }
    private static boolean positive(Long value){return value!=null&&value>0;}
    private static void requireLeaderType(String type){require("PRODUCTION".equals(type)||"PQC".equals(type),"ownReturn.leaderType");}
    private static void require(boolean valid,String field){if(!valid)throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED,field);}
}
