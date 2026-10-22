package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
class DccDistributionBodyOpenedTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileMapper files;@Mock DccControlledFileDistributionMapper distributions;
    @Mock DccControlledFileDistributionRecipientMapper recipients;@Mock GxpAuditService audit;
    @Mock cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
    @Mock DccControlledFileMessageJobMapper jobs;
    @InjectMocks DccDistributionReceiptServiceImpl service;
    DccControlledFileDO file;DccControlledFileDistributionRecipientDO row;
    @BeforeEach void fixture(){TenantContextHolder.setTenantId(1L);var now=LocalDateTime.of(2026,10,11,9,0);
        file=DccControlledFileDO.builder().id(42L).versionNo("B/1").status("ACTIVE").controlledTime(now).activatedTime(now).distributedTime(now).build();file.setTenantId(1L);
        row=DccControlledFileDistributionRecipientDO.builder().id(71L).distributionId(51L).userId(99L).messageJobId(81L).build();
        lenient().when(files.selectByIdAndTenantForUpdate(1L,42L)).thenReturn(file);
        lenient().when(distributions.selectByIdAndTenantForUpdate(1L,51L)).thenReturn(DccControlledFileDistributionDO.builder().id(51L).controlledFileId(42L).distributionMedium("PUBLIC_FOLDER").status("SENT").build());
        lenient().when(recipients.selectByIdAndTenantForUpdate(1L,71L)).thenReturn(row);
        lenient().when(recipients.updateById(any(DccControlledFileDistributionRecipientDO.class))).thenReturn(1);
        var login=new cn.iocoder.yudao.framework.security.core.LoginUser().setId(99L).setTenantId(1L).setUserType(2);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login,null));
        lenient().when(users.getUser(99L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        lenient().when(jobs.selectByIdAndTenantForUpdate(1L,81L)).thenReturn(DccControlledFileMessageJobDO.builder().id(81L).businessType("DISTRIBUTION").businessId(51L).recipientUserId(99L).status("SENT").sentAt(now).build());
    }
    @AfterEach void clear(){TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    void opened(Long actor)throws Exception{try{service.getClass().getMethod("recordDistributionBodyOpened",Long.class,Long.class,Long.class,Long.class).invoke(service,actor,42L,51L,71L);}
        catch(java.lang.reflect.InvocationTargetException e){if(e.getCause() instanceof RuntimeException runtime)throw runtime;throw e;}}
    @Test void actualSuccessfulBodyEventWritesOnlyReadAtAndAuditsExactIdentity()throws Exception{
        assertDoesNotThrow(()->service.getClass().getMethod("recordDistributionBodyOpened",Long.class,Long.class,Long.class,Long.class));opened(99L);
        var capture=ArgumentCaptor.forClass(DccControlledFileDistributionRecipientDO.class);verify(recipients).updateById(capture.capture());
        assertNotNull(capture.getValue().getReadAt());assertNull(capture.getValue().getAcknowledgedAt());assertNull(capture.getValue().getAckComment());verify(audit).append(any());
    }
    @Test void repeatedBodyEventDoesNotReplaceFirstRealTimeOrAppendDuplicateAudit()throws Exception{
        row.setReadAt(LocalDateTime.of(2026,10,11,9,30));opened(99L);verify(recipients,never()).updateById(any(DccControlledFileDistributionRecipientDO.class));verifyNoInteractions(audit);
    }
    @Test void nonRecipientOrPreEffectiveVersionCannotRecordOpen(){assertThrows(Exception.class,()->opened(100L));file.setActivatedTime(null);assertThrows(Exception.class,()->opened(99L));verifyNoInteractions(audit);}
    @Test void disabledCurrentActorOrUnsentMessageCannotRecordBodySuccess(){
        when(users.getUser(99L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(1));assertThrows(Exception.class,()->opened(99L));
        when(users.getUser(99L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0));
        when(jobs.selectByIdAndTenantForUpdate(1L,81L)).thenReturn(DccControlledFileMessageJobDO.builder().id(81L).status("FAILED").build());assertThrows(Exception.class,()->opened(99L));verifyNoInteractions(audit);
    }
    @Test void anonymousCallbackCannotCreateHumanOpenFact(){org.springframework.security.core.context.SecurityContextHolder.clearContext();assertThrows(Exception.class,()->opened(99L));verifyNoInteractions(audit);}
}
