package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.position.DccPositionAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.position.DccPositionAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.service.position.DccApprovalPositionRuntimeResolver;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.ROUTE_PREVIEW_APPROVER_NOT_FOUND;

@Service
public class DccControlledFileApprovalRouteAssigneeResolver {

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

    public Map<String, List<Long>> resolveStartUserSelectAssignees(DccControlledFileDO file, Long submitterUserId) {
        if (file == null || file.getCategoryId() == null) {
            throw exception(CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
        }
        Map<String, List<Long>> startUserSelectAssignees = buildStartUserSelectAssigneeMap(
                resolveRoute(file.getCategoryId(), submitterUserId).nodes());
        if (startUserSelectAssignees.isEmpty()) {
            throw exception(CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
        }
        return startUserSelectAssignees;
    }

    public ResolvedRoute resolveRoute(Long categoryId, Long submitterUserId) {
        DccCategoryApprovalRouteDO route = routeMapper.selectLatestActiveByCategoryId(categoryId);
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
        List<ResolvedRouteNode> resolvedNodes = routeNodes.stream()
                .map(routeNode -> resolveRouteNode(routeNode, submitterUserId))
                .toList();
        if (toPendingStatus(resolvedNodes.get(0).stageNo()) == null) {
            throw exception(CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
        }
        return new ResolvedRoute(route, resolvedNodes);
    }

    public Map<String, List<Long>> buildStartUserSelectAssigneeMap(List<ResolvedRouteNode> nodes) {
        return nodes.stream()
                .filter(node -> DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode().equals(node.stageCode()))
                .collect(Collectors.toMap(ResolvedRouteNode::stageCode,
                        ResolvedRouteNode::resolvedUserIds, (left, right) -> left, HashMap::new));
    }

    public Map<String, List<Long>> buildApproveUserSelectAssigneeMap(List<ResolvedRouteNode> nodes) {
        return nodes.stream()
                .filter(node -> !DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode().equals(node.stageCode()))
                .collect(Collectors.toMap(ResolvedRouteNode::stageCode,
                        ResolvedRouteNode::resolvedUserIds, (left, right) -> left, HashMap::new));
    }

    private ResolvedRouteNode resolveRouteNode(DccCategoryApprovalRouteNodeDO routeNode, Long submitterUserId) {
        List<Long> resolvedUserIds = resolveApprovers(routeNode, submitterUserId);
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

    private List<Long> resolveApprovers(DccCategoryApprovalRouteNodeDO routeNode, Long submitterUserId) {
        List<Long> candidateSourceIds = readCandidateSourceIds(routeNode.getCandidateSourceIds(), routeNode.getCandidateSourceId());
        if ("USER".equalsIgnoreCase(routeNode.getCandidateSourceType())) {
            if (candidateSourceIds.isEmpty()) {
                throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
            }
            validateResolvedUserIds(candidateSourceIds);
            return candidateSourceIds;
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
        validateResolvedUserIds(resolvedUserIds);
        return resolvedUserIds;
    }

    private void validateResolvedUserIds(List<Long> userIds) {
        try {
            adminUserApi.validateUserList(userIds);
        } catch (ServiceException ex) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
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

    private String toPendingStatus(Integer stageNo) {
        if (stageNo == null) {
            return null;
        }
        return switch (stageNo) {
            case 1 -> DccControlledFileStatusEnum.PENDING_DOC_CONTROL_REVIEW.getStatus();
            case 2 -> DccControlledFileStatusEnum.PENDING_MATRIX_REVIEW.getStatus();
            case 3 -> DccControlledFileStatusEnum.PENDING_MATRIX_APPROVAL.getStatus();
            case 4 -> DccControlledFileStatusEnum.PENDING_DOC_CONTROL_APPROVAL.getStatus();
            default -> null;
        };
    }

    public record ResolvedRoute(DccCategoryApprovalRouteDO route, List<ResolvedRouteNode> nodes) {
    }

    public record ResolvedRouteNode(Integer stageNo, String stageCode, String stageName, Integer stageOrder,
                                    String candidateSourceType, Long candidateSourceId, List<Long> candidateSourceIds,
                                    String approveMethod, Integer approveRatio, Boolean requireAllApprovals,
                                    List<Long> resolvedUserIds) {
    }
}
