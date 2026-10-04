package cn.iocoder.yudao.module.dcc.service.projectcode.productcreate;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductIdentityClaimDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductIdentityClaimMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectProductCreateStatusConstants;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_ADMIN_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_CLASSIFICATION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_DUPLICATE_PRODUCT_CODE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_DUPLICATE_PRODUCT_NAME;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_DUPLICATE_PROJECT_CODE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_WRITE_FAILED;

@Service
@Validated
public class DccProjectProductCreateServiceImpl implements DccProjectProductCreateService {

    private static final Set<String> CLASSIFICATIONS = Set.of("一类", "二类", "三类");
    private static final String IDENTITY_PROJECT_CODE = "PROJECT_CODE";
    private static final String IDENTITY_PRODUCT_CODE = "PRODUCT_CODE";
    private static final String IDENTITY_PRODUCT_NAME = "PRODUCT_NAME";

    @Resource
    private DccProjectProductCreateRequestMapper requestMapper;
    @Resource
    private DccProjectProductIdentityClaimMapper identityClaimMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccProductCatalogMapper productCatalogMapper;
    @Resource
    private DccProjectProductCreateFailureService failureService;
    @Resource
    private DccProjectProductCreateStateService stateService;
    @Resource
    private DccProjectProductCreateWriteService writeService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectLeaderService leaderService;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributesService attributesService;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService folderTemplateService;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectConfigurationAuditService auditSupport;
    @Resource private cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService auditService;
    @Resource private DccProjectProductAuditService productAudit;
    @Resource private DccProjectReviewerConfigurationService reviewerConfiguration;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRequest(Long applicantUserId, DccProjectProductCreateReqVO reqVO) {
        if(reqVO==null) throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        productAudit.validateReason(reqVO.getCreationReason());
        var before=productAudit.beforeCreate(reqVO.getFolderTemplateId());
        Long id=createPendingRequest(applicantUserId,reqVO,reqVO.getCreationReason().trim(),null);
        productAudit.append("dcc.project-product.create",id,"1",reqVO.getCreationReason(),before);
        return id;
    }

    private Long createPendingRequest(Long applicantUserId,DccProjectProductCreateReqVO reqVO,String creationReason,Long previousRequestId) {
        if(applicantUserId==null || applicantUserId<=0) throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        var reviewer=reviewerConfiguration.requireConfigured();
        String projectName = required(reqVO.getProjectName());
        String projectCode = required(reqVO.getProjectCode());
        var leader = leaderService.requireEnabledAccount(reqVO.getProjectLeaderUserId());
        String projectLeader = required(leader.getNickname());
        String defaultsJson = attributesService.encode(reqVO.getDefaultAttributes());
        String productCode = required(reqVO.getProductCode());
        String productName = required(reqVO.getProductName());
        String classification = required(reqVO.getClassification());
        validateClassification(classification);
        validateUnique(projectCode, productCode, productName, null);
        String folderSnapshot = folderTemplateService.captureForRequest(reqVO.getFolderTemplateId());
        DccProjectProductCreateRequestDO request = DccProjectProductCreateRequestDO.builder()
                .projectName(projectName)
                .projectCode(projectCode)
                .projectLeader(projectLeader)
                .projectLeaderUserId(reqVO.getProjectLeaderUserId())
                .defaultAttributesJson(defaultsJson)
                .folderTemplateId(reqVO.getFolderTemplateId())
                .folderTemplateSnapshotJson(folderSnapshot)
                .productCode(productCode)
                .productName(productName)
                .classification(classification)
                .remark(StrUtil.trimToNull(reqVO.getRemark()))
                .creationReason(creationReason).previousRequestId(previousRequestId)
                .status(DccProjectProductCreateStatusConstants.PENDING_REVIEW)
                .applicantUserId(applicantUserId)
                .configuredReviewerUserId(reviewer.getId())
                .configuredReviewerUsername(reviewer.getUsername())
                .configuredReviewerNickname(reviewer.getNickname())
                .submittedTime(LocalDateTime.now())
                .build();
        request.setTenantId(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
        if (requestMapper.insert(request) != 1 || request.getId() == null) {
            throw exception(cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.WRITE_INCOMPLETE,
                    "联合申请提交");
        }
        claimIdentity(request.getId(), IDENTITY_PROJECT_CODE, projectCode);
        claimIdentity(request.getId(), IDENTITY_PRODUCT_CODE, productCode);
        claimIdentity(request.getId(), IDENTITY_PRODUCT_NAME, productName);
        return request.getId();
    }

    @Override
    public List<DccProjectProductCreateRequestDO> getPendingRequests() {
        var rows = requestMapper.selectPendingList();
        var successors = new java.util.HashMap<Long, Long>();
        for (var next : requestMapper.selectByPreviousRequestIds(rows.stream().map(DccProjectProductCreateRequestDO::getId).toList())) {
            if (successors.put(next.getPreviousRequestId(), next.getId()) != null) {
                throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
            }
        }
        rows.forEach(row -> row.setResubmittedRequestId(successors.get(row.getId())));
        return rows;
    }

    /** 原驳回记录保持历史；锁定前驱后生成一个重新进入审核的新申请。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long resubmitRejectedRequest(Long applicantUserId, Long rejectedRequestId,
                                       DccProjectProductCreateReqVO reqVO) {
        DccProjectProductCreateRequestDO previous = requireRequestForUpdate(rejectedRequestId);
        if (applicantUserId == null || applicantUserId <= 0
                || !java.util.Objects.equals(previous.getApplicantUserId(), applicantUserId)
                || !DccProjectProductCreateStatusConstants.REJECTED.equals(previous.getStatus())
                || requestMapper.selectCount(DccProjectProductCreateRequestDO::getPreviousRequestId, rejectedRequestId) > 0) {
            throw cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.fail(
                    cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.REJECTED_REQUEST_REWORK_INVALID);
        }
        if (reqVO == null) throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        auditSupport.validateReason(reqVO.getResubmissionReason());
        // 已确定的新建校验/账号/模板/身份占用均复用正式创建路径；失败与前驱关联同事务回滚。
        Long id = createPendingRequest(applicantUserId, reqVO, null, rejectedRequestId);
        var next = requestMapper.selectById(id);
        if (next == null) throw exception(DCC_PROJECT_PRODUCT_CREATE_NOT_EXISTS);
        next.setPreviousRequestId(rejectedRequestId);
        if (requestMapper.updateById(next) != 1) throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        var payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("request", requestMapper.selectById(id));
        payload.put("rejectedPredecessor", previous);
        auditService.append(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.builder()
                .operationId("dcc.project-product.resubmit").subjectId("DCC_PROJECT_PRODUCT_REQUEST:" + id)
                .subjectVersion("1").reason(reqVO.getResubmissionReason().trim())
                .beforeState(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope.builder()
                        .state("ABSENT").canonicalJson("{}").build())
                .afterState(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditStateEnvelope.builder()
                        .state("PRESENT").objectVersion("1")
                        .canonicalJson(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(payload)).build())
                .idempotencyKey("DCC:PROJECT_PRODUCT:RESUBMIT:" + id)
                .source("DccProjectProductCreateServiceImpl.resubmitRejectedRequest").build());
        return id;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccProjectProductCreateRequestDO review(Long operatorUserId, Long requestId,
                                                   String reason, boolean approve) {
        DccProjectProductCreateRequestDO request = requireRequestForUpdate(requestId);
        reviewerConfiguration.assertFrozenReviewer(operatorUserId,request);
        if (!DccProjectProductCreateStatusConstants.PENDING_REVIEW.equals(request.getStatus())) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        }
        String normalizedReason = required(reason);
        productAudit.validateReason(normalizedReason);
        var before=productAudit.snapshot(requestId);
        request.setReviewerUserId(operatorUserId);
        request.setReviewedTime(LocalDateTime.now());
        if (approve) {
            validateUnique(request.getProjectCode(), request.getProductCode(), request.getProductName(),
                    request.getId());
            request.setReviewReason(normalizedReason);
            request.setStatus(DccProjectProductCreateStatusConstants.PENDING_APPROVAL);
        } else {
            request.setRejectReason(normalizedReason);
            request.setStatus(DccProjectProductCreateStatusConstants.REJECTED);
            identityClaimMapper.deleteByRequestId(request.getId());
        }
        if (requestMapper.updateById(request) != 1) {
            throw exception(cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.WRITE_INCOMPLETE,
                    "审核决定");
        }
        productAudit.append("dcc.project-product.review",requestId,"1",normalizedReason,before);
        return request;
    }

    @Override
    public DccProjectProductCreateRequestDO approve(Long operatorUserId, Long requestId,
                                                    String reason, boolean approve) {
        requireAdmin(operatorUserId);
        DccProjectProductCreateRequestDO request = stateService.markApprovalDecision(operatorUserId, requestId,
                reason, approve);
        if (!approve) {
            return request;
        }
        return writeOrMarkFailed(request.getId());
    }

    @Override
    public DccProjectProductCreateRequestDO retryWrite(Long operatorUserId, Long requestId, String reason) {
        requireAdmin(operatorUserId);
        DccProjectProductCreateRequestDO request = stateService.markRetryWriting(operatorUserId, requestId, reason);
        return writeOrMarkFailed(request.getId());
    }

    private DccProjectProductCreateRequestDO writeOrMarkFailed(Long requestId) {
        try {
            return writeService.writeApprovedRequest(requestId);
        } catch (RuntimeException ex) {
            failureService.markWriteFailed(requestId, DCC_PROJECT_PRODUCT_CREATE_WRITE_FAILED.getCode().toString(),
                    ex.getMessage());
            throw ex;
        }
    }

    private DccProjectProductCreateRequestDO requireRequestForUpdate(Long requestId) {
        DccProjectProductCreateRequestDO request = requestMapper.selectByIdForUpdate(requestId);
        if (request == null || !java.util.Objects.equals(request.getTenantId(),
                cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_NOT_EXISTS);
        }
        return request;
    }

    private void requireAdmin(Long userId) {
        AdminUserRespDTO user = userId == null ? null : adminUserApi.getUser(userId);
        if (user == null || !"admin".equals(user.getUsername())) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_ADMIN_REQUIRED);
        }
    }

    private void validateUnique(String projectCode, String productCode, String productName, Long requestId) {
        if (projectCodeMapper.selectByNormalizedProjectCode(projectCode) != null
                || claimedByOtherRequest(IDENTITY_PROJECT_CODE, projectCode, requestId)
                || activeProjectRequest(projectCode, requestId)) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_DUPLICATE_PROJECT_CODE);
        }
        if (productCatalogMapper.selectByNormalizedProductCode(productCode) != null
                || claimedByOtherRequest(IDENTITY_PRODUCT_CODE, productCode, requestId)
                || activeProductCodeRequest(productCode, requestId)) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_DUPLICATE_PRODUCT_CODE);
        }
        if (productCatalogMapper.selectByNormalizedProductName(productName) != null
                || claimedByOtherRequest(IDENTITY_PRODUCT_NAME, productName, requestId)
                || activeProductNameRequest(productName, requestId)) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_DUPLICATE_PRODUCT_NAME);
        }
    }

    private boolean claimedByOtherRequest(String identityType, String identityValue, Long requestId) {
        DccProjectProductIdentityClaimDO claim = identityClaimMapper.selectByTypeAndValue(identityType,
                normalizeIdentity(identityValue));
        return claim != null && !claim.getRequestId().equals(requestId);
    }

    private boolean activeProjectRequest(String value, Long requestId) {
        DccProjectProductCreateRequestDO existing = requestMapper.selectActiveByProjectCode(value);
        return existing != null && !existing.getId().equals(requestId);
    }

    private boolean activeProductCodeRequest(String value, Long requestId) {
        DccProjectProductCreateRequestDO existing = requestMapper.selectActiveByProductCode(value);
        return existing != null && !existing.getId().equals(requestId);
    }

    private boolean activeProductNameRequest(String value, Long requestId) {
        DccProjectProductCreateRequestDO existing = requestMapper.selectActiveByProductName(value);
        return existing != null && !existing.getId().equals(requestId);
    }

    private void validateClassification(String classification) {
        if (!CLASSIFICATIONS.contains(classification)) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_CLASSIFICATION_INVALID);
        }
    }

    private String required(String value) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        }
        return normalized;
    }

    private void claimIdentity(Long requestId, String identityType, String identityValue) {
        var claim = DccProjectProductIdentityClaimDO.builder()
                .requestId(requestId)
                .identityType(identityType)
                .identityValue(normalizeIdentity(identityValue))
                .build();
        claim.setTenantId(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
        if (identityClaimMapper.insert(claim) != 1 || claim.getId() == null) {
            throw exception(cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.WRITE_INCOMPLETE,
                    "申请身份保护");
        }
    }

    private String normalizeIdentity(String value) {
        return StrUtil.trimToEmpty(value);
    }
}
