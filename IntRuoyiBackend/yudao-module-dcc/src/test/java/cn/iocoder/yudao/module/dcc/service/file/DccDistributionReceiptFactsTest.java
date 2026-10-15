package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccDistributionReceiptFactsTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileMapper controlledFileMapper;
    @Mock DccControlledFileDistributionMapper distributionMapper;
    @Mock DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock DccControlledFileMessageJobMapper messageJobMapper;
    @Mock DccControlledFileMessageDeliveryService messageDeliveryService;
    @Mock DccSignatureVerificationService signatureVerificationService;
    @Mock DccControlledFileSignatureBindingService signatureBindingService;
    @InjectMocks DccDistributionReceiptServiceImpl service;
    final LocalDateTime signedAt = LocalDateTime.of(2026, 10, 11, 12, 30, 15);
    DccControlledFileDO file;
    DccControlledFileDistributionDO distribution;
    DccControlledFileDistributionRecipientDO recipient;
    DccControlledFileDistributionRecipientAckReqVO request;

    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);
        file = DccControlledFileDO.builder().id(42L).versionNo("B/1").status("ACTIVE")
                .controlledTime(signedAt.minusDays(2)).activatedTime(signedAt.minusDays(1))
                .distributedTime(signedAt.minusHours(1)).build();
        file.setTenantId(1L);
        distribution = DccControlledFileDistributionDO.builder().id(51L).controlledFileId(42L)
                .departmentId(61L).distributionMedium("PUBLIC_FOLDER").status("SENT").build();
        recipient = DccControlledFileDistributionRecipientDO.builder().id(71L).distributionId(51L)
                .userId(99L).messageJobId(81L).build();
        request = new DccControlledFileDistributionRecipientAckReqVO();
        request.setPassword("test-password"); request.setComment("收到本版本");
        lenient().when(controlledFileMapper.selectById(42L)).thenReturn(file);
        lenient().when(controlledFileMapper.selectByIdAndTenantForUpdate(1L,42L)).thenReturn(file);
        lenient().when(distributionMapper.selectById(51L)).thenReturn(distribution);
        lenient().when(distributionMapper.selectByIdAndTenantForUpdate(1L,51L)).thenReturn(distribution);
        lenient().when(distributionRecipientMapper.selectById(71L)).thenReturn(recipient);
        lenient().when(distributionRecipientMapper.selectByIdAndTenantForUpdate(1L,71L)).thenReturn(recipient);
        lenient().when(distributionRecipientMapper.selectListByDistributionId(51L)).thenReturn(List.of(recipient));
        lenient().when(distributionRecipientMapper.selectListByDistributionIdForUpdate(1L,51L)).thenReturn(List.of(recipient));
        lenient().when(distributionRecipientMapper.updateById(any(DccControlledFileDistributionRecipientDO.class))).thenReturn(1);
        lenient().when(distributionMapper.updateById(any(DccControlledFileDistributionDO.class))).thenReturn(1);
        lenient().when(signatureVerificationService.verifyPasswordAndCreateSignature(anyLong(),anyLong(),anyString(),anyString(),anyString(),anyString(),any()))
                .thenReturn(DccUnifiedSignatureResult.builder().signatureId(101L).controlledFileId(42L)
                        .versionNo("B/1").signedAt(signedAt).build());
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    void acknowledge() { service.acknowledgeElectronicDistribution(99L,42L,51L,71L,request); }

    @Test void pendingEffectiveCannotSignOrWrite() {
        file.setStatus("CONTROLLED_PENDING_EFFECTIVE"); file.setActivatedTime(null);
        assertThrows(RuntimeException.class,this::acknowledge);
        verifyNoInteractions(signatureVerificationService);
        verify(distributionRecipientMapper,never()).updateById(any(DccControlledFileDistributionRecipientDO.class));
    }
    @Test void signatureTimeIsAuthoritativeAndDoesNotInventOpenedFact() {
        acknowledge();
        var capture=ArgumentCaptor.forClass(DccControlledFileDistributionRecipientDO.class);
        verify(distributionRecipientMapper).updateById(capture.capture());
        assertEquals(signedAt,capture.getValue().getAcknowledgedAt());
        assertNull(capture.getValue().getReadAt());
        var summary=ArgumentCaptor.forClass(DccControlledFileDistributionDO.class);
        verify(distributionMapper).updateById(summary.capture());
        assertEquals(signedAt,summary.getValue().getAcknowledgedAt());
    }
    @Test void missingSignatureEvidenceCannotPersistReceipt() {
        when(signatureVerificationService.verifyPasswordAndCreateSignature(anyLong(),anyLong(),anyString(),anyString(),anyString(),anyString(),any())).thenReturn(null);
        assertThrows(IllegalStateException.class,this::acknowledge);
        verify(distributionRecipientMapper,never()).updateById(any(DccControlledFileDistributionRecipientDO.class));
    }
    @Test void recipientWriteMissIsFailure() {
        when(distributionRecipientMapper.updateById(any(DccControlledFileDistributionRecipientDO.class))).thenReturn(0);
        assertThrows(IllegalStateException.class,this::acknowledge);
        verify(distributionMapper,never()).updateById(any(DccControlledFileDistributionDO.class));
    }
    @Test void recoveredDistributionCannotSign() {
        distribution.setStatus("RECOVERED");
        assertThrows(RuntimeException.class,this::acknowledge);
        verifyNoInteractions(signatureVerificationService);
    }
    @Test void receiptDoesNotPermitRecipientToExtendSavedAudience() {
        var extend=new DccControlledFileDistributionRecipientSignReqVO();
        extend.setUserIds(List.of(123L)); extend.setPassword("test-password"); extend.setComment("转给别人");
        assertThrows(RuntimeException.class,()->service.createDistributionRecipientSign(99L,42L,51L,71L,extend));
        verifyNoInteractions(signatureVerificationService,messageDeliveryService);
        verify(distributionRecipientMapper,never()).insert(any(DccControlledFileDistributionRecipientDO.class));
    }
    @Test void optionalEmptyCommentStillSignsWithoutFabricatedOpinion() {
        request.setComment(null);acknowledge();
        verify(signatureVerificationService).verifyPasswordAndCreateSignature(99L,42L,"DISTRIBUTION:51:71","DISTRIBUTION","DISTRIBUTION_ACK","test-password",null);
        var capture=ArgumentCaptor.forClass(DccControlledFileDistributionRecipientDO.class);
        verify(distributionRecipientMapper).updateById(capture.capture());
        assertNull(capture.getValue().getAckComment());assertEquals(signedAt,capture.getValue().getAcknowledgedAt());
    }
    @Test void emptyStringCommentStillSignsWithoutInventedOpinion() {
        request.setComment("");acknowledge();
        verify(signatureVerificationService).verifyPasswordAndCreateSignature(99L,42L,"DISTRIBUTION:51:71","DISTRIBUTION","DISTRIBUTION_ACK","test-password","");
    }
}
