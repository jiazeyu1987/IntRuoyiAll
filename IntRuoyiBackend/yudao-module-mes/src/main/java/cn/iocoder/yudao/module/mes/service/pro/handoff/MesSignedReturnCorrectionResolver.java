package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.Objects;
import static cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.*;

@Service
public class MesSignedReturnCorrectionResolver {
    @Resource private MesProProcessPoolEventRevisionMapper revisions;
    @Resource private MesActiveOrderHandoffTaskMapper tasks;
    @Resource private MesActiveOrderHandoffOwnerResolver owners;
    @Resource private cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesCorrectionSignatureEvidenceReader correctionEvidence;
    @Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper pqcTasks;
    public MesProProcessPoolEventRevisionDO find(MesProProcessPoolEventDO event,MesProcessPoolSubmissionReviewDO previous) {
        if(previous==null||!"REJECTED".equals(previous.getReviewStatus()))return null;
        var rows=revisions.selectListByEventId(event.getId());if(rows.isEmpty())return null;
        var revision=rows.get(0); var payload=JsonUtils.parseTree(revision.getAfterPayload());
        if(!payload.path("supersededReviewId").isIntegralNumber()
                ||!Objects.equals(previous.getId(),payload.path("supersededReviewId").longValue()))return null;
        Long actor=revision.getModifiedByUserId();
        require(Objects.equals(event.getTenantId(),tenant())&&Objects.equals(previous.getTenantId(),tenant())
                &&Objects.equals(revision.getTenantId(),tenant())&&Objects.equals(event.getId(),revision.getEventId())
                &&"EFFECTIVE".equals(revision.getRevisionStatus())&&revision.getId()!=null
                &&revision.getRevisionSignatureId()!=null&&revision.getRevisionSignatureId()>0
                &&actor!=null&&Objects.equals(actor,revision.getRevisionSignatureUserId())
                &&revision.getServerRevisionTime()!=null&&previous.getReviewedAt()!=null
                &&!revision.getServerRevisionTime().isBefore(previous.getReviewedAt()),"退回再审缺少原本人有效签名更正版本");
        require(Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()),
                    MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload()))
                &&!Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getBeforePayload()),
                    MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload())),"退回再审正文未匹配本人正式更正");
        var signature=JsonUtils.parseObject(revision.getRevisionSignatureSnapshot(),MesProBatchRecordExecutionFieldAuditSignatureResult.class);
        require(signature!=null&&signature.getSignedAt()!=null&&Objects.equals(actor,signature.getActorId())
                &&Objects.equals(revision.getRevisionSignatureId(),signature.getSignatureId()),"退回更正签名快照不匹配");
        Long activeId=sourceActiveId(event);
        var pending=tasks.selectList(new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>()
                .eq(MesActiveOrderHandoffTaskDO::getTenantId,tenant()).eq(MesActiveOrderHandoffTaskDO::getActiveOrderId,activeId)
                .eq(MesActiveOrderHandoffTaskDO::getSourceType,EVENT_SIGNED_REVISION)
                .eq(MesActiveOrderHandoffTaskDO::getSourceId,event.getId()).eq(MesActiveOrderHandoffTaskDO::getRoundId,revision.getId())
                .eq(MesActiveOrderHandoffTaskDO::getStatus,"TODO")
                .eq(MesActiveOrderHandoffTaskDO::getTaskType,"PRODUCTION_SUBMIT".equals(event.getEventType())?"PRODUCTION_REVIEW":"PQC_REVIEW"));
        require(pending.size()==1&&Objects.equals(pending.get(0).getActiveOrderId(),activeId)&&Objects.equals(pending.get(0).getTenantId(),tenant())
                &&candidates(pending.get(0).getCandidateUserSnapshot()).equals(java.util.List.of(previous.getLeaderUserId())),
                "退回再审必须由正式签名补正成功产生且沿用原组长责任");
        var task=pending.get(0);
        require(Objects.equals(task.getInitiatedBy(),actor)&&Objects.equals(task.getSourceId(),event.getId())
                &&Objects.equals(task.getRoundId(),revision.getId())&&EVENT_SIGNED_REVISION.equals(task.getSourceType())
                &&"TODO".equals(task.getStatus())&&Objects.equals(task.getTaskType(),"PRODUCTION_SUBMIT".equals(event.getEventType())?"PRODUCTION_REVIEW":"PQC_REVIEW"),
                "补正再审任务与实际签名处理人、来源或轮次不一致");
        String origin=JsonUtils.parseTree(task.getResponsibilitySnapshotJson()).path("correctionOrigin").asText();
        if("OWN_RETURN_CORRECTION".equals(origin))verifyRevision(event,previous,revision,actor);
        else if("LEADER_PQC_CORRECTION".equals(origin))verifyLeaderPqcRevision(event,previous,revision,actor);
        else if("LEADER_PROFILE_CORRECTION".equals(origin))verifyProfileLeaderRevision(event,previous,revision,actor);
        else throw failure("补正再审缺少正式本人或组长补正来源");
        return revision;
    }
    public void verifyRevision(MesProProcessPoolEventDO event,MesProcessPoolSubmissionReviewDO previous,
            MesProProcessPoolEventRevisionDO revision,Long actor) {
        require(previous!=null&&"REJECTED".equals(previous.getReviewStatus())
                &&Objects.equals(previous.getEventId(),event.getId())&&Objects.equals(previous.getTenantId(),tenant()),
                "补正必须绑定原拒绝复核");
        require(Objects.equals(actor,owners.originalActor(event)),"补正必须来自原SYSTEM_USER签名本人");
        var payload=JsonUtils.parseTree(revision.getAfterPayload());
        require(payload.path("supersededReviewId").isIntegralNumber()
                &&Objects.equals(previous.getId(),payload.path("supersededReviewId").longValue()),"补正未绑定准确拒绝轮次");
        correctionEvidence.require(event,revision);
    }
    public void verifyLeaderPqcRevision(MesProProcessPoolEventDO event,MesProcessPoolSubmissionReviewDO previous,
            MesProProcessPoolEventRevisionDO revision,Long actor) {
        owners.assertPqcLeaderCorrector(event,actor);
        require(previous!=null&&"PQC".equals(previous.getLeaderType())&&"REJECTED".equals(previous.getReviewStatus())
                &&Objects.equals(previous.getEventId(),event.getId())&&Objects.equals(previous.getTenantId(),tenant())
                &&Objects.equals(revision.getTenantId(),tenant())&&Objects.equals(revision.getEventId(),event.getId())
                &&"EFFECTIVE".equals(revision.getRevisionStatus())&&Objects.equals(actor,revision.getModifiedByUserId())
                &&Objects.equals(actor,revision.getRevisionSignatureUserId())&&revision.getServerRevisionTime()!=null
                &&previous.getReviewedAt()!=null&&!revision.getServerRevisionTime().isBefore(previous.getReviewedAt()),
                "PQC组长补正必须来自原拒绝后的有效本人组长签名版本");
        var payload=JsonUtils.parseTree(revision.getAfterPayload());
        require(payload.path("supersededReviewId").isIntegralNumber()
                &&Objects.equals(previous.getId(),payload.path("supersededReviewId").longValue())
                &&Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()),
                    MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload()))
                &&!Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getBeforePayload()),
                    MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload())),"PQC组长补正正文或原拒绝轮次不一致");
        correctionEvidence.require(event,revision);
    }
    public void verifyProfileLeaderRevision(MesProProcessPoolEventDO event, MesProcessPoolSubmissionReviewDO previous,
            MesProProcessPoolEventRevisionDO revision, Long actor) {
        require("PRODUCTION_SUBMIT".equals(event.getEventType()) && previous != null
                && "PRODUCTION".equals(previous.getLeaderType()) && "REJECTED".equals(previous.getReviewStatus())
                && Objects.equals(previous.getEventId(), event.getId()) && Objects.equals(previous.getTenantId(), tenant())
                && Objects.equals(actor, previous.getLeaderUserId()) && Objects.equals(actor, owners.profileProductionLeader(event))
                && revision != null && Objects.equals(revision.getTenantId(), tenant()) && Objects.equals(revision.getEventId(), event.getId())
                && "EFFECTIVE".equals(revision.getRevisionStatus()) && Objects.equals(actor, revision.getModifiedByUserId())
                && Objects.equals(actor, revision.getRevisionSignatureUserId()) && revision.getServerRevisionTime() != null
                && previous.getReviewedAt() != null && !revision.getServerRevisionTime().isBefore(previous.getReviewedAt()),
                "临时人员组长补正必须来自原拒绝后的正式责任组长签名版本");
        var payload = JsonUtils.parseTree(revision.getAfterPayload());
        require(payload.path("supersededReviewId").isIntegralNumber()
                && Objects.equals(previous.getId(), payload.path("supersededReviewId").longValue())
                && Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()),
                    MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload()))
                && !Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getBeforePayload()),
                    MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload())),
                "临时人员组长补正正文或原拒绝轮次不一致");
        correctionEvidence.require(event, revision);
    }

    private Long sourceActiveId(MesProProcessPoolEventDO event) {
        if("PRODUCTION_SUBMIT".equals(event.getEventType())) {
            var n=JsonUtils.parseTree(event.getRawPayload()).path("activeOrderId");require(n.isIntegralNumber()&&n.longValue()>0,"补正原周期缺失");return n.longValue();
        }
        var task=pqcTasks.selectById(event.getFeedbackSourceId());require(task!=null&&Objects.equals(task.getTenantId(),tenant()),"PQC补正原周期缺失");return task.getActiveOrderId();
    }

}
