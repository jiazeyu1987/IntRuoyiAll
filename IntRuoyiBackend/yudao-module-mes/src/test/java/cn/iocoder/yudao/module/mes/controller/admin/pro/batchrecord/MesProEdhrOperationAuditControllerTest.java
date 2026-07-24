package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionGateService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_PERMISSION_CONTEXT_MISSING;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.PRO_EDHR_PERMISSION_SCOPE_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProEdhrOperationAuditControllerTest {

    @Mock
    private MesProEdhrOperationAuditService auditService;
    @Mock
    private MesProEdhrPermissionGateService permissionGateService;
    @Mock
    private MesProEdhrBatchExecutionTaskMapper batchTaskMapper;
    @Mock
    private MesProBatchRecordExecutionMapper executionMapper;
    @InjectMocks
    private MesProEdhrOperationAuditController controller;

    @Test
    void pageAndGet_delegateToAuditService() {
        MesProEdhrOperationAuditRespVO event = new MesProEdhrOperationAuditRespVO()
                .setId(1L)
                .setObjectType("RECORD_TABLE")
                .setObjectId("RPT-1001")
                .setOperationType("OPEN")
                .setResultStatus("SUCCESS")
                .setOccurredAt(LocalDateTime.of(2026, 6, 15, 16, 30));
        MesProEdhrOperationAuditPageReqVO reqVO = new MesProEdhrOperationAuditPageReqVO()
                .setBatchExecutionId(1001L)
                .setObjectType("RECORD_TABLE")
                .setObjectId("RPT-1001");
        when(auditService.getPage(reqVO)).thenReturn(new PageResult<>(List.of(event), 1L));
        when(auditService.get(1L)).thenReturn(event);

        assertSame(event, controller.page(reqVO).getData().getList().get(0));
        assertSame(event, controller.get(1L).getData());

        verify(auditService).getPage(reqVO);
        verify(auditService).get(1L);
        verify(permissionGateService, times(2)).requireAbility(argThat(command ->
                "RECORD_TABLE".equals(command.getObjectType())
                        && "RPT-1001".equals(command.getObjectId())
                        && "AUDIT_VIEW".equals(command.getAbility())
                        && "mes:pro-edhr-operation-audit:query".equals(command.getPermissionCode())));
    }

    @Test
    void page_batchExecutionAuditUsesTaskPermissionScopeForAuditView() {
        MesProEdhrOperationAuditPageReqVO reqVO = new MesProEdhrOperationAuditPageReqVO()
                .setBatchExecutionId(900000000525L)
                .setObjectType("BATCH_EXECUTION")
                .setObjectId("900000000525");
        MesProEdhrOperationAuditRespVO event = new MesProEdhrOperationAuditRespVO()
                .setId(9001L)
                .setObjectType("BATCH_EXECUTION")
                .setObjectId("900000000525")
                .setBatchExecutionId(900000000525L)
                .setOperationType("VIEW")
                .setResultStatus("SUCCESS");
        when(batchTaskMapper.selectListByBatchExecutionId(900000000525L))
                .thenReturn(List.of(new MesProEdhrBatchExecutionTaskDO()
                        .setId(3160L)
                        .setBatchExecutionId(900000000525L)
                        .setExecutionId(784L)
                        .setPermissionScopeId(801L)
                        .setRouteProcessId(900025L)
                        .setBatchRecordReportId("RPT-1")
                        .setRecordCategory("BATCH_RECORD")));
        when(auditService.getPage(reqVO)).thenReturn(new PageResult<>(List.of(event), 1L));

        assertSame(event, controller.page(reqVO).getData().getList().get(0));

        verify(permissionGateService).requireAbility(argThat(command ->
                Long.valueOf(801L).equals(command.getScopeId())
                        && "BATCH_EXECUTION_TASK".equals(command.getObjectType())
                        && "3160".equals(command.getObjectId())
                        && Long.valueOf(900000000525L).equals(command.getBatchExecutionId())
                        && Long.valueOf(784L).equals(command.getExecutionId())
                        && "AUDIT_VIEW".equals(command.getAbility())
                        && "mes:pro-edhr-operation-audit:query".equals(command.getPermissionCode())));
    }

    @Test
    void page_recordExecutionAuditUsesExecutionPermissionScopeForAuditView() {
        MesProEdhrOperationAuditPageReqVO reqVO = new MesProEdhrOperationAuditPageReqVO()
                .setObjectType("BATCH_RECORD_EXECUTION")
                .setObjectId("784")
                .setExecutionId(784L);
        MesProEdhrOperationAuditRespVO event = new MesProEdhrOperationAuditRespVO()
                .setId(9100L)
                .setObjectType("BATCH_RECORD_EXECUTION")
                .setObjectId("784")
                .setExecutionId(784L)
                .setOperationType("FIELD_SAVE")
                .setResultStatus("SUCCESS");
        when(executionMapper.selectById(784L)).thenReturn(new MesProBatchRecordExecutionDO()
                .setId(784L)
                .setRouteId(900025L)
                .setRouteProcessId(900026L)
                .setBatchRecordReportId("RPT-1")
                .setRecordCategory("BATCH_RECORD")
                .setPermissionScopeId(801L));
        when(auditService.getPage(reqVO)).thenReturn(new PageResult<>(List.of(event), 1L));

        assertSame(event, controller.page(reqVO).getData().getList().get(0));

        verify(permissionGateService).requireAbility(argThat(command ->
                Long.valueOf(801L).equals(command.getScopeId())
                        && "BATCH_RECORD_EXECUTION".equals(command.getObjectType())
                        && "784".equals(command.getObjectId())
                        && Long.valueOf(784L).equals(command.getExecutionId())
                        && Long.valueOf(900025L).equals(command.getRouteId())
                        && Long.valueOf(900026L).equals(command.getRouteProcessId())
                        && "AUDIT_VIEW".equals(command.getAbility())));
    }

    @Test
    void page_batchExecutionAuditWithoutPermissionScopeFailsFast() {
        MesProEdhrOperationAuditPageReqVO reqVO = new MesProEdhrOperationAuditPageReqVO()
                .setBatchExecutionId(900000000525L)
                .setObjectType("BATCH_EXECUTION")
                .setObjectId("900000000525");
        when(batchTaskMapper.selectListByBatchExecutionId(900000000525L))
                .thenReturn(List.of(new MesProEdhrBatchExecutionTaskDO()
                        .setId(3160L)
                        .setBatchExecutionId(900000000525L)));

        ServiceException exception = assertThrows(ServiceException.class, () -> controller.page(reqVO));

        assertEquals(PRO_EDHR_PERMISSION_SCOPE_REQUIRED.getCode(), exception.getCode());
        verify(auditService, never()).getPage(any());
    }

    @Test
    void page_missingObjectContextFailsFast() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> controller.page(new MesProEdhrOperationAuditPageReqVO()));

        assertEquals(PRO_EDHR_PERMISSION_CONTEXT_MISSING.getCode(), exception.getCode());
        verify(auditService, never()).getPage(any());
    }

    @Test
    void contractMappings_matchOperationAuditQueryEndpoints() throws Exception {
        Method page = MesProEdhrOperationAuditController.class.getDeclaredMethod("page",
                MesProEdhrOperationAuditPageReqVO.class);
        assertArrayEquals(new String[]{"/page"}, page.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-operation-audit:query')",
                page.getAnnotation(PreAuthorize.class).value());

        Method get = MesProEdhrOperationAuditController.class.getDeclaredMethod("get", Long.class);
        assertArrayEquals(new String[]{"/{id}"}, get.getAnnotation(GetMapping.class).value());
        assertEquals("id", get.getParameters()[0].getAnnotation(PathVariable.class).value());
        assertEquals("@ss.hasPermission('mes:pro-edhr-operation-audit:query')",
                get.getAnnotation(PreAuthorize.class).value());
    }
}
