package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingProgressDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingProgressMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileTrainingStatusEnum;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TRAINING_ACK_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TRAINING_VIEW_SECONDS_NOT_ENOUGH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccTrainingAssignmentAckServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileTrainingMapper trainingMapper;
    @Mock
    private DccControlledFileTrainingAssignmentMapper trainingAssignmentMapper;
    @Mock
    private DccControlledFileTrainingProgressMapper trainingProgressMapper;

    @InjectMocks
    private DccTrainingAssignmentAckServiceImpl ackService;

    @Test
    void acknowledgeTraining_marksAssignmentsAndTrainingComplete() {
        when(controlledFileMapper.selectById(900L)).thenReturn(DccControlledFileDO.builder()
                .id(900L)
                .status(DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus())
                .build());
        when(trainingMapper.selectListByControlledFileId(900L))
                .thenReturn(List.of(
                        DccControlledFileTrainingDO.builder()
                                .id(301L)
                                .controlledFileId(900L)
                                .status(DccControlledFileTrainingStatusEnum.PENDING.getCode())
                                .build()))
                .thenReturn(List.of(
                        DccControlledFileTrainingDO.builder()
                                .id(301L)
                                .controlledFileId(900L)
                                .status(DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode())
                                .build()));
        when(trainingAssignmentMapper.selectListByTrainingId(301L)).thenReturn(List.of(
                DccControlledFileTrainingAssignmentDO.builder()
                        .id(401L)
                        .trainingId(301L)
                        .userId(99L)
                        .status(DccControlledFileTrainingStatusEnum.PENDING.getCode())
                        .build()));
        when(trainingProgressMapper.selectByControlledFileIdAndUserId(900L, 99L)).thenReturn(
                DccControlledFileTrainingProgressDO.builder()
                        .id(601L)
                        .controlledFileId(900L)
                        .userId(99L)
                        .requiredViewSeconds(600)
                        .accumulatedViewSeconds(600)
                        .build());

        ackService.acknowledgeTraining(99L, 900L);

        ArgumentCaptor<DccControlledFileTrainingAssignmentDO> assignmentCaptor =
                ArgumentCaptor.forClass(DccControlledFileTrainingAssignmentDO.class);
        verify(trainingAssignmentMapper).updateById(assignmentCaptor.capture());
        assertEquals(DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode(), assignmentCaptor.getValue().getStatus());
        assertNotNull(assignmentCaptor.getValue().getAcknowledgedAt());

        ArgumentCaptor<DccControlledFileTrainingDO> trainingCaptor =
                ArgumentCaptor.forClass(DccControlledFileTrainingDO.class);
        verify(trainingMapper).updateById(trainingCaptor.capture());
        assertEquals(DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode(), trainingCaptor.getValue().getStatus());

        ArgumentCaptor<DccControlledFileDO> fileCaptor = ArgumentCaptor.forClass(DccControlledFileDO.class);
        verify(controlledFileMapper).updateById(fileCaptor.capture());
        assertEquals(DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus(), fileCaptor.getValue().getStatus());

        ArgumentCaptor<DccControlledFileTrainingProgressDO> progressCaptor =
                ArgumentCaptor.forClass(DccControlledFileTrainingProgressDO.class);
        verify(trainingProgressMapper).updateById(progressCaptor.capture());
        assertNotNull(progressCaptor.getValue().getAcknowledgedAt());
    }

    @Test
    void acknowledgeTraining_withoutPendingAssignment_throws() {
        when(controlledFileMapper.selectById(901L)).thenReturn(DccControlledFileDO.builder()
                .id(901L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(trainingMapper.selectListByControlledFileId(901L)).thenReturn(List.of(
                DccControlledFileTrainingDO.builder().id(302L).controlledFileId(901L).build()));
        when(trainingAssignmentMapper.selectListByTrainingId(302L)).thenReturn(List.of(
                DccControlledFileTrainingAssignmentDO.builder()
                        .id(402L)
                        .trainingId(302L)
                        .userId(88L)
                        .status(DccControlledFileTrainingStatusEnum.PENDING.getCode())
                        .acknowledgedAt(LocalDateTime.now())
                        .build()));
        when(trainingProgressMapper.selectByControlledFileIdAndUserId(901L, 99L)).thenReturn(
                DccControlledFileTrainingProgressDO.builder()
                        .id(602L)
                        .controlledFileId(901L)
                        .userId(99L)
                        .requiredViewSeconds(600)
                        .accumulatedViewSeconds(600)
                        .build());

        assertServiceException(() -> ackService.acknowledgeTraining(99L, 901L),
                CONTROLLED_FILE_TRAINING_ACK_NOT_ALLOWED);
    }

    @Test
    void acknowledgeTraining_beforeRequiredViewSeconds_throws() {
        when(controlledFileMapper.selectById(902L)).thenReturn(DccControlledFileDO.builder()
                .id(902L)
                .status(DccControlledFileStatusEnum.ACTIVE.getStatus())
                .build());
        when(trainingProgressMapper.selectByControlledFileIdAndUserId(902L, 99L)).thenReturn(
                DccControlledFileTrainingProgressDO.builder()
                        .id(603L)
                        .controlledFileId(902L)
                        .userId(99L)
                        .requiredViewSeconds(600)
                        .accumulatedViewSeconds(599)
                        .build());

        assertServiceException(() -> ackService.acknowledgeTraining(99L, 902L),
                CONTROLLED_FILE_TRAINING_VIEW_SECONDS_NOT_ENOUGH);
    }
}
