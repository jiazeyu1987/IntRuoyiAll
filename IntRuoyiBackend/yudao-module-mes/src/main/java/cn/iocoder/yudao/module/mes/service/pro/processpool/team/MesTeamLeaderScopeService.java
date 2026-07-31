package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import java.util.Set;

public interface MesTeamLeaderScopeService {

    Set<Long> listResponsibleEmployeeIds(Long leaderUserId, String leaderType);

    void assertCanAccessEmployee(Long leaderUserId, String leaderType, Long employeeUserId);

    void assertCanMaintainProcess(Long leaderUserId, Long processId);
}
