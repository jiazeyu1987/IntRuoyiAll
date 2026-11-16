package cn.iocoder.yudao.module.signature.service;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureSubjectAdapter;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ElectronicSignatureSessionConfirmationTest {
    @Test
    void recordsSessionConfirmationWithoutClaimingPasswordAuthentication() {
        try (var security=mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(101L);
            TenantContextHolder.setTenantId(1L);
            var users=mock(AdminUserApi.class);
            when(users.getUser(101L)).thenReturn(new AdminUserRespDTO().setId(101L).setTenantId(1L).setStatus(0));
            var records=mock(ElectronicSignatureRecordMapper.class);
            doAnswer(call->{call.getArgument(0,ElectronicSignatureRecordDO.class).setId(7L);return 1;}).when(records).insert(any(ElectronicSignatureRecordDO.class));
            var service=service(users,records);
            var result=service.confirmSession(command("PQC_PATROL_ROUND_CORRECTION",null));
            assertEquals(7L,result.signatureId());
            var saved=ArgumentCaptor.forClass(ElectronicSignatureRecordDO.class);
            verify(records).insert(saved.capture());
            assertEquals("SESSION_CONFIRMATION",saved.getValue().getAuthenticationMethod());
            assertEquals(101L,saved.getValue().getActorId());
            assertEquals(64,saved.getValue().getEvidenceHash().length());
            verify(users,never()).reauthenticateForSignature(anyLong(),any());
            when(users.getUser(101L)).thenReturn(new AdminUserRespDTO().setId(101L).setTenantId(1L).setStatus(1));
            assertThrows(ServiceException.class,()->service.confirmSession(command("PQC_PATROL_ROUND_CORRECTION",null)));
        } finally {TenantContextHolder.clear();}
    }

    @Test
    void ordinarySignatureCannotUseButtonConfirmationOrOmitItsPassword() {
        var service=service(mock(AdminUserApi.class),mock(ElectronicSignatureRecordMapper.class));
        assertThrows(ServiceException.class,()->service.confirmSession(command("FIELD_CHANGE",null)));
        assertThrows(ServiceException.class,()->service.sign(command("FIELD_CHANGE",null)));
        assertThrows(ServiceException.class,()->service.confirmSession(command("PQC_PATROL_ROUND_CORRECTION","password")));
    }

    private ElectronicSignatureCommand command(String action,String credential) {
        return new ElectronicSignatureCommand("MES",action,"MES_BATCH_RECORD","round-3-to-4","1",credential,
                "确认将第3轮移至第4轮","round-confirm",null,null);
    }
    private ElectronicSignatureServiceImpl service(AdminUserApi users,ElectronicSignatureRecordMapper records) {
        var service=new ElectronicSignatureServiceImpl();
        ReflectionTestUtils.setField(service,"adminUserApi",users);
        ReflectionTestUtils.setField(service,"signatureRecordMapper",records);
        ReflectionTestUtils.setField(service,"gxpAuditService",mock(GxpAuditService.class));
        ReflectionTestUtils.setField(service,"subjectAdapters",List.of(new ElectronicSignatureSubjectAdapter(){
            public String moduleCode(){return "MES";}
            public Set<SignatureActionDefinition> supportedActions(){return Set.of(
                new SignatureActionDefinition("MES","PQC_PATROL_ROUND_CORRECTION","MES_BATCH_RECORD","CONFIRM","轮次更正确认","v1"),
                new SignatureActionDefinition("MES","FIELD_CHANGE","MES_BATCH_RECORD","CHANGE","字段更正","v1"));}
            public boolean allowsSessionConfirmation(String action,String subject){return "PQC_PATROL_ROUND_CORRECTION".equals(action);}
            public SignatureSubjectSnapshot loadAndAuthorize(SignatureSubjectCommand cmd){return new SignatureSubjectSnapshot(
                "MES_BATCH_RECORD",cmd.subjectId(),"1","{\"round\":4}",null,null,null,null,null,null,null);}
        }));
        return service;
    }
}
