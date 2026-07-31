package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_TEAM_EMPLOYEE_BINDING_NOT_EXISTS;

@Service
@Validated
public class MesTeamEmployeeBindingServiceImpl implements MesTeamEmployeeBindingService {

    private final MesTeamLeaderScopeService scopeService;
    private final MesProcessPoolTeamEmployeeBindingMapper bindingMapper;
    private final MesProcessPoolTeamMaintenanceAuditMapper auditMapper;

    public MesTeamEmployeeBindingServiceImpl(MesTeamLeaderScopeService scopeService,
                                             MesProcessPoolTeamEmployeeBindingMapper bindingMapper,
                                             MesProcessPoolTeamMaintenanceAuditMapper auditMapper) {
        this.scopeService = scopeService;
        this.bindingMapper = bindingMapper;
        this.auditMapper = auditMapper;
    }

    @Override
    public Long addEmployeeBinding(MesTeamEmployeeBindingSaveReqBO reqBO) {
        if (reqBO == null || reqBO.getLeaderUserId() == null || reqBO.getProcessId() == null
                || reqBO.getEmployeeUserId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "teamEmployeeBinding");
        }
        scopeService.assertCanMaintainProcess(reqBO.getLeaderUserId(), reqBO.getProcessId());
        MesProcessPoolTeamEmployeeBindingDO binding = MesProcessPoolTeamEmployeeBindingDO.builder()
                .leaderUserId(reqBO.getLeaderUserId())
                .processId(reqBO.getProcessId())
                .employeeUserId(reqBO.getEmployeeUserId())
                .enabled(Boolean.TRUE)
                .build();
        bindingMapper.insert(binding);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "ADD_EMPLOYEE",
                "TEAM_EMPLOYEE_BINDING", binding.getId(), null, binding.toString());
        return binding.getId();
    }

    @Override
    public void disableEmployeeBinding(MesTeamEmployeeBindingDisableReqBO reqBO) {
        if (reqBO == null || reqBO.getBindingId() == null || reqBO.getLeaderUserId() == null) {
            throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "disableEmployeeBinding");
        }
        MesProcessPoolTeamEmployeeBindingDO binding = bindingMapper.selectById(reqBO.getBindingId());
        if (binding == null) {
            throw exception(PRO_PROCESS_POOL_TEAM_EMPLOYEE_BINDING_NOT_EXISTS, reqBO.getBindingId());
        }
        scopeService.assertCanMaintainProcess(reqBO.getLeaderUserId(), binding.getProcessId());
        MesProcessPoolTeamEmployeeBindingDO update = MesProcessPoolTeamEmployeeBindingDO.builder()
                .id(binding.getId())
                .enabled(Boolean.FALSE)
                .disabledAt(LocalDateTime.now())
                .build();
        bindingMapper.updateById(update);
        TeamMaintenanceAuditSupport.insertAudit(auditMapper, reqBO.getLeaderUserId(), "DISABLE_EMPLOYEE",
                "TEAM_EMPLOYEE_BINDING", binding.getId(), binding.toString(), update.toString());
    }
}
