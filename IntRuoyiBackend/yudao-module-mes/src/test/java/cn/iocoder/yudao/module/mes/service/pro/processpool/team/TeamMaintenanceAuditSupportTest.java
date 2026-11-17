package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TeamMaintenanceAuditSupportTest {

    @Test
    void copiedSimulationOrderAuditRecordsChineseSummaryAndFormalIdentity() {
        var mapper = mock(MesProcessPoolTeamMaintenanceAuditMapper.class);
        TeamMaintenanceAuditSupport.insertAudit(mapper, 3001L,
                "COPY_LATEST_SIMULATION_ACTIVE_ORDER", "ACTIVE_ORDER", 8102L,
                "sourceActiveOrderId=8101", "workOrderId=9102,routeVersionId=2,qaRegulationVersionId=3");

        var audit = ArgumentCaptor.forClass(MesProcessPoolTeamMaintenanceAuditDO.class);
        verify(mapper).insert(audit.capture());
        assertEquals("复制最新版本测试订单", audit.getValue().getChangeSummary());
        assertEquals("COPY_LATEST_SIMULATION_ACTIVE_ORDER", audit.getValue().getActionType());
        assertEquals("ACTIVE_ORDER", audit.getValue().getTargetType());
        assertEquals(8102L, audit.getValue().getTargetId());
        assertEquals(3001L, audit.getValue().getOperatorUserId());
        assertEquals("SUCCESS", audit.getValue().getResultStatus());
        assertNotNull(audit.getValue().getAuditTime());
    }
}
