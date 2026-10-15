package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileRouteSnapshotDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver;
import cn.iocoder.yudao.module.dcc.service.route.DccActionApprovalRoutePolicy;
import cn.iocoder.yudao.module.dcc.service.route.DccFixedApprovalRoutePolicy;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.ROUTE_PREVIEW_APPROVER_NOT_FOUND;

@Service
public class DccControlledFileApprovalRouteAssigneeResolver {
    private final DccApplicationCustomMatrixService customMatrices=new DccApplicationCustomMatrixService();
    @Resource private DccApplicationRoundService customRounds;
    public record CustomRouteSource(DccApplicationCustomMatrixService.PreviewSourceContext previewContext,
            DccApplicationCustomMatrixService.SourceContext reservedContext,DccApplicationCustomMatrixService.CanonicalSelection selection,
            DccDirectSupervisorSource.Evidence evidence,DccApprovalMatrixTokenService.CustomSupervisorProof proof) {}
    public ResolvedRoute resolveCustomPreview(Long actor,DccApplicationCustomMatrixService.PreviewSourceContext context,DccApplicationCustomMatrixService.CanonicalSelection selection){
        var saved=customMatrices.readPreviewCanonical(selection.canonical(),selection.digest(),context);
        var evidence=requireCustomOrganization(actor,context.tenantId(),context.requesterId(),null,context.projectId(),context.applicationType());
        var proof=nativeSourceTokens.issueCustomSupervisorPreview(context,selection,evidence);
        return customRoute(saved,evidence,new CustomRouteSource(context,null,selection,evidence,proof));
    }
    public ResolvedRoute resolveCustomReserved(Long actor,DccApplicationCustomMatrixService.SourceContext context,DccApplicationCustomMatrixService.CanonicalSelection selection){
        var saved=customMatrices.readCanonical(selection.canonical(),selection.digest(),context);
        var evidence=requireCustomOrganization(actor,context.tenantId(),context.requesterId(),context.controlledFileId(),context.projectId(),context.applicationType());
        var proof=nativeSourceTokens.issueCustomSupervisorReserved(context,selection,evidence);
        return customRoute(saved,evidence,new CustomRouteSource(null,context,selection,evidence,proof));
    }
    public void requireCustomExpectation(ResolvedRoute route,String digest,String token){
        var source=route.applicationCustomSource();if(source==null)throw new IllegalArgumentException("CUSTOM_ROUTE_SOURCE_REQUIRED");
        if(source.reservedContext()!=null)nativeSourceTokens.requireCustomSupervisorReserved(source.reservedContext(),source.selection(),source.evidence(),digest,token);
        else nativeSourceTokens.requireCustomSupervisorPreview(source.previewContext(),source.selection(),source.evidence(),digest,token);
    }
    private DccDirectSupervisorSource.Evidence requireCustomOrganization(Long actor,Long tenant,Long requester,Long file,Long project,String type){
        var login=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if(login==null||!Objects.equals(login.getId(),actor)||!Objects.equals(login.getTenantId(),tenant)||!Objects.equals(tenant,TenantContextHolder.getRequiredTenantId())
                ||!Integer.valueOf(2).equals(login.getUserType())||!Objects.equals(actor,requester))throw new IllegalArgumentException("CUSTOM_SOURCE_AUTHENTICATED_ACTOR_INVALID");
        requireNativeAccount(adminUserApi.getUser(requester),requester,tenant);
        var provider=nativeSupervisors.getIfAvailable();if(provider==null)throw new NativeSupervisorNotReadyException();
        var evidence=provider.require(new DccDirectSupervisorSource.Context(tenant,requester,file,project,type));
        if(evidence==null||!Objects.equals(evidence.tenantId(),tenant)||!Objects.equals(evidence.requesterId(),requester))throw new IllegalArgumentException("CUSTOM_SUPERVISOR_SOURCE_IDENTITY_INVALID");
        requireNativeAccount(adminUserApi.getUser(evidence.supervisorUserId()),evidence.supervisorUserId(),tenant);return evidence;
    }
    private ResolvedRoute customRoute(DccApplicationCustomMatrixService.SavedSelection saved,DccDirectSupervisorSource.Evidence evidence,CustomRouteSource source){
        var result=new ArrayList<ResolvedRouteNode>();
        result.add(new ResolvedRouteNode(0,"SUPERVISOR_REVIEW","直接主管独立审核与签名",0,"USER",evidence.supervisorUserId(),List.of(evidence.supervisorUserId()),"ANY",null,false,List.of(evidence.supervisorUserId())));
        for(var node:customMatrices.compile(DccApplicationCustomMatrix.ApprovalSourceMode.APPLICATION_CUSTOM,saved.applicationCustomMatrix(),saved.selectedSignoffDepartmentIds())){
            var users="DEPT".equals(node.candidateSourceType())?resolveDepartmentLeaders(node.candidateSourceIds(),false):node.candidateSourceIds();
            if("USER".equals(node.candidateSourceType()))validateResolvedUserIds(users,false);
            result.add(new ResolvedRouteNode(node.stageOrder(),node.stageCode(),switch(node.stageCode()){case "MATRIX_REVIEW"->"会签指派与实际签名";case "MATRIX_APPROVAL"->"批准并确认文件负责人";default->"文控标准化审核";},node.stageOrder(),node.candidateSourceType(),node.candidateSourceIds().get(0),node.candidateSourceIds(),node.approveMethod(),node.approveRatio(),node.requireAllApprovals(),users));
        }
        return new ResolvedRoute(null,List.copyOf(result),null,DccApplicationCustomMatrixService.FORMAT,List.of(),null,source);
    }
    @Resource private org.springframework.beans.factory.ObjectProvider<DccDirectSupervisorSource> nativeSupervisors;
    @Resource private DccApprovalMatrixTokenService nativeSourceTokens;
    public record NativeApprovalSource(ResolvedRoute route,DccDirectSupervisorSource.Evidence evidence,DccApprovalMatrixTokenService.NativeSupervisorProof proof){
        public String sourceDigest(){return proof.sourceDigest();}
        public String sourceToken(){return proof.sourceToken();}
        public Map<String,Object> frozenSource(){return Map.of("payload",proof.payload(),"sourceDigest",sourceDigest(),"sourceToken",sourceToken());}
    }
    public static final class NativeSupervisorNotReadyException extends IllegalStateException {
        public NativeSupervisorNotReadyException(){super("DIRECT_SUPERVISOR_SOURCE_NOT_CONFIGURED");}
    }
    public NativeApprovalSource resolvePublicNative(DccApprovalMatrixSourceService.Source source,Long actor,String action,Long fileId,Long projectId,String contextKey,String exactId,String exactDigest,List<Long> selectedDepartments){
        return attachPublicNative(resolvePublicMatrix(source,actor,action,selectedDepartments),actor,action,fileId,projectId,contextKey,exactId,exactDigest,selectedDepartments);
    }
    public NativeApprovalSource attachPublicNative(ResolvedRoute derived,Long actor,String action,Long fileId,Long projectId,String contextKey,String exactId,String exactDigest,List<Long> selectedDepartments){
        Long tenant=TenantContextHolder.getRequiredTenantId();var login=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if(derived==null||derived.sourceApprovalMatrix()==null||!java.util.Set.of("NEW","REVISION").contains(action==null?"":action)||actor==null||actor<=0||projectId==null||projectId<=0||fileId!=null&&fileId<=0||login==null||!Objects.equals(login.getId(),actor)||!Objects.equals(login.getTenantId(),tenant)||!Integer.valueOf(2).equals(login.getUserType()))throw new IllegalArgumentException("NATIVE_SUPERVISOR_AUTHENTICATED_CONTEXT_INVALID");
        var baseline=derived.sourceApprovalMatrix();if(!Objects.equals(baseline.getTenantId(),tenant))throw new IllegalArgumentException("NATIVE_SUPERVISOR_MATRIX_TENANT_INVALID");
        var applicant=adminUserApi.getUser(actor);requireNativeAccount(applicant,actor,tenant);
        var provider=nativeSupervisors.getIfAvailable();if(provider==null)throw new NativeSupervisorNotReadyException();
        var evidence=provider.require(new DccDirectSupervisorSource.Context(tenant,actor,fileId,projectId,"NEW".equals(action)?"UPLOAD":"REVISION"));
        if(evidence==null||!Objects.equals(evidence.tenantId(),tenant)||!Objects.equals(evidence.requesterId(),actor)||evidence.supervisorUserId()==null||evidence.supervisorUserId()<=0||!exactSourceText(evidence.sourceSystem())||!exactSourceText(evidence.sourceIdentity())||!exactSourceText(evidence.sourceVersion()))throw new IllegalArgumentException("NATIVE_SUPERVISOR_SOURCE_IDENTITY_INVALID");
        requireNativeAccount(adminUserApi.getUser(evidence.supervisorUserId()),evidence.supervisorUserId(),tenant);
        var nodes=new ArrayList<ResolvedRouteNode>();nodes.add(new ResolvedRouteNode(0,"SUPERVISOR_REVIEW","直接主管独立审核与签名",0,"USER",evidence.supervisorUserId(),List.of(evidence.supervisorUserId()),"ANY",null,false,List.of(evidence.supervisorUserId())));derived.nodes().stream().filter(node->!"SUPERVISOR_REVIEW".equals(node.stageCode())).forEach(nodes::add);
        if(!nodes.stream().map(ResolvedRouteNode::stageCode).toList().equals(List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW")))throw new IllegalArgumentException("NATIVE_SUPERVISOR_STAGE_CONTRACT_INVALID");
        var signoff=derived.nodes().stream().filter(node->"MATRIX_REVIEW".equals(node.stageCode())).toList();
        if(signoff.size()!=1 || !"DEPT".equals(signoff.get(0).candidateSourceType()))throw new IllegalArgumentException("NATIVE_SUPERVISOR_DEPARTMENT_OBLIGATIONS_INVALID");
        var actualDepartments=signoff.get(0).candidateSourceIds();
        if(selectedDepartments!=null && !new java.util.TreeSet<>(selectedDepartments).equals(new java.util.TreeSet<>(actualDepartments)))
            throw new IllegalArgumentException("NATIVE_SUPERVISOR_DEPARTMENT_EXPECTATION_INVALID");
        var proof=nativeSourceTokens.issueNativeSupervisor(new DccApprovalMatrixTokenService.Context(tenant,actor,projectId,baseline.getCategoryId(),baseline.getFileTypeTaxonomyId(),action,fileId,contextKey),baseline.getMatrixDigest(),exactId,exactDigest,actualDepartments,evidence);
        var result=new NativeApprovalSource(new ResolvedRoute(derived.route(),List.copyOf(nodes),baseline,derived.derivationVersion(),derived.runtimeGroups()),evidence,proof);
        return new NativeApprovalSource(new ResolvedRoute(derived.route(),List.copyOf(nodes),baseline,derived.derivationVersion(),derived.runtimeGroups(),result.frozenSource()),evidence,proof);
    }
    public void requireNativeExpectation(NativeApprovalSource source,String expectedDigest,String token){nativeSourceTokens.requireNativeSupervisor(source.proof(),expectedDigest,token);}
    private boolean exactSourceText(String value){return value!=null&&!value.isBlank()&&value.equals(value.strip())&&value.length()<=1024;}
    private void requireNativeAccount(AdminUserRespDTO user,Long id,Long tenant){if(user==null||!Objects.equals(user.getId(),id)||!Objects.equals(user.getTenantId(),tenant)||!Integer.valueOf(0).equals(user.getStatus())||user.getPostIds()==null||user.getPostIds().isEmpty())throw new IllegalArgumentException("NATIVE_SUPERVISOR_ENABLED_ACCOUNT_REQUIRED");}

    @Resource
    private DccCategoryApprovalRouteMapper routeMapper;
    @Resource
    private DccCategoryApprovalRouteNodeMapper routeNodeMapper;
    @Resource
    private DccPositionAssignmentMapper positionAssignmentMapper;
    @Resource
    private DccApprovalPositionRuntimeResolver positionRuntimeResolver;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private DccApprovalParticipantPostValidator approvalParticipantPostValidator;
    @Resource private DccApprovalMatrixSourceService publicMatrixSource;

    public ResolvedRoute resolvePublicMatrix(DccApprovalMatrixSourceService.Source source,Long actorId,String actionType,List<Long> selectedDepartments){
        if(source==null||!"LEGACY".equals(source.route().getActionType()))throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.Projection projection;
        try{projection=cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.project(source.rawNodes(),actionType,selectedDepartments);}
        catch(cn.iocoder.yudao.module.dcc.service.route.DccApprovalMatrixCapabilityException unsupported){
            throw exception(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_READY,unsupported.getMessage());
        }
        cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.validateRuntime(projection.nodes(),actionType);
        return new ResolvedRoute(source.route(),projection.nodes().stream().map(node->resolveRouteNode(node,actorId,false)).toList(),
                source.baseline(),cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.VERSION,projection.groups());
    }

    public Map<String, List<Long>> resolveStartUserSelectAssignees(DccControlledFileDO file, Long submitterUserId) {
        return resolveStartUserSelectAssignees(file, submitterUserId,
                file == null ? null : DccControlledFileProcessDefinitionKeys.toActionType(file.getProcessDefinitionKey()));
    }

    public Map<String, List<Long>> resolveStartUserSelectAssignees(DccControlledFileDO file, Long submitterUserId,
                                                                   String actionType) {
        if (file == null || file.getCategoryId() == null) {
            throw exception(CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
        }
        Map<String, List<Long>> startUserSelectAssignees = buildStartUserSelectAssigneeMap(
                resolveRoute(file.getCategoryId(), submitterUserId, actionType).nodes());
        if (startUserSelectAssignees.isEmpty()) {
            throw exception(CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
        }
        return startUserSelectAssignees;
    }

    public ResolvedRoute resolveRoute(Long categoryId, Long submitterUserId) {
        return resolveRoute(categoryId, submitterUserId, true);
    }

    public ResolvedRoute resolveRoute(Long categoryId, Long submitterUserId, String actionType) {
        return resolveRoute(categoryId, submitterUserId, actionType, true);
    }

    public ResolvedRoute resolveRouteForReadiness(Long categoryId, Long submitterUserId) {
        return resolveRoute(categoryId, submitterUserId, false);
    }

    public ResolvedRoute resolveRouteForReadiness(Long categoryId, Long submitterUserId, String actionType) {
        return resolveRoute(categoryId, submitterUserId, actionType, false);
    }

    public ResolvedRoute resolveRouteForReadiness(Long categoryId, Long submitterUserId, String actionType,
                                                 List<Long> selectedDepartments) {
        return resolveRoute(categoryId, submitterUserId, actionType, false, selectedDepartments);
    }

    public ResolvedRoute resolveRoute(Long categoryId, Long submitterUserId, String actionType,
                                      List<Long> selectedDepartments) {
        return resolveRoute(categoryId, submitterUserId, actionType, true, selectedDepartments);
    }

    private ResolvedRoute resolveRoute(Long categoryId, Long submitterUserId, boolean requireConfiguredPosts) {
        return resolveRoute(categoryId, submitterUserId, null, requireConfiguredPosts);
    }

    private ResolvedRoute resolveRoute(Long categoryId, Long submitterUserId, String actionType,
                                       boolean requireConfiguredPosts) {
        return resolveRoute(categoryId, submitterUserId, actionType, requireConfiguredPosts, null);
    }

    private ResolvedRoute resolveRoute(Long categoryId, Long submitterUserId, String actionType,
                                       boolean requireConfiguredPosts, List<Long> selectedDepartments) {
        List<Long> selected = normalizeSelectedDepartments(selectedDepartments);
        if (selected != null && !DccActionApprovalRoutePolicy.supports(actionType))
            throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        DccCategoryApprovalRouteDO route = StrUtil.isBlank(actionType)
                ? routeMapper.selectLatestActiveByCategoryId(categoryId)
                : routeMapper.selectLatestActiveByCategoryIdAndActionType(categoryId, actionType);
        if (route == null) {
            throw exception(CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
        }
        List<DccCategoryApprovalRouteNodeDO> routeNodes = routeNodeMapper.selectListByRouteId(route.getId()).stream()
                .sorted(Comparator.comparing(DccCategoryApprovalRouteNodeDO::getStageOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getStageNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        if (routeNodes.isEmpty()) {
            throw exception(CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
        }
        if (StrUtil.isBlank(actionType)) {
            DccFixedApprovalRoutePolicy.validateRouteNodes(routeNodes, CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        } else {
            DccActionApprovalRoutePolicy.validateRouteNodes(actionType, routeNodes, CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        }
        List<ResolvedRouteNode> resolvedNodes = routeNodes.stream()
                .map(routeNode -> selected != null && "MATRIX_REVIEW".equals(routeNode.getStageCode())
                        ? resolveSelectedDepartmentNode(routeNode, selected, requireConfiguredPosts)
                        : resolveRouteNode(routeNode, submitterUserId, requireConfiguredPosts))
                .toList();
        if (toPendingStatus(resolvedNodes.get(0).stageCode()) == null) {
            throw exception(CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
        }
        return new ResolvedRoute(route, resolvedNodes);
    }

    private List<Long> normalizeSelectedDepartments(List<Long> selected) {
        if (selected == null) return null;
        if (selected.isEmpty()) throw exception(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_READY,
                "请至少选择一个会签部门");
        if (selected.stream().anyMatch(id -> id == null || id <= 0)
                || selected.stream().distinct().count() != selected.size())
            throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        return selected.stream().sorted().toList();
    }

    private ResolvedRouteNode resolveSelectedDepartmentNode(DccCategoryApprovalRouteNodeDO node,
            List<Long> selected, boolean requireConfiguredPosts) {
        if (!"DEPT".equals(node.getCandidateSourceType())) throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        return new ResolvedRouteNode(node.getStageNo(), node.getStageCode(), node.getStageName(), node.getStageOrder(),
                "DEPT", selected.get(0), selected, node.getApproveMethod(), node.getApproveRatio(),
                node.getRequireAllApprovals(), resolveDepartmentLeaders(selected, requireConfiguredPosts));
    }

    public Map<String, List<Long>> buildStartUserSelectAssigneeMap(List<ResolvedRouteNode> nodes) {
        return buildUniqueStageAssigneeMapForStart(nodes);
    }

    /** Restores exact original configuration; never resolves a later public matrix or copies a prior signature. */
    public ResolvedRoute restoreFrozenRoute(DccControlledFileDO source, List<DccControlledFileRouteSnapshotDO> snapshots) {
        if(source!=null&&"APPLICATION_CUSTOM".equals(source.getDraftApprovalSourceMode()))return restoreCustomFrozenRoute(source,snapshots);
        String actionType=source==null?null:DccControlledFileProcessDefinitionKeys.toActionType(source.getProcessDefinitionKey());
        if (source==null || source.getId()==null || source.getCategoryId()==null
                || !Objects.equals(source.getTenantId(),TenantContextHolder.getRequiredTenantId())
                || !DccActionApprovalRoutePolicy.supports(actionType) || snapshots==null
                || snapshots.size()!=(snapshots.stream().anyMatch(row->row!=null&&row.getDerivationVersion()!=null&&"SUPERVISOR_REVIEW".equals(row.getStageCode()))?4:DccActionApprovalRoutePolicy.stageNos(actionType).size())
                || snapshots.stream().anyMatch(Objects::isNull)) throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        var ordered=snapshots.stream().sorted(Comparator.comparing(DccControlledFileRouteSnapshotDO::getStageOrder,
                Comparator.nullsLast(Integer::compareTo))).toList();
        Integer version=ordered.get(0).getRouteVersionNo();
        if(version==null || version<=0) throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        var seen=new java.util.HashSet<Integer>();
        var restored=new ArrayList<ResolvedRouteNode>();
        boolean publicDerived=ordered.get(0).getDerivationVersion()!=null;
        for(var row:ordered) {
            if(row.getStageNo()==null || !seen.add(row.getStageNo()) || !Objects.equals(source.getId(),row.getControlledFileId())
                    || !Objects.equals(version,row.getRouteVersionNo()) || StrUtil.isBlank(row.getStageName()))
                throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            boolean supervisor=publicDerived&&Integer.valueOf(0).equals(row.getStageNo());
            if(supervisor){if(!"SUPERVISOR_REVIEW".equals(row.getStageCode())||!Integer.valueOf(0).equals(row.getStageOrder())||!"USER".equals(row.getCandidateSourceType())||!"ANY".equals(row.getApproveMethod())||row.getApproveRatio()!=null||!Boolean.FALSE.equals(row.getRequireAllApprovals()))throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);}
            var stage=supervisor?null:DccActionApprovalRoutePolicy.requireStage(row.getStageNo(),CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            if(!supervisor&&(!Objects.equals(stage.stageCode(),row.getStageCode()) || !Objects.equals(stage.stageOrder(),row.getStageOrder()))
                    || !publicDerived&&(!Objects.equals(stage.approveMethod(),row.getApproveMethod())
                    || !Objects.equals(stage.approveRatio(),row.getApproveRatio())
                    || !Objects.equals(stage.requireAllApprovals(),row.getRequireAllApprovals()))
                    || row.getCandidateSourceType()==null || !List.of("DEPT","USER","POSITION").contains(row.getCandidateSourceType()))
                throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            if(publicDerived&&!("ANY".equals(row.getApproveMethod())&&Boolean.FALSE.equals(row.getRequireAllApprovals())&&row.getApproveRatio()==null
                    ||"ALL".equals(row.getApproveMethod())&&Boolean.TRUE.equals(row.getRequireAllApprovals())&&Integer.valueOf(100).equals(row.getApproveRatio())))
                throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            var candidates=parseFrozenRouteIds(row.getCandidateSourceIds());
            var users=parseFrozenRouteIds(row.getResolvedUserIds());
            if(row.getCandidateSourceId()==null || !Objects.equals(row.getCandidateSourceId(),candidates.get(0))
                    || candidates.stream().distinct().count()!=candidates.size()
                    || "MATRIX_REVIEW".equals(row.getStageCode()) && !"DEPT".equals(row.getCandidateSourceType())
                    || "DEPT".equals(row.getCandidateSourceType()) && candidates.size()!=users.size())
                throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            restored.add(new ResolvedRouteNode(row.getStageNo(),row.getStageCode(),row.getStageName(),row.getStageOrder(),
                    row.getCandidateSourceType(),row.getCandidateSourceId(),candidates,row.getApproveMethod(),
                    row.getApproveRatio(),row.getRequireAllApprovals(),users));
        }
        if(!seen.containsAll(DccActionApprovalRoutePolicy.stageNos(actionType)))
            throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        var first=ordered.get(0);
        if(first.getDerivationVersion()!=null){
            if(!cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.VERSION.equals(first.getDerivationVersion())
                    ||first.getSourceApprovalMatrixId()==null||first.getSourceApprovalMatrixVersion()==null||first.getSourceApprovalMatrixDigest()==null
                    ||ordered.stream().anyMatch(row->!Objects.equals(row.getDerivationVersion(),first.getDerivationVersion())
                    ||!Objects.equals(row.getSourceApprovalMatrixId(),first.getSourceApprovalMatrixId())
                    ||!Objects.equals(row.getSourceApprovalMatrixVersion(),first.getSourceApprovalMatrixVersion())
                    ||!Objects.equals(row.getSourceApprovalMatrixDigest(),first.getSourceApprovalMatrixDigest())))
                throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            if(first.getSourceApprovalMatrixJson()==null||first.getRuntimeApprovalNodesJson()==null||ordered.stream().anyMatch(row->
                    !Objects.equals(row.getSourceApprovalMatrixJson(),first.getSourceApprovalMatrixJson())
                            ||!Objects.equals(row.getRuntimeApprovalNodesJson(),first.getRuntimeApprovalNodesJson())))throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            var baseline=DccApprovalMatrixSourceService.readCanonical(first.getSourceApprovalMatrixJson());
            if(!Objects.equals(baseline.getTenantId(),source.getTenantId())||!Objects.equals(baseline.getCategoryId(),source.getCategoryId())
                    ||!Objects.equals(baseline.getFileTypeTaxonomyId(),source.getFileTypeTaxonomyId())||!"LEGACY".equals(baseline.getActionType())
                    ||!Objects.equals(baseline.getRouteId(),first.getSourceApprovalMatrixId())||!Objects.equals(baseline.getRouteVersionNo(),first.getSourceApprovalMatrixVersion())
                    ||!Objects.equals(baseline.getMatrixDigest(),first.getSourceApprovalMatrixDigest()))throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            var rawRuntime=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(first.getRuntimeApprovalNodesJson(),Map.class);
            if(rawRuntime==null||!"DCC_RUNTIME_APPROVAL_NODES_V1".equals(rawRuntime.get("formatVersion")))throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            var originalGroups=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(
                    cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(rawRuntime.get("groups")),cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.RuleGroup.class);
            var exactNodes=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(
                    cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(rawRuntime.get("nodes")),ResolvedRouteNode.class);
            if(!Objects.equals(exactNodes,restored))throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            Map<String,Object> nativeSource=null;if(rawRuntime.containsKey("nativeSupervisorSource")){
                if(!(rawRuntime.get("nativeSupervisorSource") instanceof Map<?,?> map))throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);nativeSource=(Map<String,Object>)map;
                nativeSourceTokens.requireRecordedNativeSupervisor(source,baseline,restored,nativeSource);
            }
            return new ResolvedRoute(DccCategoryApprovalRouteDO.builder().id(baseline.getRouteId()).categoryId(source.getCategoryId())
                    .versionNo(baseline.getRouteVersionNo()).actionType("LEGACY").build(),List.copyOf(restored),baseline,first.getDerivationVersion(),originalGroups,nativeSource);
        }
        if(ordered.stream().anyMatch(row->row.getSourceApprovalMatrixId()!=null||row.getSourceApprovalMatrixVersion()!=null||row.getSourceApprovalMatrixDigest()!=null))
            throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        return new ResolvedRoute(DccCategoryApprovalRouteDO.builder().categoryId(source.getCategoryId())
                .actionType(actionType).versionNo(version).build(),List.copyOf(restored));
    }

    public ResolvedRoute restoreCustomFrozenRoute(DccControlledFileDO file,List<DccControlledFileRouteSnapshotDO> snapshots){
        if(file==null||!Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId())||snapshots==null||snapshots.size()!=4||file.getProcessInstanceId()==null)throw new IllegalArgumentException("CUSTOM_FROZEN_ROUTE_IDENTITY_INVALID");
        var first=snapshots.get(0);String runtime=first.getRuntimeApprovalNodesJson();
        var value=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(runtime,Map.class);
        if(value==null||!"DCC_RUNTIME_APPROVAL_NODES_V1".equals(value.get("formatVersion"))||!"APPLICATION_CUSTOM".equals(value.get("sourceKind"))||value.containsKey("nativeSupervisorSource"))throw new IllegalArgumentException("CUSTOM_FROZEN_ROUTE_SOURCE_INVALID");
        var saved=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(value.get("applicationCustomSource")),CustomRouteSource.class);
        if(saved==null||saved.previewContext()!=null||saved.reservedContext()==null||saved.proof()==null)throw new IllegalArgumentException("CUSTOM_FROZEN_RESERVED_SOURCE_REQUIRED");
        var c=saved.reservedContext();String type="NEW".equals(file.getChangeType())?"UPLOAD":"REVISION";
        if(!Objects.equals(c.tenantId(),file.getTenantId())||!Objects.equals(c.projectId(),file.getDccProjectCodeId())||!Objects.equals(c.categoryId(),file.getCategoryId())||!Objects.equals(c.taxonomyId(),file.getFileTypeTaxonomyId())||!Objects.equals(c.requesterId(),file.getRequesterId())||!Objects.equals(c.controlledFileId(),file.getId())
                ||!Objects.equals(c.attributeRound(),customRounds.require(type,file.getId(),file.getProcessInstanceId()))||!Objects.equals(c.applicationType(),type)||!Objects.equals(c.applicationContextKey(),file.getDraftApplicationContextKey())||!Objects.equals(c.processDefinitionId(),file.getPreparedProcessDefinitionId())||!Objects.equals(c.definitionDigest(),file.getPreparedDefinitionDigest())||!Objects.equals(saved.selection().canonical(),file.getDraftApplicationCustomMatrixJson()))throw new IllegalArgumentException("CUSTOM_FROZEN_CONTEXT_MISMATCH");
        var selection=customMatrices.readCanonical(saved.selection().canonical(),saved.selection().digest(),c);
        nativeSourceTokens.requireRecordedCustomSupervisorReserved(c,saved.selection(),saved.evidence(),saved.proof().sourceDigest(),saved.proof().sourceToken());
        if(!Objects.equals(saved.proof().sourceDigest(),cn.hutool.crypto.digest.DigestUtil.sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(new TreeMap<>(saved.proof().payload())))))throw new IllegalArgumentException("CUSTOM_FROZEN_PROOF_PAYLOAD_MISMATCH");
        if(!Objects.equals(saved.proof().payload().get("canonical"),saved.selection().canonical()))throw new IllegalArgumentException("CUSTOM_FROZEN_PROOF_PAYLOAD_MISMATCH");
        var actual=snapshots.stream().sorted(Comparator.comparing(DccControlledFileRouteSnapshotDO::getStageOrder)).map(row->{
            if(!Objects.equals(row.getControlledFileId(),file.getId())||row.getRouteVersionNo()!=null||row.getSourceApprovalMatrixId()!=null||row.getSourceApprovalMatrixVersion()!=null||row.getSourceApprovalMatrixDigest()!=null||row.getSourceApprovalMatrixJson()!=null||!DccApplicationCustomMatrixService.FORMAT.equals(row.getDerivationVersion())||!Objects.equals(runtime,row.getRuntimeApprovalNodesJson()))throw new IllegalArgumentException("CUSTOM_FROZEN_SNAPSHOT_SOURCE_INVALID");
            return new ResolvedRouteNode(row.getStageNo(),row.getStageCode(),row.getStageName(),row.getStageOrder(),row.getCandidateSourceType(),row.getCandidateSourceId(),parseFrozenRouteIds(row.getCandidateSourceIds()),row.getApproveMethod(),row.getApproveRatio(),row.getRequireAllApprovals(),parseFrozenRouteIds(row.getResolvedUserIds()));}).toList();
        var frozen=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseArray(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(value.get("nodes")),ResolvedRouteNode.class);
        if(!actual.equals(frozen)||!actual.stream().map(ResolvedRouteNode::stageCode).toList().equals(List.of("SUPERVISOR_REVIEW","MATRIX_REVIEW","MATRIX_APPROVAL","DOC_CONTROL_REVIEW")))throw new IllegalArgumentException("CUSTOM_FROZEN_RUNTIME_NODES_INVALID");
        var expected=customMatrices.compile(c.approvalSourceMode(),selection.applicationCustomMatrix(),selection.selectedSignoffDepartmentIds());
        for(int i=0;i<3;i++){var node=actual.get(i+1);var wanted=expected.get(i);if(node.stageNo()!=i+1||node.stageOrder()!=i+1||!Objects.equals(node.candidateSourceType(),wanted.candidateSourceType())||!Objects.equals(node.candidateSourceIds(),wanted.candidateSourceIds())||!Objects.equals(node.approveMethod(),wanted.approveMethod())||!Objects.equals(node.approveRatio(),wanted.approveRatio())||!Objects.equals(node.requireAllApprovals(),wanted.requireAllApprovals())||!Objects.equals(node.candidateSourceId(),wanted.candidateSourceIds().get(0))||!"DEPT".equals(node.candidateSourceType())&&!node.resolvedUserIds().equals(node.candidateSourceIds()))throw new IllegalArgumentException("CUSTOM_FROZEN_COMPILED_NODES_INVALID");}
        var supervisor=actual.get(0);if(supervisor.stageNo()!=0||supervisor.stageOrder()!=0||!"USER".equals(supervisor.candidateSourceType())||!"ANY".equals(supervisor.approveMethod())||supervisor.approveRatio()!=null||!Boolean.FALSE.equals(supervisor.requireAllApprovals())||!supervisor.candidateSourceIds().equals(List.of(saved.evidence().supervisorUserId()))||!supervisor.resolvedUserIds().equals(supervisor.candidateSourceIds()))throw new IllegalArgumentException("CUSTOM_FROZEN_SUPERVISOR_INVALID");
        return new ResolvedRoute(null,actual,null,DccApplicationCustomMatrixService.FORMAT,List.of(),null,saved);
    }

    private List<Long> parseFrozenRouteIds(String value) {
        if(StrUtil.isBlank(value)) throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        return Arrays.stream(value.split(",",-1)).map(String::trim).map(id -> {
            if(!id.matches("[1-9][0-9]*")) throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            try { return Long.valueOf(id); }
            catch(NumberFormatException invalidId) { throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH); }
        }).toList();
    }

    /** A submitted department set replaces only this action's signoff node; later candidates stay frozen. */
    public ResolvedRoute withSelectedSignoffDepartments(ResolvedRoute route,List<Long> selected) {
        if(selected==null) return route;
        if(route==null) throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        if(route.sourceApprovalMatrix()!=null)throw new IllegalArgumentException("APPROVAL_MATRIX_SELECTION_REQUIRES_SOURCE_DERIVATION：不能覆盖新公共来源元数据");
        var departments=normalizeSelectedDepartments(selected);
        var leaders=resolveDepartmentLeaders(departments,false);
        long count=route.nodes().stream().filter(node->"MATRIX_REVIEW".equals(node.stageCode())).count();
        if(count!=1) throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
        return new ResolvedRoute(route.route(),route.nodes().stream().map(node-> {
            if(!"MATRIX_REVIEW".equals(node.stageCode())) return node;
            if(!"DEPT".equals(node.candidateSourceType())) throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            return new ResolvedRouteNode(node.stageNo(),node.stageCode(),node.stageName(),node.stageOrder(),"DEPT",
                    departments.get(0),departments,node.approveMethod(),node.approveRatio(),true,leaders);
        }).toList());
    }

    private Map<String,List<Long>> buildUniqueStageAssigneeMapForStart(List<ResolvedRouteNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Map.of();
        }
        ResolvedRouteNode firstNode = nodes.get(0);
        return buildUniqueStageAssigneeMap(nodes, node -> node == firstNode);
    }

    public Map<String, List<Long>> buildApproveUserSelectAssigneeMap(List<ResolvedRouteNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return Map.of();
        }
        ResolvedRouteNode firstNode = nodes.get(0);
        return buildUniqueStageAssigneeMap(nodes, node -> node != firstNode);
    }

    private Map<String, List<Long>> buildUniqueStageAssigneeMap(List<ResolvedRouteNode> nodes,
                                                                Predicate<ResolvedRouteNode> nodeFilter) {
        Map<String, List<Long>> assigneeMap = new HashMap<>();
        nodes.stream().filter(nodeFilter).forEach(node -> {
            if (assigneeMap.putIfAbsent(node.stageCode(), node.resolvedUserIds()) != null) {
                throw exception(CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH);
            }
        });
        return assigneeMap;
    }

    private ResolvedRouteNode resolveRouteNode(DccCategoryApprovalRouteNodeDO routeNode, Long submitterUserId,
                                               boolean requireConfiguredPosts) {
        List<Long> resolvedUserIds = resolveApprovers(routeNode, submitterUserId, requireConfiguredPosts);
        return new ResolvedRouteNode(
                routeNode.getStageNo(),
                routeNode.getStageCode(),
                routeNode.getStageName(),
                routeNode.getStageOrder(),
                routeNode.getCandidateSourceType(),
                routeNode.getCandidateSourceId(),
                readCandidateSourceIds(routeNode.getCandidateSourceIds(), routeNode.getCandidateSourceId()),
                routeNode.getApproveMethod(),
                routeNode.getApproveRatio(),
                routeNode.getRequireAllApprovals(),
                resolvedUserIds
        );
    }

    private List<Long> resolveApprovers(DccCategoryApprovalRouteNodeDO routeNode, Long submitterUserId,
                                        boolean requireConfiguredPosts) {
        List<Long> candidateSourceIds = readCandidateSourceIds(routeNode.getCandidateSourceIds(), routeNode.getCandidateSourceId());
        if ("USER".equalsIgnoreCase(routeNode.getCandidateSourceType())) {
            if (candidateSourceIds.isEmpty()) {
                throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
            }
            validateResolvedUserIds(candidateSourceIds, requireConfiguredPosts);
            return candidateSourceIds;
        }
        if ("DEPT".equalsIgnoreCase(routeNode.getCandidateSourceType())) {
            return resolveDepartmentLeaders(candidateSourceIds, requireConfiguredPosts);
        }
        if (!"POSITION".equalsIgnoreCase(routeNode.getCandidateSourceType())) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        for (Long positionId : candidateSourceIds) {
            if (positionRuntimeResolver.isUploaderDerivedPosition(positionId)) {
                userIds.addAll(positionRuntimeResolver.resolveUserIds(positionId, submitterUserId, false));
                continue;
            }
            positionAssignmentMapper.selectActiveListByPositionId(positionId).stream()
                    .flatMap(assignment -> resolveAssignmentUsers(assignment).stream())
                    .forEach(userIds::add);
        }
        if (userIds.isEmpty()) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        List<Long> resolvedUserIds = new ArrayList<>(userIds);
        validateResolvedUserIds(resolvedUserIds, requireConfiguredPosts);
        return resolvedUserIds;
    }

    private List<Long> resolveDepartmentLeaders(List<Long> departmentIds, boolean requireConfiguredPosts) {
        if (departmentIds.isEmpty()) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        try {
            deptApi.validateDeptList(departmentIds);
        } catch (ServiceException ex) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        List<DeptRespDTO> departments = deptApi.getDeptList(departmentIds);
        Map<Long, List<DeptRespDTO>> departmentMap = departments.stream()
                .collect(Collectors.groupingBy(DeptRespDTO::getId));
        List<Long> leaderUserIds = new ArrayList<>(departmentIds.size());
        for (Long departmentId : departmentIds) {
            List<DeptRespDTO> matches = departmentMap.get(departmentId);
            if (matches == null || matches.size() != 1 || matches.get(0).getLeaderUserId() == null) {
                throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
            }
            leaderUserIds.add(matches.get(0).getLeaderUserId());
        }
        validateResolvedUserIds(leaderUserIds, requireConfiguredPosts);
        return leaderUserIds;
    }

    private void validateResolvedUserIds(List<Long> userIds, boolean requireConfiguredPosts) {
        try {
            adminUserApi.validateUserList(userIds);
        } catch (ServiceException ex) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        if (requireConfiguredPosts) {
            approvalParticipantPostValidator.requireConfiguredPosts(userIds);
        }
    }

    private List<Long> resolveAssignmentUsers(DccPositionAssignmentDO assignment) {
        if (assignment.getUserId() != null) {
            return List.of(assignment.getUserId());
        }
        if ("POST".equalsIgnoreCase(assignment.getAssignmentType()) && assignment.getSystemPostId() != null) {
            return adminUserApi.getUserListByPostIds(List.of(assignment.getSystemPostId())).stream()
                    .map(AdminUserRespDTO::getId)
                    .toList();
        }
        return List.of();
    }

    private List<Long> readCandidateSourceIds(String candidateSourceIds, Long fallbackId) {
        if (StrUtil.isNotBlank(candidateSourceIds)) {
            return Arrays.stream(candidateSourceIds.split(","))
                    .filter(StrUtil::isNotBlank)
                    .map(String::trim)
                    .map(Long::valueOf)
                    .toList();
        }
        return fallbackId == null ? List.of() : List.of(fallbackId);
    }

    private String toPendingStatus(String stageCode) {
        if (stageCode == null) {
            return null;
        }
        if (DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode().equals(stageCode)) {
            return DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus();
        }
        if (DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode().equals(stageCode)) {
            return DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus();
        }
        if (DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode().equals(stageCode)) {
            return DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus();
        }
        if (DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode().equals(stageCode)) {
            return DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus();
        }
        return null;
    }

    public record ResolvedRoute(DccCategoryApprovalRouteDO route, List<ResolvedRouteNode> nodes,
            cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO sourceApprovalMatrix,String derivationVersion,
            List<cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.RuleGroup> runtimeGroups,Map<String,Object> nativeSupervisorSource,
            CustomRouteSource applicationCustomSource) {
        public ResolvedRoute {runtimeGroups=List.copyOf(runtimeGroups);}
        public ResolvedRoute(DccCategoryApprovalRouteDO route,List<ResolvedRouteNode> nodes,cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO source,String version,List<cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.RuleGroup> groups,Map<String,Object> supervisor){this(route,nodes,source,version,groups,supervisor,null);}
        public ResolvedRoute(DccCategoryApprovalRouteDO route,List<ResolvedRouteNode> nodes,cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO source,String version,List<cn.iocoder.yudao.module.dcc.service.route.DccPublicMatrixNativeDerivation.RuleGroup> groups){this(route,nodes,source,version,groups,null);}
        public ResolvedRoute(DccCategoryApprovalRouteDO route,List<ResolvedRouteNode> nodes){this(route,nodes,null,null,List.of());}
        public ResolvedRoute(DccCategoryApprovalRouteDO route,List<ResolvedRouteNode> nodes,
                cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO source,String version){this(route,nodes,source,version,List.of());}
    }

    public record ResolvedRouteNode(Integer stageNo, String stageCode, String stageName, Integer stageOrder,
                                    String candidateSourceType, Long candidateSourceId, List<Long> candidateSourceIds,
                                    String approveMethod, Integer approveRatio, Boolean requireAllApprovals,
                                    List<Long> resolvedUserIds) {
    }
}
