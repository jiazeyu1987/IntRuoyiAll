package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccPaperDistributionRecordRespVO;
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
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DISTRIBUTION_ACK_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccPaperDistributionAckServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileDistributionMapper distributionMapper;
    @Mock
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Mock
    private DccControlledFileMessageJobMapper messageJobMapper;
    @Mock
    private DccControlledFileMessageDeliveryService messageDeliveryService;
    @Mock
    private AdminUserApi adminUserApi;

    @InjectMocks
    private DccPaperDistributionAckServiceImpl ackService;

    @Test
    void acknowledgePaperDistribution_success_updatesStatusAndPaperRecipients() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .categoryId(10L)
                .title("纸质分发文件")
                .fileName("paper.pdf")
                .versionNo("A")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(distributionMapper.selectById(301L)).thenReturn(DccControlledFileDistributionDO.builder()
                .id(301L)
                .controlledFileId(900L)
                .departmentId(100L)
                .distributionMedium(DccDistributionMediumEnum.PAPER.getCode())
                .status(DccControlledFileDistributionStatusEnum.PENDING.getCode())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DISTRIBUTE))
                .thenReturn(true);
        AtomicLong messageId = new AtomicLong(8101L);
        doAnswer(invocation -> {
            DccControlledFileMessageJobDO messageJob = invocation.getArgument(0);
            messageJob.setId(messageId.getAndIncrement());
            return 1;
        }).when(messageJobMapper).insert(any(DccControlledFileMessageJobDO.class));

        ackService.acknowledgePaperDistribution(99L, 900L, 301L, List.of(120L, 121L, 120L));

        verify(adminUserApi).validateUserList(List.of(120L, 121L));
        ArgumentCaptor<DccControlledFileDistributionDO> captor =
                ArgumentCaptor.forClass(DccControlledFileDistributionDO.class);
        verify(distributionMapper).updateById(captor.capture());
        assertEquals(301L, captor.getValue().getId());
        assertEquals(DccControlledFileDistributionStatusEnum.ACKNOWLEDGED.getCode(), captor.getValue().getStatus());
        assertEquals(Long.valueOf(99L), captor.getValue().getAcknowledgedBy());
        assertNotNull(captor.getValue().getAcknowledgedAt());
        ArgumentCaptor<DccControlledFileDistributionRecipientDO> recipientCaptor =
                ArgumentCaptor.forClass(DccControlledFileDistributionRecipientDO.class);
        verify(distributionRecipientMapper, times(2)).insert(recipientCaptor.capture());
        assertEquals(List.of(120L, 121L), recipientCaptor.getAllValues().stream()
                .map(DccControlledFileDistributionRecipientDO::getUserId)
                .toList());
        assertEquals(List.of(301L, 301L), recipientCaptor.getAllValues().stream()
                .map(DccControlledFileDistributionRecipientDO::getDistributionId)
                .toList());
        assertEquals(List.of(8101L, 8102L), recipientCaptor.getAllValues().stream()
                .map(DccControlledFileDistributionRecipientDO::getMessageJobId)
                .toList());
        assertNull(recipientCaptor.getAllValues().get(0).getReadAt());
        assertNull(recipientCaptor.getAllValues().get(0).getAcknowledgedAt());
        ArgumentCaptor<DccControlledFileMessageJobDO> messageJobCaptor =
                ArgumentCaptor.forClass(DccControlledFileMessageJobDO.class);
        verify(messageJobMapper, times(2)).insert(messageJobCaptor.capture());
        assertEquals(List.of(120L, 121L), messageJobCaptor.getAllValues().stream()
                .map(DccControlledFileMessageJobDO::getRecipientUserId)
                .toList());
        assertEquals(List.of("DISTRIBUTION", "DISTRIBUTION"), messageJobCaptor.getAllValues().stream()
                .map(DccControlledFileMessageJobDO::getBusinessType)
                .toList());
        assertEquals(List.of(301L, 301L), messageJobCaptor.getAllValues().stream()
                .map(DccControlledFileMessageJobDO::getBusinessId)
                .toList());
        assertEquals(List.of("dcc_distribution", "dcc_distribution"), messageJobCaptor.getAllValues().stream()
                .map(DccControlledFileMessageJobDO::getTemplateCode)
                .toList());
        assertEquals(List.of(DccControlledFileMessageJobStatusEnum.PENDING.getCode(),
                        DccControlledFileMessageJobStatusEnum.PENDING.getCode()),
                messageJobCaptor.getAllValues().stream().map(DccControlledFileMessageJobDO::getStatus).toList());
        verify(messageDeliveryService, times(2)).dispatchMessageJob(any(DccControlledFileMessageJobDO.class), anyMap());
    }

    @Test
    void acknowledgePaperDistribution_emptyRecipients_throwsWithoutUpdating() {
        when(controlledFileMapper.selectById(906L)).thenReturn(DccControlledFileDO.builder()
                .id(906L)
                .categoryId(10L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());

        assertServiceException(() -> ackService.acknowledgePaperDistribution(99L, 906L, 307L, List.<Long>of()),
                CONTROLLED_FILE_DISTRIBUTION_ACK_NOT_ALLOWED);
        verify(distributionMapper, never()).updateById(any(DccControlledFileDistributionDO.class));
        verify(distributionRecipientMapper, never()).insert(any(DccControlledFileDistributionRecipientDO.class));
    }

    @Test
    void acknowledgePaperDistribution_invalidRecipient_propagatesValidationFailure() {
        when(controlledFileMapper.selectById(907L)).thenReturn(DccControlledFileDO.builder()
                .id(907L)
                .categoryId(10L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        doThrow(new IllegalArgumentException("用户不存在")).when(adminUserApi).validateUserList(List.of(120L));

        assertThrows(IllegalArgumentException.class,
                () -> ackService.acknowledgePaperDistribution(99L, 907L, 308L, List.of(120L)));
        verify(distributionMapper, never()).updateById(any(DccControlledFileDistributionDO.class));
        verify(distributionRecipientMapper, never()).insert(any(DccControlledFileDistributionRecipientDO.class));
    }

    @Test
    void getPaperDistributionRecords_success_returnsDedicatedScreenshotFields() {
        LocalDateTime issuedAt = LocalDateTime.of(2026, 5, 26, 9, 30);
        LocalDateTime recoveredAt = LocalDateTime.of(2026, 5, 26, 18, 10);
        when(controlledFileMapper.selectById(910L)).thenReturn(DccControlledFileDO.builder()
                .id(910L)
                .fileNumber("DCC-QP-001")
                .title("外来文件控制程序")
                .versionNo("A")
                .build());
        when(distributionMapper.selectListByControlledFileId(910L)).thenReturn(List.of(
                DccControlledFileDistributionDO.builder()
                        .id(401L)
                        .controlledFileId(910L)
                        .distributionMedium(DccDistributionMediumEnum.PAPER.getCode())
                        .status(DccControlledFileDistributionStatusEnum.RECOVERED.getCode())
                        .acknowledgedBy(99L)
                        .acknowledgedAt(issuedAt)
                        .recoveredBy(100L)
                        .recoveredAt(recoveredAt)
                        .build(),
                DccControlledFileDistributionDO.builder()
                        .id(402L)
                        .controlledFileId(910L)
                        .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                        .status(DccControlledFileDistributionStatusEnum.SENT.getCode())
                        .build()));
        when(distributionRecipientMapper.selectListByDistributionId(401L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder()
                        .id(501L)
                        .distributionId(401L)
                        .userId(120L)
                        .build(),
                DccControlledFileDistributionRecipientDO.builder()
                        .id(502L)
                        .distributionId(401L)
                        .userId(121L)
                        .build()));
        when(adminUserApi.getUserMap(any())).thenReturn(Map.of(
                99L, user(99L, "文控"),
                100L, user(100L, "回收人"),
                120L, user(120L, "接收人A"),
                121L, user(121L, "接收人B")));

        List<DccPaperDistributionRecordRespVO> records = ackService.getPaperDistributionRecords(910L);

        assertEquals(1, records.size());
        DccPaperDistributionRecordRespVO record = records.get(0);
        assertEquals(401L, record.getDistributionId());
        assertEquals(910L, record.getControlledFileId());
        assertEquals("DCC-QP-001", record.getFileNumber());
        assertEquals("外来文件控制程序", record.getFileName());
        assertEquals("A", record.getVersionNo());
        assertEquals(99L, record.getIssuerUserId());
        assertEquals("文控", record.getIssuerName());
        assertEquals(List.of(120L, 121L), record.getRecipientUserIds());
        assertEquals(List.of("接收人A", "接收人B"), record.getRecipientNames());
        assertEquals(issuedAt, record.getIssuedAt());
        assertEquals(100L, record.getRecovererUserId());
        assertEquals("回收人", record.getRecovererName());
        assertEquals(recoveredAt, record.getRecoveredAt());
        assertEquals(DccControlledFileDistributionStatusEnum.RECOVERED.getCode(), record.getStatus());
    }

    @Test
    void acknowledgePaperDistribution_nonPaperDistribution_throws() {
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder()
                .id(901L)
                .categoryId(10L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(distributionMapper.selectById(302L)).thenReturn(DccControlledFileDistributionDO.builder()
                .id(302L)
                .controlledFileId(901L)
                .departmentId(100L)
                .distributionMedium(DccDistributionMediumEnum.PUBLIC_FOLDER.getCode())
                .status(DccControlledFileDistributionStatusEnum.PENDING.getCode())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DISTRIBUTE))
                .thenReturn(true);

        assertServiceException(() -> ackService.acknowledgePaperDistribution(99L, 901L, 302L, List.of(120L)),
                CONTROLLED_FILE_DISTRIBUTION_ACK_NOT_ALLOWED);
        verify(distributionMapper, never()).updateById(org.mockito.ArgumentMatchers.any(DccControlledFileDistributionDO.class));
    }

    @Test
    void acknowledgePaperDistribution_withoutDistributePermission_throws() {
        when(controlledFileMapper.selectById(902L)).thenReturn(DccControlledFileDO.builder()
                .id(902L)
                .categoryId(10L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DISTRIBUTE))
                .thenReturn(false);

        assertServiceException(() -> ackService.acknowledgePaperDistribution(99L, 902L, 303L, List.of(120L)),
                CONTROLLED_FILE_DISTRIBUTION_ACK_NOT_ALLOWED);
    }

    @Test
    void acknowledgePaperDistribution_fileMissing_throws() {
        when(controlledFileMapper.selectById(903L)).thenReturn(null);

        assertServiceException(() -> ackService.acknowledgePaperDistribution(99L, 903L, 304L, List.of(120L)),
                CONTROLLED_FILE_NOT_EXISTS);
    }

    @Test
    void recoverPaperDistribution_success_updatesRecoveredFields() {
        when(controlledFileMapper.selectById(904L)).thenReturn(DccControlledFileDO.builder()
                .id(904L)
                .categoryId(10L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(distributionMapper.selectById(305L)).thenReturn(DccControlledFileDistributionDO.builder()
                .id(305L)
                .controlledFileId(904L)
                .departmentId(100L)
                .distributionMedium(DccDistributionMediumEnum.PAPER.getCode())
                .status(DccControlledFileDistributionStatusEnum.ACKNOWLEDGED.getCode())
                .acknowledgedBy(98L)
                .acknowledgedAt(LocalDateTime.now().minusDays(1))
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DISTRIBUTE))
                .thenReturn(true);

        ackService.recoverPaperDistribution(99L, 904L, 305L);

        ArgumentCaptor<DccControlledFileDistributionDO> captor =
                ArgumentCaptor.forClass(DccControlledFileDistributionDO.class);
        verify(distributionMapper).updateById(captor.capture());
        assertEquals(305L, captor.getValue().getId());
        assertEquals(DccControlledFileDistributionStatusEnum.RECOVERED.getCode(), captor.getValue().getStatus());
        assertEquals(Long.valueOf(99L), captor.getValue().getRecoveredBy());
        assertNotNull(captor.getValue().getRecoveredAt());
    }

    @Test
    void recoverPaperDistribution_beforeIssue_throws() {
        when(controlledFileMapper.selectById(905L)).thenReturn(DccControlledFileDO.builder()
                .id(905L)
                .categoryId(10L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(distributionMapper.selectById(306L)).thenReturn(DccControlledFileDistributionDO.builder()
                .id(306L)
                .controlledFileId(905L)
                .departmentId(100L)
                .distributionMedium(DccDistributionMediumEnum.PAPER.getCode())
                .status(DccControlledFileDistributionStatusEnum.PENDING.getCode())
                .build());
        when(permissionSupport.hasCategoryPermission(10L, 99L, DccFileCategoryPermissionActionEnum.DISTRIBUTE))
                .thenReturn(true);

        assertServiceException(() -> ackService.recoverPaperDistribution(99L, 905L, 306L),
                CONTROLLED_FILE_DISTRIBUTION_ACK_NOT_ALLOWED);
    }

    private static AdminUserRespDTO user(Long id, String nickname) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setNickname(nickname);
        return user;
    }
}
