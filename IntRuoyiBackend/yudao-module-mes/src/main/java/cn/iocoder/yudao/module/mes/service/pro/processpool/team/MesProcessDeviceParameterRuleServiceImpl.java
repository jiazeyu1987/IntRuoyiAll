package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_DEVICE_PARAMETER_LIMIT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;

@Service
@Validated
public class MesProcessDeviceParameterRuleServiceImpl implements MesProcessDeviceParameterRuleService {

    private final MesTeamLeaderScopeService scopeService;
    private final MesProcessPoolDeviceParameterRuleMapper ruleMapper;
    private final MesProcessPoolTeamMaintenanceAuditMapper auditMapper;

    public MesProcessDeviceParameterRuleServiceImpl(MesTeamLeaderScopeService scopeService,
                                                    MesProcessPoolDeviceParameterRuleMapper ruleMapper,
                                                    MesProcessPoolTeamMaintenanceAuditMapper auditMapper) {
        this.scopeService = scopeService;
        this.ruleMapper = ruleMapper;
        this.auditMapper = auditMapper;
    }

    @Override
    public Long saveRule(MesProcessDeviceParameterRuleSaveReqBO reqBO) {
        validateReq(reqBO);
        scopeService.assertCanMaintainProcess(reqBO.getLeaderUserId(), reqBO.getProcessId());
        MesProcessPoolDeviceParameterRuleDO rule = MesProcessPoolDeviceParameterRuleDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .routeProcessId(reqBO.getRouteProcessId())
                .processId(reqBO.getProcessId())
                .deviceId(reqBO.getDeviceId())
                .parameterCode(reqBO.getParameterCode())
                .parameterName(reqBO.getParameterName())
                .lowerLimit(reqBO.getLowerLimit())
                .upperLimit(reqBO.getUpperLimit())
                .valueType(reqBO.getValueType())
                .enabled(Boolean.TRUE)
                .build();
        ruleMapper.insert(rule);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "SAVE_DEVICE_PARAMETER_RULE",
                "DEVICE_PARAMETER_RULE", rule.getId(), null, rule.toString());
        return rule.getId();
    }

    private static void validateReq(MesProcessDeviceParameterRuleSaveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getProcessId() == null
                || reqBO.getDeviceId() == null || isBlank(reqBO.getParameterCode())
                || reqBO.getLowerLimit() == null || reqBO.getUpperLimit() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "deviceParameterRule");
        }
        if (reqBO.getLowerLimit().compareTo(reqBO.getUpperLimit()) > 0) {
            throw exception(PRO_PROCESS_POOL_DEVICE_PARAMETER_LIMIT_INVALID, reqBO.getParameterCode());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
