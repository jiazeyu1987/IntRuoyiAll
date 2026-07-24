package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPermissionEvaluateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPermissionEvaluateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPermissionScopeDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrPermissionScopeSaveReqVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionEvaluateCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionEvaluateResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeDetailResult;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeQueryCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeSaveCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionScopeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrPermissionScopeControllerTest {

    @Mock
    private MesProEdhrPermissionScopeService permissionScopeService;
    @InjectMocks
    private MesProEdhrPermissionScopeController controller;

    @Test
    void save_delegatesToPermissionScopeService() {
        MesProEdhrPermissionScopeSaveReqVO reqVO = new MesProEdhrPermissionScopeSaveReqVO()
                .setScopeName("route-922045")
                .setObjectType("ROUTE")
                .setObjectId("922045")
                .setRules(List.of(new MesProEdhrPermissionRuleSaveReqVO()
                        .setSubjectType("USER")
                        .setSubjectId(113L)
                        .setAbility("VIEW")
                        .setDecision("ALLOW")
                        .setPriority(10)));
        when(permissionScopeService.saveRules(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MesProEdhrPermissionScopeDetailResult()
                        .setScopeId(5001L)
                        .setScopeName("route-922045")
                        .setObjectType("ROUTE")
                        .setObjectId("922045")
                        .setVersion(1)
                        .setRules(List.of()));

        MesProEdhrPermissionScopeDetailRespVO respVO = controller.save(reqVO).getData();

        assertEquals(5001L, respVO.getScopeId());
        ArgumentCaptor<MesProEdhrPermissionScopeSaveCommand> captor =
                ArgumentCaptor.forClass(MesProEdhrPermissionScopeSaveCommand.class);
        verify(permissionScopeService).saveRules(captor.capture());
        assertEquals("ROUTE", captor.getValue().getObjectType());
        assertEquals("922045", captor.getValue().getObjectId());
        assertEquals("VIEW", captor.getValue().getRules().get(0).getAbility());
    }

    @Test
    void get_delegatesToPermissionScopeService() {
        when(permissionScopeService.getDetail(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MesProEdhrPermissionScopeDetailResult()
                        .setScopeId(5002L)
                        .setScopeName("execution-8001")
                        .setObjectType("BATCH_RECORD_EXECUTION")
                        .setObjectId("8001")
                        .setVersion(3)
                        .setRules(List.of()));

        MesProEdhrPermissionScopeDetailRespVO respVO = controller.get(null,
                "BATCH_RECORD_EXECUTION", "8001").getData();

        assertEquals(5002L, respVO.getScopeId());
        assertEquals(3, respVO.getVersion());
        ArgumentCaptor<MesProEdhrPermissionScopeQueryCommand> captor =
                ArgumentCaptor.forClass(MesProEdhrPermissionScopeQueryCommand.class);
        verify(permissionScopeService).getDetail(captor.capture());
        assertEquals("BATCH_RECORD_EXECUTION", captor.getValue().getObjectType());
        assertEquals("8001", captor.getValue().getObjectId());
    }

    @Test
    void evaluate_delegatesToPermissionScopeService() {
        MesProEdhrPermissionEvaluateReqVO reqVO = new MesProEdhrPermissionEvaluateReqVO()
                .setObjectType("RECORD_TABLE")
                .setObjectId("RPT-4001")
                .setBatchExecutionId(8001L)
                .setAbilities(List.of("VIEW", "FILL"));
        Map<String, String> decisions = new LinkedHashMap<>();
        decisions.put("VIEW", "ALLOW");
        decisions.put("FILL", "DENY");
        when(permissionScopeService.evaluate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new MesProEdhrPermissionEvaluateResult()
                        .setObjectType("RECORD_TABLE")
                        .setObjectId("RPT-4001")
                        .setDecisions(decisions)
                        .setMatchedRuleIds(List.of(11L, 12L))
                        .setOperationAuditEventId(9001L));

        MesProEdhrPermissionEvaluateRespVO respVO = controller.evaluate(reqVO).getData();

        assertEquals("ALLOW", respVO.getDecisions().get("VIEW"));
        assertEquals("DENY", respVO.getDecisions().get("FILL"));
        assertEquals(List.of(11L, 12L), respVO.getMatchedRuleIds());
        assertEquals(9001L, respVO.getOperationAuditEventId());
        ArgumentCaptor<MesProEdhrPermissionEvaluateCommand> captor =
                ArgumentCaptor.forClass(MesProEdhrPermissionEvaluateCommand.class);
        verify(permissionScopeService).evaluate(captor.capture());
        assertEquals("RECORD_TABLE", captor.getValue().getObjectType());
        assertEquals("RPT-4001", captor.getValue().getObjectId());
        assertEquals(List.of("VIEW", "FILL"), captor.getValue().getAbilities());
    }

    @Test
    void contractMappings_matchPermissionEvaluateEndpoint() throws Exception {
        Method evaluate = MesProEdhrPermissionScopeController.class.getDeclaredMethod("evaluate",
                MesProEdhrPermissionEvaluateReqVO.class);
        assertArrayEquals(new String[]{"/evaluate"}, evaluate.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-permission-scope:evaluate')",
                evaluate.getAnnotation(PreAuthorize.class).value());
        Method save = MesProEdhrPermissionScopeController.class.getDeclaredMethod("save",
                MesProEdhrPermissionScopeSaveReqVO.class);
        assertArrayEquals(new String[]{"/save"}, save.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-permission-scope:save')",
                save.getAnnotation(PreAuthorize.class).value());
        Method get = MesProEdhrPermissionScopeController.class.getDeclaredMethod("get",
                Long.class, String.class, String.class);
        assertArrayEquals(new String[]{"/get"}, get.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-permission-scope:query')",
                get.getAnnotation(PreAuthorize.class).value());
    }
}
