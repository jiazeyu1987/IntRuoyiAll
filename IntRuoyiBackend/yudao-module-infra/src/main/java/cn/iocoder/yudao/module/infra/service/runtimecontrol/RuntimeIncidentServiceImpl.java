package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentActionRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentCloseReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlIncidentRespVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED;

@Service
public class RuntimeIncidentServiceImpl implements RuntimeIncidentService {

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final List<String> SOURCE_TYPES = List.of("DIRECT", "ALERT", "HIGH_RISK_OPERATION");

    private final RuntimeIncidentStore incidentStore;
    private final RuntimeOpsResponsibilityService responsibilityService;

    public RuntimeIncidentServiceImpl(RuntimeIncidentStore incidentStore,
                                      RuntimeOpsResponsibilityService responsibilityService) {
        this.incidentStore = incidentStore;
        this.responsibilityService = responsibilityService;
    }

    @Override
    public PageResult<RuntimeControlIncidentRespVO> getIncidentsPage(RuntimeControlIncidentPageReqVO pageReqVO) {
        List<RuntimeControlIncidentRespVO> filtered = incidentStore.list().stream()
                .filter(incident -> StrUtil.isBlank(pageReqVO.getEnvironment())
                        || pageReqVO.getEnvironment().equals(incident.getEnvironment()))
                .filter(incident -> StrUtil.isBlank(pageReqVO.getStatus())
                        || pageReqVO.getStatus().equals(incident.getStatus()))
                .filter(incident -> StrUtil.isBlank(pageReqVO.getSourceType())
                        || pageReqVO.getSourceType().equals(incident.getSourceType()))
                .toList();
        int fromIndex = Math.min((pageReqVO.getPageNo() - 1) * pageReqVO.getPageSize(), filtered.size());
        int toIndex = Math.min(fromIndex + pageReqVO.getPageSize(), filtered.size());
        return new PageResult<>(filtered.subList(fromIndex, toIndex), (long) filtered.size());
    }

    @Override
    public RuntimeControlIncidentRespVO createIncident(RuntimeControlIncidentCreateReqVO reqVO, String createdBy) {
        validateCreateReq(reqVO);
        requireText(createdBy, "createdBy");
        RuntimeControlIncidentRespVO incident = new RuntimeControlIncidentRespVO();
        incident.setEnvironment(StrUtil.trim(reqVO.getEnvironment()));
        incident.setAction(StrUtil.trim(reqVO.getAction()));
        incident.setSeverity(StrUtil.trim(reqVO.getSeverity()));
        incident.setTitle(StrUtil.trim(reqVO.getTitle()));
        incident.setDescription(StrUtil.trim(reqVO.getDescription()));
        incident.setSourceType(normalizeSourceType(reqVO.getSourceType()));
        incident.setSourceId(StrUtil.trim(reqVO.getSourceId()));
        incident.setStatus(STATUS_OPEN);
        incident.setCreatedBy(StrUtil.trim(createdBy));
        incident.setCreatedAt(LocalDateTime.now());
        incident.setActions(new ArrayList<>());
        return incidentStore.save(incident);
    }

    @Override
    public RuntimeControlIncidentRespVO recordAction(Long id, RuntimeControlIncidentActionReqVO reqVO, String operator) {
        RuntimeControlIncidentRespVO incident = requireOpenIncident(id);
        validateActionReq(reqVO, operator);

        RuntimeControlIncidentActionRespVO action = new RuntimeControlIncidentActionRespVO();
        action.setAction(StrUtil.trim(reqVO.getAction()));
        action.setOperator(StrUtil.trim(operator));
        action.setVerificationResult(StrUtil.trim(reqVO.getVerificationResult()));
        action.setEvidence(StrUtil.trim(reqVO.getEvidence()));
        action.setActedAt(LocalDateTime.now());

        List<RuntimeControlIncidentActionRespVO> actions = incident.getActions() == null
                ? new ArrayList<>() : new ArrayList<>(incident.getActions());
        actions.add(action);
        incident.setActions(actions);
        return incidentStore.save(incident);
    }

    @Override
    public RuntimeControlIncidentRespVO closeIncident(Long id, RuntimeControlIncidentCloseReqVO reqVO, String closedBy) {
        RuntimeControlIncidentRespVO incident = requireOpenIncident(id);
        responsibilityService.validateRequiredOwners(incident.getEnvironment(), incident.getAction());
        validateCloseReq(reqVO, closedBy);

        incident.setOwnerGateResult(StrUtil.trim(reqVO.getOwnerGateResult()).toUpperCase(Locale.ROOT));
        incident.setVerificationResult(StrUtil.trim(reqVO.getVerificationResult()).toUpperCase(Locale.ROOT));
        incident.setRemainingRisk(StrUtil.trim(reqVO.getRemainingRisk()));
        incident.setPostmortemStatus(StrUtil.trim(reqVO.getPostmortemStatus()).toUpperCase(Locale.ROOT));
        incident.setCloseReason(StrUtil.trim(reqVO.getCloseReason()));
        incident.setClosedBy(StrUtil.trim(closedBy));
        incident.setClosedAt(LocalDateTime.now());
        incident.setStatus(STATUS_CLOSED);
        return incidentStore.save(incident);
    }

    private RuntimeControlIncidentRespVO requireOpenIncident(Long id) {
        if (id == null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "incidentId");
        }
        RuntimeControlIncidentRespVO incident = incidentStore.findById(id);
        if (incident == null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "事故不存在：" + id);
        }
        if (STATUS_CLOSED.equals(incident.getStatus())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "事故已关闭：" + id);
        }
        return incident;
    }

    private void validateCreateReq(RuntimeControlIncidentCreateReqVO reqVO) {
        requireText(reqVO.getEnvironment(), "environment");
        requireText(reqVO.getAction(), "action");
        requireText(reqVO.getSeverity(), "severity");
        requireText(reqVO.getTitle(), "title");
        requireText(reqVO.getDescription(), "description");
        String sourceType = normalizeSourceType(reqVO.getSourceType());
        if (!SOURCE_TYPES.contains(sourceType)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "sourceType");
        }
        if (!"DIRECT".equals(sourceType) && StrUtil.isBlank(reqVO.getSourceId())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "sourceId");
        }
    }

    private void validateActionReq(RuntimeControlIncidentActionReqVO reqVO, String operator) {
        requireText(reqVO.getAction(), "action");
        requireText(reqVO.getVerificationResult(), "verificationResult");
        requireText(reqVO.getEvidence(), "evidence");
        requireText(operator, "operator");
    }

    private void validateCloseReq(RuntimeControlIncidentCloseReqVO reqVO, String closedBy) {
        requireText(reqVO.getOwnerGateResult(), "责任人门禁结果");
        requireText(reqVO.getVerificationResult(), "验证结果");
        requireText(reqVO.getRemainingRisk(), "剩余风险");
        requireText(reqVO.getPostmortemStatus(), "复盘状态");
        requireText(reqVO.getCloseReason(), "关闭原因");
        requireText(closedBy, "关闭人");
        if (!"PASSED".equals(StrUtil.trim(reqVO.getOwnerGateResult()).toUpperCase(Locale.ROOT))) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "责任人门禁未通过");
        }
        if (!"PASSED".equals(StrUtil.trim(reqVO.getVerificationResult()).toUpperCase(Locale.ROOT))) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "验证结果未通过");
        }
        if (!"DONE".equals(StrUtil.trim(reqVO.getPostmortemStatus()).toUpperCase(Locale.ROOT))) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "复盘状态未完成");
        }
    }

    private void requireText(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, fieldName);
        }
    }

    private String normalizeSourceType(String sourceType) {
        return StrUtil.blankToDefault(sourceType, "").trim().toUpperCase(Locale.ROOT);
    }
}
