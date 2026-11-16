package cn.iocoder.yudao.module.mes.service.pro.processpool.team;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo.MesEmployeeSignaturePasswordChangeReqVO;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineDeviceAccountContextService;
import cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner;
import cn.iocoder.yudao.module.system.dal.mysql.signature.SignatureAuthenticationEventMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.signature.SignatureAuthenticationEventDO;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class MesEmployeeSignatureSettingsTest {
    private final MesProcessPoolTeamEmployeeProfileMapper profiles=mock(MesProcessPoolTeamEmployeeProfileMapper.class);
    private final SignatureAuthenticationEventMapper events=mock(SignatureAuthenticationEventMapper.class);
    private final MesFrontlineDeviceAccountContextService context=mock(MesFrontlineDeviceAccountContextService.class);
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
    private final MesEmployeeSignatureCredentialService service=new MesEmployeeSignatureCredentialService(profiles,encoder,Clock.fixed(Instant.parse("2026-11-10T02:00:00Z"),ZoneOffset.UTC));
    private final AuthenticatedSigner signer=new AuthenticatedSigner(1L,"MES_EMPLOYEE_PROFILE",7L,2L,"ce75d9a0-9194-4bcb-a26e-ef302b9bc956",3L,"settings-request");
    private MesProcessPoolTeamEmployeeProfileDO row;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        row=new MesProcessPoolTeamEmployeeProfileDO();row.setId(7L);row.setTenantId(1L);row.setEnabled(true);row.setLeaderUserId(9L);row.setSignatureCredentialType("PIN");row.setSignaturePinHash(encoder.encode("888888"));row.setSignatureCredentialVersion(3L);row.setSignatureCredentialGeneration(signer.credentialGeneration());
        ReflectionTestUtils.setField(service,"contextService",context); ReflectionTestUtils.setField(service,"events",events);
        when(context.resolveResponsibleLeaderUserId(2L)).thenReturn(9L);
        when(profiles.selectSignatureIdentityForUpdate(1L,7L)).thenReturn(row);
        when(events.insert(any(SignatureAuthenticationEventDO.class))).thenReturn(1);
        when(profiles.updateSignatureSettings(eq(1L),eq(7L),anyString(),anyString())).thenAnswer(call->{
            var stored=new MesProcessPoolTeamEmployeeProfileDO();org.springframework.beans.BeanUtils.copyProperties(row,stored);
            stored.setSignatureCredentialType(call.getArgument(2));stored.setSignaturePinHash(call.getArgument(3));stored.setSignatureCredentialVersion(4L);
            when(profiles.selectSignatureIdentityForUpdate(1L,7L)).thenReturn(stored);return 1;
        });
    }
    @AfterEach void clear() {TenantContextHolder.clear();}
    private MesEmployeeSignaturePasswordChangeReqVO request(String type,String pin) {var r=new MesEmployeeSignaturePasswordChangeReqVO();r.setCredentialType(type);r.setNewPin(pin);return r;}
    @Test void employeeCanChangePinWithoutCreatingPasswordAndAuditsOwnIdentity() {
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2L);
            service.updateSettings(signer,request("PIN","000000"));
            var hash=org.mockito.ArgumentCaptor.forClass(String.class);
            verify(profiles).updateSignatureSettings(eq(1L),eq(7L),eq("PIN"),hash.capture());assertTrue(encoder.matches("000000",hash.getValue()));
            verify(events).insert(argThat((SignatureAuthenticationEventDO e)->e.getActorId()==7L && e.getOperatorId()==2L && "MES_EMPLOYEE_PROFILE".equals(e.getIdentityDomain()) && e.getCredentialVersion()==4L));
            verify(profiles,never()).insertSignaturePasswordHistory(anyLong(),anyLong(),anyString(),any());
        }
    }
    @Test void employeeCannotSwitchToMissingPasswordOrWriteOutsideLeaderScope() {
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2L);
            assertThrows(IllegalArgumentException.class,()->service.updateSettings(signer,request("PASSWORD",null)));
            row.setLeaderUserId(99L);
            assertThrows(IllegalArgumentException.class,()->service.updateSettings(signer,request("PIN","888888")));
            verify(profiles,never()).updateSignatureSettings(anyLong(),anyLong(),anyString(),anyString());verifyNoInteractions(events);
        }
    }
    @Test void credentialVersionChangeStopsSettingsWrite() {
        try(var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(2L);row.setSignatureCredentialVersion(4L);
            assertThrows(IllegalArgumentException.class,()->service.updateSettings(signer,request("PIN","888888")));
            verify(profiles,never()).updateSignatureSettings(anyLong(),anyLong(),anyString(),anyString());
        }
    }
}
