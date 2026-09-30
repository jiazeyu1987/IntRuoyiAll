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

    @Transactional(rollbackFor = Exception.class)
    public DccProjectProductCreateRequestDO writeApprovedRequest(Long requestId) {
        DccProjectProductCreateRequestDO request = requestMapper.selectByIdForUpdate(requestId);
        if (request == null) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_NOT_EXISTS);
        }
        if (!DccProjectProductCreateStatusConstants.WRITING.equals(request.getStatus())) {
            throw exception(DCC_PROJECT_PRODUCT_CREATE_STATUS_INVALID);
        }
        validateUnique(request.getProjectCode(), request.getProductCode(), request.getProductName(), request.getId());
        Integer maxRowNo = productCatalogMapper.selectMaxOriginalRowNo(DATA_SOURCE);
        DccProjectCodeDO projectCode = DccProjectCodeDO.builder()
                .projectName(request.getProjectName())
                .projectCode(request.getProjectCode())
                .projectLeader(request.getProjectLeader())
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build();
        projectCodeMapper.insert(projectCode);
        DccProductCatalogDO productCatalog = DccProductCatalogDO.builder()
                .dataSource(DATA_SOURCE)
                .originalRowNo((maxRowNo == null ? 1 : maxRowNo) + 1)
                .product(request.getProductName())
                .productCode(request.getProductCode())
                .projectName(request.getProjectName())
                .projectCode(request.getProjectCode())
                .classification(request.getClassification())
                .remark(request.getRemark())
                .build();
        productCatalogMapper.insert(productCatalog);
        DccProjectProductRelationDO relation = DccProjectProductRelationDO.builder()
                .requestId(request.getId())
                .projectCodeId(projectCode.getId())
                .productCatalogId(productCatalog.getId())
                .relationStatus(RELATION_ACTIVE)
                .build();
        relationMapper.insert(relation);
        request.setGeneratedProjectCodeId(projectCode.getId());
        request.setGeneratedProductCatalogId(productCatalog.getId());
        request.setRelationId(relation.getId());
        request.setStatus(DccProjectProductCreateStatusConstants.COMPLETED);
        request.setCompletedTime(LocalDateTime.now());
        request.setWriteErrorCode(null);
        request.setWriteErrorMessage(null);
        requestMapper.updateById(request);
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
