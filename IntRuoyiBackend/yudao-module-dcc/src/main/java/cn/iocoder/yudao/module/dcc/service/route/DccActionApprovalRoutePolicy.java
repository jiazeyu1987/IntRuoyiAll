package cn.iocoder.yudao.module.dcc.service.route;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteNodeSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileChangeTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

public final class DccActionApprovalRoutePolicy {

    private static final List<Integer> ACTION_STAGE_NOS = List.of(1, 2, 3);

    private static final Map<Integer, DccFixedApprovalRoutePolicy.FixedStageDefinition> ACTION_STAGE_MAP = List.of(
            new DccFixedApprovalRoutePolicy.FixedStageDefinition(1,
                    DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(), 1,
                    true, "ALL", 100, true),
            new DccFixedApprovalRoutePolicy.FixedStageDefinition(2,
                    DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(), 2,
                    false, "ANY", null, true),
            new DccFixedApprovalRoutePolicy.FixedStageDefinition(3,
                    DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(), 3,
                    false, "ANY", null, true)
    ).stream().collect(Collectors.toMap(DccFixedApprovalRoutePolicy.FixedStageDefinition::stageNo,
            Function.identity()));

    private DccActionApprovalRoutePolicy() {
    }

    public static boolean supports(String actionType) {
        return DccControlledFileChangeTypeEnum.NEW.getCode().equals(actionType)
                || DccControlledFileChangeTypeEnum.REVISION.getCode().equals(actionType)
                || DccControlledFileChangeTypeEnum.OBSOLETE.getCode().equals(actionType);
    }

    public static DccFixedApprovalRoutePolicy.FixedStageDefinition requireStage(Integer stageNo, ErrorCode errorCode) {
        DccFixedApprovalRoutePolicy.FixedStageDefinition stageDefinition = ACTION_STAGE_MAP.get(stageNo);
        if (stageDefinition == null) {
            throw exception(errorCode);
        }
        return stageDefinition;
    }

    public static void validateSaveNodes(String actionType, List<DccApprovalRouteNodeSaveReqVO> nodes,
                                         ErrorCode errorCode) {
        if (!supports(actionType) || nodes == null || nodes.size() != stageNos(actionType).size()
                || nodes.stream().anyMatch(Objects::isNull)) {
            throw exception(errorCode);
        }
        List<Integer> stageNos = nodes.stream()
                .map(DccApprovalRouteNodeSaveReqVO::getStageNo)
                .toList();
        validateStageNos(actionType, stageNos, errorCode);
        nodes.forEach(node -> {
            DccFixedApprovalRoutePolicy.FixedStageDefinition stage = requireStage(node.getStageNo(), errorCode);
            if (!Objects.equals(stage.approveMethod(), node.getApproveMethod())
                    || !Objects.equals(stage.approveRatio(), node.getApproveRatio())
                    || !Objects.equals(stage.required(), node.getRequired())) {
                throw exception(errorCode);
            }
        });
        boolean signoffUsesDepartment = nodes.stream()
                .filter(node -> Integer.valueOf(1).equals(node.getStageNo()))
                .allMatch(node -> "DEPT".equalsIgnoreCase(node.getCandidateSourceType()));
        if (!signoffUsesDepartment) {
            throw exception(errorCode);
        }
    }

    public static void validateRouteNodes(String actionType, List<DccCategoryApprovalRouteNodeDO> nodes,
                                          ErrorCode errorCode) {
        if (!supports(actionType) || nodes == null || nodes.size() != stageNos(actionType).size()
                || nodes.stream().anyMatch(Objects::isNull)) {
            throw exception(errorCode);
        }
        List<Integer> stageNos = nodes.stream()
                .map(DccCategoryApprovalRouteNodeDO::getStageNo)
                .toList();
        validateStageNos(actionType, stageNos, errorCode);
        nodes.forEach(node -> {
            DccFixedApprovalRoutePolicy.FixedStageDefinition stage = ACTION_STAGE_MAP.get(node.getStageNo());
            if (stage == null
                    || !Objects.equals(stage.stageCode(), node.getStageCode())
                    || !Objects.equals(stage.stageOrder(), node.getStageOrder())
                    || !Objects.equals(stage.requireAllApprovals(), node.getRequireAllApprovals())
                    || !Objects.equals(stage.approveMethod(), node.getApproveMethod())
                    || !Objects.equals(stage.approveRatio(), node.getApproveRatio())
                    || !Objects.equals(stage.required(), node.getRequired())) {
                throw exception(errorCode);
            }
        });
        boolean signoffUsesDepartment = nodes.stream()
                .filter(node -> Integer.valueOf(1).equals(node.getStageNo()))
                .allMatch(node -> "DEPT".equalsIgnoreCase(node.getCandidateSourceType()));
        if (!signoffUsesDepartment) {
            throw exception(errorCode);
        }
    }

    public static List<Integer> stageNos(String actionType) {
        return DccControlledFileChangeTypeEnum.OBSOLETE.getCode().equals(actionType)
                ? List.of(1, 2) : ACTION_STAGE_NOS;
    }

    private static void validateStageNos(String actionType, List<Integer> stageNos, ErrorCode errorCode) {
        Set<Integer> seenStageNos = stageNos.stream().collect(Collectors.toSet());
        if (seenStageNos.size() != stageNos(actionType).size() || !seenStageNos.containsAll(stageNos(actionType))) {
            throw exception(errorCode);
        }
    }
}
