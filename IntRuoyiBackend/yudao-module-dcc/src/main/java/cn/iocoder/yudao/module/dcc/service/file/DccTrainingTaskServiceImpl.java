package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingExecutionPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingExecutionRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingTaskPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingTaskRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingViewSessionHeartbeatReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingViewSessionStartReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.training.vo.DccTrainingViewSessionStopReqVO;
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
import cn.iocoder.yudao.module.dcc.enums.DccAccessResultEnum;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileDistributionStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TRAINING_ACK_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_TRAINING_VIEW_SECONDS_NOT_ENOUGH;

@Service
@Validated
public class DccTrainingTaskServiceImpl implements DccTrainingTaskService {

    private static final int DEFAULT_REQUIRED_VIEW_SECONDS = 600;
    private static final int MAX_HEARTBEAT_GAP_SECONDS = 15;
    private static final String STATUS_PENDING_VIEW = "PENDING_VIEW";
    private static final String STATUS_READY_TO_ACKNOWLEDGE = "READY_TO_ACKNOWLEDGE";
    private static final String STATUS_ACKNOWLEDGED = "ACKNOWLEDGED";
    private static final Set<String> TRAINING_VISIBLE_FILE_STATUSES = Set.of(
            DccControlledFileStatusEnum.TRAINING_IN_PROGRESS.getStatus(),
            DccControlledFileStatusEnum.PENDING_MANUAL_DISTRIBUTION.getStatus(),
            DccControlledFileStatusEnum.ACTIVE.getStatus(),
            DccControlledFileStatusEnum.SUPERSEDED.getStatus(),
            DccControlledFileStatusEnum.OBSOLETE.getStatus()
    );

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileTrainingProgressMapper trainingProgressMapper;
    @Resource
    private DccControlledFileTrainingViewSessionMapper trainingViewSessionMapper;
    @Resource
    private DccControlledFileTrainingMapper trainingMapper;
    @Resource
    private DccControlledFileTrainingAssignmentMapper trainingAssignmentMapper;
    @Resource
    private DccControlledFileDistributionMapper distributionMapper;
    @Resource
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource
    private FileMapper fileMapper;
    @Resource
    private FileService fileService;
    @Resource
    private DccControlledPreviewWatermarkService watermarkService;
    @Resource
    private DccControlledFileAccessLogMapper accessLogMapper;
    @Resource
    private DccTrainingAssignmentAckService trainingAssignmentAckService;

    @Override
    public PageResult<DccTrainingTaskRespVO> getMyTrainingTaskPage(Long userId, DccTrainingTaskPageReqVO reqVO) {
        List<DccTrainingTaskRespVO> rows = trainingProgressMapper.selectListByUserId(userId).stream()
                .map(this::buildTaskRow)
                .filter(row -> matchesTaskFilter(row, reqVO))
                .sorted(Comparator.comparing(
                                DccTrainingTaskRespVO::getPublishedTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(
                                DccTrainingTaskRespVO::getProgressId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return slicePage(reqVO.getPageNo(), reqVO.getPageSize(), rows);
    }

    @Override
    public DccTrainingTaskRespVO getTrainingTask(Long userId, Long progressId) {
        return buildTaskRow(loadOwnedProgress(userId, progressId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccControlledFileBinary readTrainingPreviewFile(Long userId, Long progressId,
                                                           DccRequestAuditContext auditContext) {
        if (auditContext == null) {
            throw new IllegalArgumentException("DCC request audit context is required");
        }
        DccControlledFileTrainingProgressDO progress = loadOwnedProgress(userId, progressId);
        DccControlledFileDO file = loadTrainingVisibleFile(progress.getControlledFileId());
        FileDO publishedFile = loadPublishedFile(file);
        try {
            byte[] content = fileService.getFileContent(publishedFile.getConfigId(), publishedFile.getPath());
            LocalDateTime now = LocalDateTime.now();
            updateProgressMetadata(progress, now, false, 0);
            markDistributionRead(progress.getControlledFileId(), userId, now);
            recordAccess(progress.getControlledFileId(), userId, DccAccessTypeEnum.PREVIEW, true,
                    "TRAINING_OK", auditContext);
            return new DccControlledFileBinary(
                    publishedFile.getName(),
                    publishedFile.getType(),
                    content,
                    watermarkService.build(userId, "training", publishedFile.getName())
            );
        } catch (Exception ex) {
            recordAccess(progress.getControlledFileId(), userId, DccAccessTypeEnum.PREVIEW, false,
                    StrUtil.blankToDefault(ex.getMessage(), "TRAINING_READ_FAILED"), auditContext);
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccTrainingTaskRespVO startViewSession(Long userId, Long progressId, DccTrainingViewSessionStartReqVO reqVO) {
        DccControlledFileTrainingProgressDO progress = loadOwnedProgress(userId, progressId);
        loadTrainingVisibleFile(progress.getControlledFileId());
        LocalDateTime now = LocalDateTime.now();
        closeOtherActiveSessions(progressId, userId, reqVO.getClientSessionId(), now);
        DccControlledFileTrainingViewSessionDO existing =
                trainingViewSessionMapper.selectActiveByProgressIdAndClientSessionId(progressId, reqVO.getClientSessionId());
        if (existing == null) {
            trainingViewSessionMapper.insert(DccControlledFileTrainingViewSessionDO.builder()
                    .trainingProgressId(progressId)
                    .userId(userId)
                    .clientSessionId(reqVO.getClientSessionId())
                    .startedAt(now)
                    .lastHeartbeatAt(now)
                    .accumulatedSeconds(0)
                    .build());
        }
        updateProgressMetadata(progress, now, false, 0);
        return buildTaskRow(progressId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccTrainingTaskRespVO heartbeatViewSession(Long userId, Long progressId,
                                                      DccTrainingViewSessionHeartbeatReqVO reqVO) {
        DccControlledFileTrainingProgressDO progress = loadOwnedProgress(userId, progressId);
        loadTrainingVisibleFile(progress.getControlledFileId());
        DccControlledFileTrainingViewSessionDO session =
                trainingViewSessionMapper.selectActiveByProgressIdAndClientSessionId(progressId, reqVO.getClientSessionId());
        if (session == null || !userId.equals(session.getUserId())) {
            throw exception(CONTROLLED_FILE_TRAINING_ACK_NOT_ALLOWED);
        }
        LocalDateTime now = LocalDateTime.now();
        int increment = resolveIncrementSeconds(session.getLastHeartbeatAt(), now);
        trainingViewSessionMapper.updateById(DccControlledFileTrainingViewSessionDO.builder()
                .id(session.getId())
                .lastHeartbeatAt(now)
                .accumulatedSeconds(safeSeconds(session.getAccumulatedSeconds()) + increment)
                .build());
        updateProgressMetadata(progress, now, false, increment);
        return buildTaskRow(progressId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccTrainingTaskRespVO stopViewSession(Long userId, Long progressId, DccTrainingViewSessionStopReqVO reqVO) {
        DccControlledFileTrainingProgressDO progress = loadOwnedProgress(userId, progressId);
        loadTrainingVisibleFile(progress.getControlledFileId());
        DccControlledFileTrainingViewSessionDO session =
                trainingViewSessionMapper.selectActiveByProgressIdAndClientSessionId(progressId, reqVO.getClientSessionId());
        if (session != null && userId.equals(session.getUserId())) {
            LocalDateTime now = LocalDateTime.now();
            int increment = resolveIncrementSeconds(session.getLastHeartbeatAt(), now);
            trainingViewSessionMapper.updateById(DccControlledFileTrainingViewSessionDO.builder()
                    .id(session.getId())
                    .lastHeartbeatAt(now)
                    .endedAt(now)
                    .accumulatedSeconds(safeSeconds(session.getAccumulatedSeconds()) + increment)
                    .build());
            updateProgressMetadata(progress, now, false, increment);
        }
        return buildTaskRow(progressId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acknowledgeTraining(Long userId, Long progressId) {
        DccControlledFileTrainingProgressDO progress = loadOwnedProgress(userId, progressId);
        if (Boolean.TRUE.equals(progress.getAcknowledgedAt() != null)) {
            return;
        }
        if (safeSeconds(progress.getAccumulatedViewSeconds()) < safeSeconds(progress.getRequiredViewSeconds())) {
            throw exception(CONTROLLED_FILE_TRAINING_VIEW_SECONDS_NOT_ENOUGH);
        }
        trainingAssignmentAckService.acknowledgeTraining(userId, progress.getControlledFileId());
    }

    @Override
    public PageResult<DccTrainingExecutionRespVO> getTrainingExecutionPage(Long userId, DccTrainingExecutionPageReqVO reqVO) {
        List<DccTrainingExecutionRespVO> rows = trainingProgressMapper.selectList().stream()
                .map(this::buildExecutionRow)
                .filter(row -> matchesExecutionFilter(row, reqVO))
                .sorted(Comparator.comparing(
                                DccTrainingExecutionRespVO::getPublishedTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(
                                DccTrainingExecutionRespVO::getProgressId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return slicePage(reqVO.getPageNo(), reqVO.getPageSize(), rows);
    }

    private DccTrainingTaskRespVO buildTaskRow(Long progressId, Long userId) {
        return buildTaskRow(loadOwnedProgress(userId, progressId));
    }

    private DccTrainingTaskRespVO buildTaskRow(DccControlledFileTrainingProgressDO progress) {
        DccControlledFileDO file = controlledFileMapper.selectById(progress.getControlledFileId());
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccTrainingTaskRespVO respVO = new DccTrainingTaskRespVO();
        fillCommonTaskFields(respVO, progress, file);
        return respVO;
    }

    private DccTrainingExecutionRespVO buildExecutionRow(DccControlledFileTrainingProgressDO progress) {
        DccControlledFileDO file = controlledFileMapper.selectById(progress.getControlledFileId());
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccTrainingExecutionRespVO respVO = new DccTrainingExecutionRespVO();
        respVO.setProgressId(progress.getId());
        respVO.setControlledFileId(file.getId());
        respVO.setCategoryId(file.getCategoryId());
        respVO.setFileName(resolveBusinessFileName(file));
        respVO.setTitle(file.getTitle());
        respVO.setFileNumber(file.getFileNumber());
        respVO.setVersionNo(file.getVersionNo());
        respVO.setFileStatus(file.getStatus());
        respVO.setUserId(progress.getUserId());
        respVO.setDepartmentIds(resolveDepartmentIds(file.getId(), progress.getUserId()));
        respVO.setRequiredViewSeconds(normalizeRequiredSeconds(progress));
        respVO.setAccumulatedViewSeconds(safeSeconds(progress.getAccumulatedViewSeconds()));
        respVO.setEligibleToAcknowledge(isEligible(progress));
        respVO.setFirstViewedAt(progress.getFirstViewedAt());
        respVO.setLastViewedAt(progress.getLastViewedAt());
        respVO.setAcknowledgedAt(progress.getAcknowledgedAt());
        respVO.setPublishedTime(file.getPublishedTime());
        respVO.setStatus(resolveProgressStatus(progress));
        return respVO;
    }

    private void fillCommonTaskFields(DccTrainingTaskRespVO respVO, DccControlledFileTrainingProgressDO progress,
                                      DccControlledFileDO file) {
        respVO.setProgressId(progress.getId());
        respVO.setControlledFileId(file.getId());
        respVO.setCategoryId(file.getCategoryId());
        respVO.setFileName(resolveBusinessFileName(file));
        respVO.setTitle(file.getTitle());
        respVO.setFileNumber(file.getFileNumber());
        respVO.setVersionNo(file.getVersionNo());
        respVO.setFileStatus(file.getStatus());
        respVO.setUserId(progress.getUserId());
        respVO.setDepartmentIds(resolveDepartmentIds(file.getId(), progress.getUserId()));
        respVO.setRequiredViewSeconds(normalizeRequiredSeconds(progress));
        respVO.setAccumulatedViewSeconds(safeSeconds(progress.getAccumulatedViewSeconds()));
        respVO.setEligibleToAcknowledge(isEligible(progress));
        respVO.setFirstViewedAt(progress.getFirstViewedAt());
        respVO.setLastViewedAt(progress.getLastViewedAt());
        respVO.setAcknowledgedAt(progress.getAcknowledgedAt());
        respVO.setPublishedTime(file.getPublishedTime());
        respVO.setStatus(resolveProgressStatus(progress));
    }

    private List<Long> resolveDepartmentIds(Long controlledFileId, Long userId) {
        List<DccControlledFileTrainingDO> trainings = trainingMapper.selectListByControlledFileId(controlledFileId);
        LinkedHashSet<Long> departmentIds = new LinkedHashSet<>();
        for (DccControlledFileTrainingDO training : trainings) {
            boolean containsUser = trainingAssignmentMapper.selectListByTrainingId(training.getId()).stream()
                    .map(DccControlledFileTrainingAssignmentDO::getUserId)
                    .anyMatch(currentUserId -> currentUserId != null && currentUserId.equals(userId));
            if (containsUser) {
                departmentIds.add(training.getDepartmentId());
            }
        }
        return List.copyOf(departmentIds);
    }

    private boolean matchesTaskFilter(DccTrainingTaskRespVO row, DccTrainingTaskPageReqVO reqVO) {
        if (reqVO.getCategoryId() != null && !reqVO.getCategoryId().equals(row.getCategoryId())) {
            return false;
        }
        return StrUtil.isBlank(reqVO.getStatus()) || StrUtil.equals(reqVO.getStatus(), row.getStatus());
    }

    private boolean matchesExecutionFilter(DccTrainingExecutionRespVO row, DccTrainingExecutionPageReqVO reqVO) {
        if (reqVO.getCategoryId() != null && !reqVO.getCategoryId().equals(row.getCategoryId())) {
            return false;
        }
        return StrUtil.isBlank(reqVO.getStatus()) || StrUtil.equals(reqVO.getStatus(), row.getStatus());
    }

    private String resolveBusinessFileName(DccControlledFileDO file) {
        return StrUtil.blankToDefault(file.getFileName(), file.getTitle());
    }

    private <T> PageResult<T> slicePage(Integer pageNo, Integer pageSize, List<T> rows) {
        if (rows.isEmpty()) {
            return PageResult.empty(0L);
        }
        int resolvedPageNo = Math.max(pageNo == null ? 1 : pageNo, 1);
        int resolvedPageSize = Math.max(pageSize == null ? 10 : pageSize, 1);
        int fromIndex = Math.min((resolvedPageNo - 1) * resolvedPageSize, rows.size());
        int toIndex = Math.min(fromIndex + resolvedPageSize, rows.size());
        return new PageResult<>(rows.subList(fromIndex, toIndex), (long) rows.size());
    }

    private DccControlledFileTrainingProgressDO loadOwnedProgress(Long userId, Long progressId) {
        DccControlledFileTrainingProgressDO progress = trainingProgressMapper.selectById(progressId);
        if (progress == null || !userId.equals(progress.getUserId())) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return progress;
    }

    private DccControlledFileDO loadTrainingVisibleFile(Long controlledFileId) {
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!TRAINING_VISIBLE_FILE_STATUSES.contains(file.getStatus()) || file.getPublishedFileId() == null) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return file;
    }

    private FileDO loadPublishedFile(DccControlledFileDO file) {
        FileDO publishedFile = fileMapper.selectById(file.getPublishedFileId());
        if (publishedFile == null) {
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return publishedFile;
    }

    private void closeOtherActiveSessions(Long progressId, Long userId, String currentClientSessionId, LocalDateTime now) {
        for (DccControlledFileTrainingViewSessionDO activeSession :
                trainingViewSessionMapper.selectActiveListByProgressId(progressId)) {
            if (!userId.equals(activeSession.getUserId()) || StrUtil.equals(activeSession.getClientSessionId(), currentClientSessionId)) {
                continue;
            }
            int increment = resolveIncrementSeconds(activeSession.getLastHeartbeatAt(), now);
            trainingViewSessionMapper.updateById(DccControlledFileTrainingViewSessionDO.builder()
                    .id(activeSession.getId())
                    .lastHeartbeatAt(now)
                    .endedAt(now)
                    .accumulatedSeconds(safeSeconds(activeSession.getAccumulatedSeconds()) + increment)
                    .build());
            DccControlledFileTrainingProgressDO progress = trainingProgressMapper.selectById(progressId);
            if (progress != null) {
                updateProgressMetadata(progress, now, false, increment);
            }
        }
    }

    private int resolveIncrementSeconds(LocalDateTime lastHeartbeatAt, LocalDateTime now) {
        if (lastHeartbeatAt == null || now == null || now.isBefore(lastHeartbeatAt)) {
            return 0;
        }
        long seconds = java.time.Duration.between(lastHeartbeatAt, now).getSeconds();
        if (seconds <= 0) {
            return 0;
        }
        return (int) Math.min(seconds, MAX_HEARTBEAT_GAP_SECONDS);
    }

    private void updateProgressMetadata(DccControlledFileTrainingProgressDO progress, LocalDateTime now,
                                        boolean acknowledged, int incrementSeconds) {
        int nextAccumulated = safeSeconds(progress.getAccumulatedViewSeconds()) + Math.max(incrementSeconds, 0);
        trainingProgressMapper.updateById(DccControlledFileTrainingProgressDO.builder()
                .id(progress.getId())
                .accumulatedViewSeconds(nextAccumulated)
                .firstViewedAt(progress.getFirstViewedAt() == null ? now : null)
                .lastViewedAt(now)
                .acknowledgedAt(acknowledged ? now : null)
                .build());
        progress.setAccumulatedViewSeconds(nextAccumulated);
        if (progress.getFirstViewedAt() == null) {
            progress.setFirstViewedAt(now);
        }
        progress.setLastViewedAt(now);
        if (acknowledged) {
            progress.setAcknowledgedAt(now);
        }
    }

    private void markDistributionRead(Long controlledFileId, Long userId, LocalDateTime now) {
        for (DccControlledFileDistributionDO distribution : distributionMapper.selectListByControlledFileId(controlledFileId)) {
            boolean touchedRecipient = false;
            List<DccControlledFileDistributionRecipientDO> recipients =
                    distributionRecipientMapper.selectListByDistributionId(distribution.getId());
            for (DccControlledFileDistributionRecipientDO recipient : recipients) {
                if (!userId.equals(recipient.getUserId())) {
                    continue;
                }
                touchedRecipient = true;
                if (recipient.getReadAt() == null) {
                    distributionRecipientMapper.updateById(DccControlledFileDistributionRecipientDO.builder()
                            .id(recipient.getId())
                            .readAt(now)
                            .build());
                }
            }
            if (touchedRecipient && DccControlledFileDistributionStatusEnum.PENDING.getCode().equals(distribution.getStatus())) {
                distributionMapper.updateById(DccControlledFileDistributionDO.builder()
                        .id(distribution.getId())
                        .status(DccControlledFileDistributionStatusEnum.READ.getCode())
                        .build());
            }
        }
    }

    private void recordAccess(Long fileId, Long userId, DccAccessTypeEnum accessType, boolean allowed, String reason,
                              DccRequestAuditContext auditContext) {
        accessLogMapper.insert(DccControlledFileAccessLogDO.builder()
                .controlledFileId(fileId)
                .userId(userId)
                .actionType(accessType.name())
                .result(allowed ? DccAccessResultEnum.ALLOWED.name() : DccAccessResultEnum.DENIED.name())
                .reason(reason)
                .sourceIp(auditContext.sourceIp())
                .userAgent(auditContext.userAgent())
                .requestId(auditContext.requireRequestId("training preview"))
                .build());
    }

    private int safeSeconds(Integer value) {
        return value == null ? 0 : value;
    }

    private int normalizeRequiredSeconds(DccControlledFileTrainingProgressDO progress) {
        int requiredSeconds = safeSeconds(progress.getRequiredViewSeconds());
        return requiredSeconds > 0 ? requiredSeconds : DEFAULT_REQUIRED_VIEW_SECONDS;
    }

    private boolean isEligible(DccControlledFileTrainingProgressDO progress) {
        return progress.getAcknowledgedAt() == null
                && safeSeconds(progress.getAccumulatedViewSeconds()) >= normalizeRequiredSeconds(progress);
    }

    private String resolveProgressStatus(DccControlledFileTrainingProgressDO progress) {
        if (progress.getAcknowledgedAt() != null) {
            return STATUS_ACKNOWLEDGED;
        }
        if (isEligible(progress)) {
            return STATUS_READY_TO_ACKNOWLEDGE;
        }
        return STATUS_PENDING_VIEW;
    }
}
