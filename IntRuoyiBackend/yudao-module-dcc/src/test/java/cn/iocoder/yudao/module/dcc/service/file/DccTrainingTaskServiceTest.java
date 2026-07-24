package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingExecutionPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingExecutionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingTaskPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingTaskRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingViewSessionHeartbeatReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkOverlayRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledPreviewWatermarkRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAccessLogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingProgressDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingViewSessionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileAccessLogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingProgressMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingViewSessionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileDistributionStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccTrainingTaskServiceTest extends BaseMockitoUnitTest {

    private static final DccRequestAuditContext AUDIT_CONTEXT =
            new DccRequestAuditContext("10.8.0.61", "Training-JUnit/1.0", "REQ-TRAINING-20260528-0001");

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileTrainingProgressMapper trainingProgressMapper;
    @Mock
    private DccControlledFileTrainingViewSessionMapper trainingViewSessionMapper;
    @Mock
    private DccControlledFileTrainingMapper trainingMapper;
    @Mock
    private DccControlledFileTrainingAssignmentMapper trainingAssignmentMapper;
    @Mock
    private DccControlledFileDistributionMapper distributionMapper;
    @Mock
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Mock
    private FileMapper fileMapper;
    @Mock
    private FileService fileService;
    @Mock
    private DccControlledPreviewWatermarkService watermarkService;
    @Mock
    private DccControlledFileAccessLogMapper accessLogMapper;

    @InjectMocks
    private DccTrainingTaskServiceImpl trainingTaskService;

    @Test
    void readTrainingPreviewFile_marksProgressAndDistributionRead() throws Exception {
        when(trainingProgressMapper.selectById(1001L)).thenReturn(DccControlledFileTrainingProgressDO.builder()
                .id(1001L)
                .controlledFileId(900L)
                .userId(99L)
                .requiredViewSeconds(600)
                .accumulatedViewSeconds(0)
                .build());
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .categoryId(10L)
                .publishedFileId(501L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .title("SOP-001")
                .fileNumber("FI-001")
                .versionNo("1.0")
                .build());
        when(fileMapper.selectById(501L)).thenReturn(FileDO.builder()
                .id(501L)
                .configId(1L)
                .path("dcc/published/sample.pdf")
                .name("sample.pdf")
                .type("application/pdf")
                .build());
        when(fileService.getFileContent(1L, "dcc/published/sample.pdf")).thenReturn("pdf".getBytes());
        when(watermarkService.build(99L, "training", "sample.pdf"))
                .thenReturn(DccControlledPreviewWatermarkRespVO.builder()
                        .label("受控预览")
                        .text("受控预览")
                        .actorName("admin")
                        .actorAccount("admin")
                        .timestamp("2026-05-16 22:00:00")
                        .purpose("training")
                        .overlay(DccControlledPreviewWatermarkOverlayRespVO.builder()
                                .textColor("#6b7280")
                                .opacity(0.18D)
                                .rotationDeg(-24)
                                .gapX(260)
                                .gapY(180)
                                .fontSize(18)
                                .build())
                        .build());
        when(distributionMapper.selectListByControlledFileId(900L)).thenReturn(List.of(
                DccControlledFileDistributionDO.builder()
                        .id(301L)
                        .controlledFileId(900L)
                        .departmentId(500L)
                        .status(DccControlledFileDistributionStatusEnum.PENDING.getCode())
                        .build()));
        when(distributionRecipientMapper.selectListByDistributionId(301L)).thenReturn(List.of(
                DccControlledFileDistributionRecipientDO.builder()
                        .id(401L)
                        .distributionId(301L)
                        .userId(99L)
                        .build()));

        DccControlledFileBinary binary = trainingTaskService.readTrainingPreviewFile(99L, 1001L, AUDIT_CONTEXT);

        assertEquals("sample.pdf", binary.fileName());
        assertArrayEquals("pdf".getBytes(), binary.bytes());
        ArgumentCaptor<DccControlledFileTrainingProgressDO> progressCaptor =
                ArgumentCaptor.forClass(DccControlledFileTrainingProgressDO.class);
        verify(trainingProgressMapper).updateById(progressCaptor.capture());
        assertEquals(1001L, progressCaptor.getValue().getId());
        verify(distributionRecipientMapper).updateById(any(DccControlledFileDistributionRecipientDO.class));
        verify(distributionMapper).updateById(any(DccControlledFileDistributionDO.class));
        verify(accessLogMapper).insert(org.mockito.ArgumentMatchers.<DccControlledFileAccessLogDO>argThat(log ->
                "10.8.0.61".equals(log.getSourceIp())
                        && "Training-JUnit/1.0".equals(log.getUserAgent())
                        && "REQ-TRAINING-20260528-0001".equals(log.getRequestId())));
    }

    @Test
    void heartbeatViewSession_accumulatesSecondsAndUnlocksAcknowledgement() {
        LocalDateTime now = LocalDateTime.now();
        when(trainingProgressMapper.selectById(1002L)).thenReturn(DccControlledFileTrainingProgressDO.builder()
                .id(1002L)
                .controlledFileId(901L)
                .userId(99L)
                .requiredViewSeconds(600)
                .accumulatedViewSeconds(595)
                .build());
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder()
                .id(901L)
                .publishedFileId(502L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .fileName("sop-002.pdf")
                .title("SOP-002")
                .fileNumber("FI-002")
                .versionNo("1.0")
                .build());
        when(trainingViewSessionMapper.selectActiveByProgressIdAndClientSessionId(1002L, "session-1"))
                .thenReturn(DccControlledFileTrainingViewSessionDO.builder()
                        .id(7001L)
                        .trainingProgressId(1002L)
                        .userId(99L)
                        .clientSessionId("session-1")
                        .startedAt(now.minusSeconds(20))
                        .lastHeartbeatAt(now.minusSeconds(10))
                        .accumulatedSeconds(20)
                        .build());

        DccTrainingTaskRespVO respVO = trainingTaskService.heartbeatViewSession(99L, 1002L,
                new DccTrainingViewSessionHeartbeatReqVO().setClientSessionId("session-1"));

        ArgumentCaptor<DccControlledFileTrainingProgressDO> progressCaptor =
                ArgumentCaptor.forClass(DccControlledFileTrainingProgressDO.class);
        verify(trainingProgressMapper).updateById(progressCaptor.capture());
        assertTrue(progressCaptor.getValue().getAccumulatedViewSeconds() >= 600);
        assertTrue(Boolean.TRUE.equals(respVO.getEligibleToAcknowledge()));
        assertEquals("sop-002.pdf", respVO.getFileName());
        assertEquals("READY_TO_ACKNOWLEDGE", respVO.getStatus());
    }

    @Test
    void getTrainingExecutionPage_aggregatesDepartmentsByFileAndUser() {
        when(trainingProgressMapper.selectList()).thenReturn(List.of(
                DccControlledFileTrainingProgressDO.builder()
                        .id(1003L)
                        .controlledFileId(902L)
                        .userId(99L)
                        .requiredViewSeconds(600)
                        .accumulatedViewSeconds(300)
                        .build()));
        when(controlledFileMapper.selectById(902L)).thenReturn(DccControlledFileDO.builder()
                .id(902L)
                .categoryId(10L)
                .fileName("sop-003.pdf")
                .title("SOP-003")
                .fileNumber("FI-003")
                .versionNo("2.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(trainingMapper.selectListByControlledFileId(902L)).thenReturn(List.of(
                DccControlledFileTrainingDO.builder().id(301L).controlledFileId(902L).departmentId(500L).status("PENDING").build(),
                DccControlledFileTrainingDO.builder().id(302L).controlledFileId(902L).departmentId(600L).status("PENDING").build()));
        when(trainingAssignmentMapper.selectListByTrainingId(301L)).thenReturn(List.of(
                DccControlledFileTrainingAssignmentDO.builder().id(401L).trainingId(301L).userId(99L).status("PENDING").build()));
        when(trainingAssignmentMapper.selectListByTrainingId(302L)).thenReturn(List.of(
                DccControlledFileTrainingAssignmentDO.builder().id(402L).trainingId(302L).userId(99L).status("PENDING").build()));

        DccTrainingExecutionPageReqVO reqVO = new DccTrainingExecutionPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PageResult<DccTrainingExecutionRespVO> result = trainingTaskService.getTrainingExecutionPage(88L, reqVO);

        assertEquals(1L, result.getTotal());
        DccTrainingExecutionRespVO row = result.getList().get(0);
        assertEquals("sop-003.pdf", row.getFileName());
        assertEquals(List.of(500L, 600L), row.getDepartmentIds());
        assertEquals("PENDING_VIEW", row.getStatus());
        assertFalse(Boolean.TRUE.equals(row.getEligibleToAcknowledge()));
        verify(trainingViewSessionMapper, never()).insert(any(DccControlledFileTrainingViewSessionDO.class));
    }

    @Test
    void getTrainingExecutionPage_usesTitleWhenFileNameIsBlank() {
        when(trainingProgressMapper.selectList()).thenReturn(List.of(
                DccControlledFileTrainingProgressDO.builder()
                        .id(1004L)
                        .controlledFileId(903L)
                        .userId(99L)
                        .requiredViewSeconds(600)
                        .accumulatedViewSeconds(600)
                        .build()));
        when(controlledFileMapper.selectById(903L)).thenReturn(DccControlledFileDO.builder()
                .id(903L)
                .categoryId(10L)
                .fileName("")
                .title("受控文件名称A")
                .fileNumber("FI-004")
                .versionNo("3.0")
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(trainingMapper.selectListByControlledFileId(903L)).thenReturn(List.of(
                DccControlledFileTrainingDO.builder().id(303L).controlledFileId(903L).departmentId(500L).status("PENDING").build()));
        when(trainingAssignmentMapper.selectListByTrainingId(303L)).thenReturn(List.of(
                DccControlledFileTrainingAssignmentDO.builder().id(403L).trainingId(303L).userId(99L).status("PENDING").build()));

        DccTrainingExecutionPageReqVO reqVO = new DccTrainingExecutionPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PageResult<DccTrainingExecutionRespVO> result = trainingTaskService.getTrainingExecutionPage(88L, reqVO);

        assertEquals(1L, result.getTotal());
        assertEquals("受控文件名称A", result.getList().get(0).getFileName());
    }
}
