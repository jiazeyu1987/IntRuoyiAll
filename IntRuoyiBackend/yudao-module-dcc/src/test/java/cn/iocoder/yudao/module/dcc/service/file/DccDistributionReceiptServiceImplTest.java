package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionRecipientAckReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionRecipientSignReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileDistributionStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMessageJobStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccDistributionMediumEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccDistributionReceiptServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileDistributionMapper distributionMapper;
    @Mock
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock
    private DccControlledFileMessageJobMapper messageJobMapper;
    @Mock
    private DccControlledFileMessageDeliveryService messageDeliveryService;
    @Mock
    private DccSignatureVerificationService signatureVerificationService;

    @InjectMocks
    private DccDistributionReceiptServiceImpl receiptService;

    @Test
    void acknowledgeElectronicDistribution_success_recordsRecipientAndSignature() {
        DccControlledFileDistributionRecipientAckReqVO reqVO = new DccControlledFileDistributionRecipientAckReqVO();
        reqVO.setPassword("Pass1234");
        reqVO.setComment("已收到电子受控文件");
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .title("CODEX DCC Spec")
                .fileName("codex-spec.pdf")
                .versionNo("A")
                .effectiveDate(LocalDate.of(2026, 5, 27))
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(distributionMapper.selectById(301L)).thenReturn(DccControlledFileDistributionDO.builder()
                .id(301L)
                .controlledFileId(900L)
                .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                .status(DccControlledFileDistributionStatusEnum.SENT.getCode())
                .build());
        when(distributionRecipientMapper.selectById(501L)).thenReturn(DccControlledFileDistributionRecipientDO.builder()
                .id(501L)
                .distributionId(301L)
                .userId(99L)
                .build());
        when(distributionRecipientMapper.selectListByDistributionId(301L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder()
                        .id(501L)
                        .distributionId(301L)
                        .userId(99L)
                        .build()));

        receiptService.acknowledgeElectronicDistribution(99L, 900L, 301L, 501L, reqVO);

        verify(signatureVerificationService).verifyPasswordAndCreateSignature(99L, 900L,
                "DISTRIBUTION:301:501", "DISTRIBUTION", "DISTRIBUTION_ACK", "Pass1234", "已收到电子受控文件");
        ArgumentCaptor<DccControlledFileDistributionRecipientDO> recipientCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionRecipientDO.class);
        verify(distributionRecipientMapper).updateById(recipientCaptor.capture());
        assertEquals(501L, recipientCaptor.getValue().getId());
        assertEquals("已收到电子受控文件", recipientCaptor.getValue().getAckComment());
        assertNotNull(recipientCaptor.getValue().getAcknowledgedAt());
        ArgumentCaptor<DccControlledFileDistributionDO> distributionCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionDO.class);
        verify(distributionMapper).updateById(distributionCaptor.capture());
        assertEquals(301L, distributionCaptor.getValue().getId());
        assertEquals(DccControlledFileDistributionStatusEnum.ACKNOWLEDGED.getCode(),
                distributionCaptor.getValue().getStatus());
        assertEquals(99L, distributionCaptor.getValue().getAcknowledgedBy());
        assertNotNull(distributionCaptor.getValue().getAcknowledgedAt());
    }

    @Test
    void acknowledgeElectronicDistribution_nonRecipient_throwsWithoutSignature() {
        DccControlledFileDistributionRecipientAckReqVO reqVO = new DccControlledFileDistributionRecipientAckReqVO();
        reqVO.setPassword("Pass1234");
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder()
                .id(901L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(distributionMapper.selectById(302L)).thenReturn(DccControlledFileDistributionDO.builder()
                .id(302L)
                .controlledFileId(901L)
                .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                .status(DccControlledFileDistributionStatusEnum.SENT.getCode())
                .build());
        when(distributionRecipientMapper.selectById(502L)).thenReturn(DccControlledFileDistributionRecipientDO.builder()
                .id(502L)
                .distributionId(302L)
                .userId(100L)
                .build());

        assertServiceException(() ->
                        receiptService.acknowledgeElectronicDistribution(99L, 901L, 302L, 502L, reqVO),
                CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void acknowledgeElectronicDistribution_paperDistribution_throws() {
        DccControlledFileDistributionRecipientAckReqVO reqVO = new DccControlledFileDistributionRecipientAckReqVO();
        reqVO.setPassword("Pass1234");
        when(controlledFileMapper.selectById(902L)).thenReturn(DccControlledFileDO.builder()
                .id(902L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(distributionMapper.selectById(303L)).thenReturn(DccControlledFileDistributionDO.builder()
                .id(303L)
                .controlledFileId(902L)
                .distributionMedium(DccDistributionMediumEnum.PAPER.getCode())
                .status(DccControlledFileDistributionStatusEnum.PENDING.getCode())
                .build());

        assertServiceException(() ->
                        receiptService.acknowledgeElectronicDistribution(99L, 902L, 303L, 503L, reqVO),
                CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createDistributionRecipientSign_success_addsUniqueRecipientsAndReopensAcknowledgedDistribution() {
        DccControlledFileDistributionRecipientSignReqVO reqVO = new DccControlledFileDistributionRecipientSignReqVO();
        reqVO.setUserIds(List.of(120L, 121L, 120L));
        reqVO.setPassword("Pass1234");
        reqVO.setComment("请同步接收");
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(distributionMapper.selectById(301L)).thenReturn(DccControlledFileDistributionDO.builder()
                .id(301L)
                .controlledFileId(900L)
                .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                .status(DccControlledFileDistributionStatusEnum.ACKNOWLEDGED.getCode())
                .build());
        when(distributionRecipientMapper.selectById(501L)).thenReturn(DccControlledFileDistributionRecipientDO.builder()
                .id(501L)
                .distributionId(301L)
                .userId(99L)
                .build());
        when(distributionRecipientMapper.selectListByDistributionId(301L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder()
                        .id(501L)
                        .distributionId(301L)
                        .userId(99L)
                        .build()));
        AtomicLong messageId = new AtomicLong(910L);
        doAnswer(invocation -> {
            DccControlledFileMessageJobDO messageJob = invocation.getArgument(0);
            messageJob.setId(messageId.getAndIncrement());
            return 1;
        }).when(messageJobMapper).insert(any(DccControlledFileMessageJobDO.class));

        receiptService.createDistributionRecipientSign(99L, 900L, 301L, 501L, reqVO);

        verify(signatureVerificationService).verifyPasswordAndCreateSignature(99L, 900L,
                "DISTRIBUTION_SIGN:301:501", "DISTRIBUTION", "DISTRIBUTION_SIGN", "Pass1234", "请同步接收");
        ArgumentCaptor<DccControlledFileMessageJobDO> messageJobCaptor =
                ArgumentCaptor.forClass(DccControlledFileMessageJobDO.class);
        verify(messageJobMapper, times(2)).insert(messageJobCaptor.capture());
        assertEquals(List.of(120L, 121L), messageJobCaptor.getAllValues().stream()
                .map(DccControlledFileMessageJobDO::getRecipientUserId)
                .toList());
        assertEquals(List.of(DccControlledFileMessageJobStatusEnum.PENDING.getCode(),
                        DccControlledFileMessageJobStatusEnum.PENDING.getCode()),
                messageJobCaptor.getAllValues().stream().map(DccControlledFileMessageJobDO::getStatus).toList());
        verify(messageDeliveryService, times(2)).dispatchMessageJob(any(DccControlledFileMessageJobDO.class), anyMap());
        ArgumentCaptor<DccControlledFileDistributionRecipientDO> recipientCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionRecipientDO.class);
        verify(distributionRecipientMapper, times(2)).insert(recipientCaptor.capture());
        assertEquals(List.of(120L, 121L), recipientCaptor.getAllValues().stream()
                .map(DccControlledFileDistributionRecipientDO::getUserId)
                .toList());
        assertEquals(List.of(910L, 911L), recipientCaptor.getAllValues().stream()
                .map(DccControlledFileDistributionRecipientDO::getMessageJobId)
                .toList());
        ArgumentCaptor<DccControlledFileDistributionDO> distributionCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionDO.class);
        verify(distributionMapper).updateById(distributionCaptor.capture());
        assertEquals(301L, distributionCaptor.getValue().getId());
        assertEquals(DccControlledFileDistributionStatusEnum.SENT.getCode(), distributionCaptor.getValue().getStatus());
    }

    @Test
    void createDistributionRecipientSign_duplicateRecipient_throwsWithoutSignature() {
        DccControlledFileDistributionRecipientSignReqVO reqVO = new DccControlledFileDistributionRecipientSignReqVO();
        reqVO.setUserIds(List.of(120L));
        reqVO.setPassword("Pass1234");
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(distributionMapper.selectById(301L)).thenReturn(DccControlledFileDistributionDO.builder()
                .id(301L)
                .controlledFileId(900L)
                .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                .status(DccControlledFileDistributionStatusEnum.SENT.getCode())
                .build());
        when(distributionRecipientMapper.selectById(501L)).thenReturn(DccControlledFileDistributionRecipientDO.builder()
                .id(501L)
                .distributionId(301L)
                .userId(99L)
                .build());
        when(distributionRecipientMapper.selectListByDistributionId(301L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder()
                        .id(501L)
                        .distributionId(301L)
                        .userId(99L)
                        .build(),
                DccControlledFileDistributionRecipientDO.builder()
                        .id(502L)
                        .distributionId(301L)
                        .userId(120L)
                        .build()));

        assertServiceException(() ->
                        receiptService.createDistributionRecipientSign(99L, 900L, 301L, 501L, reqVO),
                CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        verify(signatureVerificationService, never()).verifyPasswordAndCreateSignature(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
        verify(distributionRecipientMapper, never()).insert(
                org.mockito.ArgumentMatchers.any(DccControlledFileDistributionRecipientDO.class));
    }
}
