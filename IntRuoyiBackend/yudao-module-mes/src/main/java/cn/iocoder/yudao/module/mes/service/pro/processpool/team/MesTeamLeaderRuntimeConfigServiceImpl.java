package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_DEFECT_REASON_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_DEVICE_PARAMETER_LIMIT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_DEVICE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_DEVICE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_DEVICE_UNAVAILABLE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_EMPLOYEE_PROFILE_NOT_EXISTS;

@Service
@Validated
public class MesTeamLeaderRuntimeConfigServiceImpl implements MesTeamLeaderRuntimeConfigService {

    static final String DEVICE_STATUS_ENABLED = "ENABLED";
    static final String DEVICE_STATUS_REPAIRING = "REPAIRING";
    static final String DEVICE_STATUS_DISABLED = "DISABLED";
    private static final Set<String> DEVICE_STATUSES = Set.of(
            DEVICE_STATUS_ENABLED, DEVICE_STATUS_REPAIRING, DEVICE_STATUS_DISABLED);

    private final MesTeamLeaderScopeService scopeService;
    private final MesRouteStartProductionLeaderAuthorizationService routeStartAuthorizationService;
    private final MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper;
    private final MesProcessPoolTeamEmployeeBindingMapper employeeBindingMapper;
    private final MesProcessPoolTeamDeviceMapper deviceMapper;
    private final MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    private final MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;
    private final MesProcessPoolDefectReasonMapper defectReasonMapper;
    private final MesProcessPoolTeamMaintenanceAuditMapper auditMapper;

    public MesTeamLeaderRuntimeConfigServiceImpl(MesTeamLeaderScopeService scopeService,
                                                 MesRouteStartProductionLeaderAuthorizationService routeStartAuthorizationService,
                                                 MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper,
                                                 MesProcessPoolTeamEmployeeBindingMapper employeeBindingMapper,
                                                 MesProcessPoolTeamDeviceMapper deviceMapper,
                                                 MesProcessPoolTeamProcessDeviceMapper processDeviceMapper,
                                                 MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper,
                                                 MesProcessPoolDefectReasonMapper defectReasonMapper,
                                                 MesProcessPoolTeamMaintenanceAuditMapper auditMapper) {
        this.scopeService = scopeService;
        this.routeStartAuthorizationService = routeStartAuthorizationService;
        this.employeeProfileMapper = employeeProfileMapper;
        this.employeeBindingMapper = employeeBindingMapper;
        this.deviceMapper = deviceMapper;
        this.processDeviceMapper = processDeviceMapper;
        this.parameterRuleMapper = parameterRuleMapper;
        this.defectReasonMapper = defectReasonMapper;
        this.auditMapper = auditMapper;
    }

    @Override
    public Long createEmployee(MesTeamEmployeeProfileSaveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || isBlank(reqBO.getEmployeeCode())
                || isBlank(reqBO.getEmployeeName()) || isBlank(reqBO.getEmployeeType())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "employeeProfile");
        }
        MesProcessPoolTeamEmployeeProfileDO profile = MesProcessPoolTeamEmployeeProfileDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .systemUserId(reqBO.getSystemUserId())
                .employeeCode(reqBO.getEmployeeCode())
                .employeeName(reqBO.getEmployeeName())
                .employeeType(reqBO.getEmployeeType())
                .enabled(Boolean.TRUE)
                .build();
        employeeProfileMapper.insert(profile);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "CREATE_EMPLOYEE_PROFILE",
                "TEAM_EMPLOYEE_PROFILE", profile.getId(), null, profile.toString());
        return profile.getId();
    }

    @Override
    public Long bindEmployeeToProcess(MesTeamProcessEmployeeBindingSaveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getProcessId() == null
                || reqBO.getEmployeeProfileId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "processEmployeeBinding");
        }
        scopeService.assertCanMaintainProcess(reqBO.getLeaderUserId(), reqBO.getProcessId());
        MesProcessPoolTeamEmployeeProfileDO profile = employeeProfileMapper.selectById(reqBO.getEmployeeProfileId());
        if (profile == null || !Objects.equals(profile.getLeaderUserId(), reqBO.getLeaderUserId())
                || !Boolean.TRUE.equals(profile.getEnabled())) {
            throw exception(PRO_PROCESS_POOL_TEAM_EMPLOYEE_PROFILE_NOT_EXISTS, reqBO.getEmployeeProfileId());
        }
        MesProcessPoolTeamEmployeeBindingDO binding = MesProcessPoolTeamEmployeeBindingDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .processId(reqBO.getProcessId())
                .employeeProfileId(profile.getId())
                .employeeUserId(profile.getSystemUserId())
                .enabled(Boolean.TRUE)
                .build();
        employeeBindingMapper.insert(binding);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "BIND_EMPLOYEE_PROCESS",
                "TEAM_EMPLOYEE_BINDING", binding.getId(), null, binding.toString());
        return binding.getId();
    }

    @Override
    public Long createDevice(MesTeamDeviceSaveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || isBlank(reqBO.getDeviceCode())
                || isBlank(reqBO.getDeviceName()) || isBlank(reqBO.getDeviceStatus())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "teamDevice");
        }
        if (!DEVICE_STATUSES.contains(reqBO.getDeviceStatus())) {
            throw exception(PRO_PROCESS_POOL_TEAM_DEVICE_STATUS_INVALID, reqBO.getDeviceStatus());
        }
        MesProcessPoolTeamDeviceDO device = MesProcessPoolTeamDeviceDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .deviceCode(reqBO.getDeviceCode())
                .deviceName(reqBO.getDeviceName())
                .deviceStatus(reqBO.getDeviceStatus())
                .enabled(!DEVICE_STATUS_DISABLED.equals(reqBO.getDeviceStatus()))
                .statusChangedAt(LocalDateTime.now())
                .build();
        deviceMapper.insert(device);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "CREATE_TEAM_DEVICE",
                "TEAM_DEVICE", device.getId(), null, device.toString());
        return device.getId();
    }

    @Override
    public void updateDeviceStatus(MesTeamDeviceStatusUpdateReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getDeviceId() == null
                || isBlank(reqBO.getDeviceStatus())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "teamDeviceStatus");
        }
        if (!DEVICE_STATUSES.contains(reqBO.getDeviceStatus())) {
            throw exception(PRO_PROCESS_POOL_TEAM_DEVICE_STATUS_INVALID, reqBO.getDeviceStatus());
        }
        MesProcessPoolTeamDeviceDO device = requireDevice(reqBO.getLeaderUserId(), reqBO.getDeviceId());
        MesProcessPoolTeamDeviceDO update = MesProcessPoolTeamDeviceDO.builder()
                .id(device.getId())
                .deviceStatus(reqBO.getDeviceStatus())
                .enabled(!DEVICE_STATUS_DISABLED.equals(reqBO.getDeviceStatus()))
                .statusChangedAt(LocalDateTime.now())
                .build();
        deviceMapper.updateById(update);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "UPDATE_TEAM_DEVICE_STATUS",
                "TEAM_DEVICE", device.getId(), device.toString(), update.toString());
    }

    @Override
    public Long bindDeviceToProcess(MesTeamProcessDeviceBindingSaveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getProcessId() == null
                || reqBO.getDeviceId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "processDeviceBinding");
        }
        scopeService.assertCanMaintainProcess(reqBO.getLeaderUserId(), reqBO.getProcessId());
        MesProcessPoolTeamDeviceDO device = requireDevice(reqBO.getLeaderUserId(), reqBO.getDeviceId());
        assertDeviceAvailable(device);
        MesProcessPoolTeamProcessDeviceDO binding = MesProcessPoolTeamProcessDeviceDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .processId(reqBO.getProcessId())
                .deviceId(device.getId())
                .enabled(Boolean.TRUE)
                .build();
        processDeviceMapper.insert(binding);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "BIND_DEVICE_PROCESS",
                "TEAM_PROCESS_DEVICE", binding.getId(), null, binding.toString());
        return binding.getId();
    }

    @Override
    public Long saveDeviceParameterRule(MesTeamDeviceParameterRuleSaveReqBO reqBO) {
        validateParameterRule(reqBO);
        scopeService.assertCanMaintainProcess(reqBO.getLeaderUserId(), reqBO.getProcessId());
        MesProcessPoolTeamDeviceDO device = requireDevice(reqBO.getLeaderUserId(), reqBO.getDeviceId());
        assertDeviceAvailable(device);
        MesProcessPoolDeviceParameterRuleDO rule = MesProcessPoolDeviceParameterRuleDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .routeProcessId(reqBO.getRouteProcessId())
                .processId(reqBO.getProcessId())
                .deviceId(reqBO.getDeviceId())
                .parameterCode(reqBO.getParameterCode())
                .parameterName(reqBO.getParameterName())
                .unit(reqBO.getUnit())
                .lowerLimit(reqBO.getLowerLimit())
                .upperLimit(reqBO.getUpperLimit())
                .defaultValue(reqBO.getDefaultValue())
                .valueType(reqBO.getValueType())
                .enabled(Boolean.TRUE)
                .build();
        parameterRuleMapper.insert(rule);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "SAVE_DEVICE_PARAMETER_RULE",
                "DEVICE_PARAMETER_RULE", rule.getId(), null, rule.toString());
        return rule.getId();
    }

    @Override
    public Long saveProcessDefectReason(MesTeamProcessDefectReasonSaveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getProcessId() == null
                || isBlank(reqBO.getReasonType()) || isBlank(reqBO.getReasonCode()) || isBlank(reqBO.getReasonName())) {
            throw exception(PRO_PROCESS_POOL_DEFECT_REASON_REQUIRED, "processDefectReason");
        }
        boolean lossReason = MesProcessPoolDefectReasonDO.REASON_TYPE_LOSS.equals(reqBO.getReasonType());
        if (lossReason) {
            if (reqBO.getRouteProcessId() == null) {
                throw exception(PRO_PROCESS_POOL_DEFECT_REASON_REQUIRED, "routeProcessId");
            }
            routeStartAuthorizationService.assertCanMaintainRouteProcess(reqBO.getLeaderUserId(),
                    reqBO.getRouteProcessId());
        } else {
            scopeService.assertCanMaintainProcess(reqBO.getLeaderUserId(), reqBO.getProcessId());
        }
        MesProcessPoolDefectReasonDO reason = MesProcessPoolDefectReasonDO.builder()
                .leaderUserId(lossReason ? null : reqBO.getLeaderUserId())
                .routeProcessId(reqBO.getRouteProcessId())
                .processId(reqBO.getProcessId())
                .reasonType(reqBO.getReasonType())
                .reasonCode(reqBO.getReasonCode())
                .reasonName(reqBO.getReasonName())
                .enabled(Boolean.TRUE)
                .build();
        defectReasonMapper.insert(reason);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "SAVE_PROCESS_DEFECT_REASON",
                "DEFECT_REASON", reason.getId(), null, reason.toString());
        return reason.getId();
    }

    private MesProcessPoolTeamDeviceDO requireDevice(Long leaderUserId, Long deviceId) {
        MesProcessPoolTeamDeviceDO device = deviceMapper.selectById(deviceId);
        if (device == null || !Objects.equals(device.getLeaderUserId(), leaderUserId)) {
            throw exception(PRO_PROCESS_POOL_TEAM_DEVICE_NOT_EXISTS, deviceId);
        }
        return device;
    }

    private static void assertDeviceAvailable(MesProcessPoolTeamDeviceDO device) {
        if (!Boolean.TRUE.equals(device.getEnabled()) || !DEVICE_STATUS_ENABLED.equals(device.getDeviceStatus())) {
            throw exception(PRO_PROCESS_POOL_TEAM_DEVICE_UNAVAILABLE, device.getId(), device.getDeviceStatus());
        }
    }

    private static void validateParameterRule(MesTeamDeviceParameterRuleSaveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getProcessId() == null
                || reqBO.getDeviceId() == null || isBlank(reqBO.getParameterCode())
                || reqBO.getLowerLimit() == null || reqBO.getUpperLimit() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "deviceParameterRule");
        }
        validateRange(reqBO.getParameterCode(), reqBO.getLowerLimit(), reqBO.getUpperLimit(), reqBO.getDefaultValue());
    }

    static void validateRange(String parameterCode, BigDecimal lowerLimit, BigDecimal upperLimit,
                              BigDecimal defaultValue) {
        if (lowerLimit.compareTo(upperLimit) > 0) {
            throw exception(PRO_PROCESS_POOL_DEVICE_PARAMETER_LIMIT_INVALID, parameterCode);
        }
        if (defaultValue != null && (defaultValue.compareTo(lowerLimit) < 0
                || defaultValue.compareTo(upperLimit) > 0)) {
            throw exception(PRO_PROCESS_POOL_DEVICE_PARAMETER_LIMIT_INVALID, parameterCode);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
