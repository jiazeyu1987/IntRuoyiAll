package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamDeviceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamProcessDeviceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamProcessDeviceMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
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
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_EMPLOYEE_DISPLAY_NAME_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_FORMAL_EMPLOYEE_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_FORMAL_SIGNATURE_PASSWORD_MANAGED_BY_USER;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED;

@Service
@Validated
public class MesTeamLeaderRuntimeConfigServiceImpl implements MesTeamLeaderRuntimeConfigService {

    static final String DEVICE_STATUS_ENABLED = "ENABLED";
    static final String DEVICE_STATUS_REPAIRING = "REPAIRING";
    static final String DEVICE_STATUS_DISABLED = "DISABLED";
    private static final Set<String> DEVICE_STATUSES = Set.of(
            DEVICE_STATUS_ENABLED, DEVICE_STATUS_REPAIRING, DEVICE_STATUS_DISABLED);
    private static final Set<String> NUMERIC_PARAMETER_VALUE_TYPES = Set.of(
            MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_INTEGER,
            MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_DECIMAL);

    private final MesTeamLeaderScopeService scopeService;
    private final MesRouteStartProductionLeaderAuthorizationService routeStartAuthorizationService;
    private final MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper;
    private final MesProcessPoolTeamLeaderScopeMapper scopeMapper;
    private final MesProcessPoolTeamDeviceMapper deviceMapper;
    private final MesProcessPoolTeamProcessDeviceMapper processDeviceMapper;
    private final MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper;
    private final MesProRouteProcessMapper routeProcessMapper;
    private final MesProcessPoolDefectReasonMapper defectReasonMapper;
    private final MesProcessPoolTeamMaintenanceAuditMapper auditMapper;
    private final AdminUserApi adminUserApi;
    private final PasswordEncoder passwordEncoder;

    public MesTeamLeaderRuntimeConfigServiceImpl(MesTeamLeaderScopeService scopeService,
                                                 MesRouteStartProductionLeaderAuthorizationService routeStartAuthorizationService,
                                                 MesProcessPoolTeamEmployeeProfileMapper employeeProfileMapper,
                                                 MesProcessPoolTeamLeaderScopeMapper scopeMapper,
                                                 MesProcessPoolTeamDeviceMapper deviceMapper,
                                                 MesProcessPoolTeamProcessDeviceMapper processDeviceMapper,
                                                 MesProcessPoolDeviceParameterRuleMapper parameterRuleMapper,
                                                 MesProRouteProcessMapper routeProcessMapper,
                                                 MesProcessPoolDefectReasonMapper defectReasonMapper,
                                                 MesProcessPoolTeamMaintenanceAuditMapper auditMapper,
                                                 AdminUserApi adminUserApi,
                                                 PasswordEncoder passwordEncoder) {
        this.scopeService = scopeService;
        this.routeStartAuthorizationService = routeStartAuthorizationService;
        this.employeeProfileMapper = employeeProfileMapper;
        this.scopeMapper = scopeMapper;
        this.deviceMapper = deviceMapper;
        this.processDeviceMapper = processDeviceMapper;
        this.parameterRuleMapper = parameterRuleMapper;
        this.routeProcessMapper = routeProcessMapper;
        this.defectReasonMapper = defectReasonMapper;
        this.auditMapper = auditMapper;
        this.adminUserApi = adminUserApi;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<MesTeamFormalUserCandidateBO> searchFormalUserCandidates(Long leaderUserId, String keyword) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "formalUserCandidateLeader");
        }
        String normalizedKeyword = normalizeText(keyword);
        if (normalizedKeyword == null) {
            return List.of();
        }
        return adminUserApi.getUserListByNickname(normalizedKeyword).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(MesTeamLeaderRuntimeConfigServiceImpl::resolveUserDisplayName,
                        Comparator.nullsLast(String::compareTo)).thenComparing(AdminUserRespDTO::getId))
                .limit(20)
                .map(user -> new MesTeamFormalUserCandidateBO(user.getId(), resolveUserDisplayName(user)))
                .toList();
    }

    @Override
    public List<MesProcessPoolTeamEmployeeProfileDO> listEmployeeProfiles(Long leaderUserId, Boolean enabled) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "employeeProfileLeader");
        }
        return employeeProfileMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamEmployeeProfileDO>()
                .eq(MesProcessPoolTeamEmployeeProfileDO::getLeaderUserId, leaderUserId)
                .eqIfPresent(MesProcessPoolTeamEmployeeProfileDO::getEnabled, enabled)
                .orderByAsc(MesProcessPoolTeamEmployeeProfileDO::getId));
    }

    @Override
    public Long createTemporaryEmployee(MesTeamTemporaryEmployeeCreateReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || isBlank(reqBO.getDisplayName())
                || isBlank(reqBO.getSignaturePassword())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "temporaryEmployee");
        }
        String displayName = reqBO.getDisplayName().trim();
        assertDisplayNameAvailable(reqBO.getLeaderUserId(), displayName, null);
        MesProcessPoolTeamEmployeeProfileDO profile = MesProcessPoolTeamEmployeeProfileDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .employeeCode("TMP-" + System.currentTimeMillis())
                .employeeName(displayName)
                .displayName(displayName)
                .employeeType("TEMPORARY")
                .signaturePasswordHash(passwordEncoder.encode(reqBO.getSignaturePassword()))
                .signaturePasswordUpdatedAt(LocalDateTime.now())
                .enabled(Boolean.TRUE)
                .build();
        employeeProfileMapper.insert(profile);
        syncProductionEmployeeScope(profile);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), reqBO.getLeaderUserId(),
                "CREATE_TEMPORARY_EMPLOYEE", "TEAM_EMPLOYEE_PROFILE", profile.getId(), "SUCCESS",
                "新增临时工：" + displayName, null, profile.toString());
        return profile.getId();
    }

    @Override
    public Long linkFormalEmployee(MesTeamFormalEmployeeLinkReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getSystemUserId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "formalEmployee");
        }
        AdminUserRespDTO user = requireFormalUser(reqBO.getSystemUserId());
        assertFormalUserNotLinked(reqBO.getLeaderUserId(), reqBO.getSystemUserId());
        String displayName = normalizeText(reqBO.getDisplayName());
        if (displayName == null) {
            displayName = resolveUserDisplayName(user);
        }
        if (displayName == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "formalEmployeeDisplayName");
        }
        assertDisplayNameAvailable(reqBO.getLeaderUserId(), displayName, null);
        MesProcessPoolTeamEmployeeProfileDO profile = MesProcessPoolTeamEmployeeProfileDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .systemUserId(reqBO.getSystemUserId())
                .employeeCode("USER-" + reqBO.getSystemUserId())
                .employeeName(displayName)
                .displayName(displayName)
                .employeeType("FORMAL")
                .enabled(Boolean.TRUE)
                .build();
        employeeProfileMapper.insert(profile);
        syncProductionEmployeeScope(profile);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), reqBO.getLeaderUserId(),
                "LINK_FORMAL_EMPLOYEE", "TEAM_EMPLOYEE_PROFILE", profile.getId(), "SUCCESS",
                "关联正式工：" + displayName, null, profile.toString());
        return profile.getId();
    }

    @Override
    public void renameEmployee(MesTeamEmployeeDisplayNameUpdateReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getEmployeeProfileId() == null
                || isBlank(reqBO.getDisplayName())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "employeeDisplayName");
        }
        MesProcessPoolTeamEmployeeProfileDO profile = requireEmployeeProfile(reqBO.getLeaderUserId(),
                reqBO.getEmployeeProfileId());
        String displayName = reqBO.getDisplayName().trim();
        assertDisplayNameAvailable(reqBO.getLeaderUserId(), displayName, profile.getId());
        MesProcessPoolTeamEmployeeProfileDO update = MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(profile.getId())
                .displayName(displayName)
                .employeeName(displayName)
                .build();
        employeeProfileMapper.updateById(update);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), reqBO.getLeaderUserId(),
                "RENAME_EMPLOYEE", "TEAM_EMPLOYEE_PROFILE", profile.getId(), "SUCCESS",
                "修改生产人员显示名：" + displayName, profile.toString(), update.toString());
    }

    @Override
    public void updateEmployeeEnabled(MesTeamEmployeeStatusUpdateReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getEmployeeProfileId() == null
                || reqBO.getEnabled() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "employeeStatus");
        }
        MesProcessPoolTeamEmployeeProfileDO profile = requireEmployeeProfile(reqBO.getLeaderUserId(),
                reqBO.getEmployeeProfileId());
        boolean enabled = Boolean.TRUE.equals(reqBO.getEnabled());
        MesProcessPoolTeamEmployeeProfileDO update = MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(profile.getId())
                .enabled(enabled)
                .disabledAt(enabled ? null : LocalDateTime.now())
                .build();
        employeeProfileMapper.updateById(update);
        syncProductionEmployeeScopeEnabled(profile, enabled);
        String actionType = enabled ? "ENABLE_EMPLOYEE" : "DISABLE_EMPLOYEE";
        String actionName = enabled ? "启用生产人员：" : "禁用生产人员：";
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), reqBO.getLeaderUserId(),
                actionType, "TEAM_EMPLOYEE_PROFILE", profile.getId(), "SUCCESS",
                actionName + resolveProfileDisplayName(profile), profile.toString(), update.toString());
    }

    @Override
    public void resetTemporaryEmployeeSignaturePassword(MesTeamTempSignaturePasswordResetReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getEmployeeProfileId() == null
                || isBlank(reqBO.getSignaturePassword())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "temporarySignaturePassword");
        }
        MesProcessPoolTeamEmployeeProfileDO profile = requireEmployeeProfile(reqBO.getLeaderUserId(),
                reqBO.getEmployeeProfileId());
        if (!"TEMPORARY".equals(profile.getEmployeeType())) {
            throw exception(PRO_PROCESS_POOL_TEAM_FORMAL_SIGNATURE_PASSWORD_MANAGED_BY_USER,
                    resolveProfileDisplayName(profile));
        }
        MesProcessPoolTeamEmployeeProfileDO update = MesProcessPoolTeamEmployeeProfileDO.builder()
                .id(profile.getId())
                .signaturePasswordHash(passwordEncoder.encode(reqBO.getSignaturePassword()))
                .signaturePasswordUpdatedAt(LocalDateTime.now())
                .build();
        employeeProfileMapper.updateById(update);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), reqBO.getLeaderUserId(),
                "RESET_TEMP_SIGNATURE_PASSWORD", "TEAM_EMPLOYEE_PROFILE", profile.getId(), "SUCCESS",
                "重置临时工签名密码：" + resolveProfileDisplayName(profile), profile.toString(), update.toString());
    }

    @Override
    public List<MesProcessPoolTeamMaintenanceAuditDO> listEmployeeAuditRecords(Long leaderUserId, Long employeeProfileId) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "employeeAuditLeader");
        }
        return auditMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamMaintenanceAuditDO>()
                .eq(MesProcessPoolTeamMaintenanceAuditDO::getLeaderUserId, leaderUserId)
                .eq(MesProcessPoolTeamMaintenanceAuditDO::getTargetType, "TEAM_EMPLOYEE_PROFILE")
                .eqIfPresent(MesProcessPoolTeamMaintenanceAuditDO::getTargetId, employeeProfileId)
                .orderByDesc(MesProcessPoolTeamMaintenanceAuditDO::getAuditTime)
                .orderByDesc(MesProcessPoolTeamMaintenanceAuditDO::getId));
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
                .displayName(reqBO.getEmployeeName())
                .employeeType(reqBO.getEmployeeType())
                .enabled(Boolean.TRUE)
                .build();
        employeeProfileMapper.insert(profile);
        syncProductionEmployeeScope(profile);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "CREATE_EMPLOYEE_PROFILE",
                "TEAM_EMPLOYEE_PROFILE", profile.getId(), null, profile.toString());
        return profile.getId();
    }

    private void syncProductionEmployeeScope(MesProcessPoolTeamEmployeeProfileDO profile) {
        Long employeeUserId = resolveProductionEmployeeUserId(profile);
        if (profile == null || profile.getLeaderUserId() == null || employeeUserId == null) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "productionEmployeeScope");
        }
        MesProcessPoolTeamLeaderScopeDO existing = scopeMapper.selectProductionEmployeeScope(
                profile.getLeaderUserId(), employeeUserId);
        if (existing == null) {
            MesProcessPoolTeamLeaderScopeDO scope = MesProcessPoolTeamLeaderScopeDO.builder()
                    .leaderUserId(profile.getLeaderUserId())
                    .leaderType(MesProcessPoolTeamLeaderScopeDO.LEADER_TYPE_PRODUCTION)
                    .scopeType(MesProcessPoolTeamLeaderScopeDO.SCOPE_TYPE_EMPLOYEE)
                    .employeeUserId(employeeUserId)
                    .enabled(Boolean.TRUE)
                    .build();
            int inserted = scopeMapper.insert(scope);
            if (inserted <= 0) {
                throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "productionEmployeeScopeInsert");
            }
            return;
        }
        if (!Boolean.TRUE.equals(existing.getEnabled())) {
            int updated = scopeMapper.updateById(MesProcessPoolTeamLeaderScopeDO.builder()
                    .id(existing.getId())
                    .enabled(Boolean.TRUE)
                    .build());
            if (updated <= 0) {
                throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "productionEmployeeScopeEnable");
            }
        }
    }

    private void syncProductionEmployeeScopeEnabled(MesProcessPoolTeamEmployeeProfileDO profile, boolean enabled) {
        Long employeeUserId = resolveProductionEmployeeUserId(profile);
        if (profile == null || profile.getLeaderUserId() == null || employeeUserId == null) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "productionEmployeeScopeStatus");
        }
        MesProcessPoolTeamLeaderScopeDO existing = scopeMapper.selectProductionEmployeeScope(
                profile.getLeaderUserId(), employeeUserId);
        if (existing == null) {
            if (enabled) {
                syncProductionEmployeeScope(profile);
            }
            return;
        }
        if (Objects.equals(existing.getEnabled(), enabled)) {
            return;
        }
        int updated = scopeMapper.updateById(MesProcessPoolTeamLeaderScopeDO.builder()
                .id(existing.getId())
                .enabled(enabled)
                .build());
        if (updated <= 0) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "productionEmployeeScopeStatus");
        }
    }

    private Long resolveProductionEmployeeUserId(MesProcessPoolTeamEmployeeProfileDO profile) {
        if (profile == null) {
            return null;
        }
        return profile.getSystemUserId() != null ? profile.getSystemUserId() : profile.getId();
    }

    @Override
    public List<MesProcessPoolTeamDeviceDO> listDevices(Long leaderUserId, Boolean enabled) {
        if (leaderUserId == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "teamDeviceLeader");
        }
        return deviceMapper.selectList(new LambdaQueryWrapperX<MesProcessPoolTeamDeviceDO>()
                .eq(MesProcessPoolTeamDeviceDO::getLeaderUserId, leaderUserId)
                .eqIfPresent(MesProcessPoolTeamDeviceDO::getEnabled, enabled)
                .orderByAsc(MesProcessPoolTeamDeviceDO::getId));
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
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getRouteProcessId() == null
                || reqBO.getDeviceId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "processDeviceBinding");
        }
        MesProRouteProcessDO routeProcess = requireAuthorizedRouteProcess(reqBO.getLeaderUserId(),
                reqBO.getRouteProcessId());
        MesProcessPoolTeamDeviceDO device = requireDevice(reqBO.getLeaderUserId(), reqBO.getDeviceId());
        assertDeviceAvailable(device);
        List<MesProcessPoolTeamProcessDeviceDO> existingBindings = processDeviceMapper.selectList(
                new LambdaQueryWrapperX<MesProcessPoolTeamProcessDeviceDO>()
                        .eq(MesProcessPoolTeamProcessDeviceDO::getLeaderUserId, reqBO.getLeaderUserId())
                        .eq(MesProcessPoolTeamProcessDeviceDO::getProcessId, routeProcess.getProcessId())
                        .eq(MesProcessPoolTeamProcessDeviceDO::getDeviceId, device.getId())
                        .eq(MesProcessPoolTeamProcessDeviceDO::getEnabled, Boolean.TRUE)
                        .orderByAsc(MesProcessPoolTeamProcessDeviceDO::getId));
        if (!existingBindings.isEmpty()) {
            return existingBindings.get(0).getId();
        }
        MesProcessPoolTeamProcessDeviceDO binding = MesProcessPoolTeamProcessDeviceDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .processId(routeProcess.getProcessId())
                .deviceId(device.getId())
                .enabled(Boolean.TRUE)
                .build();
        processDeviceMapper.insert(binding);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "BIND_DEVICE_ROUTE_PROCESS",
                "TEAM_PROCESS_DEVICE", binding.getId(), null, binding.toString());
        return binding.getId();
    }

    @Override
    public Long saveDeviceParameterRule(MesTeamDeviceParameterRuleSaveReqBO reqBO) {
        validateParameterRule(reqBO);
        MesProRouteProcessDO routeProcess = requireAuthorizedRouteProcess(reqBO.getLeaderUserId(),
                reqBO.getRouteProcessId());
        MesProcessPoolTeamDeviceDO device = requireDevice(reqBO.getLeaderUserId(), reqBO.getDeviceId());
        assertDeviceAvailable(device);
        assertDeviceMappedToProcess(reqBO.getLeaderUserId(), routeProcess.getProcessId(), device.getId());
        MesProcessPoolDeviceParameterRuleDO existing = parameterRuleMapper.selectOne(
                new LambdaQueryWrapperX<MesProcessPoolDeviceParameterRuleDO>()
                        .eq(MesProcessPoolDeviceParameterRuleDO::getRouteProcessId, routeProcess.getId())
                        .eq(MesProcessPoolDeviceParameterRuleDO::getDeviceId, device.getId())
                        .eq(MesProcessPoolDeviceParameterRuleDO::getParameterCode, reqBO.getParameterCode()));
        MesProcessPoolDeviceParameterRuleDO rule = MesProcessPoolDeviceParameterRuleDO.builder()
                .id(existing == null ? null : existing.getId())
                .leaderUserId(reqBO.getLeaderUserId())
                .routeProcessId(routeProcess.getId())
                .processId(routeProcess.getProcessId())
                .deviceId(device.getId())
                .parameterCode(reqBO.getParameterCode())
                .parameterName(reqBO.getParameterName())
                .unit(reqBO.getUnit())
                .lowerLimit(reqBO.getLowerLimit())
                .upperLimit(reqBO.getUpperLimit())
                .defaultValue(reqBO.getTargetValue())
                .valueType(reqBO.getValueType())
                .standardText(reqBO.getStandardText())
                .enabled(Boolean.TRUE)
                .build();
        if (existing == null) {
            parameterRuleMapper.insert(rule);
            TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "CREATE_DEVICE_PARAMETER_RULE",
                    "DEVICE_PARAMETER_RULE", rule.getId(), null, rule.toString());
            return rule.getId();
        }
        parameterRuleMapper.updateById(rule);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "UPDATE_DEVICE_PARAMETER_RULE",
                "DEVICE_PARAMETER_RULE", existing.getId(), existing.toString(), rule.toString());
        return existing.getId();
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

    private MesProRouteProcessDO requireAuthorizedRouteProcess(Long leaderUserId, Long routeProcessId) {
        routeStartAuthorizationService.assertCanMaintainRouteProcess(leaderUserId, routeProcessId);
        MesProRouteProcessDO routeProcess = routeProcessMapper.selectById(routeProcessId);
        if (routeProcess == null || routeProcess.getProcessId() == null) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "routeProcessId=" + routeProcessId);
        }
        return routeProcess;
    }

    private void assertDeviceMappedToProcess(Long leaderUserId, Long processId, Long deviceId) {
        List<MesProcessPoolTeamProcessDeviceDO> bindings = processDeviceMapper.selectList(
                new LambdaQueryWrapperX<MesProcessPoolTeamProcessDeviceDO>()
                        .eq(MesProcessPoolTeamProcessDeviceDO::getLeaderUserId, leaderUserId)
                        .eq(MesProcessPoolTeamProcessDeviceDO::getProcessId, processId)
                        .eq(MesProcessPoolTeamProcessDeviceDO::getDeviceId, deviceId)
                        .eq(MesProcessPoolTeamProcessDeviceDO::getEnabled, Boolean.TRUE));
        if (bindings.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_TEAM_SCOPE_REQUIRED, "processDeviceBinding");
        }
    }

    private MesProcessPoolTeamEmployeeProfileDO requireEmployeeProfile(Long leaderUserId, Long employeeProfileId) {
        MesProcessPoolTeamEmployeeProfileDO profile = employeeProfileMapper.selectById(employeeProfileId);
        if (profile == null || !Objects.equals(profile.getLeaderUserId(), leaderUserId)) {
            throw exception(PRO_PROCESS_POOL_TEAM_EMPLOYEE_PROFILE_NOT_EXISTS, employeeProfileId);
        }
        return profile;
    }

    private AdminUserRespDTO requireFormalUser(Long systemUserId) {
        adminUserApi.validateUser(systemUserId);
        AdminUserRespDTO user = adminUserApi.getUser(systemUserId);
        if (user == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "formalEmployeeUser");
        }
        return user;
    }

    private void assertFormalUserNotLinked(Long leaderUserId, Long systemUserId) {
        List<MesProcessPoolTeamEmployeeProfileDO> profiles = employeeProfileMapper.selectList(
                new LambdaQueryWrapperX<MesProcessPoolTeamEmployeeProfileDO>()
                        .eq(MesProcessPoolTeamEmployeeProfileDO::getLeaderUserId, leaderUserId)
                        .eq(MesProcessPoolTeamEmployeeProfileDO::getSystemUserId, systemUserId));
        if (!profiles.isEmpty()) {
            throw exception(PRO_PROCESS_POOL_TEAM_FORMAL_EMPLOYEE_DUPLICATE, systemUserId);
        }
    }

    private void assertDisplayNameAvailable(Long leaderUserId, String displayName, Long excludeProfileId) {
        List<MesProcessPoolTeamEmployeeProfileDO> profiles = employeeProfileMapper.selectList(
                new LambdaQueryWrapperX<MesProcessPoolTeamEmployeeProfileDO>()
                        .eq(MesProcessPoolTeamEmployeeProfileDO::getLeaderUserId, leaderUserId)
                        .eq(MesProcessPoolTeamEmployeeProfileDO::getEnabled, Boolean.TRUE));
        boolean duplicated = profiles.stream()
                .filter(profile -> !Objects.equals(profile.getId(), excludeProfileId))
                .map(MesTeamLeaderRuntimeConfigServiceImpl::resolveProfileDisplayName)
                .filter(Objects::nonNull)
                .anyMatch(existingName -> existingName.equals(displayName));
        if (duplicated) {
            throw exception(PRO_PROCESS_POOL_TEAM_EMPLOYEE_DISPLAY_NAME_DUPLICATE, displayName);
        }
    }

    private static String resolveUserDisplayName(AdminUserRespDTO user) {
        if (user == null) {
            return null;
        }
        String nickname = normalizeText(user.getNickname());
        return nickname != null ? nickname : normalizeText(user.getUsername());
    }

    private static String resolveProfileDisplayName(MesProcessPoolTeamEmployeeProfileDO profile) {
        if (profile == null) {
            return null;
        }
        String displayName = normalizeText(profile.getDisplayName());
        return displayName != null ? displayName : normalizeText(profile.getEmployeeName());
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getRouteProcessId() == null
                || reqBO.getDeviceId() == null || isBlank(reqBO.getParameterCode()) || isBlank(reqBO.getValueType())
                || isBlank(reqBO.getStandardText())) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "deviceParameterRule");
        }
        if (MesProcessPoolDeviceParameterRuleDO.VALUE_TYPE_TEXT_STANDARD.equals(reqBO.getValueType())) {
            if (reqBO.getLowerLimit() != null || reqBO.getUpperLimit() != null || reqBO.getTargetValue() != null) {
                throw exception(PRO_PROCESS_POOL_DEVICE_PARAMETER_LIMIT_INVALID, reqBO.getParameterCode());
            }
            return;
        }
        if (!NUMERIC_PARAMETER_VALUE_TYPES.contains(reqBO.getValueType())
                || reqBO.getLowerLimit() == null || reqBO.getUpperLimit() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "deviceParameterRule");
        }
        validateRange(reqBO.getParameterCode(), reqBO.getLowerLimit(), reqBO.getUpperLimit(), reqBO.getTargetValue());
    }

    static void validateRange(String parameterCode, BigDecimal lowerLimit, BigDecimal upperLimit,
                              BigDecimal targetValue) {
        if (lowerLimit.compareTo(upperLimit) > 0) {
            throw exception(PRO_PROCESS_POOL_DEVICE_PARAMETER_LIMIT_INVALID, parameterCode);
        }
        if (targetValue != null
                && (targetValue.compareTo(lowerLimit) < 0 || targetValue.compareTo(upperLimit) > 0)) {
            throw exception(PRO_PROCESS_POOL_DEVICE_PARAMETER_LIMIT_INVALID, parameterCode);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
