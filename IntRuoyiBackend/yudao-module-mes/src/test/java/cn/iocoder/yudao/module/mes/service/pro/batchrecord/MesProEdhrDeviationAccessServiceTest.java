package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProEdhrDeviationAccessServiceTest {
    @Mock MesProEdhrDeviationMapper deviationMapper;
    @Mock MesProEdhrDeviationHandlingMapper handlingMapper;
    @Mock SecurityFrameworkService securityFrameworkService;
    @InjectMocks MesProEdhrDeviationAccessService service;
    private final MesProEdhrDeviationDO deviation = new MesProEdhrDeviationDO().setId(901L)
            .setTenantId(71L).setReceiverId(201L).setInitiatorUserId(200L);
    @BeforeEach void login() {
        TenantContextHolder.setTenantId(71L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new LoginUser().setId(202L).setTenantId(71L).setUserType(2), null, List.of()));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); SecurityContextHolder.clearContext(); }
    @Test void correctiveOwnerCanReadOnlyAssignedDeviationWithoutGlobalQuery() {
        when(deviationMapper.selectByTenantAndId(71L,901L)).thenReturn(deviation);
        when(handlingMapper.selectByTenantAndDeviationId(71L,901L)).thenReturn(
                new MesProEdhrDeviationHandlingDO().setTenantId(71L).setCorrectiveOwnerId(202L));
        assertDoesNotThrow(() -> service.requireRead(901L));
        assertThrows(ServiceException.class, () -> service.requireRead(902L));
        assertEquals(202L, service.scopedReader());
    }
    @Test void unrelatedUserCannotReadDetail() {
        when(deviationMapper.selectByTenantAndId(71L,901L)).thenReturn(deviation);
        when(handlingMapper.selectByTenantAndDeviationId(71L,901L)).thenReturn(
                new MesProEdhrDeviationHandlingDO().setCorrectiveOwnerId(203L));
        assertThrows(ServiceException.class, () -> service.requireRead(901L));
    }
    @Test void designatedSignatureOwnerCanReadWithoutGlobalQuery() {
        when(deviationMapper.selectByTenantAndId(71L,901L)).thenReturn(deviation);
        when(handlingMapper.selectByTenantAndDeviationId(71L,901L)).thenReturn(
                new MesProEdhrDeviationHandlingDO().setReviewerAssignmentsJson("{\"VERIFIER\":202}"));
        assertDoesNotThrow(() -> service.requireRead(901L));
    }
    @Test void wrongTenantCannotReadEvenWithGlobalPermission() {
        when(deviationMapper.selectByTenantAndId(71L,901L)).thenReturn(deviation.setTenantId(99L));
        assertThrows(ServiceException.class, () -> service.requireRead(901L));
    }
    @Test void onlyReceiverWithHandlePermissionCanEditWholeInvestigation() {
        when(securityFrameworkService.hasPermission("mes:pro-edhr-deviation:handle")).thenReturn(true);
        assertFalse(service.canEditInvestigation(202L,deviation,null));
        assertFalse(service.canEditInvestigation(200L,deviation,null));
        assertTrue(service.canEditInvestigation(201L,deviation,null));
        when(securityFrameworkService.hasPermission("mes:pro-edhr-deviation:handle")).thenReturn(false);
        assertFalse(service.canEditInvestigation(201L,deviation,null));
    }
}
