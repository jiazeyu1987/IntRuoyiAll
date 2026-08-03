package cn.iocoder.yudao.module.dcc.service.projectcode.onboarding;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.onboarding.DccProductOnboardingCreateReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProductOnboardingRequestDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProductOnboardingRequestMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProductOnboardingStatusConstants;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import cn.iocoder.yudao.module.mdm.controller.admin.product.vo.MdmProductSaveReqVO;
import cn.iocoder.yudao.module.mdm.enums.MdmProductStatusConstants;
import cn.iocoder.yudao.module.mdm.service.product.MdmProductCodePolicy;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PRODUCT_ONBOARDING_DUPLICATE_PROJECT_CODE;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PRODUCT_ONBOARDING_MDM_PRODUCT_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PRODUCT_ONBOARDING_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PRODUCT_ONBOARDING_REQUIRED_FIELD_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PRODUCT_ONBOARDING_STATUS_INVALID;

@Service
@Validated
public class DccProductOnboardingServiceImpl implements DccProductOnboardingService {

    @Resource
    private DccProductOnboardingRequestMapper requestMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private MdmProductApi mdmProductApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRequest(Long applicantUserId, DccProductOnboardingCreateReqVO reqVO) {
        NormalizedOnboardingRequest normalized = normalizeCreateReq(reqVO);
        validateProjectCodeAvailable(normalized.projectName(), normalized.projectCode());
        ProductSnapshot productSnapshot = resolveProductSnapshotForRequest(normalized);
        DccProductOnboardingRequestDO request = DccProductOnboardingRequestDO.builder()
                .productMasterId(productSnapshot.productMasterId())
                .productCode(productSnapshot.productCode())
                .dccProductCode(productSnapshot.dccProductCode())
                .productNameCn(productSnapshot.nameCn())
                .productNameEn(productSnapshot.nameEn())
                .modelSpecification(productSnapshot.modelSpecification())
                .productCategory(productSnapshot.category())
                .docControlNo(normalized.docControlNo())
                .projectName(normalized.projectName())
                .projectCode(normalized.projectCode())
                .category(normalized.category())
                .commissionedProduction(normalized.commissionedProduction())
                .projectLeader(normalized.projectLeader())
                .projectEngineer(normalized.projectEngineer())
                .storageLocation(normalized.storageLocation())
                .priority(normalized.priority())
                .status(DccProductOnboardingStatusConstants.PENDING_APPROVAL)
                .applicantUserId(applicantUserId)
                .build();
        requestMapper.insert(request);
        return request.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccProductOnboardingRequestDO approveRequest(Long approverUserId, Long requestId) {
        DccProductOnboardingRequestDO request = requestMapper.selectById(requestId);
        if (request == null) {
            throw exception(PRODUCT_ONBOARDING_NOT_EXISTS);
        }
        if (!DccProductOnboardingStatusConstants.PENDING_APPROVAL.equals(request.getStatus())) {
            throw exception(PRODUCT_ONBOARDING_STATUS_INVALID);
        }
        ProductSnapshot product = resolveEnabledProductForApproval(request);
        validateProjectCodeAvailable(request.getProjectName(), request.getProjectCode(), request.getId());

        DccProjectCodeDO projectCode = DccProjectCodeDO.builder()
                .productMasterId(product.productMasterId())
                .docControlNo(request.getDocControlNo())
                .projectName(request.getProjectName())
                .projectCode(request.getProjectCode())
                .category(request.getCategory())
                .commissionedProduction(request.getCommissionedProduction())
                .projectLeader(request.getProjectLeader())
                .projectEngineer(request.getProjectEngineer())
                .storageLocation(request.getStorageLocation())
                .priority(request.getPriority())
                .status(DccProjectCodeStatusConstants.ENABLE)
                .build();
        projectCodeMapper.insert(projectCode);

        request.setProductMasterId(product.productMasterId());
        request.setProductCode(product.productCode());
        request.setDccProductCode(product.dccProductCode());
        request.setProductNameCn(product.nameCn());
        request.setProductNameEn(product.nameEn());
        request.setModelSpecification(product.modelSpecification());
        request.setProductCategory(product.category());
        request.setStatus(DccProductOnboardingStatusConstants.APPROVED);
        request.setApproverUserId(approverUserId);
        request.setApprovedTime(LocalDateTime.now());
        request.setGeneratedProjectCodeId(projectCode.getId());
        requestMapper.updateById(request);
        return request;
    }

    private ProductSnapshot resolveProductSnapshotForRequest(NormalizedOnboardingRequest request) {
        if (request.productMasterId() != null) {
            MdmProductRespDTO product = mdmProductApi.getProduct(request.productMasterId());
            if (product == null) {
                throw exception(PRODUCT_ONBOARDING_MDM_PRODUCT_INVALID, "MDM product does not exist");
            }
            return toSnapshot(product);
        }
        requireText(request.productCode(), "产品编码不能为空");
        requireDccProductCode(request.dccProductCode());
        requireText(request.productNameCn(), "产品中文名不能为空");
        return new ProductSnapshot(null, request.productCode(), request.dccProductCode(), request.productNameCn(),
                request.productNameEn(), request.modelSpecification(), request.productCategory());
    }

    private ProductSnapshot resolveEnabledProductForApproval(DccProductOnboardingRequestDO request) {
        try {
            if (request.getProductMasterId() != null) {
                return toSnapshot(mdmProductApi.getEnabledDccProduct(request.getProductMasterId()));
            }
            MdmProductSaveReqVO createReqVO = new MdmProductSaveReqVO();
            createReqVO.setProductCode(request.getProductCode());
            createReqVO.setDccProductCode(request.getDccProductCode());
            createReqVO.setNameCn(request.getProductNameCn());
            createReqVO.setNameEn(request.getProductNameEn());
            createReqVO.setModelSpecification(request.getModelSpecification());
            createReqVO.setCategory(request.getProductCategory());
            createReqVO.setStatus(MdmProductStatusConstants.ENABLE);
            Long productId = mdmProductApi.createProduct(createReqVO);
            return toSnapshot(mdmProductApi.getEnabledDccProduct(productId));
        } catch (RuntimeException ex) {
            throw exception(PRODUCT_ONBOARDING_MDM_PRODUCT_INVALID, ex.getMessage());
        }
    }

    private ProductSnapshot toSnapshot(MdmProductRespDTO product) {
        if (product == null || product.getId() == null) {
            throw exception(PRODUCT_ONBOARDING_MDM_PRODUCT_INVALID, "MDM product does not exist");
        }
        String dccProductCode = requireDccProductCode(product.getDccProductCode());
        String nameCn = requireText(product.getNameCn(), "产品中文名不能为空");
        return new ProductSnapshot(product.getId(), StrUtil.trimToNull(product.getProductCode()), dccProductCode,
                nameCn, StrUtil.trimToNull(product.getNameEn()), StrUtil.trimToNull(product.getModelSpecification()),
                StrUtil.trimToNull(product.getCategory()));
    }

    private void validateProjectCodeAvailable(String projectName, String projectCode) {
        validateProjectCodeAvailable(projectName, projectCode, null);
    }

    private void validateProjectCodeAvailable(String projectName, String projectCode, Long ignoredRequestId) {
        if (projectCodeMapper.selectByProjectNameAndProjectCode(projectName, projectCode) != null) {
            throw exception(PRODUCT_ONBOARDING_DUPLICATE_PROJECT_CODE);
        }
        DccProductOnboardingRequestDO pendingRequest = requestMapper
                .selectPendingByProjectNameAndProjectCode(projectName, projectCode);
        if (pendingRequest != null
                && (ignoredRequestId == null || !ignoredRequestId.equals(pendingRequest.getId()))) {
            throw exception(PRODUCT_ONBOARDING_DUPLICATE_PROJECT_CODE);
        }
    }

    private NormalizedOnboardingRequest normalizeCreateReq(DccProductOnboardingCreateReqVO reqVO) {
        String projectName = requireText(reqVO.getProjectName(), "项目名称不能为空");
        String projectCode = requireText(reqVO.getProjectCode(), "项目代码不能为空");
        return new NormalizedOnboardingRequest(reqVO.getProductMasterId(),
                StrUtil.trimToNull(reqVO.getProductCode()),
                MdmProductCodePolicy.normalize(reqVO.getDccProductCode()),
                StrUtil.trimToNull(reqVO.getProductNameCn()),
                StrUtil.trimToNull(reqVO.getProductNameEn()),
                StrUtil.trimToNull(reqVO.getModelSpecification()),
                StrUtil.trimToNull(reqVO.getProductCategory()),
                StrUtil.trimToNull(reqVO.getDocControlNo()),
                projectName,
                projectCode,
                StrUtil.trimToNull(reqVO.getCategory()),
                StrUtil.trimToNull(reqVO.getCommissionedProduction()),
                StrUtil.trimToNull(reqVO.getProjectLeader()),
                StrUtil.trimToNull(reqVO.getProjectEngineer()),
                StrUtil.trimToNull(reqVO.getStorageLocation()),
                StrUtil.trimToNull(reqVO.getPriority()));
    }

    private String requireText(String value, String message) {
        String normalized = StrUtil.trimToNull(value);
        if (normalized == null) {
            throw exception(PRODUCT_ONBOARDING_REQUIRED_FIELD_MISSING, message);
        }
        return normalized;
    }

    private String requireDccProductCode(String value) {
        String normalized = MdmProductCodePolicy.normalize(value);
        if (!MdmProductCodePolicy.isValidDccProductCode(normalized)) {
            throw exception(PRODUCT_ONBOARDING_MDM_PRODUCT_INVALID, "DCC 产品编号必须为 14 位字母或数字");
        }
        return normalized;
    }

    private record ProductSnapshot(Long productMasterId,
                                   String productCode,
                                   String dccProductCode,
                                   String nameCn,
                                   String nameEn,
                                   String modelSpecification,
                                   String category) {
    }

    private record NormalizedOnboardingRequest(Long productMasterId,
                                               String productCode,
                                               String dccProductCode,
                                               String productNameCn,
                                               String productNameEn,
                                               String modelSpecification,
                                               String productCategory,
                                               String docControlNo,
                                               String projectName,
                                               String projectCode,
                                               String category,
                                               String commissionedProduction,
                                               String projectLeader,
                                               String projectEngineer,
                                               String storageLocation,
                                               String priority) {
    }
}
