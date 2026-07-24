package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
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

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;

@Service
@Validated
public class DccDistributionReceiptServiceImpl implements DccDistributionReceiptService {

    private static final String ACTION_TYPE_DISTRIBUTION_ACK = "DISTRIBUTION_ACK";
    private static final String ACTION_TYPE_DISTRIBUTION_SIGN = "DISTRIBUTION_SIGN";
    private static final String SIGNATURE_STAGE_DISTRIBUTION = "DISTRIBUTION";
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
    private DccControlledFileMessageJobMapper messageJobMapper;
    @Resource
    private DccControlledFileMessageDeliveryService messageDeliveryService;
    @Resource
    private DccSignatureVerificationService signatureVerificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acknowledgeElectronicDistribution(Long userId, Long controlledFileId, Long distributionId,
                                                  Long recipientId,
                                                  DccControlledFileDistributionRecipientAckReqVO reqVO) {
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!ACK_ALLOWED_FILE_STATUSES.contains(file.getStatus())) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        }
        DccControlledFileDistributionDO distribution = distributionMapper.selectById(distributionId);
        if (distribution == null
                || !controlledFileId.equals(distribution.getControlledFileId())
                || !DccDistributionMediumEnum.PUBLIC_FOLDER.getCode().equals(distribution.getDistributionMedium())) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        }
        DccControlledFileDistributionRecipientDO recipient = distributionRecipientMapper.selectById(recipientId);
        if (recipient == null
                || !distributionId.equals(recipient.getDistributionId())
                || !userId.equals(recipient.getUserId())) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        }
        if (recipient.getAcknowledgedAt() != null) {
            return;
        }

        String comment = StrUtil.trimToEmpty(reqVO.getComment());
        String signatureTaskId = buildDistributionSignatureTaskId(distributionId, recipientId);
        signatureVerificationService.verifyPasswordAndCreateSignature(userId, controlledFileId,
                signatureTaskId, SIGNATURE_STAGE_DISTRIBUTION, ACTION_TYPE_DISTRIBUTION_ACK,
                reqVO.getPassword(), comment);

        LocalDateTime acknowledgedAt = LocalDateTime.now();
        distributionRecipientMapper.updateById(DccControlledFileDistributionRecipientDO.builder()
                .id(recipientId)
                .readAt(recipient.getReadAt() != null ? recipient.getReadAt() : acknowledgedAt)
                .acknowledgedAt(acknowledgedAt)
                .ackComment(comment)
                .build());
        if (isAllRecipientsAcknowledged(distributionId, recipientId)) {
            distributionMapper.updateById(DccControlledFileDistributionDO.builder()
                    .id(distributionId)
                    .status(DccControlledFileDistributionStatusEnum.ACKNOWLEDGED.getCode())
                    .acknowledgedBy(userId)
                    .acknowledgedAt(acknowledgedAt)
                    .build());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createDistributionRecipientSign(Long userId, Long controlledFileId, Long distributionId,
                                                Long recipientId,
                                                DccControlledFileDistributionRecipientSignReqVO reqVO) {
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!ACK_ALLOWED_FILE_STATUSES.contains(file.getStatus())) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        }
        DccControlledFileDistributionDO distribution = distributionMapper.selectById(distributionId);
        if (distribution == null
                || !controlledFileId.equals(distribution.getControlledFileId())
                || !DccDistributionMediumEnum.PUBLIC_FOLDER.getCode().equals(distribution.getDistributionMedium())
                || DccControlledFileDistributionStatusEnum.RECOVERED.getCode().equals(distribution.getStatus())) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        }
        DccControlledFileDistributionRecipientDO currentRecipient = distributionRecipientMapper.selectById(recipientId);
        if (currentRecipient == null
                || !distributionId.equals(currentRecipient.getDistributionId())
                || !userId.equals(currentRecipient.getUserId())) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        }

        List<Long> signUserIds = reqVO.getUserIds().stream()
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        List::copyOf));
        if (signUserIds.isEmpty()) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        }
        Set<Long> existingUserIds = distributionRecipientMapper.selectListByDistributionId(distributionId).stream()
                .map(DccControlledFileDistributionRecipientDO::getUserId)
                .collect(java.util.stream.Collectors.toSet());
        if (signUserIds.stream().anyMatch(existingUserIds::contains)) {
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        }

        String comment = StrUtil.trimToEmpty(reqVO.getComment());
        signatureVerificationService.verifyPasswordAndCreateSignature(userId, controlledFileId,
                buildDistributionSignTaskId(distributionId, recipientId), SIGNATURE_STAGE_DISTRIBUTION,
                ACTION_TYPE_DISTRIBUTION_SIGN,
                reqVO.getPassword(), comment);

        signUserIds.forEach(signUserId -> {
            DccControlledFileMessageJobDO messageJob = createDistributionMessageJob(file, distributionId, signUserId);
            distributionRecipientMapper.insert(DccControlledFileDistributionRecipientDO.builder()
                    .distributionId(distributionId)
                    .userId(signUserId)
                    .messageJobId(messageJob.getId())
                    .build());
        });
        if (DccControlledFileDistributionStatusEnum.ACKNOWLEDGED.getCode().equals(distribution.getStatus())) {
            distributionMapper.updateById(DccControlledFileDistributionDO.builder()
                    .id(distributionId)
                    .status(DccControlledFileDistributionStatusEnum.SENT.getCode())
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

    private boolean isAllRecipientsAcknowledged(Long distributionId, Long currentRecipientId) {
        List<DccControlledFileDistributionRecipientDO> recipients =
                distributionRecipientMapper.selectListByDistributionId(distributionId);
        return !recipients.isEmpty() && recipients.stream()
                .allMatch(item -> currentRecipientId.equals(item.getId()) || item.getAcknowledgedAt() != null);
    }

    private String buildDistributionSignatureTaskId(Long distributionId, Long recipientId) {
        return "DISTRIBUTION:" + distributionId + ":" + recipientId;
    }

    private String buildDistributionSignTaskId(Long distributionId, Long recipientId) {
        return "DISTRIBUTION_SIGN:" + distributionId + ":" + recipientId;
    }

}
