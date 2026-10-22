package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DccDistributionReceiptServiceImplTest extends BaseMockitoUnitTest {
    @Mock DccControlledFileMapper controlledFileMapper;
    @Mock DccControlledFileDistributionMapper distributionMapper;
    @Mock DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock DccSignatureVerificationService signatureVerificationService;
    @Mock DccControlledFileSignatureBindingService signatureBindingService;
    @InjectMocks DccDistributionReceiptServiceImpl service;
    DccControlledFileDistributionDO distribution;
    DccControlledFileDistributionRecipientDO recipient;
    DccControlledFileDistributionRecipientAckReqVO request;
    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);
        var now=LocalDateTime.of(2026,10,11,9,0);
        var file=DccControlledFileDO.builder().id(42L).versionNo("B/1").status("ACTIVE")
                .controlledTime(now).activatedTime(now).distributedTime(now).build();file.setTenantId(1L);
        distribution=DccControlledFileDistributionDO.builder().id(51L).controlledFileId(42L)
                .distributionMedium("PUBLIC_FOLDER").status("SENT").build();
        recipient=DccControlledFileDistributionRecipientDO.builder().id(71L).distributionId(51L).userId(99L).build();
        request=new DccControlledFileDistributionRecipientAckReqVO();request.setPassword("test-value");request.setComment("收到");
        lenient().when(controlledFileMapper.selectByIdAndTenantForUpdate(1L,42L)).thenReturn(file);
        lenient().when(distributionMapper.selectByIdAndTenantForUpdate(1L,51L)).thenReturn(distribution);
        lenient().when(distributionRecipientMapper.selectByIdAndTenantForUpdate(1L,71L)).thenReturn(recipient);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test void nonRecipientCannotSign() {
        recipient.setUserId(100L);
        assertThrows(RuntimeException.class,()->service.acknowledgeElectronicDistribution(99L,42L,51L,71L,request));
        verifyNoInteractions(signatureVerificationService);
    }
    @Test void paperReceiptCannotBecomeElectronicReceipt() {
        distribution.setDistributionMedium("PAPER");
        assertThrows(RuntimeException.class,()->service.acknowledgeElectronicDistribution(99L,42L,51L,71L,request));
        verifyNoInteractions(signatureVerificationService);
    }
    @Test void acknowledgedReplayDoesNotWriteNewSignatureOrChangeOriginalTimes() {
        var original=LocalDateTime.of(2026,10,11,9,0);recipient.setAcknowledgedAt(original);
        service.acknowledgeElectronicDistribution(99L,42L,51L,71L,request);
        verifyNoInteractions(signatureVerificationService);
        verify(distributionRecipientMapper,never()).updateById(any(DccControlledFileDistributionRecipientDO.class));
        assertEquals(original,recipient.getAcknowledgedAt());
    }
    @Test void recipientCannotCreateAdditionalAudience() {
        var add=new DccControlledFileDistributionRecipientSignReqVO();
        add.setUserIds(java.util.List.of(120L));add.setPassword("test-value");add.setComment("转发");
        assertThrows(RuntimeException.class,()->service.createDistributionRecipientSign(99L,42L,51L,71L,add));
        verifyNoInteractions(signatureVerificationService);
        verify(distributionRecipientMapper,never()).insert(any(DccControlledFileDistributionRecipientDO.class));
    }
}
