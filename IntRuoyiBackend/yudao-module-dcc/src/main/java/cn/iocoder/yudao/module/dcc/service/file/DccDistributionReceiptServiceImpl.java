package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionRecipientAckReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileDistributionRecipientSignReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;

@Service
@Validated
public class DccDistributionReceiptServiceImpl implements DccDistributionReceiptService {
    private static final Set<String> RECEIPT_FILE_STATUSES = Set.of("ACTIVE", "SUPERSEDED", "OBSOLETE");
    private static final Set<String> RECEIPT_DISTRIBUTION_STATUSES = Set.of("PENDING", "SENT", "READ", "ACKNOWLEDGED");
    @Resource private DccControlledFileMapper controlledFileMapper;
    @Resource private DccControlledFileDistributionMapper distributionMapper;
    @Resource private DccControlledFileDistributionRecipientMapper distributionRecipientMapper;
    @Resource private DccSignatureVerificationService signatureVerificationService;
    @Resource private DccControlledFileSignatureBindingService signatureBindingService;
    @Resource private GxpAuditService gxpAuditService;
    @Resource private cn.iocoder.yudao.module.system.api.user.AdminUserApi adminUserApi;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper messageJobMapper;

    @Override
    @Transactional(rollbackFor=Exception.class)
    @GxpWriteOperation(operationId="dcc.distribution.recipient.body-opened")
    public void recordDistributionBodyOpened(Long userId,Long controlledFileId,Long distributionId,Long recipientId) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(userId==null || controlledFileId==null || distributionId==null || recipientId==null)
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        var login=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        var currentUser=adminUserApi.getUser(userId);
        if(TenantContextHolder.isIgnore() || login==null || !Objects.equals(login.getId(),userId) || !Objects.equals(login.getTenantId(),tenant) || !Objects.equals(login.getUserType(),2)
                || currentUser==null || !Objects.equals(currentUser.getId(),userId) || !Objects.equals(currentUser.getTenantId(),tenant)
                || !Integer.valueOf(0).equals(currentUser.getStatus()))throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        var file=controlledFileMapper.selectByIdAndTenantForUpdate(tenant,controlledFileId);
        if(file==null)throw exception(CONTROLLED_FILE_NOT_EXISTS);
        var scope=distributionMapper.selectByIdAndTenantForUpdate(tenant,distributionId);
        var recipient=distributionRecipientMapper.selectByIdAndTenantForUpdate(tenant,recipientId);
        if(!Objects.equals(file.getTenantId(),tenant) || file.getControlledTime()==null || file.getActivatedTime()==null || file.getDistributedTime()==null
                || !RECEIPT_FILE_STATUSES.contains(file.getStatus()) || scope==null || !Objects.equals(scope.getControlledFileId(),controlledFileId)
                || !"PUBLIC_FOLDER".equals(scope.getDistributionMedium()) || !RECEIPT_DISTRIBUTION_STATUSES.contains(scope.getStatus())
                || recipient==null || !Objects.equals(recipient.getDistributionId(),distributionId) || !Objects.equals(recipient.getUserId(),userId))
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        var job=recipient.getMessageJobId()==null?null:messageJobMapper.selectByIdAndTenantForUpdate(tenant,recipient.getMessageJobId());
        if(job==null || !"SENT".equals(job.getStatus()) || job.getSentAt()==null || !"DISTRIBUTION".equals(job.getBusinessType())
                || !Objects.equals(job.getBusinessId(),distributionId) || !Objects.equals(job.getRecipientUserId(),userId))
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        if(recipient.getReadAt()!=null)return;
        var openedAt=LocalDateTime.now().withNano(0);
        if(distributionRecipientMapper.updateById(DccControlledFileDistributionRecipientDO.builder().id(recipientId).readAt(openedAt).build())!=1)
            throw new IllegalStateException("真实正文打开事实未持久化");
        var before=new java.util.TreeMap<String,Object>();before.put("controlledFileId",controlledFileId);before.put("versionNo",file.getVersionNo());
        before.put("distributionId",distributionId);before.put("recipientId",recipientId);before.put("userId",userId);before.put("readAt",null);
        var after=new java.util.TreeMap<String,Object>(before);after.put("readAt",openedAt);
        // Keep the real authenticated recipient context; automatic SYSTEM_ACTOR helper is intentionally not used.
        gxpAuditService.append(GxpAuditCommand.builder().operationId("dcc.distribution.recipient.body-opened")
                .subjectId("DCC_DISTRIBUTION_RECIPIENT:"+tenant+":"+recipientId).subjectVersion(file.getVersionNo())
                .reason("已授权并成功读取本次精确下发版本正文")
                .beforeState(GxpAuditStateEnvelope.builder().state("UNOPENED").objectVersion(file.getVersionNo()).canonicalJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(before)).build())
                .afterState(GxpAuditStateEnvelope.builder().state("BODY_OPENED").objectVersion(file.getVersionNo()).canonicalJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(after)).build())
                .idempotencyKey("DCC_BODY_OPEN:"+tenant+":"+recipientId).source("DccDistributionReceiptServiceImpl.recordDistributionBodyOpened").build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acknowledgeElectronicDistribution(Long userId, Long controlledFileId, Long distributionId,
            Long recipientId, DccControlledFileDistributionRecipientAckReqVO request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (userId == null || controlledFileId == null || distributionId == null || recipientId == null)
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        // Serialize all recipients beneath the same exact version; aggregate with current locking reads.
        DccControlledFileDO file = controlledFileMapper.selectByIdAndTenantForUpdate(tenantId, controlledFileId);
        if (file == null) throw exception(CONTROLLED_FILE_NOT_EXISTS);
        if (!Objects.equals(file.getTenantId(), tenantId) || !RECEIPT_FILE_STATUSES.contains(file.getStatus())
                || file.getControlledTime() == null || file.getActivatedTime() == null || file.getDistributedTime() == null)
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        DccControlledFileDistributionDO distribution = distributionMapper.selectByIdAndTenantForUpdate(tenantId, distributionId);
        if (distribution == null || !Objects.equals(controlledFileId, distribution.getControlledFileId())
                || !"PUBLIC_FOLDER".equals(distribution.getDistributionMedium())
                || !RECEIPT_DISTRIBUTION_STATUSES.contains(distribution.getStatus()))
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        DccControlledFileDistributionRecipientDO recipient = distributionRecipientMapper.selectByIdAndTenantForUpdate(tenantId, recipientId);
        if (recipient == null || !Objects.equals(distributionId, recipient.getDistributionId())
                || !Objects.equals(userId, recipient.getUserId()))
            throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
        if (recipient.getAcknowledgedAt() != null) return;
        if (request == null || StrUtil.isBlank(request.getPassword())
                || request.getComment() != null && request.getComment().length() > 1000)
            throw new IllegalArgumentException("签收密码必填，可选意见不得超过1000字");
        String comment = request.getComment() == null ? null : request.getComment().trim();
        DccUnifiedSignatureResult signature = signatureVerificationService.verifyPasswordAndCreateSignature(
                userId, controlledFileId, "DISTRIBUTION:" + distributionId + ":" + recipientId,
                "DISTRIBUTION", "DISTRIBUTION_ACK", request.getPassword(), comment);
        if (signature == null || signature.getSignatureId() == null || signature.getSignedAt() == null
                || !Objects.equals(controlledFileId, signature.getControlledFileId())
                || !Objects.equals(file.getVersionNo(), signature.getVersionNo()))
            throw new IllegalStateException("签收缺少本次精确文件版本的正式签名证据");
        signatureBindingService.bindElectronicReceipt(file, signature.getSignatureId(), userId, distributionId, recipientId);
        LocalDateTime acknowledgedAt = signature.getSignedAt();
        // Opening is a separate fact. A signature must never fabricate readAt.
        if (distributionRecipientMapper.updateById(DccControlledFileDistributionRecipientDO.builder()
                .id(recipientId).acknowledgedAt(acknowledgedAt).ackComment(comment).build()) != 1)
            throw new IllegalStateException("签收回执未持久化");
        var recipients = distributionRecipientMapper.selectListByDistributionIdForUpdate(tenantId, distributionId);
        if (recipients == null || recipients.isEmpty() || recipients.stream().anyMatch(Objects::isNull)
                || recipients.stream().noneMatch(row -> Objects.equals(row.getId(), recipientId)))
            throw new IllegalStateException("签收汇总缺少正式收件名单");
        boolean complete = recipients.stream().allMatch(row -> Objects.equals(row.getId(), recipientId) || row.getAcknowledgedAt() != null);
        if (complete) {
            LocalDateTime completedAt = recipients.stream().map(DccControlledFileDistributionRecipientDO::getAcknowledgedAt)
                    .filter(Objects::nonNull).max(LocalDateTime::compareTo).filter(time -> time.isAfter(acknowledgedAt))
                    .orElse(acknowledgedAt);
            if (distributionMapper.updateById(DccControlledFileDistributionDO.builder().id(distributionId)
                    .status("ACKNOWLEDGED").acknowledgedBy(userId).acknowledgedAt(completedAt).build()) != 1)
                throw new IllegalStateException("下发签收完成状态未持久化");
        }
    }

    @Override
    public void createDistributionRecipientSign(Long userId, Long controlledFileId, Long distributionId,
            Long recipientId, DccControlledFileDistributionRecipientSignReqVO request) {
        // Receipt authority cannot alter the frozen audience or grant another user's content access.
        throw exception(CONTROLLED_FILE_DISTRIBUTION_RECEIPT_NOT_ALLOWED);
    }
}
