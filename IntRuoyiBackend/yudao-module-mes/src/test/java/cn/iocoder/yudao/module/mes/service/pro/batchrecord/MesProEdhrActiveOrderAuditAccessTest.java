package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrPermissionErrorCodeConstants.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrActiveOrderAuditAccessTest {
    @Mock PermissionApi permissionApi;
    @Mock MesProEdhrBatchExecutionMapper batchMapper;
    @Mock MesProEdhrBatchExecutionOriginMapper originMapper;
    @Mock MesProcessPoolActiveOrderMapper activeOrderMapper;
    @Mock MesProEdhrBatchExecutionVisibilityService visibilityService;
    @InjectMocks MesProEdhrActiveOrderAuditAccess access;
    MockedStatic<SecurityFrameworkUtils> security;
    @BeforeEach void login() {
        TenantContextHolder.setTenantId(168L);
        security=mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(7L);
    }
    @AfterEach void clear() {security.close();TenantContextHolder.clear();}
    void permissions() {
        when(permissionApi.hasAnyPermissions(7L,"mes:pro-edhr-operation-audit:query")).thenReturn(true);
        when(permissionApi.hasAnyPermissions(7L,"mes:pro-edhr-batch-execution:query")).thenReturn(true);
    }
    MesProEdhrBatchExecutionDO batch() {
        var b=new MesProEdhrBatchExecutionDO().setId(10L).setTenantId(168L).setWorkOrderId(20L)
                .setRouteId(30L).setRouteVersionId(40L).setStatus(40);
        when(batchMapper.selectById(10L)).thenReturn(b);return b;
    }
    MesProEdhrBatchExecutionOriginDO origin() {
        var o=new MesProEdhrBatchExecutionOriginDO().setId(1L).setTenantId(168L).setBatchExecutionId(10L)
                .setWorkOrderId(20L).setActiveOrderId(50L).setEntryType("ACTIVE_ORDER_COMPLETION").setRelationStatus("CAPTURED");
        when(originMapper.selectListByBatchExecutionId(10L)).thenReturn(List.of(o));return o;
    }
    MesProcessPoolActiveOrderDO active() {
        var a=new MesProcessPoolActiveOrderDO().setId(50L).setWorkOrderId(20L)
                .setRouteId(30L).setRouteVersionId(40L).setLeaderUserId(7L).setActiveStatus("REMOVED");
        a.setTenantId(168L);
        when(activeOrderMapper.selectByIdIgnoreDeleted(50L)).thenReturn(a);return a;
    }
    @Test void formalHistoricalBatchStillUsesCurrentVisibilityAndRevocationRules() {
        permissions();var b=batch();origin();active();
        access.requireAuditView(10L);
        verify(visibilityService).requireVisibleBatch(b,7L);
        doThrow(new ServiceException(403,"handover revoked")).when(visibilityService).requireVisibleBatch(b,7L);
        assertEquals(403,assertThrows(ServiceException.class,()->access.requireAuditView(10L)).getCode());
    }
    @Test void auditPermissionAloneDoesNotGrantBatchAccess() {
        when(permissionApi.hasAnyPermissions(7L,"mes:pro-edhr-operation-audit:query")).thenReturn(true);
        assertEquals(PRO_EDHR_OBJECT_PERMISSION_DENIED.getCode(),
                assertThrows(ServiceException.class,()->access.requireAuditView(10L)).getCode());
        verifyNoInteractions(batchMapper,originMapper,activeOrderMapper,visibilityService);
    }
    @Test void independentBatchWithoutTaskScopeStillFails() {
        permissions();batch();when(originMapper.selectListByBatchExecutionId(10L)).thenReturn(List.of());
        assertEquals(PRO_EDHR_PERMISSION_SCOPE_REQUIRED.getCode(),
                assertThrows(ServiceException.class,()->access.requireAuditView(10L)).getCode());
        verifyNoInteractions(visibilityService);
    }
    @Test void crossTenantBatchCannotUseCurrentTenantPermissions() {
        permissions();batch().setTenantId(1L);
        assertThrows(ServiceException.class,()->access.requireAuditView(10L));
        verifyNoInteractions(originMapper,visibilityService);
    }
    @ParameterizedTest @ValueSource(strings={"tenant","batch","workOrder","entryType","relation","ambiguous"})
    void malformedFormalOriginsAreRejectedBeforeVisibility(String defect) {
        permissions();batch();var o=origin();
        switch(defect) {
            case "tenant" -> o.setTenantId(1L);
            case "batch" -> o.setBatchExecutionId(11L);
            case "workOrder" -> o.setWorkOrderId(21L);
            case "entryType" -> o.setEntryType("MANUAL");
            case "relation" -> o.setRelationStatus("PENDING");
            case "ambiguous" -> when(originMapper.selectListByBatchExecutionId(10L)).thenReturn(List.of(o,
                    new MesProEdhrBatchExecutionOriginDO().setTenantId(168L).setBatchExecutionId(10L).setActiveOrderId(51L)));
        }
        assertThrows(ServiceException.class,()->access.requireAuditView(10L));
        verifyNoInteractions(activeOrderMapper,visibilityService);
    }
    @ParameterizedTest @ValueSource(strings={"tenant","workOrder","route","version","holder"})
    void unrelatedActiveOrderCannotProvideReadAuthority(String defect) {
        permissions();batch();origin();var a=active();
        switch(defect) {
            case "tenant" -> a.setTenantId(1L);
            case "workOrder" -> a.setWorkOrderId(21L);
            case "route" -> a.setRouteId(31L);
            case "version" -> a.setRouteVersionId(41L);
            case "holder" -> a.setLeaderUserId(null);
        }
        assertThrows(ServiceException.class,()->access.requireAuditView(10L));
        verifyNoInteractions(visibilityService);
    }
}
