package cn.iocoder.yudao.module.dcc.service.route;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.module.dcc.controller.admin.route.vo.DccApprovalRouteNodeSaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

public final class DccFixedApprovalRoutePolicy {

    private static final List<Integer> FIXED_STAGE_NOS = List.of(1, 2, 3, 4);

    private static final Map<Integer, FixedStageDefinition> FIXED_STAGE_MAP = List.of(
            new FixedStageDefinition(1, DccControlledFileStageCodeEnum.DOC_CONTROL_REVIEW.getCode(),
                    1, false, "ANY", null, true),
            new FixedStageDefinition(2, DccControlledFileStageCodeEnum.MATRIX_REVIEW.getCode(),
                    2, true, "ALL", 100, true),
            new FixedStageDefinition(3, DccControlledFileStageCodeEnum.MATRIX_APPROVAL.getCode(),
                    3, false, "ANY", null, true),
            new FixedStageDefinition(4, DccControlledFileStageCodeEnum.DOC_CONTROL_APPROVAL.getCode(),
                    4, false, "ANY", null, true)
    ).stream().collect(Collectors.toMap(FixedStageDefinition::stageNo, Function.identity()));

    private DccFixedApprovalRoutePolicy() {
    }

    public static FixedStageDefinition requireStage(Integer stageNo, ErrorCode errorCode) {
        FixedStageDefinition stageDefinition = FIXED_STAGE_MAP.get(stageNo);
        if (stageDefinition == null) {
            throw exception(errorCode);
        }
        return stageDefinition;
    }

    public static void validateSaveNodes(List<DccApprovalRouteNodeSaveReqVO> nodes, ErrorCode errorCode) {
        if (nodes == null || nodes.size() != FIXED_STAGE_NOS.size() || nodes.stream().anyMatch(Objects::isNull)) {
            throw exception(errorCode);
        }
        List<Integer> stageNos = nodes.stream()
                .map(DccApprovalRouteNodeSaveReqVO::getStageNo)
                .toList();
        validateFixedStageNos(stageNos, errorCode);
        nodes.forEach(node -> {
            FixedStageDefinition stage = requireStage(node.getStageNo(), errorCode);
            if (!matchesFixedPolicy(stage, node.getApproveMethod(), node.getApproveRatio(), node.getRequired())) {
                throw exception(errorCode);
            }
        });
    }

    public static void validateRouteNodes(List<DccCategoryApprovalRouteNodeDO> nodes, ErrorCode errorCode) {
        if (nodes == null || nodes.size() != FIXED_STAGE_NOS.size() || nodes.stream().anyMatch(Objects::isNull)) {
            throw exception(errorCode);
        }
        List<Integer> stageNos = nodes.stream()
                .map(DccCategoryApprovalRouteNodeDO::getStageNo)
                .toList();
        validateFixedStageNos(stageNos, errorCode);
        nodes.forEach(node -> {
            FixedStageDefinition stage = requireStage(node.getStageNo(), errorCode);
            if (!Objects.equals(stage.stageCode(), node.getStageCode())
                    || !Objects.equals(stage.stageOrder(), node.getStageOrder())
                    || !Objects.equals(stage.requireAllApprovals(), node.getRequireAllApprovals())
                    || !matchesFixedPolicy(stage, node.getApproveMethod(), node.getApproveRatio(), node.getRequired())) {
                throw exception(errorCode);
            }
        });
    }

    private static void validateFixedStageNos(List<Integer> stageNos, ErrorCode errorCode) {
        if (stageNos.size() != FIXED_STAGE_NOS.size()) {
            throw exception(errorCode);
        }
        Set<Integer> seenStageNos = stageNos.stream().collect(Collectors.toSet());
        if (seenStageNos.size() != FIXED_STAGE_NOS.size() || !seenStageNos.containsAll(FIXED_STAGE_NOS)) {
            throw exception(errorCode);
        }
    }

    private static boolean matchesFixedPolicy(FixedStageDefinition stage, String approveMethod,
                                              Integer approveRatio, Boolean required) {
        return Objects.equals(stage.approveMethod(), approveMethod)
                && Objects.equals(stage.approveRatio(), approveRatio)
                && Objects.equals(stage.required(), required);
    }

    public record FixedStageDefinition(Integer stageNo, String stageCode, Integer stageOrder,
                                       Boolean requireAllApprovals, String approveMethod,
                                       Integer approveRatio, Boolean required) {
    }
}
