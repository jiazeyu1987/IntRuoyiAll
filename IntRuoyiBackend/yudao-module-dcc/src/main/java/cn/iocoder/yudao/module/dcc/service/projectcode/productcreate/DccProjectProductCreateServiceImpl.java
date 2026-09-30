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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRequest(Long applicantUserId, DccProjectProductCreateReqVO reqVO) {
        String projectName = required(reqVO.getProjectName());
        String projectCode = required(reqVO.getProjectCode());
        String projectLeader = required(reqVO.getProjectLeader());
        String productCode = required(reqVO.getProductCode());
        String productName = required(reqVO.getProductName());
        String classification = required(reqVO.getClassification());
        validateClassification(classification);
        validateUnique(projectCode, productCode, productName, null);
        DccProjectProductCreateRequestDO request = DccProjectProductCreateRequestDO.builder()
                .projectName(projectName)
                .projectCode(projectCode)
                .projectLeader(projectLeader)
                .productCode(productCode)
                .productName(productName)
                .classification(classification)
                .remark(StrUtil.trimToNull(reqVO.getRemark()))
                .status(DccProjectProductCreateStatusConstants.PENDING_REVIEW)
                .applicantUserId(applicantUserId)
                .submittedTime(LocalDateTime.now())
                .build();
        requestMapper.insert(request);
        claimIdentity(request.getId(), IDENTITY_PROJECT_CODE, projectCode);
        claimIdentity(request.getId(), IDENTITY_PRODUCT_CODE, productCode);
        claimIdentity(request.getId(), IDENTITY_PRODUCT_NAME, productName);
        return request.getId();
    }

    @Override
    public List<DccProjectProductCreateRequestDO> getPendingRequests() {
        return requestMapper.selectPendingList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccProjectProductCreateRequestDO review(Long operatorUserId, Long requestId,
                                                   String reason, boolean approve) {
        requireAdmin(operatorUserId);
        DccProjectProductCreateRequestDO request = requireRequestForUpdate(requestId);
        if (!DccProjectProductCreateStatusConstants.PENDING_REVIEW.equals(request.getStatus())) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        }
        String normalizedReason = required(reason);
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
        requestMapper.updateById(request);
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
    public DccProjectProductCreateRequestDO retryWrite(Long operatorUserId, Long requestId) {
        requireAdmin(operatorUserId);
        DccProjectProductCreateRequestDO request = stateService.markRetryWriting(requestId);
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
        if (request == null) {
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
        identityClaimMapper.insert(DccProjectProductIdentityClaimDO.builder()
                .requestId(requestId)
                .identityType(identityType)
                .identityValue(normalizeIdentity(identityValue))
                .build());
    }

    private String normalizeIdentity(String value) {
        return StrUtil.trimToEmpty(value);
    }
}
