package cn.iocoder.yudao.module.dcc.service.file;

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
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TRAINING_ACK_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TRAINING_VIEW_SECONDS_NOT_ENOUGH;

@Service
@Validated
public class DccTrainingAssignmentAckServiceImpl implements DccTrainingAssignmentAckService {

    private static final Set<String> ACK_ALLOWED_STATUSES = Set.of(
            DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus(),
            DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus(),
            DccControlledFileStatusEnum.ACTIVE.getStatus(),
            DccControlledFileStatusEnum.SUPERSEDED.getStatus(),
            DccControlledFileStatusEnum.OBSOLETE.getStatus()
    );

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileTrainingMapper trainingMapper;
    @Resource
    private DccControlledFileTrainingAssignmentMapper trainingAssignmentMapper;
    @Resource
    private DccControlledFileTrainingProgressMapper trainingProgressMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acknowledgeTraining(Long userId, Long controlledFileId) {
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!ACK_ALLOWED_STATUSES.contains(file.getStatus())) {
            throw exception(CONTROLLED_FILE_TRAINING_ACK_NOT_ALLOWED);
        }
        DccControlledFileTrainingProgressDO progress =
                trainingProgressMapper.selectByControlledFileIdAndUserId(controlledFileId, userId);
        if (progress == null) {
            throw exception(CONTROLLED_FILE_TRAINING_ACK_NOT_ALLOWED);
        }
        int requiredViewSeconds = progress.getRequiredViewSeconds() == null ? 600 : progress.getRequiredViewSeconds();
        int accumulatedViewSeconds = progress.getAccumulatedViewSeconds() == null ? 0 : progress.getAccumulatedViewSeconds();
        if (accumulatedViewSeconds < requiredViewSeconds) {
            throw exception(CONTROLLED_FILE_TRAINING_VIEW_SECONDS_NOT_ENOUGH);
        }
        boolean updatedAny = false;
        LocalDateTime acknowledgedAt = LocalDateTime.now();
        for (DccControlledFileTrainingDO training : trainingMapper.selectListByControlledFileId(controlledFileId)) {
            List<DccControlledFileTrainingAssignmentDO> assignments = trainingAssignmentMapper.selectListByTrainingId(training.getId());
            List<DccControlledFileTrainingAssignmentDO> currentUserPendingAssignments = assignments.stream()
                    .filter(assignment -> userId.equals(assignment.getUserId()))
                    .filter(assignment -> !DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode().equals(assignment.getStatus()))
                    .toList();
            for (DccControlledFileTrainingAssignmentDO assignment : currentUserPendingAssignments) {
                updatedAny = true;
                trainingAssignmentMapper.updateById(DccControlledFileTrainingAssignmentDO.builder()
                        .id(assignment.getId())
                        .status(DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode())
                        .acknowledgedAt(acknowledgedAt)
                        .build());
            }
            boolean allAcknowledged = assignments.stream().allMatch(assignment ->
                    DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode().equals(assignment.getStatus())
                            || currentUserPendingAssignments.stream().anyMatch(updated -> updated.getId().equals(assignment.getId())));
            if (allAcknowledged) {
                trainingMapper.updateById(DccControlledFileTrainingDO.builder()
                        .id(training.getId())
                        .status(DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode())
                        .build());
            }
        }
        if (!updatedAny) {
            throw exception(CONTROLLED_FILE_TRAINING_ACK_NOT_ALLOWED);
        }
        boolean allTrainingsAcknowledged = trainingMapper.selectListByControlledFileId(controlledFileId).stream()
                .allMatch(training -> DccControlledFileTrainingStatusEnum.ACKNOWLEDGED.getCode().equals(training.getStatus()));
        if (allTrainingsAcknowledged
                && DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus().equals(file.getStatus())) {
            controlledFileMapper.updateById(DccControlledFileDO.builder()
                    .id(controlledFileId)
                    .status(DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus())
                    .build());
        }
        trainingProgressMapper.updateById(DccControlledFileTrainingProgressDO.builder()
                .id(progress.getId())
                .acknowledgedAt(acknowledgedAt)
                .build());
    }
}
