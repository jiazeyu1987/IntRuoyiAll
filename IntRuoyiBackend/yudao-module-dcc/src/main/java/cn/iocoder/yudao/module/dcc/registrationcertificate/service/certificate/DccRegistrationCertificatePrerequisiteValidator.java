package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateEntrustedEnterprise;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateProductionRelation;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.MdmEnterpriseApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import cn.iocoder.yudao.module.mdm.api.product.MdmProductApi;
import cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO;
import cn.iocoder.yudao.module.mdm.enums.MdmEnterpriseTypeEnum;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_APPROVAL_DATE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DATE_ORDER_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FIRST_OBTAINED_DATE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_OWNER_COMPANY_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PRODUCT_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PRODUCT_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECT_CODE_TENANT_MISMATCH;

@Component
public class DccRegistrationCertificatePrerequisiteValidator {

    private static final Set<String> OWNED_COMPANY = Set.of(MdmEnterpriseTypeEnum.OWNED_COMPANY.getType());
    private static final Set<String> ENTRUSTED_TYPES = Set.of(
            MdmEnterpriseTypeEnum.OWNED_COMPANY.getType(), MdmEnterpriseTypeEnum.ENTRUSTED_PARTY.getType());
    private static final int CERTIFICATE_NO_MAX_LENGTH = 128;
    private static final int CLASSIFICATION_MAX_LENGTH = 64;
    private static final int REGISTRANT_NAME_MAX_LENGTH = 255;

    private final MdmCompanyScopeApi companyScopeApi;
    private final MdmEnterpriseApi enterpriseApi;
    private final MdmProductApi productApi;
    private final DccProjectCodeService projectCodeService;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificatePrerequisiteValidator(
            MdmCompanyScopeApi companyScopeApi,
            MdmEnterpriseApi enterpriseApi,
            MdmProductApi productApi,
            DccProjectCodeService projectCodeService,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.companyScopeApi = require(companyScopeApi, "companyScopeApi");
        this.enterpriseApi = require(enterpriseApi, "enterpriseApi");
        this.productApi = require(productApi, "productApi");
        this.projectCodeService = require(projectCodeService, "projectCodeService");
        this.businessClock = require(businessClock, "businessClock");
    }

    public DccRegistrationCertificateResolvedDraft validate(
            Long tenantId, Long actorId, DccRegistrationCertificateDraftData draft) {
        requirePositive(tenantId, REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        requirePositive(actorId, REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        requirePositive(draft.ownerCompanyId(), REGISTRATION_CERTIFICATE_OWNER_COMPANY_REQUIRED);
        requirePositive(draft.productMasterId(), REGISTRATION_CERTIFICATE_PRODUCT_REQUIRED);
        validateRequiredText(draft);
        validateDates(draft);

        validateCompanyScope(actorId, draft.ownerCompanyId());
        List<MdmEnterpriseRespDTO> owners;
        try {
            owners = enterpriseApi.getEnabledEnterprises(List.of(draft.ownerCompanyId()), OWNED_COMPANY);
        } catch (RuntimeException exception) {
            throw dependencyFailure(REGISTRATION_CERTIFICATE_OWNER_COMPANY_REQUIRED, exception);
        }
        requireExactEnterprises(owners, List.of(draft.ownerCompanyId()), tenantId,
                REGISTRATION_CERTIFICATE_OWNER_COMPANY_REQUIRED);

        MdmProductRespDTO product;
        try {
            product = productApi.getEnabledDccProduct(draft.productMasterId());
        } catch (RuntimeException exception) {
            throw dependencyFailure(REGISTRATION_CERTIFICATE_PRODUCT_INVALID, exception);
        }
        if (product == null || !draft.productMasterId().equals(product.getId())
                || product.getNameCn() == null || product.getNameCn().isBlank()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PRODUCT_INVALID);
        }
        validateProjectCode(tenantId, actorId, draft.projectCodeId(), draft.productMasterId());

        List<Long> entrustedIds = draft.entrustedEnterpriseIds();
        if (entrustedIds == null || entrustedIds.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(entrustedIds).size() != entrustedIds.size()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID);
        }
        List<MdmEnterpriseRespDTO> enterprises = List.of();
        if (!entrustedIds.isEmpty()) {
            try {
                enterprises = enterpriseApi.getEnabledEnterprises(entrustedIds, ENTRUSTED_TYPES);
            } catch (RuntimeException exception) {
                throw dependencyFailure(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID, exception);
            }
            requireExactEnterprises(enterprises, entrustedIds, tenantId,
                    REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID);
        }
        List<DccRegistrationCertificateEntrustedEnterprise> entrusted = enterprises.stream()
                .map(item -> new DccRegistrationCertificateEntrustedEnterprise(item.getId(), item.getName()))
                .toList();
        try {
            return new DccRegistrationCertificateResolvedDraft(product.getNameCn(),
                    new DccRegistrationCertificateProductionRelation(
                            Boolean.TRUE.equals(draft.entrustedProduction()),
                            Boolean.TRUE.equals(draft.selfProduction()), entrusted));
        } catch (IllegalArgumentException exception) {
            throw mapped(REGISTRATION_CERTIFICATE_PRODUCTION_RELATION_INVALID, exception);
        }
    }

    public void validateCompanyScope(Long actorId, Long ownerCompanyId) {
        requirePositive(actorId, REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        requirePositive(ownerCompanyId, REGISTRATION_CERTIFICATE_OWNER_COMPANY_REQUIRED);
        try {
            companyScopeApi.validateUserCompanyAccess(actorId, ownerCompanyId);
        } catch (RuntimeException exception) {
            throw dependencyFailure(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED, exception);
        }
    }

    private void validateProjectCode(Long tenantId, Long actorId, Long projectCodeId, Long productMasterId) {
        if (projectCodeId == null) {
            return;
        }
        requirePositive(projectCodeId, REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID);
        DccProjectCodeDO projectCode;
        try {
            projectCode = projectCodeService.getProjectCode(actorId, projectCodeId);
        } catch (RuntimeException exception) {
            throw dependencyFailure(REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID, exception);
        }
        if (projectCode == null || !projectCodeId.equals(projectCode.getId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_INVALID);
        }
        if (!tenantId.equals(projectCode.getTenantId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_TENANT_MISMATCH);
        }
        if (!DccProjectCodeStatusConstants.ENABLE.equals(projectCode.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_DISABLED);
        }
        if (!productMasterId.equals(projectCode.getProductMasterId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECT_CODE_PRODUCT_MISMATCH);
        }
    }

    private void validateDates(DccRegistrationCertificateDraftData draft) {
        LocalDate first = draft.firstObtainedDate();
        LocalDate approval = draft.approvalDate();
        LocalDate effective = draft.effectiveDate();
        LocalDate expiry = draft.expiryDate();
        if (first == null || approval == null || effective == null || expiry == null
                || first.isAfter(approval) || approval.isAfter(effective) || !effective.isBefore(expiry)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DATE_ORDER_INVALID);
        }
        LocalDate businessDate = businessClock.businessDate();
        if (first.isAfter(businessDate)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FIRST_OBTAINED_DATE_INVALID);
        }
        if (approval.isAfter(businessDate)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_APPROVAL_DATE_INVALID);
        }
    }

    private void validateRequiredText(DccRegistrationCertificateDraftData draft) {
        if (isBlank(draft.certificateNo()) || isBlank(draft.classification()) || isBlank(draft.registrantName())
                || isBlank(draft.modelSpecification()) || isBlank(draft.structureComposition())
                || isBlank(draft.intendedUse()) || isBlank(draft.technicalRequirements())
                || isBlank(draft.residenceAddress()) || isBlank(draft.productionAddress())
                || normalizedLengthExceeds(draft.certificateNo(), CERTIFICATE_NO_MAX_LENGTH)
                || normalizedLengthExceeds(draft.classification(), CLASSIFICATION_MAX_LENGTH)
                || normalizedLengthExceeds(draft.registrantName(), REGISTRANT_NAME_MAX_LENGTH)
                || draft.entrustedProduction() == null || draft.selfProduction() == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
    }

    private static boolean normalizedLengthExceeds(String value, int maxLength) {
        return value != null && value.trim().length() > maxLength;
    }

    private static void requireExactEnterprises(List<MdmEnterpriseRespDTO> actual, List<Long> expectedIds,
                                                 Long tenantId,
                                                 cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        if (actual == null || actual.size() != expectedIds.size()) {
            throw new ServiceException(errorCode);
        }
        for (int index = 0; index < expectedIds.size(); index++) {
            MdmEnterpriseRespDTO item = actual.get(index);
            if (item == null || !expectedIds.get(index).equals(item.getId())
                    || !tenantId.equals(item.getTenantId()) || item.getName() == null || item.getName().isBlank()) {
                throw new ServiceException(errorCode);
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static void requirePositive(Long value, cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        if (value == null || value <= 0) {
            throw new ServiceException(errorCode);
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    private static ServiceException mapped(cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode,
                                           RuntimeException cause) {
        ServiceException exception = new ServiceException(errorCode);
        exception.initCause(cause);
        return exception;
    }

    private static RuntimeException dependencyFailure(
            cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode, RuntimeException cause) {
        if (!(cause instanceof ServiceException)) {
            return cause;
        }
        return mapped(errorCode, cause);
    }
}
