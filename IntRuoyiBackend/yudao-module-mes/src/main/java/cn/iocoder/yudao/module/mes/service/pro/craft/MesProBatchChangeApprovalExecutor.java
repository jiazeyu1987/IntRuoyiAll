package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.businessapproval.model.*;
import cn.iocoder.yudao.module.bpm.businessapproval.service.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftChangeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.MesProCraftChangeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.service.pro.route.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import static cn.iocoder.yudao.module.mes.service.pro.craft.MesProCraftParameters.invalid;
import static cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteVersionLifecycleServiceImpl.*;

/** 一张变更单对应一个审批请求；全部候选和主数据在同一事务中生效。 */
@Component
@Transactional(rollbackFor = Exception.class)
public class MesProBatchChangeApprovalExecutor implements BusinessApprovalEffectExecutor {
    public static final String CODE = "MES_ROUTE_BATCH_CHANGE_PUBLISH";
    public static final String OBJECT_TYPE = "ROUTE_BATCH_CHANGE";
    public static final String TREE_CHANGE_ID = "routeTreeChangeId";
    @Resource private MesProCraftService crafts;
    @Resource private MesProCraftChangeMapper changes;
    @Resource private MesProRouteVersionMapper versions;
    @Resource private MesProRouteControlledContentAdapter platform;
    @Resource private ObjectProvider<BusinessApprovalOrchestrator> orchestrator;

    @Override public String getExecutorCode() { return CODE; }
    @Override public String getBpmProcessDefinitionKey() { return "mes-route-batch-change-approval-v1"; }

    public void submit(Long id) {
        crafts.lockCatalog();
        MesProCraftChangeDO change = requireChange(id);
        Long actor = SecurityFrameworkUtils.getLoginUserId();
        requireActor(actor);
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("businessType", CODE);
        variables.put("changeId", id.toString());
        variables.put("changeReason", change.getReason());
        variables.put("affectedRouteCount", candidateIds(change).size());
        variables.put("changeFingerprint", fingerprint(change));
        if ("TREE".equals(change.getChangeType())) {
            MesProRouteVersionDO root = requireTreeRoot(change);
            JSONObject snapshot = JSON.parseObject(root.getRouteSnapshotJson());
            variables.put("businessType", MesProRouteVersionBusinessApprovalEffectExecutor.EXECUTOR_CODE);
            variables.put(TREE_CHANGE_ID, id.toString());
            variables.put("routeId", root.getRouteId());
            variables.put("routeVersionId", root.getId());
            variables.put("routeVersionNo", root.getVersionNo());
            variables.put("routeCode", snapshot.getString("routeCode"));
            variables.put("routeName", snapshot.getString("routeName"));
            // 目录作为普通路线版本解析同一策略和流程，审批结果仍整批生效。
            orchestrator.getObject().submit(BusinessApprovalContext.builder()
                    .tenantId(TenantContextHolder.getRequiredTenantId()).dataDomain("MES").systemCode("MES")
                    .objectType("ROUTE_VERSION").objectId(root.getId().toString()).objectVersion(root.getVersionNo())
                    .actionCode("PUBLISH").objectState(STATUS_DRAFT).applicantUserId(actor)
                    .reason(change.getReason()).variables(variables).build());
            return;
        }
        orchestrator.getObject().submit(BusinessApprovalContext.builder()
                .tenantId(TenantContextHolder.getRequiredTenantId()).dataDomain("MES").systemCode("MES")
                .objectType("TREE".equals(change.getChangeType()) ? "ROUTE_TREE_CHANGE" : OBJECT_TYPE).objectId(id.toString()).objectVersion("1")
                .actionCode("PUBLISH").objectState("PREPARING").applicantUserId(actor)
                .reason(change.getReason()).variables(variables).build());
    }

    @Override public void precheck(BusinessApprovalContext context) {
        MesProCraftChangeDO change = requireContext(context);
        requireStatus(change, "PREPARING");
        requireCandidates(change, STATUS_DRAFT);
    }

    @Override public BusinessApprovalEffectResult executeDirect(BusinessApprovalContext context, BusinessApprovalRequest request) {
        MesProCraftChangeDO change = requireContext(context);
        requireStatus(change, "PREPARING");
        attach(change, request, null);
        return publish(change, context.getApplicantUserId(), STATUS_DRAFT, "BUSINESS_APPROVAL:" + request.getRequestId() + ":DIRECT_APPROVED");
    }

    @Override public BusinessApprovalEffectResult markPending(BusinessApprovalContext context, BusinessApprovalRequest request) {
        MesProCraftChangeDO change = requireContext(context);
        requireStatus(change, "PREPARING");
        String processId = request.getProcessInstanceId();
        if (processId == null || processId.isBlank()) throw invalid("批量变更审批流程未启动");
        requireActor(context.getApplicantUserId());
        for (MesProRouteVersionDO version : requireCandidates(change, STATUS_DRAFT)) {
            platform.recordSubmitted(version, context.getApplicantUserId(), processId);
            version.setLifecycleStatus(STATUS_PENDING_APPROVAL);
            version.setSubmittedBy(context.getApplicantUserId());
            version.setSubmittedTime(LocalDateTime.now());
            version.setApprovalProcessInstanceId(processId);
            saveVersion(version);
        }
        attach(change, request, processId);
        return BusinessApprovalEffectResult.pending("PENDING");
    }

    @Override public BusinessApprovalEffectResult executeApproved(BusinessApprovalContext context, BusinessApprovalRequest request, Long actor) {
        MesProCraftChangeDO change = requireContext(context);
        requireRequest(change, request);
        if ("CANCELLED".equals(change.getStatus())) return BusinessApprovalEffectResult.cancelled("CANCELLED");
        if ("APPLIED".equals(change.getStatus())) return BusinessApprovalEffectResult.completed("APPLIED");
        requireStatus(change, "PENDING");
        return publish(change, actor, STATUS_PENDING_APPROVAL, "BUSINESS_APPROVAL:" + request.getProcessInstanceId() + ":APPROVED");
    }

    @Override public BusinessApprovalEffectResult reject(BusinessApprovalContext context, BusinessApprovalRequest request, Long actor, String reason) {
        terminate(context, request, actor, reason, true);
        return BusinessApprovalEffectResult.rejected("CANCELLED");
    }
    @Override public BusinessApprovalEffectResult cancel(BusinessApprovalContext context, BusinessApprovalRequest request, Long actor, String reason) {
        terminate(context, request, actor, reason, false);
        return BusinessApprovalEffectResult.cancelled("CANCELLED");
    }

    private void terminate(BusinessApprovalContext context, BusinessApprovalRequest request, Long actor, String reason, boolean rejected) {
        MesProCraftChangeDO change = requireContext(context);
        requireRequest(change, request);
        if ("CANCELLED".equals(change.getStatus())) return;
        requireStatus(change, "PENDING");
        requireActor(actor);
        for (MesProRouteVersionDO version : requireCandidates(change, STATUS_PENDING_APPROVAL)) {
            if (rejected) platform.recordRejected(version, actor, reason, "BUSINESS_APPROVAL:" + request.getProcessInstanceId() + ":REJECTED");
            else platform.recordWithdrawn(version, actor);
            version.setLifecycleStatus(rejected ? STATUS_REJECTED : STATUS_DRAFT);
            saveVersion(version);
        }
        crafts.abortBatchFromApproval(change, actor);
    }

    private BusinessApprovalEffectResult publish(MesProCraftChangeDO change, Long actor, String expected, String event) {
        requireActor(actor);
        List<MesProRouteVersionDO> candidates = requireCandidates(change, expected);
        for (MesProRouteVersionDO version : candidates) {
            if (STATUS_DRAFT.equals(expected)) {
                platform.recordSubmitted(version, actor, null);
                version.setSubmittedBy(actor);
                version.setSubmittedTime(LocalDateTime.now());
            }
            version.setLifecycleStatus(STATUS_READY_TO_PUBLISH);
            saveVersion(version);
            platform.recordApproved(version, actor, event);
        }
        if (candidates.isEmpty()) crafts.completeEmptyBatch(change);
        else crafts.onRouteReady(candidates.get(0), actor);
        if (!"APPLIED".equals(requireChange(change.getId()).getStatus())) throw invalid("整批变更未完成发布");
        return BusinessApprovalEffectResult.completed("APPLIED");
    }

    private void attach(MesProCraftChangeDO change, BusinessApprovalRequest request, String processId) {
        if (request == null || request.getRequestId() == null) throw invalid("批量变更审批请求缺失");
        change.setApprovalRequestId(request.getRequestId());
        change.setApprovalProcessInstanceId(processId);
        change.setStatus("PENDING");
        if (changes.updateById(change) != 1) throw invalid("批量变更审批状态保存失败");
    }
    private MesProCraftChangeDO requireContext(BusinessApprovalContext context) {
        crafts.lockCatalog();
        if (context == null || !Objects.equals(TenantContextHolder.getRequiredTenantId(), context.getTenantId())
                || !"MES".equals(context.getSystemCode()) || !"MES".equals(context.getDataDomain())
                || !Set.of(OBJECT_TYPE, "ROUTE_TREE_CHANGE").contains(context.getObjectType()) || !"PUBLISH".equals(context.getActionCode())
                || !"PREPARING".equals(context.getObjectState())) throw invalid("批量变更审批上下文不正确");
        Long id;
        try { id = Long.valueOf(context.getObjectId()); } catch (RuntimeException e) { throw invalid("批量变更编号不正确"); }
        MesProCraftChangeDO change = requireChange(id);
        if (!Objects.equals("TREE".equals(change.getChangeType()) ? "ROUTE_TREE_CHANGE" : OBJECT_TYPE, context.getObjectType()))
            throw invalid("审批对象与变更类型不一致");
        if (context.getVariables() == null || !Objects.equals(fingerprint(change), context.getVariables().get("changeFingerprint")))
            throw invalid("批量变更内容或影响范围已变化，需要重新提交审批");
        return change;
    }
    private MesProCraftChangeDO requireChange(Long id) {
        MesProCraftChangeDO change = changes.selectById(id);
        if (change == null || !"BATCH".equals(change.getApprovalScope())) throw invalid("批量变更审批单不存在");
        return change;
    }
    /** 将普通路线审批保存的目录身份还原为整批身份，不重新解析审批策略。 */
    public BusinessApprovalContext treeContext(BusinessApprovalContext routeContext) {
        if (routeContext == null || !"ROUTE_VERSION".equals(routeContext.getObjectType())
                || !STATUS_DRAFT.equals(routeContext.getObjectState()) || routeContext.getVariables() == null)
            throw invalid("目录路线审批上下文不正确");
        Long changeId;
        try { changeId = Long.valueOf(String.valueOf(routeContext.getVariables().get(TREE_CHANGE_ID))); }
        catch (RuntimeException ex) { throw invalid("目录整批变更编号无效"); }
        MesProCraftChangeDO change = requireChange(changeId);
        MesProRouteVersionDO root = requireTreeRoot(change);
        if (!Objects.equals(root.getId().toString(), routeContext.getObjectId())
                || !Objects.equals(root.getVersionNo(), routeContext.getObjectVersion()))
            throw invalid("目录审批版本与整批变更不一致");
        BusinessApprovalContext batchContext = routeContext.toBuilder().objectType("ROUTE_TREE_CHANGE")
                .objectId(changeId.toString()).objectVersion("1").objectState("PREPARING").build();
        requireContext(batchContext);
        return batchContext;
    }
    private MesProRouteVersionDO requireTreeRoot(MesProCraftChangeDO change) {
        if (!"TREE".equals(change.getChangeType())) throw invalid("不是目录整批变更");
        JSONObject proposal = JSON.parseObject(change.getAfterJson());
        Long rootId = proposal.getLong("approvalRouteVersionId");
        if (rootId == null || !candidateIds(change).contains(rootId)) throw invalid("目录审批版本缺失");
        MesProRouteVersionDO root = versions.selectById(rootId);
        if (root == null || !Objects.equals(root.getRouteId(), proposal.getLong("rootRouteId"))
                || !Objects.equals(crafts.groupId(root), change.getId())) throw invalid("目录审批版本归属不正确");
        return root;
    }
    private List<Long> candidateIds(MesProCraftChangeDO change) { return JSON.parseArray(change.getCandidateIdsJson()).toJavaList(Long.class); }
    private List<MesProRouteVersionDO> requireCandidates(MesProCraftChangeDO change, String status) {
        List<MesProRouteVersionDO> result = new ArrayList<>();
        for (Long id : candidateIds(change)) {
            MesProRouteVersionDO version = versions.selectById(id);
            if (version == null || !Objects.equals(crafts.groupId(version), change.getId())
                    || !status.equals(version.getLifecycleStatus()) || Boolean.TRUE.equals(version.getActive())
                    || !Objects.equals(JSON.parseObject(change.getCandidateHashesJson()).getString(id.toString()), version.getRouteSnapshotSha256()))
                throw invalid("关联路线版本或状态已变化，整批不能生效");
            result.add(version);
        }
        return result;
    }
    private void requireRequest(MesProCraftChangeDO change, BusinessApprovalRequest request) {
        if (request == null || !Objects.equals(change.getApprovalRequestId(), request.getRequestId())
                || !Objects.equals(change.getApprovalProcessInstanceId(), request.getProcessInstanceId())) throw invalid("审批请求与批量变更不一致");
    }
    private void requireStatus(MesProCraftChangeDO change, String status) {
        if (!status.equals(change.getStatus())) throw invalid("批量变更状态不允许当前操作");
    }
    private void requireActor(Long actor) { if (actor == null || actor <= 0) throw invalid("批量变更操作人缺失"); }
    private void saveVersion(MesProRouteVersionDO version) { if (versions.updateById(version) != 1) throw invalid("关联路线状态保存失败"); }
    static String fingerprint(MesProCraftChangeDO change) {
        return DigestUtil.sha256Hex(JSON.toJSONString(Arrays.asList(change.getId(), change.getChangeType(),
                change.getBeforeJson(), change.getAfterJson(), change.getCandidateIdsJson(), change.getCandidateHashesJson(), change.getReason())));
    }
}
