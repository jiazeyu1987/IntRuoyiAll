package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackMaterialReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.MesProFrontlineFeedbackPayloadReqVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDefectReasonOption;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterial;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_MATERIAL_INVALID;

@Component
public class MesProFrontlineFeedbackMaterialSubmissionValidator {

    private final MesFrontlineLossReasonValidator lossReasonValidator;

    public MesProFrontlineFeedbackMaterialSubmissionValidator(
            MesFrontlineLossReasonValidator lossReasonValidator) {
        this.lossReasonValidator = lossReasonValidator;
    }

    public MesProFrontlineFeedbackMaterialSubmission validate(
            List<MesFrontlineProcessMaterial> frozenMaterials,
            List<MesFrontlineDefectReasonOption> frozenLossReasons,
            List<MesProFrontlineFeedbackMaterialReqVO> requestedMaterials) {
        Map<Long, MesFrontlineProcessMaterial> frozenById = indexFrozenMaterials(frozenMaterials);
        Map<Long, MesProFrontlineFeedbackMaterialReqVO> requestedById = indexRequestedMaterials(requestedMaterials);
        if (!frozenById.keySet().equals(requestedById.keySet())) {
            throw invalid("物料集合与冻结工序不一致：expected=" + frozenById.keySet()
                    + ", actual=" + requestedById.keySet());
        }
        List<MesProFrontlineFeedbackMaterialSubmission.Material> validated = new ArrayList<>();
        BigDecimal progressQuantity = null;
        BigDecimal totalLossQuantity = BigDecimal.ZERO;
        for (MesFrontlineProcessMaterial frozen : frozenMaterials) {
            MesProFrontlineFeedbackMaterialReqVO requested = requestedById.get(frozen.materialId());
            validateQuantities(frozen.materialId(), requested.getOutputQuantity(), requested.getLossQuantity());
            List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> normalizedLossDetails =
                    normalizeLossDetails(frozenLossReasons, requested.getLossDetails(), requested.getLossQuantity());
            BigDecimal outputQuantity = requested.getOutputQuantity();
            progressQuantity = progressQuantity == null ? outputQuantity : progressQuantity.min(outputQuantity);
            totalLossQuantity = totalLossQuantity.add(requested.getLossQuantity());
            validated.add(new MesProFrontlineFeedbackMaterialSubmission.Material(
                    frozen.materialId(), frozen.materialCode(), frozen.materialName(),
                    frozen.materialSpecification(), frozen.bomQuantity(), outputQuantity,
                    requested.getLossQuantity(), normalizedLossDetails, requested.getSelectedDevice(),
                    requested.getDeviceParameterReadings() == null
                            ? List.of() : List.copyOf(requested.getDeviceParameterReadings())));
        }
        return new MesProFrontlineFeedbackMaterialSubmission(
                Objects.requireNonNull(progressQuantity), totalLossQuantity, List.copyOf(validated));
    }

    public MesFrontlineLossReasonSnapshot validateProcessPayload(
            MesProFrontlineFeedbackPayloadReqVO payload,
            List<MesFrontlineDefectReasonOption> frozenLossReasons) {
        if (payload == null) {
            throw invalid("报工载荷不能为空");
        }
        BigDecimal outputQuantity = payload.getOutputQuantity();
        BigDecimal lossQuantity = payload.getLossQuantity();
        if (outputQuantity == null || outputQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalid("输出数量必须大于 0");
        }
        if (lossQuantity == null || lossQuantity.compareTo(BigDecimal.ZERO) < 0
                || lossQuantity.compareTo(outputQuantity) > 0) {
            throw invalid("损耗数量不能小于 0 或大于输出数量");
        }
        List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> lossDetails = payload.getLossDetails();
        BigDecimal detailTotal = lossDetails == null ? BigDecimal.ZERO : lossDetails.stream()
                .map(detail -> detail == null || detail.getQuantity() == null
                        ? BigDecimal.ZERO : detail.getQuantity())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (detailTotal.compareTo(lossQuantity) != 0) {
            throw invalid("损耗数量必须等于各损耗原因数量之和");
        }
        List<MesFrontlineLossReasonSnapshot> snapshots = lossReasonValidator.requireSnapshotLossReasons(
                frozenLossReasons, lossDetails, lossQuantity);
        return snapshots == null || snapshots.isEmpty() ? null : snapshots.get(0);
    }

    private Map<Long, MesFrontlineProcessMaterial> indexFrozenMaterials(
            List<MesFrontlineProcessMaterial> frozenMaterials) {
        if (frozenMaterials == null || frozenMaterials.isEmpty()) {
            throw invalid("冻结工序物料不能为空");
        }
        Map<Long, MesFrontlineProcessMaterial> result = new LinkedHashMap<>();
        for (MesFrontlineProcessMaterial material : frozenMaterials) {
            if (material == null || material.materialId() == null || material.materialId() <= 0
                    || result.putIfAbsent(material.materialId(), material) != null) {
                throw invalid("冻结工序物料身份缺失或重复");
            }
        }
        return result;
    }

    private Map<Long, MesProFrontlineFeedbackMaterialReqVO> indexRequestedMaterials(
            List<MesProFrontlineFeedbackMaterialReqVO> requestedMaterials) {
        if (requestedMaterials == null || requestedMaterials.isEmpty()) {
            throw invalid("提交物料明细不能为空");
        }
        Map<Long, MesProFrontlineFeedbackMaterialReqVO> result = new LinkedHashMap<>();
        for (MesProFrontlineFeedbackMaterialReqVO material : requestedMaterials) {
            Long materialId = material == null ? null : material.getMaterialId();
            if (materialId == null || materialId <= 0 || result.putIfAbsent(materialId, material) != null) {
                throw invalid("提交物料身份缺失或重复：" + materialId);
            }
        }
        return result;
    }

    private void validateQuantities(Long materialId, BigDecimal outputQuantity, BigDecimal lossQuantity) {
        if (outputQuantity == null || outputQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid("物料完成数量不能为空或小于 0：" + materialId);
        }
        if (lossQuantity == null || lossQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid("物料损耗数量不能为空或小于 0：" + materialId);
        }
        if (lossQuantity.compareTo(outputQuantity) > 0) {
            throw invalid("损耗数量不能大于完成数量：" + materialId);
        }
    }

    private List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> normalizeLossDetails(
            List<MesFrontlineDefectReasonOption> frozenLossReasons,
            List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> lossDetails,
            BigDecimal lossQuantity) {
        List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> positiveDetails =
                lossDetails == null ? List.of() : lossDetails.stream()
                        .filter(Objects::nonNull)
                        .peek(detail -> {
                            if (detail.getQuantity() == null
                                    || detail.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
                                throw invalid("损耗原因数量不能为空或小于 0");
                            }
                        })
                        .filter(detail -> detail.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                        .toList();
        BigDecimal detailTotal = positiveDetails.stream()
                .map(MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (detailTotal.compareTo(lossQuantity) != 0) {
            throw invalid("损耗数量必须等于各损耗原因数量之和");
        }
        List<MesFrontlineLossReasonSnapshot> reasonSnapshots =
                lossReasonValidator.requireSnapshotLossReasons(
                        frozenLossReasons, positiveDetails, lossQuantity);
        if (reasonSnapshots.size() != positiveDetails.size()) {
            throw invalid("损耗原因快照数量不一致");
        }
        List<MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO> normalized = new ArrayList<>();
        for (int index = 0; index < positiveDetails.size(); index++) {
            MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO detail = positiveDetails.get(index);
            MesFrontlineLossReasonSnapshot reason = reasonSnapshots.get(index);
            normalized.add(new MesProFrontlineFeedbackPayloadReqVO.LossDetailReqVO()
                    .setReasonId(reason.reasonId())
                    .setReasonCode(reason.reasonCode())
                    .setReasonName(reason.reasonName())
                    .setQuantity(detail.getQuantity()));
        }
        return List.copyOf(normalized);
    }

    private static cn.iocoder.yudao.framework.common.exception.ServiceException invalid(String detail) {
        return exception(PRO_FRONTLINE_FEEDBACK_MATERIAL_INVALID, detail);
    }
}
