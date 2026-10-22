package cn.iocoder.yudao.module.mes.service.pro.processpool.team;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.system.dal.mysql.signature.SignatureAuthenticationEventMapper;
import cn.iocoder.yudao.module.system.api.signature.dto.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MesEmployeeSignatureAuthenticationServiceTest {
    @Test void wrongPasswordCommitsANormalRejectionFactAndNeverReturnsASigner() {
        var profiles=mock(MesProcessPoolTeamEmployeeProfileMapper.class);var events=mock(SignatureAuthenticationEventMapper.class);var encoder=mock(PasswordEncoder.class);
        var row=new MesProcessPoolTeamEmployeeProfileDO();row.setId(7L);row.setTenantId(1L);row.setEnabled(true);row.setSignatureCredentialStatus("ACTIVE");row.setSignaturePasswordHash("hash");row.setSignaturePasswordUpdatedAt(LocalDateTime.of(2026,10,9,10,0));row.setSignatureCredentialVersion(1L);row.setSignatureCredentialGeneration("ce75d9a0-9194-4bcb-a26e-ef302b9bc956");
        when(profiles.selectSignatureIdentityForUpdate(1L,7L)).thenReturn(row);when(profiles.writeSignatureFailures(eq(1L),eq(7L),eq(1),any(),isNull())).thenReturn(1);
        when(events.insert(any(cn.iocoder.yudao.module.system.dal.dataobject.signature.SignatureAuthenticationEventDO.class))).thenAnswer(i->{((cn.iocoder.yudao.module.system.dal.dataobject.signature.SignatureAuthenticationEventDO)i.getArgument(0)).setId(71L);return 1;});
        TenantContextHolder.setTenantId(1L);
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2L);
            var service=new MesEmployeeSignatureAuthenticationService(profiles,events,encoder,Clock.fixed(Instant.parse("2026-10-10T02:00:00Z"),ZoneOffset.UTC));
            var result=service.authenticate(new SignatureAuthenticationRequest(1L,"MES_EMPLOYEE_PROFILE",7L,2L,"request","wrong"));
            assertFalse(result.succeeded());assertNull(result.signer());assertEquals("BAD_CREDENTIALS",result.rejectionCode());verify(events).insert(any(cn.iocoder.yudao.module.system.dal.dataobject.signature.SignatureAuthenticationEventDO.class));
        } finally {TenantContextHolder.clear();}
    }
    @Test void passwordCallbacksAreNotPartOfAuthenticatedSignerContract() {
        assertEquals(7,AuthenticatedSigner.class.getRecordComponents().length);
        assertThrows(IllegalArgumentException.class,()->new AuthenticatedSigner(1L,"MES_EMPLOYEE_PROFILE",7L,2L,"ce75d9a0-9194-4bcb-a26e-ef302b9bc956",0L,"request"));
    }
}
