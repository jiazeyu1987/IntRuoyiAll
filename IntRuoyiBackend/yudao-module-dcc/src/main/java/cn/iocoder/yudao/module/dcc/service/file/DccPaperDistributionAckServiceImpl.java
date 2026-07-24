package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
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
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DISTRIBUTION_ACK_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;

@Service
@Validated
public class DccPaperDistributionAckServiceImpl implements DccPaperDistributionAckService {

    private static final String MESSAGE_BUSINESS_TYPE_DISTRIBUTION = "DISTRIBUTION";
    private static final String MESSAGE_TEMPLATE_DISTRIBUTION = "dcc_distribution";
    private static final Set<String> ACK_ALLOWED_FILE_STATUSES = Set.of(
            DccControlledFileStatusEnum.ACTIVE.getStatus(),
            DccControlledFileStatusEnum.SUPERSEDED.getStatus(),
            DccControlledFileStatusEnum.OBSOLETE.getStatus()
    );

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileDistributionMapper distributionMapper;
    @Resource
    private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource
    private DccControlledFileMessageJobMapper messageJobMapper;
    @Resource
    private DccControlledFileMessageDeliveryService messageDeliveryService;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public List<DccPaperDistributionRecordRespVO> getPaperDistributionRecords(Long controlledFileId) {
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        List<DccControlledFileDistributionDO> paperDistributions =
                distributionMapper.selectListByControlledFileId(controlledFileId).stream()
                        .filter(distribution -> DccDistributionMediumEnum.PAPER.getCode()
                                .equals(distribution.getDistributionMedium()))
                        .toList();
        Map<Long, List<DccControlledFileDistributionRecipientDO>> recipientMap = paperDistributions.stream()
                .collect(Collectors.toMap(DccControlledFileDistributionDO::getId,
                        distribution -> distributionRecipientMapper.selectListByDistributionId(distribution.getId())));
        Set<Long> userIds = new LinkedHashSet<>();
        paperDistributions.forEach(distribution -> {
            addUserId(userIds, distribution.getAcknowledgedBy());
            addUserId(userIds, distribution.getRecoveredBy());
            recipientMap.getOrDefault(distribution.getId(), List.of()).forEach(recipient ->
                    addUserId(userIds, recipient.getUserId()));
        });
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty() ? Map.of() : adminUserApi.getUserMap(userIds);
        if (userIds.stream().anyMatch(userId -> !userMap.containsKey(userId))) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_ACK_NOT_ALLOWED);
        }
        return paperDistributions.stream()
                .map(distribution -> toPaperDistributionRecord(file, distribution,
                        recipientMap.getOrDefault(distribution.getId(), List.of()), userMap))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acknowledgePaperDistribution(Long userId, Long controlledFileId, Long distributionId,
                                             List<Long> recipientUserIds) {
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        List<Long> normalizedRecipientUserIds = normalizeRecipientUserIds(recipientUserIds);
        if (normalizedRecipientUserIds.isEmpty()) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_ACK_NOT_ALLOWED);
        }
        adminUserApi.validateUserList(normalizedRecipientUserIds);
        if (!ACK_ALLOWED_FILE_STATUSES.contains(file.getStatus())
                || !permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.DISTRIBUTE)) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_ACK_NOT_ALLOWED);
        }
        DccControlledFileDistributionDO distribution = distributionMapper.selectById(distributionId);
        if (distribution == null
                || !controlledFileId.equals(distribution.getControlledFileId())
                || !DccDistributionMediumEnum.PAPER.getCode().equals(distribution.getDistributionMedium())) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_ACK_NOT_ALLOWED);
        }
        if (DccControlledFileDistributionStatusEnum.ACKNOWLEDGED.getCode().equals(distribution.getStatus())) {
            return;
        }
        LocalDateTime acknowledgedAt = LocalDateTime.now();
        replacePaperRecipients(file, distribution.getId(), normalizedRecipientUserIds);
        distributionMapper.updateById(DccControlledFileDistributionDO.builder()
                .id(distribution.getId())
                .status(DccControlledFileDistributionStatusEnum.ACKNOWLEDGED.getCode())
                .acknowledgedBy(userId)
                .acknowledgedAt(acknowledgedAt)
                .build());
    }

    private static List<Long> normalizeRecipientUserIds(List<Long> recipientUserIds) {
        if (recipientUserIds == null) {
            return List.of();
        }
        return recipientUserIds.stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf));
    }

    private void replacePaperRecipients(DccControlledFileDO file, Long distributionId, List<Long> recipientUserIds) {
        distributionRecipientMapper.delete(DccControlledFileDistributionRecipientDO::getDistributionId, distributionId);
        for (Long recipientUserId : recipientUserIds) {
            DccControlledFileMessageJobDO messageJob = createDistributionMessageJob(file, distributionId,
                    recipientUserId);
            distributionRecipientMapper.insert(DccControlledFileDistributionRecipientDO.builder()
                    .distributionId(distributionId)
                    .userId(recipientUserId)
                    .messageJobId(messageJob.getId())
                    .build());
        }
    }

    private DccControlledFileMessageJobDO createDistributionMessageJob(DccControlledFileDO file, Long distributionId,
                                                                       Long recipientUserId) {
        DccControlledFileMessageJobDO messageJob = DccControlledFileMessageJobDO.builder()
                .businessType(MESSAGE_BUSINESS_TYPE_DISTRIBUTION)
                .businessId(distributionId)
                .templateCode(MESSAGE_TEMPLATE_DISTRIBUTION)
                .recipientUserId(recipientUserId)
                .status(DccControlledFileMessageJobStatusEnum.PENDING.getCode())
                .build();
        messageJobMapper.insert(messageJob);
        messageDeliveryService.dispatchMessageJob(messageJob, buildDistributionNotifyParams(file));
        return messageJob;
    }

    private Map<String, Object> buildDistributionNotifyParams(DccControlledFileDO file) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("title", StrUtil.blankToDefault(file.getTitle(), file.getFileName()));
        params.put("version", StrUtil.blankToDefault(file.getVersionNo(), "-"));
        if (file.getEffectiveDate() != null) {
            params.put("effectiveDate", file.getEffectiveDate().toString());
        }
        return params;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverPaperDistribution(Long userId, Long controlledFileId, Long distributionId) {
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!ACK_ALLOWED_FILE_STATUSES.contains(file.getStatus())
                || !permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.DISTRIBUTE)) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_ACK_NOT_ALLOWED);
        }
        DccControlledFileDistributionDO distribution = distributionMapper.selectById(distributionId);
        if (distribution == null
                || !controlledFileId.equals(distribution.getControlledFileId())
                || !DccDistributionMediumEnum.PAPER.getCode().equals(distribution.getDistributionMedium())
                || !DccControlledFileDistributionStatusEnum.ACKNOWLEDGED.getCode().equals(distribution.getStatus())) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_ACK_NOT_ALLOWED);
        }
        LocalDateTime recoveredAt = LocalDateTime.now();
        distributionMapper.updateById(DccControlledFileDistributionDO.builder()
                .id(distribution.getId())
                .status(DccControlledFileDistributionStatusEnum.RECOVERED.getCode())
                .recoveredBy(userId)
                .recoveredAt(recoveredAt)
                .build());
    }

    private static void addUserId(Set<Long> userIds, Long userId) {
        if (userId != null) {
            userIds.add(userId);
        }
    }

    private static DccPaperDistributionRecordRespVO toPaperDistributionRecord(
            DccControlledFileDO file,
            DccControlledFileDistributionDO distribution,
            List<DccControlledFileDistributionRecipientDO> recipients,
            Map<Long, AdminUserRespDTO> userMap) {
        DccPaperDistributionRecordRespVO respVO = new DccPaperDistributionRecordRespVO();
        respVO.setDistributionId(distribution.getId());
        respVO.setControlledFileId(file.getId());
        respVO.setFileNumber(file.getFileNumber());
        respVO.setFileName(StrUtil.blankToDefault(file.getFileName(), file.getTitle()));
        respVO.setVersionNo(file.getVersionNo());
        respVO.setIssuerUserId(distribution.getAcknowledgedBy());
        respVO.setIssuerName(getUserName(userMap, distribution.getAcknowledgedBy()));
        respVO.setRecipientUserIds(recipients.stream()
                .map(DccControlledFileDistributionRecipientDO::getUserId)
                .toList());
        respVO.setRecipientNames(respVO.getRecipientUserIds().stream()
                .map(userId -> getUserName(userMap, userId))
                .toList());
        respVO.setIssuedAt(distribution.getAcknowledgedAt());
        respVO.setRecovererUserId(distribution.getRecoveredBy());
        respVO.setRecovererName(getUserName(userMap, distribution.getRecoveredBy()));
        respVO.setRecoveredAt(distribution.getRecoveredAt());
        respVO.setStatus(distribution.getStatus());
        return respVO;
    }

    private static String getUserName(Map<Long, AdminUserRespDTO> userMap, Long userId) {
        if (userId == null) {
            return null;
        }
        return userMap.get(userId).getNickname();
    }
}
