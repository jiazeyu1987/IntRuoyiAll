package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceCreateReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileObsoleteAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileTrainingAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileObsoleteAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingAssignmentMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileTrainingMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMasterStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileMessageJobStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_OBSOLETE_REASON_REQUIRED;

@Service
@Validated
public class DccControlledFileObsoleteServiceImpl implements DccControlledFileObsoleteService {

    static final String MESSAGE_BUSINESS_TYPE_OBSOLETE = "OBSOLETE";
    private static final String MESSAGE_TEMPLATE_OBSOLETE = "dcc_obsolete";

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DccControlledFileObsoleteAuditMapper obsoleteAuditMapper;
    @Resource
    private DccControlledFileDistributionMapper distributionMapper;
    @Resource
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource
    private DccControlledFileTrainingMapper trainingMapper;
    @Resource
    private DccControlledFileTrainingAssignmentMapper trainingAssignmentMapper;
    @Resource
    private DccControlledFileMessageJobMapper messageJobMapper;
    @Resource
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource
    private DccControlledFileMessageDeliveryService messageDeliveryService;
    @Resource
    private DccObsoleteFileStorageService obsoleteFileStorageService;
    @Resource
    private DccControlledContentAdapter platformAdapter;
    @Resource
    private FormCenterRuntimeService formCenterRuntimeService;
    @Resource
    private DccControlledFilePendingActionGuard pendingActionGuard;
    @Resource
    private DccControlledFileApprovalRouteAssigneeResolver approvalRouteAssigneeResolver;

    @Override
    public void precheckObsoleteControlledFile(Long userId, Long id, DccControlledFileObsoleteReqVO reqVO) {
        requireObsoleteAllowed(userId, id, reqVO, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormInstanceRespVO obsoleteControlledFile(Long userId, Long id, DccControlledFileObsoleteReqVO reqVO) {
        DccControlledFileDO file = requireObsoleteAllowed(userId, id, reqVO, true);
        if (StrUtil.isBlank(reqVO.getIdempotencyKey())) {
            throw new IllegalArgumentException("DCC obsolete idempotencyKey is required");
        }
        Map<String, Object> formData = buildObsoleteFormData(file, reqVO);
        FormInstanceCreateReqVO createReqVO = new FormInstanceCreateReqVO();
        createReqVO.setContext(buildObsoleteContext(file, reqVO));
        createReqVO.setIdempotencyKey(reqVO.getIdempotencyKey());
        createReqVO.setFormData(formData);
        FormInstanceRespVO draft = formCenterRuntimeService.createInstance(createReqVO, userId);

        FormInstanceSubmitReqVO submitReqVO = new FormInstanceSubmitReqVO();
        submitReqVO.setFormData(formData);
        Map<String, List<Long>> startUserSelectAssignees = reqVO.getStartUserSelectAssignees();
        if (startUserSelectAssignees == null || startUserSelectAssignees.isEmpty()) {
            startUserSelectAssignees = approvalRouteAssigneeResolver.resolveStartUserSelectAssignees(file, userId);
        }
        submitReqVO.setStartUserSelectAssignees(startUserSelectAssignees);
        return formCenterRuntimeService.submitInstance(draft.getId(), submitReqVO, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyApprovedObsoleteControlledFile(Long userId, Long id, DccControlledFileObsoleteReqVO reqVO) {
        DccControlledFileDO file = requireObsoleteAllowed(userId, id, reqVO, false);

        obsoleteFileStorageService.moveControlledFileArtifactsToObsoleteFolder(file);

        LocalDateTime now = LocalDateTime.now();
        controlledFileMapper.updateById(DccControlledFileDO.builder()
                .id(file.getId())
                .status(DccControlledFileStatusEnum.OBSOLETE.getStatus())
                .obsoletedBy(userId)
                .obsoletedTime(now)
                .obsoleteReason(reqVO.getReason())
                .build());

        obsoleteAuditMapper.insert(DccControlledFileObsoleteAuditDO.builder()
                .controlledFileId(file.getId())
                .operatorId(userId)
                .obsoleteReason(reqVO.getReason())
                .statusBefore(file.getStatus())
                .statusAfter(DccControlledFileStatusEnum.OBSOLETE.getStatus())
                .build());

        DccControlledFileMasterDO master = file.getMasterId() == null ? null : controlledFileMasterMapper.selectById(file.getMasterId());
        if (master != null && file.getId().equals(master.getCurrentActiveControlledFileId())) {
            controlledFileMasterMapper.updateById(DccControlledFileMasterDO.builder()
                    .id(master.getId())
                    .currentActiveControlledFileId(null)
                    .status(DccControlledFileMasterStatusEnum.OBSOLETE_CHAIN.getCode())
                    .build());
        }

        for (Long recipientUserId : resolveObsoleteNotificationRecipientUserIds(file, userId)) {
            DccControlledFileMessageJobDO messageJob = DccControlledFileMessageJobDO.builder()
                    .businessType(MESSAGE_BUSINESS_TYPE_OBSOLETE)
                    .businessId(file.getId())
                    .templateCode(MESSAGE_TEMPLATE_OBSOLETE)
                    .recipientUserId(recipientUserId)
                    .status(DccControlledFileMessageJobStatusEnum.PENDING.getCode())
                    .build();
            int inserted = messageJobMapper.insert(messageJob);
            if (inserted <= 0) {
                throw new IllegalStateException("Failed to persist obsolete notification");
            }
            messageDeliveryService.dispatchMessageJob(messageJob, buildObsoleteNotifyParams(file, reqVO.getReason()));
        }
        platformAdapter.recordObsoleted(file, userId, reqVO.getReason(), "dcc-obsolete:" + file.getId());
    }

    private BusinessActionContextReqVO buildObsoleteContext(DccControlledFileDO file, DccControlledFileObsoleteReqVO reqVO) {
        BusinessActionContextReqVO context = new BusinessActionContextReqVO();
        context.setDataDomain("DCC");
        context.setSystemCode("DCC");
        context.setObjectType("CONTROLLED_FILE");
        context.setObjectId(String.valueOf(file.getId()));
        context.setObjectVersion(file.getVersionNo());
        context.setActionCode("OBSOLETE");
        context.setObjectState(file.getStatus());
        context.setProductCode(file.getProductCode());
        context.setCategoryCode(file.getCategoryId() == null ? null : String.valueOf(file.getCategoryId()));
        context.setReason(reqVO.getReason());
        return context;
    }

    private Map<String, Object> buildObsoleteFormData(DccControlledFileDO file, DccControlledFileObsoleteReqVO reqVO) {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("controlledFileId", file.getId());
        formData.put("reason", reqVO.getReason());
        return formData;
    }

    private DccControlledFileDO requireObsoleteAllowed(Long userId, Long id, DccControlledFileObsoleteReqVO reqVO,
            boolean enforcePendingActionGuard) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getReason())) {
            throw exception(CONTROLLED_FILE_OBSOLETE_REASON_REQUIRED);
        }
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())
                || !permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.OBSOLETE)) {
            throw exception(CONTROLLED_FILE_OBSOLETE_NOT_ALLOWED);
        }
        if (enforcePendingActionGuard) {
            pendingActionGuard.assertNoPendingBusinessAction(file);
        }
        return file;
    }

    private Map<String, Object> buildObsoleteNotifyParams(DccControlledFileDO file, String obsoleteReason) {
        Map<String, Object> params = new java.util.LinkedHashMap<>();
        params.put("title", StrUtil.blankToDefault(file.getTitle(), file.getFileName()));
        params.put("version", StrUtil.blankToDefault(file.getVersionNo(), "-"));
        params.put("reason", StrUtil.blankToDefault(obsoleteReason, "-"));
        return params;
    }

    private Set<Long> resolveObsoleteNotificationRecipientUserIds(DccControlledFileDO file, Long operatorUserId) {
        Set<Long> userIds = resolveAffectedRecipientUserIds(file.getId());
        if (userIds.isEmpty()) {
            addFirstPresent(userIds, file.getRequesterId(), file.getSubmitterId(), operatorUserId);
        }
        return userIds;
    }

    private Set<Long> resolveAffectedRecipientUserIds(Long controlledFileId) {
        Set<Long> userIds = new LinkedHashSet<>();
        distributionMapper.selectListByControlledFileId(controlledFileId).forEach(distribution ->
                distributionRecipientMapper.selectListByDistributionId(distribution.getId()).stream()
                        .map(DccControlledFileDistributionRecipientDO::getUserId)
                        .forEach(userIds::add));
        trainingMapper.selectListByControlledFileId(controlledFileId).forEach(training ->
                trainingAssignmentMapper.selectListByTrainingId(training.getId()).stream()
                        .map(DccControlledFileTrainingAssignmentDO::getUserId)
                        .forEach(userIds::add));
        return userIds;
    }

    private void addFirstPresent(Set<Long> userIds, Long... candidateUserIds) {
        for (Long userId : candidateUserIds) {
            if (userId != null) {
                userIds.add(userId);
                return;
            }
        }
    }
}
