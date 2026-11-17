package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDeviationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesDeviationResponsibilityRecoveryTest {
    private final MesDeviationResponsibilityRecoveryService service = new MesDeviationResponsibilityRecoveryService();
    private final MesProEdhrDeviationMapper deviations = mock(MesProEdhrDeviationMapper.class);
    private final MesDeviationAdministratorService admins = mock(MesDeviationAdministratorService.class);
    private final SecurityFrameworkService permissions = mock(SecurityFrameworkService.class);
    private final MesProEdhrWorkTaskService tasks = mock(MesProEdhrWorkTaskService.class);
    private final MesProEdhrOperationAuditService audit = mock(MesProEdhrOperationAuditService.class);
    private MesProEdhrDeviationDO deviation;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(71L);
        ReflectionTestUtils.setField(service,"deviationMapper",deviations);
        ReflectionTestUtils.setField(service,"administratorService",admins);
        ReflectionTestUtils.setField(service,"securityFrameworkService",permissions);
        ReflectionTestUtils.setField(service,"gxpAuditService",mock(GxpAuditService.class));
        ReflectionTestUtils.setField(service,"auditService",audit);
        ReflectionTestUtils.setField(service,"workTaskService",tasks);
        when(permissions.hasPermission("mes:pro-edhr-deviation:handle")).thenReturn(true);
        var target=new MesDeviationAdministratorRespVO(); target.setId(201L); target.setNickname("接收人");
        when(admins.requireReceiver(201L)).thenReturn(target);
        deviation=new MesProEdhrDeviationDO().setId(901L).setTenantId(71L).setStatus("OPEN").setVersion(3);
        when(deviations.selectByTenantAndIdForUpdate(71L,901L)).thenReturn(deviation);
        when(deviations.assignMissingReceiver(71L,901L,3,201L,"接收人",101L)).thenReturn(1);
    }
    @AfterEach void clear(){ TenantContextHolder.clear(); }
    @Test void explicitAssignmentCreatesTaskAndAuditsSelectedIdentity() {
        service.assignReceiver(101L,request());
        verify(admins).requireReceiver(101L);
        verify(admins).requireReceiver(201L);
        verify(tasks).assignDeviationTask(eq(deviation),eq("DEVIATION_RECEIVE"),eq(201L),contains("经确认"));
        var command=ArgumentCaptor.forClass(MesProEdhrOperationAuditCommand.class);
        verify(audit).recordInCallerTransaction(command.capture());
        assertTrue(command.getValue().getMetadataJson().contains("\"receiverId\":201"));
        assertNotEquals(command.getValue().getBeforeSummaryHash(),command.getValue().getAfterSummaryHash());
        assertEquals(64,command.getValue().getBeforeSummaryHash().length());
        assertEquals(201L,deviation.getReceiverId());
    }
    @Test void existingReceiverCannotBeOverwritten() {
        deviation.setReceiverId(301L);
        assertThrows(ServiceException.class,()->service.assignReceiver(101L,request())); noWrites();
    }
    @Test void closedAndCrossTenantRecordsCannotBeRecovered() {
        deviation.setStatus("CLOSED");
        assertThrows(ServiceException.class,()->service.assignReceiver(101L,request()));
        deviation.setStatus("OPEN").setTenantId(72L);
        assertThrows(ServiceException.class,()->service.assignReceiver(101L,request())); noWrites();
    }
    @Test void staleVersionAndConcurrentAssignmentFail() {
        var req=request(); req.setExpectedDeviationVersion(2);
        assertThrows(ServiceException.class,()->service.assignReceiver(101L,req)); noWrites();
        when(deviations.assignMissingReceiver(71L,901L,3,201L,"接收人",101L)).thenReturn(0);
        assertThrows(ServiceException.class,()->service.assignReceiver(101L,request()));
        verifyNoInteractions(tasks,audit);
    }
    @Test void permissionsReasonAndAdministratorEligibilityAreRequired() {
        when(permissions.hasPermission(anyString())).thenReturn(false);
        assertThrows(ServiceException.class,()->service.assignReceiver(101L,request())); noWrites();
        when(permissions.hasPermission(anyString())).thenReturn(true);
        var req=request();req.setReason("  ");
        assertThrows(ServiceException.class,()->service.assignReceiver(101L,req)); noWrites();
        when(admins.requireReceiver(101L)).thenThrow(new IllegalStateException("不是有效管理员"));
        assertThrows(IllegalStateException.class,()->service.assignReceiver(101L,request())); noWrites();
    }
    @Test void invalidSelectedReceiverFailsBeforeWrites() {
        when(admins.requireReceiver(201L)).thenThrow(new IllegalStateException("接收人已停用"));
        assertThrows(IllegalStateException.class,()->service.assignReceiver(101L,request())); noWrites();
    }
    @Test void taskDispatchFailureIsPropagatedInsteadOfReportingSuccess() {
        when(tasks.assignDeviationTask(any(),anyString(),anyLong(),anyString()))
                .thenThrow(new IllegalStateException("正式路线缺失"));
        assertThrows(IllegalStateException.class,()->service.assignReceiver(101L,request()));
        verifyNoInteractions(audit);
    }
    private void noWrites() {
        verify(deviations,never()).assignMissingReceiver(anyLong(),anyLong(),anyInt(),anyLong(),any(),anyLong());
        verifyNoInteractions(tasks,audit);
    }
    private MesDeviationReceiverAssignReqVO request() {
        var req=new MesDeviationReceiverAssignReqVO();req.setDeviationId(901L);req.setExpectedDeviationVersion(3);
        req.setReceiverId(201L);req.setReason("经确认补录历史责任人");return req;
    }
}
