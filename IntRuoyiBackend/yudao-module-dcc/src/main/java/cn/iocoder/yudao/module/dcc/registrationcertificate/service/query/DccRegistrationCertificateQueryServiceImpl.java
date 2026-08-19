package cn.iocoder.yudao.module.dcc.registrationcertificate.service.query;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateQueryMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy.DccRegistrationCertificateAccessPolicyService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.MdmEnterpriseApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_NOT_EXISTS;

@Service
public class DccRegistrationCertificateQueryServiceImpl implements DccRegistrationCertificateQueryService {

    private static final Set<String> OWNED_COMPANY = Set.of("OWNED_COMPANY");

    private final DccRegistrationCertificateQueryMapper queryMapper;
    private final MdmCompanyScopeApi companyScopeApi;
    private final MdmEnterpriseApi enterpriseApi;
    private final DccRegistrationCertificateReadAuditService readAuditService;
    private final DccRegistrationCertificateAccessPolicyService accessPolicyService;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateQueryServiceImpl(
            DccRegistrationCertificateQueryMapper queryMapper,
            MdmCompanyScopeApi companyScopeApi,
            MdmEnterpriseApi enterpriseApi,
            DccRegistrationCertificateReadAuditService readAuditService,
            DccRegistrationCertificateAccessPolicyService accessPolicyService,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.queryMapper = require(queryMapper, "queryMapper");
        this.companyScopeApi = require(companyScopeApi, "companyScopeApi");
        this.enterpriseApi = require(enterpriseApi, "enterpriseApi");
        this.readAuditService = require(readAuditService, "readAuditService");
        this.accessPolicyService = require(accessPolicyService, "accessPolicyService");
        this.businessClock = require(businessClock, "businessClock");
    }

    @Override
    public PageResult<DccRegistrationCertificatePageItem> getPage(
            Long tenantId, Long actorId, DccRegistrationCertificatePageQuery query,
            DccRequestAuditContext auditContext) {
        DccRegistrationCertificatePageQuery normalized = normalize(query);
        List<Long> scopedCompanyIds = scopedCompanyIds(actorId);
        long total = queryMapper.countPage(tenantId, scopedCompanyIds, normalized);
        List<DccRegistrationCertificateQueryRecord> rows = total == 0 ? List.of()
                : queryMapper.selectPage(tenantId, scopedCompanyIds, normalized,
                normalized.getPageSize(), offset(normalized));
        Map<Long, String> companyNames = companyNames(tenantId, rows);
        rows.forEach(row -> readAuditService.record(successAudit(
                tenantId, actorId, row, "PAGE", auditContext, "page")));
        return new PageResult<>(rows.stream()
                .map(row -> pageItem(row, companyNames.get(row.getOwnerCompanyId())))
                .toList(), total);
    }

    @Override
    public DccRegistrationCertificateDetail getDetail(
            Long tenantId, Long actorId, Long certificateId, DccRequestAuditContext auditContext) {
        List<Long> scopedCompanyIds = scopedCompanyIds(actorId);
        DccRegistrationCertificateQueryRecord row = queryMapper.selectDetail(
                tenantId, scopedCompanyIds, certificateId);
        if (row == null) {
            recordDetailFailure(tenantId, actorId, certificateId, auditContext,
                    String.valueOf(REGISTRATION_CERTIFICATE_NOT_EXISTS.getCode()), "not_found_or_out_of_scope");
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        if ("OLD".equals(row.getStatus())) {
            try {
                accessPolicyService.assertOldViewAllowed(tenantId, actorId, certificateId, businessClock.now());
            } catch (ServiceException exception) {
                recordDetailFailure(tenantId, actorId, certificateId, auditContext,
                        String.valueOf(exception.getCode()), "old_view_grant_denied");
                throw exception;
            }
        }
        Map<Long, String> companyNames = companyNames(tenantId, List.of(row));
        readAuditService.record(successAudit(tenantId, actorId, row, "DETAIL", auditContext, "detail"));
        return detail(row, companyNames.get(row.getOwnerCompanyId()));
    }

    @Override
    public PageResult<DccRegistrationCertificateOldIndexItem> getOldIndexPage(
            Long tenantId, Long actorId, DccRegistrationCertificatePageQuery query,
            DccRequestAuditContext auditContext) {
        DccRegistrationCertificatePageQuery normalized = normalize(query);
        List<Long> scopedCompanyIds = scopedCompanyIds(actorId);
        long total = queryMapper.countOldIndex(tenantId, scopedCompanyIds, normalized);
        List<DccRegistrationCertificateQueryRecord> rows = total == 0 ? List.of()
                : queryMapper.selectOldIndexPage(tenantId, scopedCompanyIds, normalized,
                normalized.getPageSize(), offset(normalized));
        Map<Long, String> companyNames = companyNames(tenantId, rows);
        rows.forEach(row -> readAuditService.record(successAudit(
                tenantId, actorId, row, "OLD_INDEX", auditContext, "old-index")));
        return new PageResult<>(rows.stream()
                .map(row -> oldIndexItem(row, companyNames.get(row.getOwnerCompanyId())))
                .toList(), total);
    }

    private List<Long> scopedCompanyIds(Long actorId) {
        Set<Long> ids = companyScopeApi.getEnabledCompanyIdsForUser(actorId);
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        return ids.stream().sorted().toList();
    }

    private Map<Long, String> companyNames(Long tenantId, List<DccRegistrationCertificateQueryRecord> rows) {
        List<Long> companyIds = rows.stream()
                .map(DccRegistrationCertificateQueryRecord::getOwnerCompanyId)
                .distinct()
                .sorted()
                .toList();
        if (companyIds.isEmpty()) {
            return Map.of();
        }
        List<MdmEnterpriseRespDTO> companies = enterpriseApi.getEnabledEnterprises(companyIds, OWNED_COMPANY);
        if (companies == null || companies.size() != companyIds.size()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
        }
        Map<Long, MdmEnterpriseRespDTO> byId = companies.stream()
                .collect(Collectors.toMap(MdmEnterpriseRespDTO::getId, Function.identity(), (a, b) -> b,
                        LinkedHashMap::new));
        Map<Long, String> names = new LinkedHashMap<>();
        for (Long companyId : companyIds) {
            MdmEnterpriseRespDTO company = byId.get(companyId);
            if (company == null || !tenantId.equals(company.getTenantId())
                    || !"OWNED_COMPANY".equals(company.getType())
                    || company.getName() == null || company.getName().isBlank()) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED);
            }
            names.put(companyId, company.getName());
        }
        return names;
    }

    private DccRegistrationCertificateReadAuditCommand successAudit(
            Long tenantId, Long actorId, DccRegistrationCertificateQueryRecord row,
            String operation, DccRequestAuditContext auditContext, String source) {
        return DccRegistrationCertificateReadAuditCommand.builder()
                .tenantId(tenantId)
                .ownerCompanyId(row.getOwnerCompanyId())
                .certificateId(row.getCertificateId())
                .versionId(row.getVersionId())
                .snapshotId(row.getSnapshotId())
                .businessFileId(row.getRegistrationFileId())
                .operation(operation)
                .actorId(actorId)
                .result("SUCCESS")
                .resultCode("OK")
                .requestTraceId(auditContext.requireRequestId("registration certificate " + source))
                .detailJson(JsonUtils.toJsonString(Map.of("source", source)))
                .build();
    }

    private void recordDetailFailure(
            Long tenantId, Long actorId, Long certificateId, DccRequestAuditContext auditContext,
            String resultCode, String reason) {
        readAuditService.record(DccRegistrationCertificateReadAuditCommand.builder()
                .tenantId(tenantId)
                .requestedCertificateId(certificateId)
                .operation("DETAIL")
                .actorId(actorId)
                .result("FAILURE")
                .resultCode(resultCode)
                .requestTraceId(auditContext.requireRequestId("registration certificate detail"))
                .detailJson(JsonUtils.toJsonString(Map.of("reason", reason)))
                .build());
    }

    private static DccRegistrationCertificatePageItem pageItem(
            DccRegistrationCertificateQueryRecord row, String ownerCompanyName) {
        return DccRegistrationCertificatePageItem.builder()
                .certificateId(row.getCertificateId())
                .versionId(row.getVersionId())
                .snapshotId(row.getSnapshotId())
                .ownerCompanyId(row.getOwnerCompanyId())
                .ownerCompanyName(ownerCompanyName)
                .productName(row.getProductName())
                .certificateNo(row.getCertificateNo())
                .versionNo(row.getVersionNo())
                .status(row.getStatus())
                .hasProjectCode(row.getProjectCodeId() != null)
                .hasRegistrationFile(row.getRegistrationFileId() != null)
                .firstObtainedDate(row.getFirstObtainedDate())
                .approvalDate(row.getApprovalDate())
                .effectiveDate(row.getEffectiveDate())
                .expiryDate(row.getExpiryDate())
                .build();
    }

    private static DccRegistrationCertificateDetail detail(
            DccRegistrationCertificateQueryRecord row, String ownerCompanyName) {
        return DccRegistrationCertificateDetail.builder()
                .certificateId(row.getCertificateId())
                .versionId(row.getVersionId())
                .snapshotId(row.getSnapshotId())
                .ownerCompanyId(row.getOwnerCompanyId())
                .ownerCompanyName(ownerCompanyName)
                .productMasterId(row.getProductMasterId())
                .productName(row.getProductName())
                .projectCodeId(row.getProjectCodeId())
                .certificateNo(row.getCertificateNo())
                .versionNo(row.getVersionNo())
                .status(row.getStatus())
                .firstObtainedDate(row.getFirstObtainedDate())
                .approvalDate(row.getApprovalDate())
                .effectiveDate(row.getEffectiveDate())
                .expiryDate(row.getExpiryDate())
                .classification(row.getClassification())
                .registrantName(row.getRegistrantName())
                .modelSpecification(row.getModelSpecification())
                .structureComposition(row.getStructureComposition())
                .intendedUse(row.getIntendedUse())
                .technicalRequirements(row.getTechnicalRequirements())
                .residenceAddress(row.getResidenceAddress())
                .productionAddress(row.getProductionAddress())
                .entrustedProduction(row.getEntrustedProduction())
                .selfProduction(row.getSelfProduction())
                .entrustedEnterprisesJson(row.getEntrustedEnterprisesJson())
                .hasRegistrationFile(row.getRegistrationFileId() != null)
                .build();
    }

    private static DccRegistrationCertificateOldIndexItem oldIndexItem(
            DccRegistrationCertificateQueryRecord row, String ownerCompanyName) {
        return DccRegistrationCertificateOldIndexItem.builder()
                .certificateId(row.getCertificateId())
                .versionId(row.getVersionId())
                .ownerCompanyId(row.getOwnerCompanyId())
                .ownerCompanyName(ownerCompanyName)
                .productName(row.getProductName())
                .certificateNo(row.getCertificateNo())
                .versionNo(row.getVersionNo())
                .expiryDate(row.getExpiryDate())
                .status(row.getStatus())
                .build();
    }

    private static DccRegistrationCertificatePageQuery normalize(DccRegistrationCertificatePageQuery query) {
        DccRegistrationCertificatePageQuery value = query == null
                ? DccRegistrationCertificatePageQuery.builder().build() : query;
        if (value.getPageNo() == null || value.getPageNo() <= 0) {
            value.setPageNo(1);
        }
        if (value.getPageSize() == null || value.getPageSize() <= 0) {
            value.setPageSize(10);
        }
        if (value.getPageSize() > 100) {
            value.setPageSize(100);
        }
        return value;
    }

    private static int offset(DccRegistrationCertificatePageQuery query) {
        return (query.getPageNo() - 1) * query.getPageSize();
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
