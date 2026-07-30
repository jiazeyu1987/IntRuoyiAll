package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesTeamEmployeeBindingServiceTest {

    @Mock
    private MesTeamLeaderScopeService scopeService;
    @Mock
    private MesProcessPoolTeamEmployeeBindingMapper bindingMapper;
    @Mock
    private MesProcessPoolTeamMaintenanceAuditMapper auditMapper;

    private MesTeamEmployeeBindingService service;

    @BeforeEach
    void setUp() {
        service = new MesTeamEmployeeBindingServiceImpl(scopeService, bindingMapper, auditMapper);
    }

    @Test
    void shouldAddEmployeeBindingInsideLeaderProcessScope() {
        when(bindingMapper.insert(any(MesProcessPoolTeamEmployeeBindingDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolTeamEmployeeBindingDO.class).setId(8201L);
            return 1;
        });

        Long bindingId = service.addEmployeeBinding(MesTeamEmployeeBindingSaveReqBO.builder()
                .leaderUserId(3001L)
                .processId(6001L)
                .employeeUserId(2001L)
                .build());

        assertEquals(8201L, bindingId);
        verify(scopeService).assertCanMaintainProcess(3001L, 6001L);
        ArgumentCaptor<MesProcessPoolTeamEmployeeBindingDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolTeamEmployeeBindingDO.class);
        verify(bindingMapper).insert(captor.capture());
        assertTrue(captor.getValue().getEnabled());
        ArgumentCaptor<MesProcessPoolTeamMaintenanceAuditDO> auditCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamMaintenanceAuditDO.class);
        verify(auditMapper).insert(auditCaptor.capture());
        assertNull(auditCaptor.getValue().getBeforeSnapshot());
        assertTrue(auditCaptor.getValue().getAfterSnapshot().startsWith("{\"snapshotText\""));
    }

    @Test
    void shouldDisableEmployeeBindingWithoutDeletingHistory() {
        when(bindingMapper.selectById(8201L)).thenReturn(MesProcessPoolTeamEmployeeBindingDO.builder()
                .id(8201L)
                .leaderUserId(3001L)
                .processId(6001L)
                .employeeUserId(2001L)
                .enabled(Boolean.TRUE)
                .build());

        service.disableEmployeeBinding(MesTeamEmployeeBindingDisableReqBO.builder()
                .bindingId(8201L)
                .leaderUserId(3001L)
                .build());

        ArgumentCaptor<MesProcessPoolTeamEmployeeBindingDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolTeamEmployeeBindingDO.class);
        verify(bindingMapper).updateById(captor.capture());
        assertEquals(8201L, captor.getValue().getId());
        assertFalse(captor.getValue().getEnabled());
        assertNotNull(captor.getValue().getDisabledAt());
        ArgumentCaptor<MesProcessPoolTeamMaintenanceAuditDO> auditCaptor =
                ArgumentCaptor.forClass(MesProcessPoolTeamMaintenanceAuditDO.class);
        verify(auditMapper).insert(auditCaptor.capture());
        assertTrue(auditCaptor.getValue().getBeforeSnapshot().startsWith("{\"snapshotText\""));
        assertTrue(auditCaptor.getValue().getAfterSnapshot().startsWith("{\"snapshotText\""));
    }
}
