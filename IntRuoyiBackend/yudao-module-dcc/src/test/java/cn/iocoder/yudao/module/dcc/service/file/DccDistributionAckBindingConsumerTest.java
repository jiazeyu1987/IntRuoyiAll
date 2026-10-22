package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionRecipientAckReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Current typed ACK consumer; historical RED is retained separately, no production reflection/fallback. */
class DccDistributionAckBindingConsumerTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileMapper files;
    @Mock DccControlledFileDistributionMapper distributions;
    @Mock DccControlledFileDistributionRecipientMapper recipients;
    @Mock DccSignatureVerificationService signatureVerificationService;
    @Mock DccControlledFileSignatureBindingService signatureBindingService;
    @InjectMocks DccDistributionReceiptServiceImpl service;
    DccControlledFileDO file;DccControlledFileDistributionRecipientDO recipient;
    DccControlledFileDistributionRecipientAckReqVO request;
    DccUnifiedSignatureResult signature;
    @BeforeEach void fixture(){
        TenantContextHolder.setTenantId(1L);var now=LocalDateTime.of(2026,10,13,10,0);
        file=DccControlledFileDO.builder().id(42L).versionNo("B/1").status("ACTIVE").controlledTime(now).activatedTime(now).distributedTime(now)
                .publishedFileId(100L).stampedFileId(100L).build();file.setTenantId(1L);
        recipient=DccControlledFileDistributionRecipientDO.builder().id(71L).distributionId(51L).userId(99L).messageJobId(81L).build();
        signature=DccUnifiedSignatureResult.builder().signatureId(101L).controlledFileId(42L).versionNo("B/1").signedAt(now).build();
        request=new DccControlledFileDistributionRecipientAckReqVO();request.setPassword("task-fixture-password");request.setComment(null);
        lenient().when(files.selectByIdAndTenantForUpdate(1L,42L)).thenReturn(file);
        lenient().when(distributions.selectByIdAndTenantForUpdate(1L,51L)).thenReturn(DccControlledFileDistributionDO.builder().id(51L).controlledFileId(42L).distributionMedium("PUBLIC_FOLDER").status("SENT").build());
        lenient().when(recipients.selectByIdAndTenantForUpdate(1L,71L)).thenReturn(recipient);
        lenient().when(signatureVerificationService.verifyPasswordAndCreateSignature(99L,42L,"DISTRIBUTION:51:71","DISTRIBUTION","DISTRIBUTION_ACK","task-fixture-password",null)).thenAnswer(ignored->signature);
        lenient().when(recipients.selectListByDistributionIdForUpdate(1L,51L)).thenReturn(List.of(recipient));
        lenient().when(recipients.updateById(any(DccControlledFileDistributionRecipientDO.class))).thenReturn(1);
        lenient().when(distributions.updateById(any(DccControlledFileDistributionDO.class))).thenReturn(1);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    void acknowledge(){service.acknowledgeElectronicDistribution(99L,42L,51L,71L,request);}
    @Test void newAckMustBindExactlyItsSignatureBeforeRecipientPersistence(){
        var updated=new java.util.concurrent.atomic.AtomicBoolean();
        when(recipients.updateById(any(DccControlledFileDistributionRecipientDO.class))).thenAnswer(call->{
            verify(signatureBindingService).bindElectronicReceipt(file,101L,99L,51L,71L);updated.set(true);return 1;
        });
        acknowledge();assertTrue(updated.get());
        verify(signatureBindingService,never()).bindPublishedCopy(any(),anyLong(),anyLong(),anyString());
    }
    @Test void bindingFailureMustPropagateBeforeAnyRecipientOrSummaryWrite(){
        doThrow(new IllegalStateException("unreadable published copy binding")).when(signatureBindingService).bindElectronicReceipt(file,101L,99L,51L,71L);
        assertThrows(IllegalStateException.class,this::acknowledge);
        verify(recipients,never()).updateById(any(DccControlledFileDistributionRecipientDO.class));
        verify(distributions,never()).updateById(any(DccControlledFileDistributionDO.class));
    }
    @Test void wrongVersionSignatureCannotBindOrPersistReceipt(){signature=DccUnifiedSignatureResult.builder().signatureId(101L).controlledFileId(42L).versionNo("C/1").signedAt(signature.getSignedAt()).build();
        assertThrows(IllegalStateException.class,this::acknowledge);verifyNoInteractions(signatureBindingService);verify(recipients,never()).updateById(any(DccControlledFileDistributionRecipientDO.class));}
    @Test void acknowledgedReplayHasNoNewSignatureBindingOrReceiptWrite(){recipient.setAcknowledgedAt(signature.getSignedAt());acknowledge();verifyNoInteractions(signatureVerificationService,signatureBindingService);verify(recipients,never()).updateById(any(DccControlledFileDistributionRecipientDO.class));}
    @Test void rejectedSignatureEvidenceCannotCallCopyBinding(){signature=null;assertThrows(IllegalStateException.class,this::acknowledge);verifyNoInteractions(signatureBindingService);verify(recipients,never()).updateById(any(DccControlledFileDistributionRecipientDO.class));}
}
