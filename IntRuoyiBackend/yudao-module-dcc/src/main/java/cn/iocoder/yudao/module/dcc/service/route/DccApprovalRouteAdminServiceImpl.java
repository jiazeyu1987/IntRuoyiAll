package cn.iocoder.yudao.module.dcc.service.route;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteNodeRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteNodeSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePreviewReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRoutePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccApprovalPositionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccApprovalPositionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccApprovalModeEnum;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver;
import cn.iocoder.yudao.module.dcc.service.file.DccApprovalParticipantPostValidator;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Function;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_ROUTE_NODE_EMPTY;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_ROUTE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.ROUTE_PREVIEW_APPROVER_NOT_FOUND;

@Service
@Validated
public class DccApprovalRouteAdminServiceImpl implements DccApprovalRouteAdminService {

    static final ErrorCode APPROVAL_ROUTE_FIXED_STAGE_INVALID =
            new ErrorCode(1_080_000_103, "审批路线节点不符合当前动作的固定审批策略，且所有必经阶段必须完整配置");

    @Resource
    private DccFileCategoryMapper categoryMapper;
    @Resource
    private DccCategoryApprovalRouteMapper routeMapper;
    @Resource
    private DccCategoryApprovalRouteNodeMapper routeNodeMapper;
    @Resource
    private DccApprovalPositionMapper approvalPositionMapper;
    @Resource
    private DccPositionAssignmentMapper positionAssignmentMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private DccApprovalPositionRuntimeResolver positionRuntimeResolver;
    @Resource
    private DccApprovalParticipantPostValidator approvalParticipantPostValidator;

    @Override
    public PageResult<DccApprovalRouteRespVO> getRoutePage(DccApprovalRoutePageReqVO reqVO) {
        PageResult<DccCategoryApprovalRouteDO> routePage = routeMapper.selectPage(reqVO);
        if (routePage.getList().isEmpty()) {
            return new PageResult<>(List.of(), routePage.getTotal());
        }
        List<DccCategoryApprovalRouteDO> routeList = routePage.getList();
        Map<Long, DccFileCategoryDO> categoryMap = categoryMapper.selectBatchIds(routeList.stream()
                        .map(DccCategoryApprovalRouteDO::getCategoryId)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(DccFileCategoryDO::getId, Function.identity()));
        List<Long> routeIds = routeList.stream().map(DccCategoryApprovalRouteDO::getId).toList();
        List<DccCategoryApprovalRouteNodeDO> routeNodes = routeNodeMapper.selectListByRouteIds(routeIds);
        Map<Long, List<DccCategoryApprovalRouteNodeDO>> routeNodeMap = routeNodes.stream()
                .collect(Collectors.groupingBy(DccCategoryApprovalRouteNodeDO::getRouteId));
        Map<Long, DccApprovalPositionDO> positionMap = loadPositionMap(routeNodes);
        return new PageResult<>(CollectionUtils.convertList(routeList,
                route -> toRouteResp(route, categoryMap.get(route.getCategoryId()),
                        routeNodeMap.getOrDefault(route.getId(), List.of()), positionMap)), routePage.getTotal());
    }

    @Override
    public List<DccCategoryApprovalRouteDO> getRoutes(Long categoryId) {
        validateCategoryExists(categoryId);
        return routeMapper.selectList(DccCategoryApprovalRouteDO::getCategoryId, categoryId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccCategoryApprovalRouteDO saveRoute(Long categoryId, DccApprovalRouteSaveReqVO reqVO) {
        validateCategoryExists(categoryId);
        if (reqVO.getNodes() == null || reqVO.getNodes().isEmpty()) {
            throw exception(APPROVAL_ROUTE_NODE_EMPTY);
        }
        String actionType = normalizeActionType(reqVO.getActionType());
        validateStages(actionType, reqVO.getNodes());
        Integer maxVersion = routeMapper.selectMaxVersionNoIncludingDeletedByActionType(categoryId, actionType);
        LocalDateTime now = LocalDateTime.now();
        DccCategoryApprovalRouteDO route = DccCategoryApprovalRouteDO.builder()
                .categoryId(categoryId)
                .actionType(actionType)
                .versionNo(maxVersion + 1)
                .active(Boolean.TRUE)
                .effectiveTime(reqVO.getEffectiveTime())
                .remark(reqVO.getRemark())
                .build();
        routeMapper.insert(route);

        if (isEffectiveAt(reqVO.getEffectiveTime(), now)) {
            List<DccCategoryApprovalRouteDO> oldRoutes = routeMapper.selectList(DccCategoryApprovalRouteDO::getCategoryId, categoryId);
            oldRoutes.stream()
                    .filter(item -> !item.getId().equals(route.getId()))
                    .filter(item -> Boolean.TRUE.equals(item.getActive()))
                    .filter(item -> actionType.equals(normalizeActionType(item.getActionType())))
                    .filter(item -> isEffectiveAt(item.getEffectiveTime(), now))
                    .forEach(item -> routeMapper.updateById(DccCategoryApprovalRouteDO.builder()
                            .id(item.getId())
                            .active(Boolean.FALSE)
                            .build()));
        }

        CollectionUtils.convertList(reqVO.getNodes(), nodeReq -> toRouteNode(route.getId(), nodeReq))
                .forEach(routeNodeMapper::insert);
        return route;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoute(Long id) {
        DccCategoryApprovalRouteDO route = routeMapper.selectById(id);
        if (route == null) {
            throw exception(APPROVAL_ROUTE_NOT_EXISTS);
        }
        routeNodeMapper.delete(DccCategoryApprovalRouteNodeDO::getRouteId, id);
        routeMapper.deleteById(id);
    }

    @Override
    public List<DccApprovalRoutePreviewRespVO> previewRoute(DccApprovalRoutePreviewReqVO reqVO) {
        validateCategoryExists(reqVO.getCategoryId());
        String actionType = normalizeActionType(reqVO.getActionType());
        DccCategoryApprovalRouteDO route = isLegacyActionType(actionType)
                ? routeMapper.selectLatestActiveByCategoryId(reqVO.getCategoryId())
                : routeMapper.selectLatestActiveByCategoryIdAndActionType(reqVO.getCategoryId(), actionType);
        if (route == null) {
            throw exception(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_ROUTE_NOT_EXISTS);
        }
        List<DccCategoryApprovalRouteNodeDO> nodes = routeNodeMapper.selectList(DccCategoryApprovalRouteNodeDO::getRouteId, route.getId()).stream()
                .sorted(Comparator.comparing(DccCategoryApprovalRouteNodeDO::getStageOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getSort)
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getStageNo))
                .toList();
        if (nodes.isEmpty()) {
            throw exception(APPROVAL_ROUTE_NODE_EMPTY);
        }
        validateRouteNodes(actionType, nodes);
        return CollectionUtils.convertList(nodes, this::previewNode);
    }

    private DccApprovalRouteRespVO toRouteResp(DccCategoryApprovalRouteDO route, DccFileCategoryDO category,
                                               List<DccCategoryApprovalRouteNodeDO> nodes,
                                               Map<Long, DccApprovalPositionDO> positionMap) {
        DccApprovalRouteRespVO respVO = new DccApprovalRouteRespVO();
        respVO.setId(route.getId());
        respVO.setCategoryId(route.getCategoryId());
        respVO.setCategoryName(category == null ? null : category.getName());
        respVO.setActionType(route.getActionType());
        respVO.setVersionNo(route.getVersionNo());
        respVO.setActive(route.getActive());
        respVO.setStatusLabel(Boolean.TRUE.equals(route.getActive()) ? "启用" : "停用");
        respVO.setEffectiveTime(route.getEffectiveTime());
        respVO.setRemark(route.getRemark());
        List<DccApprovalRouteNodeRespVO> nodeRespList = CollectionUtils.convertList(sortNodes(nodes), this::toRouteNodeResp);
        respVO.setNodeCount(nodeRespList.size());
        respVO.setNodeSummary(buildNodeSummary(nodeRespList, positionMap));
        respVO.setNodes(nodeRespList);
        return respVO;
    }

    private DccApprovalRouteNodeRespVO toRouteNodeResp(DccCategoryApprovalRouteNodeDO node) {
        DccApprovalRouteNodeRespVO respVO = new DccApprovalRouteNodeRespVO();
        respVO.setId(node.getId());
        respVO.setRouteId(node.getRouteId());
        respVO.setStageNo(node.getStageNo());
        respVO.setStageCode(node.getStageCode());
        respVO.setStageName(node.getStageName());
        respVO.setStageOrder(node.getStageOrder());
        respVO.setCandidateSourceType(node.getCandidateSourceType());
        respVO.setCandidateSourceId(node.getCandidateSourceId());
        respVO.setCandidateSourceIds(readCandidateSourceIds(node.getCandidateSourceIds(), node.getCandidateSourceId()));
        respVO.setApproveMethod(node.getApproveMethod());
        respVO.setApproveRatio(node.getApproveRatio());
        respVO.setRequireAllApprovals(node.getRequireAllApprovals());
        respVO.setRequired(node.getRequired());
        respVO.setSort(node.getSort());
        respVO.setStageType(node.getStageType());
        respVO.setSubjectLabel(node.getSubjectLabel());
        respVO.setMarker(node.getMarker());
        respVO.setSubjectType(node.getSubjectType());
        respVO.setSubjectId(node.getSubjectId());
        respVO.setSubjectName(node.getSubjectName());
        respVO.setSubjectDepartmentPath(node.getSubjectDepartmentPath());
        respVO.setRuleRemark(node.getRuleRemark());
        return respVO;
    }

    private List<DccCategoryApprovalRouteNodeDO> sortNodes(List<DccCategoryApprovalRouteNodeDO> nodes) {
        return nodes.stream()
                .sorted(Comparator.comparing(DccCategoryApprovalRouteNodeDO::getStageOrder,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getSort,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DccCategoryApprovalRouteNodeDO::getStageNo,
                                Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private Map<Long, DccApprovalPositionDO> loadPositionMap(List<DccCategoryApprovalRouteNodeDO> nodes) {
        Set<Long> positionIds = nodes.stream()
                .filter(node -> "POSITION".equalsIgnoreCase(node.getCandidateSourceType()))
                .flatMap(node -> readCandidateSourceIds(node.getCandidateSourceIds(), node.getCandidateSourceId()).stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (positionIds.isEmpty()) {
            return Map.of();
        }
        return approvalPositionMapper.selectBatchIds(positionIds).stream()
                .collect(Collectors.toMap(DccApprovalPositionDO::getId, Function.identity(), (left, right) -> left));
    }

    private String buildNodeSummary(List<DccApprovalRouteNodeRespVO> nodes,
                                    Map<Long, DccApprovalPositionDO> positionMap) {
        if (nodes.isEmpty()) {
            return "未配置节点";
        }
        return nodes.stream()
                .map(node -> String.format("%s. %s：%s",
                        node.getStageNo() == null ? "-" : node.getStageNo(),
                        StrUtil.blankToDefault(node.getStageName(), "未命名阶段"),
                        buildCandidateSummary(node, positionMap)))
                .collect(Collectors.joining(" / "));
    }

    private String buildCandidateSummary(DccApprovalRouteNodeRespVO node,
                                         Map<Long, DccApprovalPositionDO> positionMap) {
        if (StrUtil.isNotBlank(node.getSubjectName())) {
            return node.getSubjectName();
        }
        List<Long> candidateIds = node.getCandidateSourceIds() == null ? List.of() : node.getCandidateSourceIds();
        if (candidateIds.isEmpty()) {
            return "-";
        }
        if ("POSITION".equalsIgnoreCase(node.getCandidateSourceType())) {
            return candidateIds.stream()
                    .map(id -> {
                        DccApprovalPositionDO position = positionMap.get(id);
                        return position == null ? "岗位#" + id : position.getName();
                    })
                    .collect(Collectors.joining("、"));
        }
        if ("USER".equalsIgnoreCase(node.getCandidateSourceType())) {
            return candidateIds.stream().map(id -> "用户#" + id).collect(Collectors.joining("、"));
        }
        return candidateIds.stream().map(String::valueOf).collect(Collectors.joining("、"));
    }

    private DccApprovalRoutePreviewRespVO previewNode(DccCategoryApprovalRouteNodeDO node) {
        List<Long> candidateSourceIds = readCandidateSourceIds(node.getCandidateSourceIds(), node.getCandidateSourceId());
        List<Long> resolvedUserIds;
        Integer approvalMode;
        if ("USER".equalsIgnoreCase(node.getCandidateSourceType())) {
            validateUserCandidateIds(candidateSourceIds);
            resolvedUserIds = candidateSourceIds;
        } else if ("DEPT".equalsIgnoreCase(node.getCandidateSourceType())) {
            resolvedUserIds = resolveDepartmentLeaders(candidateSourceIds);
        } else {
            if (!"POSITION".equalsIgnoreCase(node.getCandidateSourceType())) {
                throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
            }
            boolean hasUploaderDerivedPosition = candidateSourceIds.stream()
                    .anyMatch(positionRuntimeResolver::isUploaderDerivedPosition);
            LinkedHashSet<Long> previewUserIds = new LinkedHashSet<>();
            for (Long positionId : candidateSourceIds) {
                if (positionRuntimeResolver.isUploaderDerivedPosition(positionId)) {
                    previewUserIds.addAll(positionRuntimeResolver.resolveUserIds(positionId, null, true));
                    continue;
                }
                positionAssignmentMapper.selectActiveListByPositionId(positionId).stream()
                        .flatMap(item -> resolveAssignmentUsers(item).stream())
                        .forEach(previewUserIds::add);
            }
            resolvedUserIds = new ArrayList<>(previewUserIds);
            if (resolvedUserIds.isEmpty() && !hasUploaderDerivedPosition) {
                throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
            }
            if (!resolvedUserIds.isEmpty()) {
                validateUserCandidateIds(resolvedUserIds);
            }
        }
        if (resolvedUserIds.isEmpty() && !"POSITION".equalsIgnoreCase(node.getCandidateSourceType())) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        approvalMode = "ANY".equals(node.getApproveMethod()) ? DccApprovalModeEnum.ANY_ONE.getMode()
                : DccApprovalModeEnum.ALL_REQUIRED.getMode();
        DccApprovalRoutePreviewRespVO respVO = new DccApprovalRoutePreviewRespVO();
        respVO.setStageNo(node.getStageNo());
        respVO.setStageCode(node.getStageCode());
        respVO.setStageName(node.getStageName());
        respVO.setStageOrder(node.getStageOrder());
        respVO.setApprovalMode(approvalMode);
        respVO.setCandidateSourceType(node.getCandidateSourceType());
        respVO.setCandidateSourceIds(candidateSourceIds);
        respVO.setRequireAllApprovals(node.getRequireAllApprovals());
        respVO.setResolvedUserIds(resolvedUserIds);
        return respVO;
    }

    private List<Long> resolveDepartmentLeaders(List<Long> departmentIds) {
        if (departmentIds.isEmpty()) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        try {
            deptApi.validateDeptList(departmentIds);
        } catch (ServiceException ex) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        Map<Long, List<DeptRespDTO>> deptMap = deptApi.getDeptList(departmentIds).stream()
                .collect(Collectors.groupingBy(DeptRespDTO::getId));
        List<Long> leaderUserIds = new ArrayList<>(departmentIds.size());
        for (Long departmentId : departmentIds) {
            List<DeptRespDTO> matches = deptMap.get(departmentId);
            if (matches == null || matches.size() != 1 || matches.get(0).getLeaderUserId() == null) {
                throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
            }
            leaderUserIds.add(matches.get(0).getLeaderUserId());
        }
        validateUserCandidateIds(leaderUserIds);
        return leaderUserIds;
    }

    private void validateUserCandidateIds(List<Long> candidateSourceIds) {
        if (candidateSourceIds.isEmpty()) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        try {
            adminUserApi.validateUserList(candidateSourceIds);
        } catch (ServiceException ex) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        approvalParticipantPostValidator.requireConfiguredPosts(candidateSourceIds);
    }

    private DccCategoryApprovalRouteNodeDO toRouteNode(Long routeId, DccApprovalRouteNodeSaveReqVO reqVO) {
        DccCategoryApprovalRouteDO route = routeMapper.selectById(routeId);
        String actionType = normalizeActionType(route == null ? null : route.getActionType());
        DccFixedApprovalRoutePolicy.FixedStageDefinition stageDefinition = isLegacyActionType(actionType)
                ? DccFixedApprovalRoutePolicy.requireStage(reqVO.getStageNo(), APPROVAL_ROUTE_FIXED_STAGE_INVALID)
                : DccActionApprovalRoutePolicy.requireStage(reqVO.getStageNo(), APPROVAL_ROUTE_FIXED_STAGE_INVALID);
        List<Long> candidateSourceIds = readSaveCandidateSourceIds(reqVO);
        return DccCategoryApprovalRouteNodeDO.builder()
                .routeId(routeId)
                .stageNo(reqVO.getStageNo())
                .stageCode(stageDefinition.stageCode())
                .stageName(reqVO.getStageName())
                .stageOrder(stageDefinition.stageOrder())
                .candidateSourceType(reqVO.getCandidateSourceType())
                .candidateSourceId(candidateSourceIds.get(0))
                .candidateSourceIds(formatCandidateSourceIds(candidateSourceIds))
                .approveMethod(stageDefinition.approveMethod())
                .approveRatio(stageDefinition.approveRatio())
                .requireAllApprovals(stageDefinition.requireAllApprovals())
                .required(stageDefinition.required())
                .sort(reqVO.getSort())
                .build();
    }

    private List<Long> readSaveCandidateSourceIds(DccApprovalRouteNodeSaveReqVO reqVO) {
        List<Long> candidateSourceIds = reqVO.getCandidateSourceIds() == null ? List.of() : reqVO.getCandidateSourceIds().stream()
                .filter(Objects::nonNull)
                .toList();
        if (candidateSourceIds.isEmpty() && reqVO.getCandidateSourceId() != null) {
            candidateSourceIds = List.of(reqVO.getCandidateSourceId());
        }
        if (candidateSourceIds.isEmpty()) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        return candidateSourceIds;
    }

    private String formatCandidateSourceIds(List<Long> candidateSourceIds) {
        return candidateSourceIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private DccFileCategoryDO validateCategoryExists(Long categoryId) {
        DccFileCategoryDO category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw exception(FILE_CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    private boolean isEffectiveAt(LocalDateTime effectiveTime, LocalDateTime selectionTime) {
        return effectiveTime == null || !effectiveTime.isAfter(selectionTime);
    }

    private void validateRouteNodes(String actionType, List<DccCategoryApprovalRouteNodeDO> nodes) {
        if (!isLegacyActionType(actionType)) {
            DccActionApprovalRoutePolicy.validateRouteNodes(actionType, nodes, APPROVAL_ROUTE_FIXED_STAGE_INVALID);
            return;
        }
        validateFixedStages(nodes.stream().map(node -> {
            DccApprovalRouteNodeSaveReqVO reqNode = new DccApprovalRouteNodeSaveReqVO();
            reqNode.setStageNo(node.getStageNo());
            reqNode.setStageName(node.getStageName());
            reqNode.setCandidateSourceType(node.getCandidateSourceType());
            reqNode.setCandidateSourceId(node.getCandidateSourceId());
            reqNode.setCandidateSourceIds(readCandidateSourceIds(node.getCandidateSourceIds(), node.getCandidateSourceId()));
            reqNode.setApproveMethod(node.getApproveMethod());
            reqNode.setApproveRatio(node.getApproveRatio());
            reqNode.setRequired(node.getRequired());
            reqNode.setSort(node.getSort());
            return reqNode;
        }).toList());
    }

    private void validateStages(String actionType, List<DccApprovalRouteNodeSaveReqVO> nodes) {
        if (isLegacyActionType(actionType)) {
            DccFixedApprovalRoutePolicy.validateSaveNodes(nodes, APPROVAL_ROUTE_FIXED_STAGE_INVALID);
            return;
        }
        DccActionApprovalRoutePolicy.validateSaveNodes(actionType, nodes, APPROVAL_ROUTE_FIXED_STAGE_INVALID);
    }

    private void validateFixedStages(List<DccApprovalRouteNodeSaveReqVO> nodes) {
        validateStages("LEGACY", nodes);
    }

    private String normalizeActionType(String actionType) {
        String normalized = StrUtil.blankToDefault(actionType, "LEGACY").trim().toUpperCase();
        if (isLegacyActionType(normalized) || DccActionApprovalRoutePolicy.supports(normalized)) {
            return normalized;
        }
        throw exception(APPROVAL_ROUTE_FIXED_STAGE_INVALID);
    }

    private boolean isLegacyActionType(String actionType) {
        return StrUtil.isBlank(actionType) || "LEGACY".equalsIgnoreCase(actionType);
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

    private List<Long> readCandidateSourceIds(String candidateSourceIds, Long candidateSourceId) {
        if (StrUtil.isNotBlank(candidateSourceIds)) {
            return List.of(candidateSourceIds.split(",")).stream()
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .map(Long::valueOf)
                    .toList();
        }
        return candidateSourceId == null ? List.of() : List.of(candidateSourceId);
    }

}
