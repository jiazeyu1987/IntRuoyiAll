package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRoutePreviewRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessBlockerRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRouteReadinessRespVO;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_UPLOADER_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.APPROVAL_POSITION_UPLOADER_MAPPING_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ROUTE_NOT_READY;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.ROUTE_PREVIEW_APPROVER_NOT_FOUND;

@Service
public class DccControlledFileRouteReadinessService {

    static final String BLOCKER_POST = "APPROVER_POST_MISSING";
    static final String BLOCKER_PERMISSION = "APPROVER_STAGE_PERMISSION_MISSING";
    static final String BLOCKER_AUTHORIZATION = "APPROVER_SIGNATURE_NOT_AUTHORIZED";
    static final String BLOCKER_IMAGE = "APPROVER_SIGNATURE_IMAGE_INVALID";
    static final String BLOCKER_ORGANIZATION = "SUBMITTER_ORG_MAPPING_INVALID";

    private static final String REVIEW_PERMISSION = "dcc:controlled-file:review";
    private static final String APPROVE_PERMISSION = "dcc:controlled-file:approve";

    @Resource
    private DccControlledFileApprovalRouteAssigneeResolver routeAssigneeResolver;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private DccElectronicSignatureAuthorizationService signatureAuthorizationService;
    @Resource
    private DccElectronicSignatureImageService signatureImageService;

    public RouteReadinessEvaluation evaluate(Long categoryId, Long submitterUserId, List<Long> selectedSignoffUserIds) {
        DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute;
        try {
            resolvedRoute = routeAssigneeResolver.resolveRouteForReadiness(categoryId, submitterUserId);
        } catch (ServiceException ex) {
            if (ex.getCode() != APPROVAL_POSITION_UPLOADER_CONTEXT_REQUIRED.getCode()
                    && ex.getCode() != APPROVAL_POSITION_UPLOADER_MAPPING_INVALID.getCode()) {
                throw ex;
            }
            DccControlledFileRouteReadinessBlockerRespVO blocker =
                    DccControlledFileRouteReadinessBlockerRespVO.builder()
                            .reasonCode(BLOCKER_ORGANIZATION)
                            .message("提交人部门或部门负责人配置不完整：" + ex.getMessage())
                            .userId(submitterUserId)
                            .build();
            return new RouteReadinessEvaluation(null, DccControlledFileRouteReadinessRespVO.builder()
                    .ready(false)
                    .nodes(List.of())
                    .blockers(List.of(blocker))
                    .build());
        }
        resolvedRoute = applySelectedSignoffUsers(resolvedRoute, selectedSignoffUserIds);
        return evaluateResolvedRoute(resolvedRoute);
    }

    private RouteReadinessEvaluation evaluateResolvedRoute(
            DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute) {
        LinkedHashSet<Long> userIds = resolvedRoute.nodes().stream()
                .flatMap(node -> node.resolvedUserIds().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<Long> orderedUserIds = new ArrayList<>(userIds);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserList(orderedUserIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(AdminUserRespDTO::getId, user -> user,
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, Boolean> authorizationMap = signatureAuthorizationService.getAuthorizationMap(orderedUserIds);
        Map<Long, Boolean> imageValidityMap = resolveImageValidity(orderedUserIds);
        List<DccControlledFileRouteReadinessBlockerRespVO> blockers = new ArrayList<>();
        for (DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode node : resolvedRoute.nodes()) {
            String requiredPermission = requiredPermission(node.stageCode());
            for (Long userId : node.resolvedUserIds()) {
                AdminUserRespDTO user = userMap.get(userId);
                String userName = userName(userId, user);
                if (user == null || CollUtil.isEmpty(user.getPostIds())) {
                    blockers.add(blocker(BLOCKER_POST, "审批人未配置系统岗位", node, userId, userName));
                }
                if (!permissionApi.hasAnyPermissions(userId, requiredPermission)) {
                    blockers.add(blocker(BLOCKER_PERMISSION, "审批人缺少当前阶段文控权限", node, userId, userName));
                }
                if (!Boolean.TRUE.equals(authorizationMap.get(userId))) {
                    blockers.add(blocker(BLOCKER_AUTHORIZATION, "审批人未获电子签名授权", node, userId, userName));
                }
                if (!Boolean.TRUE.equals(imageValidityMap.get(userId))) {
                    blockers.add(blocker(BLOCKER_IMAGE, "审批人未配置有效签名图片", node, userId, userName));
                }
            }
        }
        return new RouteReadinessEvaluation(resolvedRoute, DccControlledFileRouteReadinessRespVO.builder()
                .ready(blockers.isEmpty())
                .nodes(resolvedRoute.nodes().stream().map(this::toPreviewNode).toList())
                .blockers(blockers)
                .build());
    }

    private Map<Long, Boolean> resolveImageValidity(Collection<Long> userIds) {
        Map<Long, Boolean> result = new LinkedHashMap<>();
        for (Long userId : userIds) {
            try {
                DccElectronicSignatureImageSnapshot snapshot = signatureImageService.requireActiveSnapshot(userId);
                result.put(userId, snapshot != null && snapshot.getImageId() != null
                        && snapshot.getFileId() != null && "VALID".equals(snapshot.getVerifiedStatus()));
            } catch (ServiceException ex) {
                result.put(userId, false);
            }
        }
        return result;
    }

    private DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute applySelectedSignoffUsers(
            DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute,
            List<Long> selectedSignoffUserIds) {
        if (selectedSignoffUserIds == null || selectedSignoffUserIds.isEmpty()) {
            return resolvedRoute;
        }
        LinkedHashSet<Long> normalizedUserIds = selectedSignoffUserIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedUserIds.size() != selectedSignoffUserIds.size()) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        try {
            adminUserApi.validateUserList(normalizedUserIds);
        } catch (ServiceException ex) {
            throw exception(ROUTE_PREVIEW_APPROVER_NOT_FOUND);
        }
        List<DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode> updatedNodes = new ArrayList<>();
        boolean replaced = false;
        for (DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode node : resolvedRoute.nodes()) {
            if (DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode().equals(node.stageCode())) {
                updatedNodes.add(new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode(
                        node.stageNo(), node.stageCode(), node.stageName(), node.stageOrder(),
                        node.candidateSourceType(), node.candidateSourceId(), node.candidateSourceIds(),
                        node.approveMethod(), node.approveRatio(), node.requireAllApprovals(),
                        new ArrayList<>(normalizedUserIds)));
                replaced = true;
            } else {
                updatedNodes.add(node);
            }
        }
        if (!replaced) {
            throw exception(CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
        }
        return new DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute(resolvedRoute.route(), updatedNodes);
    }

    private String requiredPermission(String stageCode) {
        if (DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode().equals(stageCode)
                || DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode().equals(stageCode)) {
            return REVIEW_PERMISSION;
        }
        if (DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode().equals(stageCode)
                || DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode().equals(stageCode)) {
            return APPROVE_PERMISSION;
        }
        throw exception(CONTROLLED_FILE_ROUTE_NOT_CONFIGURED);
    }

    private DccControlledFileRouteReadinessBlockerRespVO blocker(
            String reasonCode, String message,
            DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode node,
            Long userId, String userName) {
        return DccControlledFileRouteReadinessBlockerRespVO.builder()
                .reasonCode(reasonCode)
                .message(message)
                .stageNo(node.stageNo())
                .stageCode(node.stageCode())
                .stageName(node.stageName())
                .userId(userId)
                .userName(userName)
                .build();
    }

    private String userName(Long userId, AdminUserRespDTO user) {
        if (user == null) {
            return "用户#" + userId;
        }
        return StrUtil.blankToDefault(StrUtil.trim(user.getNickname()),
                StrUtil.blankToDefault(StrUtil.trim(user.getUsername()), "用户#" + userId));
    }

    private DccControlledFileRoutePreviewRespVO toPreviewNode(
            DccControlledFileApprovalRouteAssigneeResolver.ResolvedRouteNode node) {
        DccControlledFileRoutePreviewRespVO respVO = new DccControlledFileRoutePreviewRespVO();
        respVO.setStageNo(node.stageNo());
        respVO.setStageCode(node.stageCode());
        respVO.setStageName(node.stageName());
        respVO.setStageOrder(node.stageOrder());
        respVO.setCandidateSourceType(node.candidateSourceType());
        respVO.setCandidateSourceId(node.candidateSourceId());
        respVO.setCandidateSourceIds(node.candidateSourceIds());
        respVO.setApproveMethod(node.approveMethod());
        respVO.setApproveRatio(node.approveRatio());
        respVO.setRequireAllApprovals(node.requireAllApprovals());
        respVO.setResolvedUserIds(node.resolvedUserIds());
        return respVO;
    }

    public record RouteReadinessEvaluation(
            DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute resolvedRoute,
            DccControlledFileRouteReadinessRespVO response) {

        public DccControlledFileApprovalRouteAssigneeResolver.ResolvedRoute requireReady() {
            if (resolvedRoute == null || !Boolean.TRUE.equals(response.getReady())) {
                String summary = response.getBlockers().stream()
                        .map(blocker -> blocker.getUserName() == null
                                ? blocker.getMessage()
                                : blocker.getUserName() + "：" + blocker.getMessage())
                        .distinct()
                        .collect(Collectors.joining("；"));
                throw exception(CONTROLLED_FILE_ROUTE_NOT_READY, summary);
            }
            return resolvedRoute;
        }
    }
}
