package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDefectReasonDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDefectReasonMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesDefectReasonCatalogServiceTest {

    @Mock
    private MesTeamLeaderScopeService scopeService;
    @Mock
    private MesProcessPoolDefectReasonMapper reasonMapper;
    @Mock
    private MesProcessPoolTeamMaintenanceAuditMapper auditMapper;

    private MesDefectReasonCatalogService service;

    @BeforeEach
    void setUp() {
        service = new MesDefectReasonCatalogServiceImpl(scopeService, reasonMapper, auditMapper);
    }

    @Test
    void shouldCreateDefectReasonInsideLeaderScopeAndKeepAudit() {
        when(reasonMapper.insert(any(MesProcessPoolDefectReasonDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolDefectReasonDO.class).setId(8301L);
            return 1;
        });

        Long reasonId = service.createReason(MesDefectReasonSaveReqBO.builder()
                .leaderUserId(3001L)
                .processId(6001L)
                .reasonType(MesProcessPoolDefectReasonDO.REASON_TYPE_PQC_FAILURE)
                .reasonCode("SEAL_FAIL")
                .reasonName("封口不良")
                .build());

        assertEquals(8301L, reasonId);
        verify(scopeService).assertCanMaintainProcess(3001L, 6001L);
        ArgumentCaptor<MesProcessPoolDefectReasonDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolDefectReasonDO.class);
        verify(reasonMapper).insert(captor.capture());
        assertEquals("SEAL_FAIL", captor.getValue().getReasonCode());
        assertEquals("封口不良", captor.getValue().getReasonName());
        assertTrue(captor.getValue().getEnabled());
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }
}
