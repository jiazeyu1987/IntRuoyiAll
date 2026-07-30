package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolDeviceParameterRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolDeviceParameterRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamMaintenanceAuditMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProcessDeviceParameterRuleServiceTest {

    @Mock
    private MesTeamLeaderScopeService scopeService;
    @Mock
    private MesProcessPoolDeviceParameterRuleMapper ruleMapper;
    @Mock
    private MesProcessPoolTeamMaintenanceAuditMapper auditMapper;

    private MesProcessDeviceParameterRuleService service;

    @BeforeEach
    void setUp() {
        service = new MesProcessDeviceParameterRuleServiceImpl(scopeService, ruleMapper, auditMapper);
    }

    @Test
    void shouldAddProcessDeviceParameterLimitInsideLeaderScope() {
        when(ruleMapper.insert(any(MesProcessPoolDeviceParameterRuleDO.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, MesProcessPoolDeviceParameterRuleDO.class).setId(8401L);
            return 1;
        });

        Long ruleId = service.saveRule(validRuleReq());

        assertEquals(8401L, ruleId);
        verify(scopeService).assertCanMaintainProcess(3001L, 6001L);
        ArgumentCaptor<MesProcessPoolDeviceParameterRuleDO> captor =
                ArgumentCaptor.forClass(MesProcessPoolDeviceParameterRuleDO.class);
        verify(ruleMapper).insert(captor.capture());
        MesProcessPoolDeviceParameterRuleDO rule = captor.getValue();
        assertEquals(7001L, rule.getDeviceId());
        assertEquals("pressure", rule.getParameterCode());
        assertEquals(new BigDecimal("20"), rule.getLowerLimit());
        assertEquals(new BigDecimal("40"), rule.getUpperLimit());
        assertTrue(rule.getEnabled());
        verify(auditMapper).insert(any(MesProcessPoolTeamMaintenanceAuditDO.class));
    }

    @Test
    void shouldRejectInvalidParameterLimitRange() {
        MesProcessDeviceParameterRuleSaveReqBO req = validRuleReq()
                .setLowerLimit(new BigDecimal("50"))
                .setUpperLimit(new BigDecimal("40"));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveRule(req));

        assertEquals(ErrorCodeConstants.PRO_PROCESS_POOL_DEVICE_PARAMETER_LIMIT_INVALID.getCode(), ex.getCode());
        verify(ruleMapper, never()).insert(any(MesProcessPoolDeviceParameterRuleDO.class));
    }

    private static MesProcessDeviceParameterRuleSaveReqBO validRuleReq() {
        return MesProcessDeviceParameterRuleSaveReqBO.builder()
                .leaderUserId(3001L)
                .processId(6001L)
                .deviceId(7001L)
                .parameterCode("pressure")
                .parameterName("压力")
                .lowerLimit(new BigDecimal("20"))
                .upperLimit(new BigDecimal("40"))
                .valueType("DECIMAL")
                .build();
    }
}
