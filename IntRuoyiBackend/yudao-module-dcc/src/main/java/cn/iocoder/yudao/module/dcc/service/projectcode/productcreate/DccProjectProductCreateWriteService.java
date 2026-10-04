package cn.iocoder.yudao.module.dcc.service.projectcode.productcreate;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog.DccProductCatalogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductCreateRequestDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductIdentityClaimDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectProductRelationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductIdentityClaimMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductRelationMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.enums.DccProjectProductCreateStatusConstants;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_DUPLICATE_PRODUCT_CODE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_DUPLICATE_PRODUCT_NAME;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_DUPLICATE_PROJECT_CODE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID;
import static cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributeErrors.WRITE_INCOMPLETE;

@Service
public class DccProjectProductCreateWriteService {

    private static final String DATA_SOURCE = "瑛泰产品";
    private static final String RELATION_ACTIVE = "ACTIVE";
    private static final String IDENTITY_PROJECT_CODE = "PROJECT_CODE";
    private static final String IDENTITY_PRODUCT_CODE = "PRODUCT_CODE";
    private static final String IDENTITY_PRODUCT_NAME = "PRODUCT_NAME";

    @Resource
    private DccProjectProductCreateRequestMapper requestMapper;
    @Resource
    private DccProjectProductRelationMapper relationMapper;
    @Resource
    private DccProjectProductIdentityClaimMapper identityClaimMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccProductCatalogMapper productCatalogMapper;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectLeaderService leaderService;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributesService attributesService;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService folderTemplateService;
    @Resource private DccProjectProductAuditService productAudit;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccessService;

    @Transactional(rollbackFor = Exception.class)
    public DccProjectProductCreateRequestDO writeApprovedRequest(Long requestId) {
        DccProjectProductCreateRequestDO request = requestMapper.selectByIdForUpdate(requestId);
        if (request == null || !java.util.Objects.equals(request.getTenantId(), cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_NOT_EXISTS);
        }
        if (!DccProjectProductCreateStatusConstants.WRITING.equals(request.getStatus())) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        }
        validateUnique(request.getProjectCode(), request.getProductCode(), request.getProductName(), request.getId());
        leaderService.requireEnabledAccount(request.getProjectLeaderUserId());
        attributesService.readValue(request.getDefaultAttributesJson());
        folderTemplateService.parse(request.getFolderTemplateSnapshotJson());
        Integer maxRowNo = productCatalogMapper.selectMaxOriginalRowNo(DATA_SOURCE);
        DccProjectCodeDO projectCode = DccProjectCodeDO.builder()
                .projectName(request.getProjectName())
                .projectCode(request.getProjectCode())
                .projectLeader(request.getProjectLeader())
                .projectLeaderUserId(request.getProjectLeaderUserId())
                .defaultAttributesJson(request.getDefaultAttributesJson())
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build();
        projectCode.setTenantId(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
        if (projectCodeMapper.insert(projectCode) != 1 || projectCode.getId() == null) {
            throw exception(WRITE_INCOMPLETE, "项目代码");
        }
        projectAccessService.initializeApprovedProjectLeaderOwner(projectCode.getId(),
                request.getProjectLeaderUserId(), request.getWriteReason());
        String version=productAudit.attemptVersion(request.getWriteAttemptNo());
        productAudit.validateReason(request.getWriteReason());
        var before=productAudit.snapshot(requestId);
        folderTemplateService.generate(projectCode.getId(), request.getFolderTemplateId(), request.getFolderTemplateSnapshotJson());
        DccProductCatalogDO productCatalog = DccProductCatalogDO.builder()
                .tenantId(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId())
                .dataSource(DATA_SOURCE)
                .originalRowNo((maxRowNo == null ? 1 : maxRowNo) + 1)
                .product(request.getProductName())
                .productCode(request.getProductCode())
                .projectName(request.getProjectName())
                .projectCode(request.getProjectCode())
                .classification(request.getClassification())
                .remark(request.getRemark())
                .build();
        if (productCatalogMapper.insert(productCatalog) != 1 || productCatalog.getId() == null) {
            throw exception(WRITE_INCOMPLETE, "产品目录");
        }
        DccProjectProductRelationDO relation = DccProjectProductRelationDO.builder()
                .requestId(request.getId())
                .projectCodeId(projectCode.getId())
                .productCatalogId(productCatalog.getId())
                .relationStatus(RELATION_ACTIVE)
                .build();
        relation.setTenantId(cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId());
        if (relationMapper.insert(relation) != 1 || relation.getId() == null) {
            throw exception(WRITE_INCOMPLETE, "项目产品关系");
        }
        request.setGeneratedProjectCodeId(projectCode.getId());
        request.setGeneratedProductCatalogId(productCatalog.getId());
        request.setRelationId(relation.getId());
        request.setStatus(DccProjectProductCreateStatusConstants.COMPLETED);
        request.setCompletedTime(LocalDateTime.now());
        request.setWriteErrorCode(null);
        request.setWriteErrorMessage(null);
        if (requestMapper.updateById(request) != 1) throw exception(WRITE_INCOMPLETE, "申请完成状态");
        productAudit.append("dcc.project-product.complete",requestId,version,request.getWriteReason(),before);
        return request;
    }

    private void validateUnique(String projectCode, String productCode, String productName, Long requestId) {
        if (projectCodeMapper.selectByNormalizedProjectCode(projectCode) != null
                || claimedByOtherRequest(IDENTITY_PROJECT_CODE, projectCode, requestId)
                || activeProjectRequest(projectCode, requestId)) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_DUPLICATE_PROJECT_CODE);
        }
        if (productCatalogMapper.selectByNormalizedProductCode(StrUtil.trimToEmpty(productCode)) != null
                || claimedByOtherRequest(IDENTITY_PRODUCT_CODE, productCode, requestId)
                || activeProductCodeRequest(productCode, requestId)) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_DUPLICATE_PRODUCT_CODE);
        }
        if (productCatalogMapper.selectByNormalizedProductName(StrUtil.trimToEmpty(productName)) != null
                || claimedByOtherRequest(IDENTITY_PRODUCT_NAME, productName, requestId)
                || activeProductNameRequest(productName, requestId)) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_DUPLICATE_PRODUCT_NAME);
        }
    }

    private boolean claimedByOtherRequest(String identityType, String identityValue, Long requestId) {
        DccProjectProductIdentityClaimDO claim = identityClaimMapper.selectByTypeAndValue(identityType,
                StrUtil.trimToEmpty(identityValue));
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
}
