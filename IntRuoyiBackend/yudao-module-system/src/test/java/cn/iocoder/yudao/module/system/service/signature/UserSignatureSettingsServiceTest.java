package cn.iocoder.yudao.module.system.service.signature;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserSignatureSettingsReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.dataobject.signature.SignatureAuthenticationEventDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.mysql.signature.SignatureAuthenticationEventMapper;
import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class UserSignatureSettingsServiceTest {
    private final AdminUserMapper users=mock(AdminUserMapper.class);
    private final SignatureAuthenticationEventMapper events=mock(SignatureAuthenticationEventMapper.class);
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
    private final UserSignatureSettingsService service=new UserSignatureSettingsService(users,encoder,events,Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"),ZoneOffset.UTC));
    private AdminUserDO user;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(7L); var login=new LoginUser(); login.setId(12L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));
        user=new AdminUserDO().setId(12L).setStatus(0).setPassword(encoder.encode("test-password")).setSignatureCredentialType("PIN").setSignaturePinHash(encoder.encode("888888")).setSignatureCredentialVersion(3L).setSignatureCredentialGeneration("7bd606e5-51e5-4f5c-9a1c-62619cf1ec9d");user.setTenantId(7L);
        when(users.selectSessionSubject(7L,12L)).thenReturn(user);
        when(users.selectPermissionSubjectForUpdate(7L,12L)).thenReturn(user);
        when(events.insert(any(SignatureAuthenticationEventDO.class))).thenReturn(1);
        when(users.updateSignatureSettings(eq(7L),eq(12L),anyString(),anyString())).thenAnswer(call->{
            var stored=new AdminUserDO();org.springframework.beans.BeanUtils.copyProperties(user,stored);
            stored.setSignatureCredentialType(call.getArgument(2)); stored.setSignaturePinHash(call.getArgument(3));stored.setSignatureCredentialVersion(4L);
            when(users.selectPermissionSubjectForUpdate(7L,12L)).thenReturn(stored); return 1;
        });
    }
    @AfterEach void clear() {TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    private UserSignatureSettingsReqVO request(String type,String pin) {var r=new UserSignatureSettingsReqVO();r.setPassword("test-password");r.setCredentialType(type);r.setNewPin(pin);return r;}
    @Test void returnsOnlySelectedMethodAndDoesNotExposeHash() {assertEquals("PIN",service.get().credentialType());assertEquals(1,UserSignatureSettingsService.Settings.class.getRecordComponents().length);}
    @Test void changesPinPreservingLeadingZerosAndAuditContainsNoCredential() {
        service.update(request("PIN","000000"));
        var captor=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(users).updateSignatureSettings(eq(7L),eq(12L),eq("PIN"),captor.capture());
        assertTrue(encoder.matches("000000",captor.getValue()));assertNotEquals("000000",captor.getValue());
        verify(events).insert(argThat((SignatureAuthenticationEventDO e)->e.getActorId()==12L && e.getTenantId()==7L && e.getCredentialVersion()==4L && "CHANGE_PIN_AND_METHOD".equals(e.getOperationType())));
    }
    @Test void switchesToPasswordWithoutChangingPinOrLoginPassword() {
        service.update(request("PASSWORD",null));
        verify(users).updateSignatureSettings(7L,12L,"PASSWORD",user.getSignaturePinHash());
        verify(events).insert(argThat((SignatureAuthenticationEventDO e)->"CHANGE_METHOD".equals(e.getOperationType())));
    }
    @Test void rejectsWrongPasswordAndInvalidPinsWithoutWriting() {
        var r=request("PIN","123456"); r.setPassword("wrong");assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.update(r));
        for(String invalid:List.of("12345","1234567","abcdef","１２３４５６")) assertThrows(IllegalArgumentException.class,()->service.update(request("PIN",invalid)));
        verify(users,never()).updateSignatureSettings(anyLong(),anyLong(),anyString(),anyString()); verifyNoInteractions(events);
    }
    @Test void disabledUserCannotChangeSettings() {user.setStatus(1);assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.update(request("PIN","888888")));verify(users,never()).updateSignatureSettings(anyLong(),anyLong(),anyString(),anyString());}
}
