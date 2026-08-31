package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackMaterialDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMaterialMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_MATERIAL_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackErrorCodeConstants.PRO_FRONTLINE_FEEDBACK_MATERIAL_PERSIST_FAILED;

@Service
public class MesProFeedbackMaterialServiceImpl implements MesProFeedbackMaterialService {

    private final MesProFeedbackMaterialMapper materialMapper;

    public MesProFeedbackMaterialServiceImpl(MesProFeedbackMaterialMapper materialMapper) {
        this.materialMapper = materialMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createMaterials(MesProFeedbackMaterialCreateCommand command) {
        validateCommand(command);
        List<MesProFeedbackMaterialDO> rows = command.entries().stream()
                .map(entry -> toDataObject(command, entry))
                .toList();
        if (!Boolean.TRUE.equals(materialMapper.insertBatch(rows))) {
            throw exception(PRO_FRONTLINE_FEEDBACK_MATERIAL_PERSIST_FAILED, command.feedbackId());
        }
    }

    private void validateCommand(MesProFeedbackMaterialCreateCommand command) {
        if (command == null) {
            throw invalid("创建命令不能为空");
        }
        requirePositive(command.feedbackId(), "feedbackId");
        requirePositive(command.activeOrderId(), "activeOrderId");
        requirePositive(command.workOrderId(), "workOrderId");
        requirePositive(command.routeId(), "routeId");
        requirePositive(command.routeVersionId(), "routeVersionId");
        requirePositive(command.routeProcessId(), "routeProcessId");
        requirePositive(command.processId(), "processId");
        if (command.entries() == null || command.entries().isEmpty()) {
            throw invalid("物料明细不能为空");
        }
        Set<Long> materialIds = new LinkedHashSet<>();
        for (MesProFeedbackMaterialCreateCommand.Entry entry : command.entries()) {
            validateEntry(entry);
            if (!materialIds.add(entry.materialId())) {
                throw invalid("物料重复：" + entry.materialId());
            }
        }
    }

    private void validateEntry(MesProFeedbackMaterialCreateCommand.Entry entry) {
        if (entry == null) {
            throw invalid("物料明细不能为空");
        }
        requirePositive(entry.materialId(), "materialId");
        requireText(entry.materialCode(), "materialCode");
        requireText(entry.materialName(), "materialName");
        requirePositive(entry.bomQuantity(), "bomQuantity");
        requireNonNegative(entry.outputQuantity(), "完成数量不能小于 0");
        requireNonNegative(entry.lossQuantity(), "损耗数量不能小于 0");
        if (entry.lossQuantity().compareTo(entry.outputQuantity()) > 0) {
            throw invalid("损耗数量不能大于完成数量：" + entry.materialId());
        }
        requireText(entry.lossDetailsJson(), "lossDetailsJson");
        requireText(entry.deviceParameterReadingsJson(), "deviceParameterReadingsJson");
    }

    private MesProFeedbackMaterialDO toDataObject(MesProFeedbackMaterialCreateCommand command,
                                                   MesProFeedbackMaterialCreateCommand.Entry entry) {
        return MesProFeedbackMaterialDO.builder()
                .feedbackId(command.feedbackId())
                .activeOrderId(command.activeOrderId())
                .workOrderId(command.workOrderId())
                .routeId(command.routeId())
                .routeVersionId(command.routeVersionId())
                .routeProcessId(command.routeProcessId())
                .processId(command.processId())
                .materialId(entry.materialId())
                .materialCode(entry.materialCode().trim())
                .materialName(entry.materialName().trim())
                .materialSpecification(normalize(entry.materialSpecification()))
                .bomQuantity(entry.bomQuantity())
                .outputQuantity(entry.outputQuantity())
                .lossQuantity(entry.lossQuantity())
                .lossDetailsJson(entry.lossDetailsJson())
                .selectedDeviceJson(normalize(entry.selectedDeviceJson()))
                .deviceParameterReadingsJson(entry.deviceParameterReadingsJson())
                .version(1)
                .build();
    }

    private static void requirePositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw invalid(field + " 必须为正数");
        }
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalid(field + " 必须大于 0");
        }
    }

    private static void requireNonNegative(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid(message);
        }
    }

    private static void requireText(String value, String field) {
        if (normalize(value) == null) {
            throw invalid(field + " 不能为空");
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static cn.iocoder.yudao.framework.common.exception.ServiceException invalid(String detail) {
        return exception(PRO_FRONTLINE_FEEDBACK_MATERIAL_INVALID, detail);
    }
}
