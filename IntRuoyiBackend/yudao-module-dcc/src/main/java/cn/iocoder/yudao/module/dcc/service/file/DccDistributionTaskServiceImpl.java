package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionTaskRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccDistributionRecipientReceiptRespVO;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMessageJobMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileDistributionRecipientMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccDistributionMediumEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.ArrayList;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;

@Service
@Validated
public class DccDistributionTaskServiceImpl implements DccDistributionTaskService {

    static final String STATUS_READY_TO_ACKNOWLEDGE = "READY_TO_ACKNOWLEDGE";

    private static final Set<String> DISTRIBUTION_VISIBLE_FILE_STATUSES = Set.of(
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
    @Resource private DccControlledFileMessageJobMapper messageJobMapper;
    @Resource private DccControlledFileSignatureMapper signatureMapper;
    @Resource private PermissionApi permissionApi;
    @Resource private AdminUserApi adminUserApi;
    @Resource private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.file.DccDistributionBatchMapper batchMapper;
    @Resource private DccDistributionBatchService batchService;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccessService;

    private void requireDistributionScope(Long userId,DccControlledFileDO file) {
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(userId==null || TenantContextHolder.isIgnore() || !Objects.equals(file.getTenantId(),tenant)
                || !permissionApi.hasAnyRoles(userId,"doc_control") || !permissionApi.hasAnyPermissions(userId,"dcc:controlled-file:distribute")
                || !permissionSupport.hasCategoryPermission(file.getCategoryId(),userId,DccFileCategoryPermissionActionEnum.DISTRIBUTE))
            throw new IllegalArgumentException("无本版本文控下发功能或类别资格");
        var user=adminUserApi.getUser(userId);
        if(user==null || !Objects.equals(user.getId(),userId) || !Objects.equals(user.getTenantId(),tenant) || !Integer.valueOf(0).equals(user.getStatus()))
            throw new IllegalArgumentException("下发办理需要本租户当前启用账号");
        if(file.getDccProjectCodeId()==null)throw new IllegalArgumentException("本版本缺少正式项目身份，无法判断下发范围");
        projectAccessService.assertProjectViewerOrAbove(userId,file.getDccProjectCodeId());
        if(!projectAccessService.listReadableProjectIds(userId).contains(file.getDccProjectCodeId()))throw new IllegalArgumentException("本版本项目不在正式可读硬范围");
    }

    @Override
    @cn.iocoder.yudao.module.system.service.gxpaudit.GxpWriteOperation(operationId="dcc.distribution.batch.retry")
    public cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionResultRespVO retryAutomaticDistribution(
            Long userId,cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionRetryReqVO request) {
        if(request==null || request.getControlledFileId()==null || request.getControlledFileId()<=0 || request.getBatchId()==null || request.getBatchId()<=0
                || request.getVersionNo()==null || request.getVersionNo().isBlank() || request.getApprovalProcessInstanceId()==null || request.getApprovalProcessInstanceId().isBlank())
            throw new IllegalArgumentException("自动下发重试缺少完整精确版本批次身份");
        var login=cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUser();
        if(login==null || !Objects.equals(login.getId(),userId) || !Objects.equals(login.getTenantId(),TenantContextHolder.getRequiredTenantId()) || !Integer.valueOf(2).equals(login.getUserType()))
            throw new IllegalArgumentException("自动下发重试需要真实登录本租户文控账号");
        var file=controlledFileMapper.selectById(request.getControlledFileId());
        if(file==null)throw exception(CONTROLLED_FILE_NOT_EXISTS);
        requireDistributionScope(userId,file);
        var batch=batchMapper.selectById(request.getBatchId());
        if(batch==null || !Objects.equals(batch.getTenantId(),file.getTenantId()) || !Objects.equals(batch.getControlledFileId(),file.getId())
                || !Objects.equals(batch.getVersionNo(),request.getVersionNo()) || !Objects.equals(file.getVersionNo(),request.getVersionNo())
                || !Objects.equals(batch.getApprovalProcessInstanceId(),request.getApprovalProcessInstanceId()) || !Objects.equals(file.getProcessInstanceId(),request.getApprovalProcessInstanceId())
                || file.getControlledTime()==null || file.getActivatedTime()==null || !DISTRIBUTION_VISIBLE_FILE_STATUSES.contains(file.getStatus()))
            throw new IllegalArgumentException("自动下发重试批次、版本、轮次或正式受控生效事实不匹配");
        batchService.retryAutomaticBatch(batch.getId(),userId);
        return getAutomaticDistributionResult(userId,file.getId());
    }

    @Override
    public cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionResultRespVO getAutomaticDistributionResult(Long userId,Long fileId) {
        var file=controlledFileMapper.selectById(fileId);
        if(file==null)throw exception(CONTROLLED_FILE_NOT_EXISTS);
        Long tenant=TenantContextHolder.getRequiredTenantId();
        requireDistributionScope(userId,file);
        var response=new cn.iocoder.yudao.module.dcc.controller.admin.distribution.vo.DccAutomaticDistributionResultRespVO();
        response.setControlledFileId(fileId);response.setVersionNo(file.getVersionNo());response.setApprovalProcessInstanceId(file.getProcessInstanceId());
        var batch=batchMapper.selectVersion(tenant,fileId,file.getProcessInstanceId());
        if(batch==null){response.setStatus("NOT_CREATED");return response;}
        if(!Objects.equals(batch.getControlledFileId(),fileId) || !Objects.equals(batch.getVersionNo(),file.getVersionNo())
                || !Objects.equals(batch.getApprovalProcessInstanceId(),file.getProcessInstanceId()))throw new IllegalStateException("下发批次与准确版本身份不一致");
        response.setBatchId(batch.getId());response.setEventKey(batch.getEventKey());response.setStatus(batch.getStatus());
        response.setErrorCode(batch.getErrorCode());response.setErrorMessage(batch.getErrorMessage());response.setFailureAt(batch.getFailureAt());
        response.setPreparedAt(batch.getDispatchPreparedAt());response.setSentAt(batch.getSentAt());response.setSnapshotAt(batch.getSnapshotAt());response.setPolicyVersion(batch.getSnapshotPolicyVersion());
        return response;
    }

    @Override
    public List<DccDistributionRecipientReceiptRespVO> getDistributionRecipientReceipts(Long userId, Long fileId) {
        var file = controlledFileMapper.selectById(fileId);
        if (file == null) throw exception(CONTROLLED_FILE_NOT_EXISTS);
        requireDistributionScope(userId,file);
        if (file.getControlledTime() == null || file.getActivatedTime() == null || file.getDistributedTime() == null)
            return List.of();
        List<DccDistributionRecipientReceiptRespVO> rows = new ArrayList<>();
        for (var distribution : distributionMapper.selectListByControlledFileId(fileId)) {
            if (!"PUBLIC_FOLDER".equals(distribution.getDistributionMedium())) continue;
            for (var recipient : distributionRecipientMapper.selectListByDistributionId(distribution.getId())) {
                var job = recipient.getMessageJobId() == null ? null : messageJobMapper.selectById(recipient.getMessageJobId());
                if (job == null || !"DISTRIBUTION".equals(job.getBusinessType())
                        || !Objects.equals(job.getBusinessId(), distribution.getId())
                        || !Objects.equals(job.getRecipientUserId(), recipient.getUserId())
                        || !Set.of("PENDING", "SENT", "FAILED").contains(job.getStatus())
                        || "SENT".equals(job.getStatus()) && job.getSentAt() == null
                        || !"SENT".equals(job.getStatus()) && job.getSentAt() != null)
                    throw new IllegalStateException("下发发送事实与收件义务不一致");
                var row = new DccDistributionRecipientReceiptRespVO();
                row.setControlledFileId(fileId); row.setVersionNo(file.getVersionNo());
                row.setDistributionId(distribution.getId()); row.setRecipientId(recipient.getId());
                row.setDepartmentId(distribution.getDepartmentId()); row.setUserId(recipient.getUserId());
                var user = adminUserApi.getUser(recipient.getUserId());
                if (user != null) row.setCurrentUserName(user.getNickname());
                row.setMessageStatus(job.getStatus()); row.setErrorMessage(job.getErrorMessage());
                row.setSentAt(job.getSentAt()); row.setReadAt(recipient.getReadAt());
                row.setAcknowledgedAt(recipient.getAcknowledgedAt());
                row.setDistributionCompletedAt(distribution.getAcknowledgedAt());
                if (recipient.getAcknowledgedAt() != null) {
                    var signature = signatureMapper.selectActionSignature(fileId,
                            "DISTRIBUTION:" + distribution.getId() + ":" + recipient.getId(), recipient.getUserId(), "DISTRIBUTION_ACK");
                    if (signature == null || signature.getId() == null || !Objects.equals(signature.getControlledFileId(), fileId)
                            || !Objects.equals(signature.getVersionNo(), file.getVersionNo())
                            || !Objects.equals(signature.getActorId(), recipient.getUserId())
                            || !Objects.equals(signature.getSignedAt(), recipient.getAcknowledgedAt()))
                        throw new IllegalStateException("签收记录缺少精确版本的正式签名或时间不一致");
                    row.setSignatureId(signature.getId()); row.setSignedAt(signature.getSignedAt());
                    row.setSignerNameSnapshot(signature.getActorNicknameSnapshot());
                    row.setSignerDepartmentNameSnapshot(signature.getActorDeptNameSnapshot());
                }
                rows.add(row);
            }
        }
        return rows;
    }

    @Override
    public PageResult<DccDistributionTaskRespVO> getMyDistributionTaskPage(Long userId,
                                                                          DccDistributionTaskPageReqVO reqVO) {
        List<DccDistributionTaskRespVO> rows = distributionRecipientMapper.selectListByUserId(userId).stream()
                .filter(recipient -> recipient.getAcknowledgedAt() == null)
                .map(this::buildTaskRowOrNull)
                .filter(Objects::nonNull)
                .filter(row -> matchesTaskFilter(row, reqVO))
                .sorted(Comparator.comparing(
                                DccDistributionTaskRespVO::getPublishedTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(
                                DccDistributionTaskRespVO::getRecipientId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return slicePage(reqVO.getPageNo(), reqVO.getPageSize(), rows);
    }

    private DccDistributionTaskRespVO buildTaskRowOrNull(DccControlledFileDistributionRecipientDO recipient) {
        DccControlledFileDistributionDO distribution = distributionMapper.selectById(recipient.getDistributionId());
        if (distribution == null) {
            return null;
        }
        if (!DccDistributionMediumEnum.PUBLIC_FOLDER.getCode().equals(distribution.getDistributionMedium())
                || !Set.of("PENDING", "SENT", "READ", "ACKNOWLEDGED").contains(distribution.getStatus())
                || recipient.getMessageJobId() == null) {
            return null;
        }
        DccControlledFileDO file = controlledFileMapper.selectById(distribution.getControlledFileId());
        if (file == null) {
            return null;
        }
        if (!DISTRIBUTION_VISIBLE_FILE_STATUSES.contains(file.getStatus()) || file.getControlledTime() == null
                || file.getActivatedTime() == null || file.getDistributedTime() == null) {
            return null;
        }
        DccDistributionTaskRespVO respVO = new DccDistributionTaskRespVO();
        respVO.setRecipientId(recipient.getId());
        respVO.setDistributionId(distribution.getId());
        respVO.setControlledFileId(file.getId());
        respVO.setCategoryId(file.getCategoryId());
        respVO.setFileName(StrUtil.blankToDefault(file.getFileName(), file.getTitle()));
        respVO.setTitle(file.getTitle());
        respVO.setFileNumber(file.getFileNumber());
        respVO.setVersionNo(file.getVersionNo());
        respVO.setFileStatus(file.getStatus());
        respVO.setUserId(recipient.getUserId());
        respVO.setDepartmentId(distribution.getDepartmentId());
        respVO.setDistributionMedium(distribution.getDistributionMedium());
        respVO.setReadAt(recipient.getReadAt());
        respVO.setAcknowledgedAt(recipient.getAcknowledgedAt());
        respVO.setPublishedTime(file.getPublishedTime());
        respVO.setStatus(STATUS_READY_TO_ACKNOWLEDGE);
        return respVO;
    }

    private boolean matchesTaskFilter(DccDistributionTaskRespVO row, DccDistributionTaskPageReqVO reqVO) {
        if (reqVO.getCategoryId() != null && !reqVO.getCategoryId().equals(row.getCategoryId())) {
            return false;
        }
        return StrUtil.isBlank(reqVO.getStatus()) || StrUtil.equals(reqVO.getStatus(), row.getStatus());
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
}
