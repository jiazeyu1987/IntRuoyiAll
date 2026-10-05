package cn.iocoder.yudao.module.mes.service.pro.handoff;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditSignatureResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffContract.*;

/** Commands called inside the transaction of the corresponding successful business action. */
@Service
@Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
public class MesActiveOrderHandoffService {
    @Resource private MesActiveOrderHandoffTaskMapper tasks;
    @Resource private MesProcessPoolActiveOrderMapper orders;
    @Resource private MesProProcessPoolEventMapper events;
    @Resource private MesProcessPoolSubmissionReviewMapper reviews;
    @Resource private MesProProcessPoolEventRevisionMapper revisions;
    @Resource private MesPqcInspectionTaskMapper pqcTasks;
    @Resource private MesActiveOrderHandoffOwnerResolver owners;
    @Resource private MesQaHandoffAssignmentService qaAssignment;
    @Resource private MesPqcHandoffAssignmentService pqcAssignment;
    @Resource private MesActiveOrderHandoffDeliveryService delivery;
    @Resource private MesActiveOrderHandoffAudit audit;
    @Resource private MesSignedReturnCorrectionResolver correctionResolver;
    @Resource private MesProcessPoolReportAllocationMapper allocations;
    @Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper workOrders;
    @Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper routeProcesses;
    @Resource private cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper processes;

    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    public List<MesActiveOrderHandoffTaskDO> listOwnTasks(Long actor) {
        require(actor!=null,"请先登录"); return tasks.own(tenant(),actor);
    }
    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    public List<MesActiveOrderHandoffTaskDO> listOwnReturnTasks(Long actor,String leaderType) {
        require(Set.of("PRODUCTION","PQC").contains(leaderType),"退回业务类型无效");
        return listOwnTasks(actor).stream().filter(t->"TODO".equals(t.getStatus())
                &&(leaderType+"_RETURN").equals(t.getTaskType())&&current(t)).toList();
    }
    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    public MesActiveOrderHandoffTaskDO findOwnReturn(Long eventId,Long rejectedReviewId,Long actor) {
        var rows=listOwnTasks(actor).stream().filter(t->Set.of("PRODUCTION_RETURN","PQC_RETURN").contains(t.getTaskType())
                &&"PROCESS_POOL_EVENT".equals(t.getSourceType())&&Objects.equals(eventId,t.getSourceId())
                &&Objects.equals(rejectedReviewId,t.getRoundId())&&"TODO".equals(t.getStatus())).toList();
        require(rows.size()==1,"该本人退回任务不存在、已完成或已关闭");
        var task=rows.get(0); access(task,actor); require(current(task),"旧周期退回任务禁止办理"); return task;
    }

    public void completeReturnAndScheduleReview(Long eventId,Long rejectedReviewId,Long revisionId,Long actorUserId) {
        audit.lock(); var task=findOwnReturn(eventId,rejectedReviewId,actorUserId);
        var revision=revisions.selectById(revisionId); var event=event(eventId); var previous=reviews.selectById(rejectedReviewId);
        require(previous!=null&&Objects.equals(previous.getEventId(),eventId)&&"REJECTED".equals(previous.getReviewStatus())
                &&Objects.equals(previous.getTenantId(),tenant()),"原正式拒绝记录不匹配");
        require(revision!=null&&Objects.equals(revision.getTenantId(),tenant())&&Objects.equals(revision.getEventId(),eventId)
                &&"EFFECTIVE".equals(revision.getRevisionStatus())&&Objects.equals(revision.getModifiedByUserId(),actorUserId)
                &&Objects.equals(revision.getRevisionSignatureUserId(),actorUserId)&&revision.getRevisionSignatureId()!=null
                &&revision.getRevisionSignatureId()>0,"本人更正缺少有效签名版本");
        var after=JsonUtils.parseTree(revision.getAfterPayload());
        require(after.path("supersededReviewId").isIntegralNumber()
                &&Objects.equals(after.path("supersededReviewId").longValue(),rejectedReviewId)
                &&Objects.equals(MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()),
                    MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(revision.getAfterPayload())),"更正版本未正式覆盖原退回记录");
        var signature=JsonUtils.parseObject(revision.getRevisionSignatureSnapshot(),MesProBatchRecordExecutionFieldAuditSignatureResult.class);
        require(signature!=null&&signature.getSignedAt()!=null&&Objects.equals(signature.getActorId(),actorUserId)
                &&Objects.equals(signature.getSignatureId(),revision.getRevisionSignatureId()),"本人更正签名快照不匹配");
        correctionResolver.verifyRevision(event,previous,revision,actorUserId);
        var order=order(task.getActiveOrderId(),true);
        String type=task.getTaskType().replace("_RETURN","_REVIEW");
        Long routeProcessId=submissionRouteProcess(event,order);
        require(Objects.equals(task.getRouteProcessId(),routeProcessId),"本人退回工序与原正式提交来源不一致");
        close(task,actorUserId,revisionId,"DONE");
        // The original rejected review owns this round; current responsibility edits cannot redirect it.
        owners.user(previous.getLeaderUserId(),"mes:pro-process-pool-team-leader:review");
        create(order,type,EVENT_SIGNED_REVISION,eventId,revisionId,previous.getLeaderUserId().toString(),actorUserId,
                "PRODUCTION_REVIEW".equals(type),revision.getChangeReason(),routeProcessId,false,"OWN_RETURN_CORRECTION",null);
    }

    /** Existing formal leader correction has its own frozen origin; it never impersonates an own return. */
    public void completeLeaderPqcCorrectionAndScheduleReview(Long eventId,Long rejectedReviewId,Long revisionId,Long actor) {
        audit.lock();var event=event(eventId);var previous=reviews.selectById(rejectedReviewId);
        var latestReview=reviews.selectLatestByEventIdForUpdate(eventId);
        require(previous!=null&&latestReview!=null&&Objects.equals(latestReview.getId(),rejectedReviewId),"组长补正必须绑定最新原拒绝记录");
        var revision=revisions.selectById(revisionId);require(revision!=null,"组长正式补正版本不存在");
        var latestRevisions=revisions.selectListByEventId(eventId);
        require(!latestRevisions.isEmpty()&&Objects.equals(latestRevisions.get(0).getId(),revisionId),"组长补正版本已被后续版本覆盖");
        correctionResolver.verifyLeaderPqcRevision(event,previous,revision,actor);
        var order=order(active(event),true);
        Long routeProcessId=submissionRouteProcess(event,order);
        var returned=tasks.byIdentity(order.getId(),"PQC_RETURN","PROCESS_POOL_EVENT",eventId,rejectedReviewId);
        require(returned!=null&&Objects.equals(returned.getTenantId(),tenant())
                &&Objects.equals(returned.getRouteProcessId(),routeProcessId)
                &&candidates(returned.getCandidateUserSnapshot()).equals(List.of(owners.originalActor(event))),"组长补正缺少原正式本人退回记录");
        close(returned,actor,revisionId,"DONE");
        owners.user(previous.getLeaderUserId(),"mes:pro-process-pool-team-leader:review");
        create(order,"PQC_REVIEW",EVENT_SIGNED_REVISION,eventId,revisionId,previous.getLeaderUserId().toString(),actor,
                false,revision.getChangeReason(),routeProcessId,false,"LEADER_PQC_CORRECTION",null);
    }

    public void productionReady(Long activeOrderId,Long actor) {
        audit.lock(); var order=order(activeOrderId,true);
        String receivers=owners.productionEmployees(order);
        createDefaultRound(order,"PRODUCTION_HANDOFF","ACTIVE_ORDER",activeOrderId,activeOrderId,receivers,actor,true,
                "当轮正式人员责任已确认，请接手生产",null,false);
    }
    public void productionSubmitted(Long activeOrderId,Long eventId,Long actor) {
        audit.lock(); var order=order(activeOrderId,true); var event=event(eventId);
        require("PRODUCTION_SUBMIT".equals(event.getEventType())&&Objects.equals(activeOrderId,active(event)),"生产提交与活跃周期不一致");
        var identity=owners.submissionIdentity(event); require(Objects.equals(identity.signerId(),actor),"生产提交接手人与原签名人不一致");
        if(identity.isSystemUser()) closeMatching(activeOrderId,"PRODUCTION_HANDOFF","ACTIVE_ORDER",activeOrderId,actor,eventId);
        else {
            require(Objects.equals(owners.profileProductionLeader(event),order.getLeaderUserId()),"临时提交不属于原周期正式生产责任");
            // This is automatic closure from verified business submission, attributed to its actual operator.
            // A profile signer never becomes an inbox candidate or a system-user signer.
            for(var task:tasks.selectList(new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>()
                    .eq(MesActiveOrderHandoffTaskDO::getTenantId,tenant()).eq(MesActiveOrderHandoffTaskDO::getActiveOrderId,activeOrderId)
                    .eq(MesActiveOrderHandoffTaskDO::getTaskType,"PRODUCTION_HANDOFF").eq(MesActiveOrderHandoffTaskDO::getSourceType,"ACTIVE_ORDER")
                    .eq(MesActiveOrderHandoffTaskDO::getSourceId,activeOrderId).eq(MesActiveOrderHandoffTaskDO::getStatus,"TODO"))) {
                validate(task); require(Objects.equals(task.getWorkOrderId(),event.getWorkOrderId()),"临时提交接手任务工单不一致");
                close(task,identity.operatorId(),eventId,"DONE");
            }
        }
        Long operator=identity.isSystemUser()?actor:identity.operatorId();
        createDefaultRound(order,"PRODUCTION_REVIEW","PROCESS_POOL_EVENT",eventId,eventId,owners.productionLeader(order).toString(),operator,true,
                "本人生产提交已完成，请组长签名复核",event.getRouteProcessId(),false);
    }
    public void pqcSubmitted(Long pqcTaskId,Long eventId,Long actor) {
        audit.lock(); var event=event(eventId); var pqc=owners.pqcTask(event);
        require(Objects.equals(pqc.getId(),pqcTaskId)&&"SUBMITTED".equals(pqc.getTaskStatus()),"PQC提交正式任务来源不一致");
        require(Objects.equals(owners.originalActor(event),actor),"PQC提交接手人与原签名人不一致");
        var order=order(pqc.getActiveOrderId(),true);
        Long routeProcessId=submissionRouteProcess(event,order);
        Long leader=owners.pqcLeader(event);
        closePqcHandoff(order,pqc,actor,leader,eventId);
        createDefaultRound(order,"PQC_REVIEW","PROCESS_POOL_EVENT",eventId,eventId,leader.toString(),actor,false,
                "本人PQC检验提交已完成，请组长签名复核",routeProcessId,false);
    }
    public void reviewed(Long eventId,Long reviewId) {
        audit.lock(); var event=event(eventId); var review=reviews.selectById(reviewId);
        require(review!=null&&Objects.equals(tenant(),review.getTenantId())&&Objects.equals(eventId,review.getEventId())
                &&review.getReviewSignatureId()!=null&&review.getReviewSignatureId()>0
                &&Objects.equals(review.getLeaderUserId(),review.getReviewSignatureUserId()),"组长正式复核签名来源不一致");
        boolean production="PRODUCTION_SUBMIT".equals(event.getEventType()); var order=order(active(event),true);
        var latest=reviews.selectLatestByEventIdForUpdate(eventId);
        require(latest!=null&&Objects.equals(latest.getId(),reviewId),"旧复核轮次禁止关闭或创建新轮交接");
        Long routeProcessId=submissionRouteProcess(event,order);
        Long round=review.getSourceRevisionId()==null?eventId:review.getSourceRevisionId();
        closeReviewRound(order.getId(),production?"PRODUCTION_REVIEW":"PQC_REVIEW",
                review.getSourceRevisionId()==null?"PROCESS_POOL_EVENT":EVENT_SIGNED_REVISION,eventId,round,review.getLeaderUserId(),reviewId);
        if("REJECTED".equals(review.getReviewStatus())) {
            require(review.getReviewRemark()!=null&&!review.getReviewRemark().isBlank(),"退回原因不能为空");
            var identity=owners.submissionIdentity(event);
            if(identity.isSystemUser()) createDefaultRound(order,production?"PRODUCTION_RETURN":"PQC_RETURN","PROCESS_POOL_EVENT",eventId,reviewId,
                    identity.signerId().toString(),review.getLeaderUserId(),false,review.getReviewRemark(),routeProcessId,false);
            else {
                require(production && Objects.equals(owners.profileProductionLeader(event),review.getLeaderUserId()),"临时人员退回原生产组长责任不一致");
                create(order,"PRODUCTION_REVIEW",EVENT_REJECTED_REVIEW,eventId,reviewId,review.getLeaderUserId().toString(),
                        review.getLeaderUserId(),false,"临时人员报工退回，请责任组长更正后重新签名复核："+review.getReviewRemark(),routeProcessId,false,"LEADER_PROFILE_RETURN",null);
            }
        } else {
            require("APPROVED".equals(review.getReviewStatus()),"正式复核结果无效");
            if(production) closeDecisionHandoff(order.getId(),review.getLeaderUserId(),reviewId);
            MesPqcHandoffAssignmentService.ResolvedAssignment assignment=null;
            if(production) for(var pqc:pqcTasks.selectListByActiveOrderId(order.getId())) {
                if(!"PENDING".equals(pqc.getTaskStatus())||!Objects.equals(pqc.getRouteProcessId(),event.getRouteProcessId()))continue;
                Long pqcRouteProcessId=pqcRouteProcess(pqc,order);
                if(assignment==null) assignment=pqcAssignment.resolve(order.getRouteId());
                create(order,"PQC_HANDOFF","PQC_INSPECTION_TASK",pqc.getId(),pqc.getId(),assignment.candidateUserSnapshot(),
                        review.getLeaderUserId(),true,"生产组长复核已通过，请接手对应PQC任务",pqcRouteProcessId,false,null,assignment);
            }
        }
    }

    public void completeProfileLeaderCorrection(Long eventId, Long rejectedReviewId, Long revisionId, Long actor) {
        audit.lock(); var event=event(eventId); var previous=reviews.selectById(rejectedReviewId);
        var latest=reviews.selectLatestByEventIdForUpdate(eventId); var revision=revisions.selectById(revisionId);
        require(previous!=null && latest!=null && Objects.equals(latest.getId(),rejectedReviewId)
                && Objects.equals(actor,previous.getLeaderUserId()) && Objects.equals(actor,owners.profileProductionLeader(event)),
                "临时人员组长更正必须绑定当前原拒绝轮次");
        require(revision!=null && Objects.equals(revision.getEventId(),eventId) && Objects.equals(revision.getTenantId(),tenant())
                && Objects.equals(revision.getModifiedByUserId(),actor) && Objects.equals(revision.getRevisionSignatureUserId(),actor)
                && "EFFECTIVE".equals(revision.getRevisionStatus()), "责任组长更正未形成正式签名版本");
        var latestRevisions=revisions.selectListByEventId(eventId);
        require(!latestRevisions.isEmpty() && Objects.equals(latestRevisions.get(0).getId(),revisionId),"组长补正版本已被后续版本覆盖");
        correctionResolver.verifyProfileLeaderRevision(event,previous,revision,actor);
        var order=order(active(event),true);
        var returned=tasks.byIdentity(order.getId(),"PRODUCTION_REVIEW",EVENT_REJECTED_REVIEW,eventId,rejectedReviewId);
        access(returned,actor);
        require("LEADER_PROFILE_RETURN".equals(JsonUtils.parseTree(returned.getResponsibilitySnapshotJson()).path("correctionOrigin").asText()),
                "临时人员组长补正缺少原正式退回来源");
        close(returned,actor,revisionId,"DONE");
        create(order,"PRODUCTION_REVIEW",EVENT_SIGNED_REVISION,eventId,revisionId,actor.toString(),actor,true,
                "临时人员报工已由责任组长签名更正，请对新版本独立复核",event.getRouteProcessId(),false,"LEADER_PROFILE_CORRECTION",null);
    }

    /** A shared event keeps its original review cycle; downstream work belongs to formal allocation targets. */
    public void allocationReviewed(Long eventId, Long reviewId, Collection<MesProcessPoolReportAllocationDO> targets) {
        audit.lock(); var event = event(eventId); var review = reviews.selectById(reviewId);
        var latest = reviews.selectLatestByEventIdForUpdate(eventId);
        require("PRODUCTION_SUBMIT".equals(event.getEventType()) && review != null
                && Objects.equals(tenant(), review.getTenantId()) && Objects.equals(eventId, review.getEventId())
                && "APPROVED".equals(review.getReviewStatus()) && review.getReviewSignatureId() != null
                && review.getReviewSignatureId() > 0 && Objects.equals(review.getLeaderUserId(), review.getReviewSignatureUserId())
                && latest != null && Objects.equals(reviewId, latest.getId()), "分配交接缺少当轮正式生产复核签名");
        var source = order(active(event), false);
        Long round = review.getSourceRevisionId() == null ? eventId : review.getSourceRevisionId();
        var reviewTask = tasks.byIdentity(source.getId(), "PRODUCTION_REVIEW", review.getSourceRevisionId()==null?"PROCESS_POOL_EVENT":EVENT_SIGNED_REVISION, eventId, round);
        access(reviewTask, review.getLeaderUserId());
        require("ACTIVE".equals(source.getActiveStatus()) || ("DONE".equals(reviewTask.getStatus())
                && Objects.equals(reviewId, reviewTask.getCompletionSourceId())), "归档源周期不能产生新的复核动作");
        close(reviewTask, review.getLeaderUserId(), reviewId, "DONE");
        var current = allocations.selectListByEventIdForUpdate(eventId);
        Set<Long> handled = new HashSet<>();
        for (var target : targets) {
            require(target.getId() != null && current.stream().anyMatch(a -> Objects.equals(a.getId(), target.getId())
                    && Objects.equals(a.getActiveOrderId(), target.getActiveOrderId())
                    && Objects.equals(a.getWorkOrderId(), target.getWorkOrderId())
                    && Objects.equals(a.getRouteProcessId(), target.getRouteProcessId())
                    && Objects.equals(a.getProcessId(), target.getProcessId())
                    && a.getAllocatedQuantity().compareTo(target.getAllocatedQuantity()) == 0), "PQC交接目标不是当前正式分配");
            var order = order(target.getActiveOrderId(), true);
            require(Objects.equals(order.getWorkOrderId(), target.getWorkOrderId()), "分配目标工单与正式周期不一致");
            if (!handled.add(order.getId())) continue;
            closeDecisionHandoff(order.getId(), review.getLeaderUserId(), reviewId);
            MesPqcHandoffAssignmentService.ResolvedAssignment assignment = null;
            for (var pqc : pqcTasks.selectListByActiveOrderId(order.getId())) {
                if (!"PENDING".equals(pqc.getTaskStatus()) || !Objects.equals(pqc.getRouteProcessId(), target.getRouteProcessId())) continue;
                require(Objects.equals(tenant(), pqc.getTenantId()) && Objects.equals(order.getWorkOrderId(), pqc.getWorkOrderId())
                        && Objects.equals(target.getProcessId(), pqc.getProcessId()), "目标PQC正式任务身份不一致");
                Long pqcRouteProcessId = pqcRouteProcess(pqc, order);
                if (assignment == null) assignment = pqcAssignment.resolve(order.getRouteId());
                create(order, "PQC_HANDOFF", "PQC_INSPECTION_TASK", pqc.getId(), pqc.getId(), assignment.candidateUserSnapshot(),
                        review.getLeaderUserId(), true, "生产分配已正式复核，请接手对应PQC任务", pqcRouteProcessId, false, null, assignment);
            }
        }
    }
    public void qaCreated(MesProEdhrNonconformanceReviewDO review,Long activeOrderId,Long actor) {
        audit.lock(); var order=order(activeOrderId,true);
        require(review!=null&&review.getId()!=null&&Objects.equals(review.getWorkOrderId(),order.getWorkOrderId()),"QA评审正式来源缺失");
        String candidates=qaAssignment.resolve(order.getRouteId());
        createDefaultRound(order,"QA_REVIEW","NONCONFORMANCE_REVIEW",review.getId(),review.getId(),candidates,actor,false,
                review.getNonconformanceReason(),null,false);
    }
    public void assertQaCanDispose(Long reviewId,Long activeOrderId,Long actor) {
        var task=tasks.byIdentity(activeOrderId,"QA_REVIEW","NONCONFORMANCE_REVIEW",reviewId,reviewId);
        access(task,actor); require("TODO".equals(task.getStatus())&&current(task),"QA评审已关闭或属于旧周期");
        owners.user(actor,MesQaHandoffAssignmentService.DISPOSE_PERMISSION);
    }
    public void qaDisposed(MesProEdhrNonconformanceReviewDO review,Long oldActiveOrderId,Long successorId,Long actor) {
        audit.lock(); var task=tasks.byIdentity(oldActiveOrderId,"QA_REVIEW","NONCONFORMANCE_REVIEW",review.getId(),review.getId());
        access(task,actor); require("closed".equals(review.getReviewStatus())&&Objects.equals(actor,review.getQaUserId())
                &&review.getClosedAt()!=null,"QA处置尚未正式完成"); close(task,actor,review.getId(),"DONE");
        var target=order(successorId==null?oldActiveOrderId:successorId,false);
        boolean voided="void".equals(review.getDisposition());
        if(successorId!=null||voided)retireCycle(oldActiveOrderId,actor,review.getId());
        createDefaultRound(target,"QA_DECISION_HANDOFF","NONCONFORMANCE_REVIEW",review.getId(),review.getId(),
                owners.productionLeader(target).toString(),actor,true,review.getDisposition()+"："+review.getReviewOpinion(),null,voided);
        if(successorId!=null)productionReady(successorId,actor);
    }
    public void retireCycle(Long activeOrderId,Long actor,Long source) {
        audit.lock(); for(var task:tasks.selectList(new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>()
                .eq(MesActiveOrderHandoffTaskDO::getActiveOrderId,activeOrderId).eq(MesActiveOrderHandoffTaskDO::getStatus,"TODO")))
            close(task,actor,source,"CANCELED");
    }
    public void productionLeaderContinued(Long activeOrderId,Long actor,Long businessSourceId) {
        audit.lock();require(businessSourceId!=null&&businessSourceId>0,"组长接手完成必须来自正式业务成功记录");
        var order=order(activeOrderId,true);require(Objects.equals(owners.productionLeader(order),actor),"仅当轮原生产组长可完成QA结果接手");
        closeDecisionHandoff(activeOrderId,actor,businessSourceId);
    }
    private void closeDecisionHandoff(Long activeOrderId,Long actor,Long source) {
        for(var task:tasks.selectList(new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>()
                .eq(MesActiveOrderHandoffTaskDO::getActiveOrderId,activeOrderId).eq(MesActiveOrderHandoffTaskDO::getTaskType,"QA_DECISION_HANDOFF")
                .eq(MesActiveOrderHandoffTaskDO::getStatus,"TODO"))) { access(task,actor);close(task,actor,source,"DONE"); }
    }
    private void closeMatching(Long activeId,String type,String sourceType,Long sourceId,Long actor,Long completionSource) {
        var rows=tasks.selectList(new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>()
                .eq(MesActiveOrderHandoffTaskDO::getActiveOrderId,activeId).eq(MesActiveOrderHandoffTaskDO::getTaskType,type)
                .eq(MesActiveOrderHandoffTaskDO::getSourceType,sourceType).eq(MesActiveOrderHandoffTaskDO::getSourceId,sourceId)
                .eq(MesActiveOrderHandoffTaskDO::getStatus,"TODO"));
        for(var task:rows) { access(task,actor); close(task,actor,completionSource,"DONE"); }
    }
    private void closePqcHandoff(MesProcessPoolActiveOrderDO order,
            MesPqcInspectionTaskDO pqc,
            Long actor,Long leader,Long eventId) {
        var rows=tasks.selectList(new LambdaQueryWrapperX<MesActiveOrderHandoffTaskDO>()
                .eq(MesActiveOrderHandoffTaskDO::getActiveOrderId,order.getId()).eq(MesActiveOrderHandoffTaskDO::getTaskType,"PQC_HANDOFF")
                .eq(MesActiveOrderHandoffTaskDO::getSourceType,"PQC_INSPECTION_TASK").eq(MesActiveOrderHandoffTaskDO::getSourceId,pqc.getId())
                .eq(MesActiveOrderHandoffTaskDO::getStatus,"TODO"));
        for(var task:rows) {
            require(Objects.equals(task.getTenantId(),tenant())&&Objects.equals(task.getActiveOrderId(),order.getId())
                    &&Objects.equals(task.getWorkOrderId(),order.getWorkOrderId())&&"PQC_HANDOFF".equals(task.getTaskType())
                    &&"PQC_INSPECTION_TASK".equals(task.getSourceType())&&Objects.equals(task.getSourceId(),pqc.getId())
                    &&Objects.equals(task.getRoundId(),pqc.getId())&&Objects.equals(task.getRouteProcessId(),pqc.getRouteProcessId())
                    &&"TODO".equals(task.getStatus()),"PQC接手与本次已成功提交的正式任务不一致");
            if(candidates(task.getCandidateUserSnapshot()).contains(actor)) access(task,actor);
            else {
                var snapshot=JsonUtils.parseTree(task.getResponsibilitySnapshotJson());var handlers=snapshot.path("handlerLeaderUserIds");
                require(Objects.equals(actor,leader)&&"SYSTEM_USER".equals(snapshot.path("identityDomain").asText())
                        &&snapshot.path("activeOrderId").isIntegralNumber()&&snapshot.path("activeOrderId").longValue()==order.getId()
                        &&snapshot.path("workOrderId").isIntegralNumber()&&snapshot.path("workOrderId").longValue()==order.getWorkOrderId()
                        &&snapshot.path("routeId").isIntegralNumber()&&snapshot.path("routeId").longValue()==order.getRouteId()
                        &&"PQC_INSPECTION_TASK".equals(snapshot.path("sourceType").asText())
                        &&snapshot.path("sourceId").isIntegralNumber()&&snapshot.path("sourceId").longValue()==pqc.getId()
                        &&snapshot.path("roundId").isIntegralNumber()&&snapshot.path("roundId").longValue()==pqc.getId()
                        &&snapshot.path("routeProcessId").isIntegralNumber()&&snapshot.path("routeProcessId").longValue()==pqc.getRouteProcessId()
                        &&snapshot.path("processId").isIntegralNumber()&&snapshot.path("processId").longValue()==pqc.getProcessId()
                        &&handlers.isArray()&&!handlers.isEmpty(),"PQC本人组长提交缺少原交接冻结的正式办理责任");
                var frozen=new HashSet<Long>();
                for(var handler:handlers) require(handler.isIntegralNumber()&&handler.longValue()>0&&frozen.add(handler.longValue()),"PQC交接冻结的组长身份无效");
                require(frozen.contains(actor),"本次提交组长不是该任务冻结的正式办理人");
            }
            close(task,actor,eventId,"DONE");
        }
    }
    private void closeReviewRound(Long activeId,String type,String sourceType,Long eventId,Long roundId,Long actor,Long completionSource) {
        var task=tasks.byIdentity(activeId,type,sourceType,eventId,roundId);
        access(task,actor);close(task,actor,completionSource,"DONE");
    }
    private void close(MesActiveOrderHandoffTaskDO candidate,Long actor,Long source,String state) {
        var task=tasks.lock(candidate.getId()); require(task!=null&&Objects.equals(tenant(),task.getTenantId()),"交接记录不存在");
        if(!"TODO".equals(task.getStatus())) {
            require(Objects.equals(state,task.getStatus())&&Objects.equals(source,task.getCompletionSourceId()),"交接已由另一正式动作关闭"); return;
        }
        audit.lock(); require(tasks.close(tenant(),task.getId(),task.getRowVersion(),state,actor,source,LocalDateTime.now())==1,"交接版本变化，请刷新");
        var after=tasks.selectById(task.getId());audit.append("task-closed",MesActiveOrderHandoffService.class.getName()+"#close",
                "MES_HANDOFF_TASK",task.getId(),after.getRowVersion(),task,after,"正式业务成功关闭交接");
    }
    private MesActiveOrderHandoffTaskDO createDefaultRound(MesProcessPoolActiveOrderDO order,String type,String source,Long sourceId,Long round,
            String receivers,Long initiator,boolean notificationOnly,String reason,Long routeProcessId,boolean completed) {
        return create(order,type,source,sourceId,round,receivers,initiator,notificationOnly,reason,routeProcessId,completed,null,null);
    }
    private MesActiveOrderHandoffTaskDO create(MesProcessPoolActiveOrderDO order,String type,String source,Long sourceId,Long round,
            String receivers,Long initiator,boolean notificationOnly,String reason,Long routeProcessId,boolean completed,String correctionOrigin,
            MesPqcHandoffAssignmentService.ResolvedAssignment pqcAssignmentSnapshot) {
        var existing=tasks.byIdentity(order.getId(),type,source,sourceId,round);
        if(existing!=null) {
            require(Objects.equals(existing.getCandidateUserSnapshot(),receivers),"交接重放与原冻结责任不一致");
            if(correctionOrigin!=null) require(correctionOrigin.equals(JsonUtils.parseTree(existing.getResponsibilitySnapshotJson()).path("correctionOrigin").asText()),
                    "交接重放与原正式补正来源不一致");
            return existing;
        }
        require(reason!=null&&!reason.isBlank(),"交接原因或业务意见缺失");
        var workOrder=workOrders.selectById(order.getWorkOrderId());
        require(workOrder!=null&&Objects.equals(workOrder.getTenantId(),tenant())&&workOrder.getCode()!=null&&!workOrder.getCode().isBlank(),"交接缺少正式工单编码");
        String processLabel="整单";Long processId=null;
        if(routeProcessId!=null) {
            var routeProcess=routeProcesses.selectById(routeProcessId);
            require(routeProcess!=null&&Objects.equals(routeProcess.getRouteId(),order.getRouteId()),"交接工序不属于当轮正式路线");
            var process=processes.selectById(routeProcess.getProcessId());
            require(process!=null&&process.getName()!=null&&!process.getName().isBlank(),"交接缺少正式工序名称");
            processId=process.getId();processLabel=process.getName();
        }
        boolean originalSubmission="PROCESS_POOL_EVENT".equals(source)&&type.endsWith("_REVIEW");
        var initiatorIdentity=owners.initiatorSnapshot(initiator,originalSubmission?event(sourceId):null);
        var responsibility=new LinkedHashMap<String,Object>();
        responsibility.put("identityDomain","SYSTEM_USER");responsibility.put("userIds",candidates(receivers));
        responsibility.put("activeOrderId",order.getId());responsibility.put("sourceType",source);responsibility.put("sourceId",sourceId);responsibility.put("roundId",round);
        responsibility.put("workOrderId",order.getWorkOrderId());responsibility.put("workOrderCode",workOrder.getCode());
        responsibility.put("routeProcessId",routeProcessId);responsibility.put("processId",processId);responsibility.put("processName",processLabel);
        if("PQC_HANDOFF".equals(type)) {
            require(pqcAssignmentSnapshot!=null&&pqcAssignmentSnapshot.ruleId()!=null&&pqcAssignmentSnapshot.ruleId()>0
                    &&Objects.equals(pqcAssignmentSnapshot.routeId(),order.getRouteId())
                    &&Objects.equals(pqcAssignmentSnapshot.candidateUserSnapshot(),receivers)
                    &&pqcAssignmentSnapshot.handlerLeaderUserIds()!=null&&!pqcAssignmentSnapshot.handlerLeaderUserIds().isEmpty(),"PQC接手缺少当前路线正式派发责任规则");
            responsibility.put("routeId",order.getRouteId());
            responsibility.put("handlerLeaderUserIds",pqcAssignmentSnapshot.handlerLeaderUserIds());
            responsibility.put("pqcAssignmentRule",pqcAssignmentSnapshot.snapshot());
        }
        if(Set.of("PROCESS_POOL_EVENT",EVENT_SIGNED_REVISION).contains(source)&&Set.of("PQC_REVIEW","PQC_RETURN").contains(type)) {
            var pqc=owners.pqcTask(event(sourceId));
            responsibility.put("pqcTaskId",pqc.getId());responsibility.put("qaProcessId",pqc.getQaProcessId());
            responsibility.put("routeVersionId",pqc.getRouteVersionId());responsibility.put("regulationVersionId",pqc.getRegulationVersionId());
        }
        if(EVENT_SIGNED_REVISION.equals(source)||EVENT_REJECTED_REVIEW.equals(source)) {
            require(correctionOrigin!=null&&!correctionOrigin.isBlank(),"补正轮次必须明确正式来源");
            responsibility.put("correctionOrigin",correctionOrigin);
        }
        responsibility.put("initiator",initiatorIdentity);responsibility.put("activeStatus",order.getActiveStatus());responsibility.put("businessStatus",order.getBusinessStatus());
        String businessState=switch(type) {
            case "PRODUCTION_HANDOFF" -> "人员已确认，待生产接手";
            case "PRODUCTION_REVIEW" -> "LEADER_PROFILE_RETURN".equals(correctionOrigin)?"临时人员报工已退回，待责任组长签名更正":originalSubmission?"生产已提交，待组长复核":"生产已签名补正，待组长再审";
            case "PQC_HANDOFF" -> "生产复核已通过，待PQC检验";
            case "PQC_REVIEW" -> originalSubmission?"PQC已提交，待组长复核":"PQC已签名补正，待组长再审";
            case "PRODUCTION_RETURN","PQC_RETURN" -> "组长已退回，待签名更正";
            case "QA_REVIEW" -> "不合格评审已创建，待QA处置";
            case "QA_DECISION_HANDOFF" -> completed?"QA已正式作废，结果只读":"QA已正式处置，待原生产组长接手";
            default -> throw failure("未知交接业务状态");
        };
        responsibility.put("handoffBusinessState",businessState);
        String frozenReason=reason+"\n【工单："+workOrder.getCode()+"；工序："+processLabel+"；发起人："+initiatorIdentity.get("name")
                +"（"+initiatorIdentity.get("domain")+"）；状态："+businessState+"】";
        var task=new MesActiveOrderHandoffTaskDO().setId(IdUtil.getSnowflakeNextId()).setActiveOrderId(order.getId())
                .setWorkOrderId(order.getWorkOrderId()).setRouteProcessId(routeProcessId).setTaskType(type)
                .setSourceType(source).setSourceId(sourceId).setRoundId(round).setCandidateUserSnapshot(receivers)
                .setResponsibilitySnapshotJson(JsonUtils.toJsonString(responsibility))
                .setInitiatedBy(initiator).setNotificationOnly(notificationOnly).setStatus(completed?"DONE":"TODO")
                .setReason(frozenReason).setRowVersion(0);
        task.setTenantId(tenant()); if(completed)task.setCompletedBy(initiator).setCompletedAt(LocalDateTime.now()).setCompletionSourceId(sourceId);
        task.setActionUrl(actionUrl(task));validate(task);
        require(tasks.insert(task)==1,"正式交接任务未持久化");
        audit.append("task-created",MesActiveOrderHandoffService.class.getName()+"#create","MES_HANDOFF_TASK",task.getId(),0,null,task,reason);
        delivery.schedule(task); return task;
    }
    private String actionUrl(MesActiveOrderHandoffTaskDO task) {
        String path=switch(task.getTaskType()) {
            case "PRODUCTION_HANDOFF","PRODUCTION_RETURN" -> "/mes/pro/feedback/edhr-batch-production-fill";
            case "PQC_HANDOFF","PQC_RETURN" -> "/mes/pro/feedback/edhr-batch-pqc-fill";
            case "PQC_REVIEW" -> "/mes/pro/process-pool/pqc-leader";
            case "QA_REVIEW" -> "/mes/pro/feedback/edhr-nonconformance-review";
            case "QA_DECISION_HANDOFF" -> "DONE".equals(task.getStatus())?"/user/profile":"/mes/pro/process-pool/production-leader";
            default -> "/mes/pro/process-pool/production-leader";
        };
        String sourceKey=switch(task.getSourceType()) {case "PROCESS_POOL_EVENT",EVENT_SIGNED_REVISION,EVENT_REJECTED_REVIEW->"eventId";case "PQC_INSPECTION_TASK"->"pqcTaskId";case "NONCONFORMANCE_REVIEW"->"reviewId";case "ACTIVE_ORDER"->"cycleId";default->throw failure("交接来源类型无效");};
        String url=path+"?activeOrderId="+task.getActiveOrderId()+"&"+sourceKey+"="+task.getSourceId()+"&handoffTaskId="+task.getId()+"&roundId="+task.getRoundId()+"&handoffType="+task.getTaskType();
        if("QA_DECISION_HANDOFF".equals(task.getTaskType())&&"DONE".equals(task.getStatus()))url+="&tab=notifyMessage";
        if(task.getTaskType().endsWith("_RETURN"))url+="&returnTaskId="+task.getId()+"&rejectedReviewId="+task.getRoundId();
        return url;
    }
    private MesProProcessPoolEventDO event(Long id) { var e=events.selectById(id);require(e!=null&&Objects.equals(e.getTenantId(),tenant()),"正式提交事件不存在");return e; }
    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    public Long active(MesProProcessPoolEventDO event) {
        if("PQC_INSPECTION".equals(event.getEventType())) {
            return owners.pqcTask(event).getActiveOrderId();
        }
        var node=JsonUtils.parseTree(event.getRawPayload()).path("activeOrderId");require(node.isIntegralNumber()&&node.longValue()>0,"生产提交缺少冻结活跃周期身份");return node.longValue();
    }
    private Long submissionRouteProcess(MesProProcessPoolEventDO event,MesProcessPoolActiveOrderDO order) {
        if("PRODUCTION_SUBMIT".equals(event.getEventType())) return event.getRouteProcessId();
        return pqcRouteProcess(owners.pqcTask(event),order);
    }
    private Long pqcRouteProcess(MesPqcInspectionTaskDO task,MesProcessPoolActiveOrderDO order) {
        require(task!=null&&task.getId()!=null&&task.getId()>0&&Objects.equals(task.getTenantId(),tenant())
                &&Objects.equals(task.getActiveOrderId(),order.getId())&&Objects.equals(task.getWorkOrderId(),order.getWorkOrderId())
                &&Objects.equals(task.getRouteId(),order.getRouteId()),"PQC正式任务不属于当轮工单、路线或活跃周期");
        var routeProcess=routeProcesses.selectById(task.getRouteProcessId());
        require(routeProcess!=null&&Objects.equals(routeProcess.getId(),task.getRouteProcessId())
                &&Objects.equals(routeProcess.getRouteId(),task.getRouteId())&&Objects.equals(routeProcess.getProcessId(),task.getProcessId()),
                "PQC正式任务的路线工序与MES工序关联不一致");
        return task.getRouteProcessId();
    }
    private MesProcessPoolActiveOrderDO order(Long id,boolean active) {
        var order=orders.selectByIdForUpdate(id);require(order!=null&&Objects.equals(order.getTenantId(),tenant()),"活跃周期不存在");
        if(active)require("ACTIVE".equals(order.getActiveStatus()),"旧周期已关闭，禁止办理");return order;
    }
    public record NavigationContext(MesActiveOrderHandoffTaskDO task, boolean current, boolean processable,
            boolean profileLeaderCorrection) { }
    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    public NavigationContext navigationContext(Long taskId,Long actor) {
        var task=tasks.selectById(taskId);access(task,actor);validate(task);
        boolean isCurrent=current(task);
        require(!"CANCELED".equals(task.getStatus()),"旧周期交接已取消，请在历史记录中查看，禁止办理新周期");
        require(isCurrent||("QA_DECISION_HANDOFF".equals(task.getTaskType())&&"DONE".equals(task.getStatus())
                &&task.getReason().startsWith("void：")),"旧周期交接已失效，禁止办理；请查看原周期历史");
        boolean profileCorrection=false;
        if(isCurrent && "TODO".equals(task.getStatus()) && Set.of("PRODUCTION_REVIEW","PQC_REVIEW").contains(task.getTaskType())) {
            var previous=reviews.selectLatestByEventIdForUpdate(task.getSourceId());
            if(EVENT_REJECTED_REVIEW.equals(task.getSourceType())) {
                require("PRODUCTION_REVIEW".equals(task.getTaskType()),"临时人员补正只能属于原生产复核");
                require(previous!=null && "REJECTED".equals(previous.getReviewStatus()) && Objects.equals(previous.getId(),task.getRoundId()),"临时人员更正交接不是当前拒绝轮次");
                var event=event(task.getSourceId());
                require("LEADER_PROFILE_RETURN".equals(JsonUtils.parseTree(task.getResponsibilitySnapshotJson()).path("correctionOrigin").asText())
                        && !owners.submissionIdentity(event).isSystemUser()
                        && Objects.equals(actor,previous.getLeaderUserId()) && Objects.equals(actor,owners.profileProductionLeader(event)),
                        "临时人员更正交接缺少原组长正式责任");
                profileCorrection=true;
            } else if(EVENT_SIGNED_REVISION.equals(task.getSourceType())) {
                var correction=correctionResolver.find(event(task.getSourceId()),previous);
                require(correction!=null && Objects.equals(task.getRoundId(),correction.getId()),
                        "再审交接不是当前正式签名补正轮次");
            }
        }
        return new NavigationContext(task,isCurrent,isCurrent&&"TODO".equals(task.getStatus()),profileCorrection);
    }
    @Transactional(readOnly=true,propagation=Propagation.SUPPORTS)
    public boolean current(MesActiveOrderHandoffTaskDO task) {
        var order=orders.selectById(task.getActiveOrderId());return order!=null&&Objects.equals(tenant(),order.getTenantId())&&"ACTIVE".equals(order.getActiveStatus());
    }
}
