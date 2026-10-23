package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real task-only H2 outer transaction; account/storage/Mapper ports are explicit isolated doubles.
 * Whole native Completion test separately proves real SQL rollback, HMAC and platform messages. */
class DccElectronicReceiptBindingTest {
    DccControlledFileSignatureBindingService service;
    DccControlledFileSignatureBindingMapper bindings;
    DccControlledFileSignatureMapper signatures;
    DccControlledFileMapper files;
    DccControlledFileDistributionMapper scopes;
    DccControlledFileDistributionRecipientMapper recipients;
    DccControlledFileMessageJobMapper jobs;
    FileService storage;
    GxpAuditService audit;
    TransactionTemplate tx;
    DccControlledFileDO file;
    DccControlledFileSignatureDO signature;
    DccControlledFileDistributionDO scope;
    DccControlledFileDistributionRecipientDO recipient;
    DccControlledFileMessageJobDO job;
    final Map<Long,DccControlledFileSignatureBindingDO> saved=new HashMap<>();
    final byte[] bytes="task-only published PDF".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    @BeforeEach void fixture()throws Exception{
        TenantContextHolder.setTenantId(1L);login(99L,1L,2);
        var data=new DriverManagerDataSource("jdbc:h2:mem:w05_receipt_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa","");tx=new TransactionTemplate(new DataSourceTransactionManager(data));
        service=new DccControlledFileSignatureBindingService();bindings=mock(DccControlledFileSignatureBindingMapper.class);signatures=mock(DccControlledFileSignatureMapper.class);files=mock(DccControlledFileMapper.class);scopes=mock(DccControlledFileDistributionMapper.class);recipients=mock(DccControlledFileDistributionRecipientMapper.class);jobs=mock(DccControlledFileMessageJobMapper.class);storage=mock(FileService.class);audit=mock(GxpAuditService.class);
        // New dependencies are wired only once the authorized method exists, retaining a valid RED before implementation.
        wire("bindingMapper",bindings);wire("signatureMapper",signatures);wire("fileService",storage);
        for(var entry:Map.of("controlledFileMapper",files,"distributionMapper",scopes,"distributionRecipientMapper",recipients,"messageJobMapper",jobs,"gxpAuditService",audit).entrySet())if(Arrays.stream(service.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals(entry.getKey())))wire(entry.getKey(),entry.getValue());
        var at=LocalDateTime.of(2026,10,13,1,0);file=DccControlledFileDO.builder().id(20L).tenantId(1L).versionNo("A/1").status("ACTIVE").publishedFileId(500L).controlledTime(at).activatedTime(at).distributedTime(at).build();
        signature=DccControlledFileSignatureDO.builder().id(70L).controlledFileId(20L).revisionId(20L).versionNo("A/1").actorId(99L).taskId("DISTRIBUTION:30:40").actionType("DISTRIBUTION_ACK").meaningCode("DISTRIBUTION_ACK").signaturePurpose("DISTRIBUTION_ACK").signedAt(at).passwordVerified(true).evidenceHash("a".repeat(64)).evidenceHashAlgorithm("HMAC_SHA256").evidenceStatus("VALID").build();
        scope=DccControlledFileDistributionDO.builder().id(30L).tenantId(1L).controlledFileId(20L).distributionMedium("PUBLIC_FOLDER").status("SENT").build();recipient=DccControlledFileDistributionRecipientDO.builder().id(40L).tenantId(1L).distributionId(30L).userId(99L).messageJobId(50L).build();job=DccControlledFileMessageJobDO.builder().id(50L).tenantId(1L).businessType("DISTRIBUTION").businessId(30L).recipientUserId(99L).status("SENT").sentAt(at).build();
        when(files.selectByIdAndTenantForUpdate(1L,20L)).thenReturn(file);when(signatures.selectById(70L)).thenReturn(signature);when(scopes.selectByIdAndTenantForUpdate(1L,30L)).thenReturn(scope);when(recipients.selectByIdAndTenantForUpdate(1L,40L)).thenReturn(recipient);when(jobs.selectByIdAndTenantForUpdate(1L,50L)).thenReturn(job);when(bindings.selectBySignatureId(anyLong())).thenAnswer(c->saved.get(c.getArgument(0)));when(bindings.insert(any(DccControlledFileSignatureBindingDO.class))).thenAnswer(c->{var row=c.<DccControlledFileSignatureBindingDO>getArgument(0);row.setId(100L);saved.put(row.getSignatureId(),row);return 1;});
        when(storage.getFile(500L)).thenReturn(cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO.builder().id(500L).configId(1L).path("task/published.pdf").build());when(storage.getFileContent(1L,"task/published.pdf")).thenReturn(bytes);
    }
    void wire(String field,Object value){ReflectionTestUtils.setField(service,field,value);}
    void login(Long actor,Long tenant,Integer type){SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(actor).setTenantId(tenant).setUserType(type),new org.springframework.mock.web.MockHttpServletRequest());}
    void bind(){ReflectionTestUtils.invokeMethod(service,"bindElectronicReceipt",file,70L,99L,30L,40L);}
    void inTransaction(){tx.executeWithoutResult(s->bind());}
    @AfterEach void cleanup(){TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    @Test void bindsOnlyTheNewSignatureAndExactReplayIsReadOnly(){inTransaction();assertEquals(Set.of(70L),saved.keySet());var row=saved.get(70L);assertEquals(99L,row.getBoundBy());assertTrue(row.getBindingEventKey().length()<=128);inTransaction();verify(bindings,times(1)).insert(any(DccControlledFileSignatureBindingDO.class));verify(signatures,never()).selectListByControlledFileId(anyLong());verify(signatures,never()).updateById(any(DccControlledFileSignatureDO.class));verify(audit,times(1)).append(any());}
    @ParameterizedTest @ValueSource(strings={"login","tenant","type","ignore","inactive","file","version","actor","meaning","task","scope","recipient","unsent"})
    void wrongIdentityCannotReadBytesOrWriteBinding(String invalid){switch(invalid){case "login"->login(88L,1L,2);case "tenant"->login(99L,2L,2);case "type"->login(99L,1L,1);case "ignore"->TenantContextHolder.setIgnore(true);case "inactive"->file.setActivatedTime(null);case "file"->signature.setControlledFileId(21L);case "version"->signature.setVersionNo("A/2");case "actor"->signature.setActorId(88L);case "meaning"->signature.setMeaningCode("MATRIX_APPROVAL_APPROVE");case "task"->signature.setTaskId("DISTRIBUTION:30:41");case "scope"->scope.setControlledFileId(21L);case "recipient"->recipient.setUserId(88L);case "unsent"->job.setStatus("PENDING");}assertThrows(RuntimeException.class,this::inTransaction);verifyNoInteractions(storage);verify(bindings,never()).insert(any(DccControlledFileSignatureBindingDO.class));verifyNoInteractions(audit);}
    @Test void noOuterTransactionIsRejectedBeforeReadingPorts(){assertThrows(RuntimeException.class,this::bind);verifyNoInteractions(storage,bindings,files,signatures,audit);}
    @Test void changedPublishedBytesCannotRewriteAnExistingReceipt()throws Exception{inTransaction();when(storage.getFileContent(1L,"task/published.pdf")).thenReturn(new byte[]{9});assertThrows(RuntimeException.class,this::inTransaction);verify(bindings,times(1)).insert(any(DccControlledFileSignatureBindingDO.class));verify(bindings,never()).updateById(any(DccControlledFileSignatureBindingDO.class));}
    @ParameterizedTest @ValueSource(strings={"actor","event","hash"})
    void anotherActorOrEventOnExistingBindingIsRejected(String bad){inTransaction();var old=saved.get(70L);var changed=service.createBindingEvent(signature,file,500L,"task/published.pdf",bytes,"actor".equals(bad)?88L:99L,"event".equals(bad)?"other-server-event":old.getBindingEventKey());if("hash".equals(bad))changed.setBindingHash("0".repeat(64));saved.put(70L,changed);assertThrows(RuntimeException.class,this::inTransaction);verify(bindings,times(1)).insert(any(DccControlledFileSignatureBindingDO.class));}
    @Test void missingPhysicalBytesFailWithoutBinding()throws Exception{when(storage.getFileContent(1L,"task/published.pdf")).thenThrow(new IllegalStateException("actual external storage failure"));assertThrows(RuntimeException.class,this::inTransaction);assertTrue(saved.isEmpty());verifyNoInteractions(audit);}
    @Test void insertMustReportOnePersistedBinding(){when(bindings.insert(any(DccControlledFileSignatureBindingDO.class))).thenReturn(0);assertThrows(RuntimeException.class,this::inTransaction);verifyNoInteractions(audit);}
    @Test void humanAuditFailurePropagatesToTheOuterTransaction(){doThrow(new IllegalStateException("formal GxP unavailable")).when(audit).append(any());assertEquals("formal GxP unavailable",assertThrows(IllegalStateException.class,this::inTransaction).getMessage());}
}
