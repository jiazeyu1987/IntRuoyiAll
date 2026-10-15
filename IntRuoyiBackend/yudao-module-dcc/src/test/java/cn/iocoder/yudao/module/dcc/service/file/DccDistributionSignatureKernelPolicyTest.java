package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.api.*;
import cn.iocoder.yudao.module.signature.api.dto.*;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccDistributionSignatureKernelPolicyTest extends BaseMockitoUnitTest {
    @Mock AdminUserApi users;
    @Mock ElectronicSignatureRecordMapper records;
    @Mock GxpAuditService audit;
    ElectronicSignatureServiceImpl service;
    @BeforeEach void fixture(){
        TenantContextHolder.setTenantId(1L);service=new ElectronicSignatureServiceImpl();
        ReflectionTestUtils.setField(service,"adminUserApi",users);ReflectionTestUtils.setField(service,"signatureRecordMapper",records);
        ReflectionTestUtils.setField(service,"gxpAuditService",audit);
        ReflectionTestUtils.setField(service,"subjectAdapters",List.of(new DccControlledFileSignatureSubjectAdapter()));
        lenient().when(records.insert(any(ElectronicSignatureRecordDO.class))).thenAnswer(call->{call.getArgument(0,ElectronicSignatureRecordDO.class).setId(801L);return 1;});
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    ElectronicSignatureCommand command(String module,String action,String subjectType,String reason){
        var evidence=DccControlledFileSignatureEvidence.builder().revisionId(42L).versionNo("B/1")
                .recordHashSnapshot("record-hash").sourceFileHash("source-hash").evidenceHash("evidence-hash").canonicalPayload("{}").build();
        var subject=DccControlledFileSignatureSubjectAdapter.encodeSubjectId(42L,"DISTRIBUTION:51:71","DISTRIBUTION",action,action,evidence);
        return new ElectronicSignatureCommand(module,action,subjectType,subject,
                DccControlledFileSignatureSubjectAdapter.subjectVersion(subject),"test-credential",reason,"receipt-idem",null,null);
    }
    ElectronicSignatureResult sign(ElectronicSignatureCommand command){
        try(var login=mockStatic(SecurityFrameworkUtils.class)){login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);return service.sign(command);}
    }
    @Test void actualKernelAcceptsNullEmptyWhitespaceReceiptReasonsAndPreservesDefaultMeaning(){
        for(String reason:new String[]{null,"","   "}) {
            var result=sign(command("DCC","DISTRIBUTION_ACK","DCC_CONTROLLED_FILE",reason));
            assertEquals(801L,result.signatureId());assertNotNull(result.signedAt());assertEquals("VALID",result.verificationStatus());
        }
        var capture=ArgumentCaptor.forClass(ElectronicSignatureRecordDO.class);
        verify(records,times(3)).insert(capture.capture());
        for(var record:capture.getAllValues()) {assertEquals("",record.getReason());assertEquals("分发确认",record.getMeaningLabel());assertEquals(99L,record.getActorId());}
        verify(users,times(3)).reauthenticateForSignature(99L,"test-credential");
        var auditCapture=ArgumentCaptor.forClass(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class);
        verify(audit,times(3)).append(auditCapture.capture());
        assertTrue(auditCapture.getAllValues().stream().allMatch(command->"signature.dcc.distribution-receipt.create".equals(command.getOperationId())));
    }
    @Test void requiredApprovalAssignmentAndRejectReasonsAreNotRelaxed(){
        for(String action:List.of("APPROVE","REJECT","ASSIGN"))
            assertThrows(ServiceException.class,()->sign(command("DCC",action,"DCC_CONTROLLED_FILE",null)));
        verifyNoInteractions(users);verify(records,never()).insert(any(ElectronicSignatureRecordDO.class));
    }
    @Test void unknownActionAdapterAndSubjectAreRejectedBeforeAnyAuthentication(){
        for(var command:List.of(command("DCC","UNKNOWN","DCC_CONTROLLED_FILE","reason"),
                command("MISSING","DISTRIBUTION_ACK","DCC_CONTROLLED_FILE","reason"),
                command("DCC","DISTRIBUTION_ACK","UNKNOWN","reason")))
            assertThrows(ServiceException.class,()->sign(command));
        verifyNoInteractions(users);verify(records,never()).insert(any(ElectronicSignatureRecordDO.class));
    }
    @Test void duplicateAdaptersCannotChooseAnOptionalPolicyArbitrarily(){
        ReflectionTestUtils.setField(service,"subjectAdapters",List.of(new DccControlledFileSignatureSubjectAdapter(),new DccControlledFileSignatureSubjectAdapter()));
        assertThrows(ServiceException.class,()->sign(command("DCC","DISTRIBUTION_ACK","DCC_CONTROLLED_FILE","reason")));
        verifyNoInteractions(users);
    }
    @Test void optionalReasonDoesNotBypassPasswordReauthentication(){
        doThrow(new IllegalArgumentException("invalid password")).when(users).reauthenticateForSignature(99L,"test-credential");
        assertThrows(IllegalArgumentException.class,()->sign(command("DCC","DISTRIBUTION_ACK","DCC_CONTROLLED_FILE",null)));
        verify(records,never()).insert(any(ElectronicSignatureRecordDO.class));verifyNoInteractions(audit);
    }
    @Test void otherRegisteredAdapterKeepsDefaultRequiredReasonEvenWithSameActionName(){
        ElectronicSignatureSubjectAdapter ordinary=new ElectronicSignatureSubjectAdapter(){
            public String moduleCode(){return "OTHER";}
            public Set<SignatureActionDefinition> supportedActions(){return Set.of(new SignatureActionDefinition("OTHER","DISTRIBUTION_ACK","OTHER_OBJECT","ACK","确认","v1"));}
            public SignatureSubjectSnapshot loadAndAuthorize(SignatureSubjectCommand command){throw new AssertionError("blank reason must be rejected before authorization");}
        };
        ReflectionTestUtils.setField(service,"subjectAdapters",List.of(ordinary));
        assertThrows(ServiceException.class,()->sign(command("OTHER","DISTRIBUTION_ACK","OTHER_OBJECT",null)));
        verifyNoInteractions(users);verify(records,never()).insert(any(ElectronicSignatureRecordDO.class));
    }
    @Test void missingAdaptersRejectOptionalActionRatherThanUseCallerActionAsPolicy(){
        ReflectionTestUtils.setField(service,"subjectAdapters",List.of());
        assertThrows(ServiceException.class,()->sign(command("DCC","DISTRIBUTION_ACK","DCC_CONTROLLED_FILE",null)));
        verifyNoInteractions(users);
    }
    @Test void registeredActionMustStillBeAuthorizedForExactSubject(){
        var request=command("DCC","DISTRIBUTION_ACK","DCC_CONTROLLED_FILE",null);
        var wrongVersion=new ElectronicSignatureCommand(request.moduleCode(),request.actionCode(),request.subjectType(),request.subjectId(),
                "wrong-version",request.credential(),request.reason(),request.idempotencyKey(),null,null);
        assertThrows(IllegalArgumentException.class,()->sign(wrongVersion));
        verify(records,never()).insert(any(ElectronicSignatureRecordDO.class));verifyNoInteractions(audit);
    }
    @Test void optionalReceiptPolicyHasNewVersionAndExistingApprovalPolicyRemainsOriginal(){
        var adapter=new DccControlledFileSignatureSubjectAdapter();
        var receipt=adapter.supportedActions().stream().filter(action->"DISTRIBUTION_ACK".equals(action.actionCode())).findFirst().orElseThrow();
        var approve=adapter.supportedActions().stream().filter(action->"APPROVE".equals(action.actionCode())).findFirst().orElseThrow();
        assertEquals("dcc-distribution-receipt-optional-comment-v2",receipt.policyVersion());
        assertEquals("dcc-controlled-file-signature-v1",approve.policyVersion());
    }
}
