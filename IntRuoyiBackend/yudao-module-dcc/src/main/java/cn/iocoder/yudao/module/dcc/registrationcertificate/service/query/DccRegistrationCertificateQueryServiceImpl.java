package cn.iocoder.yudao.module.dcc.registrationcertificate.service.query;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateQueryMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.accesspolicy.DccRegistrationCertificateAccessPolicyService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditCommand;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateReadAuditService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateOperationAudit;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.audit.DccRegistrationCertificateOperationAuditService;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateReminderEvaluation;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.reminder.DccRegistrationCertificateReminderService;
import cn.iocoder.yudao.module.dcc.service.file.DccRequestAuditContext;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.MdmEnterpriseApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_COMPANY_SCOPE_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_STATE_INVALID;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_SORT_INVALID;

@Service
public class DccRegistrationCertificateQueryServiceImpl implements DccRegistrationCertificateQueryService {

    private static final Set<String> OWNED_COMPANY = Set.of("OWNED_COMPANY");
    private static final Set<String> SORT_ORDERS = Set.of("asc", "desc");
    private static final Set<String> CURRENT_SORT_FIELDS = Set.of(
            "certificateNo",
            "ownerCompanyName",
            "productName",
            "classification",
            "projectCode",
            "versionNo",
            "status",
            "hasProjectCode",
            "hasRegistrationFile",
            "approvalDate",
            "effectiveDate",
            "expiryDate",
            "reminder",
            "remark");
    private static final Set<String> OLD_INDEX_SORT_FIELDS = Set.of(
            "certificateNo",
            "ownerCompanyName",
            "productName",
            "classification",
            "versionNo",
            "status",
            "expiryDate");
    private static final Set<String> CURRENT_REMINDER_STATES = Set.of(
            "NORMAL",
            "T_30",
            "T_8",
            "T_2",
            "T_1");

    private final DccRegistrationCertificateQueryMapper queryMapper;
    private final MdmCompanyScopeApi companyScopeApi;
    private final MdmEnterpriseApi enterpriseApi;
    private final DccRegistrationCertificateReadAuditService readAuditService;
    private final DccRegistrationCertificateOperationAuditService operationAuditService;
    private final DccRegistrationCertificateAccessPolicyService accessPolicyService;
    private final DccRegistrationCertificateBusinessClock businessClock;
    private final DccRegistrationCertificateReminderService reminderService;

    public DccRegistrationCertificateQueryServiceImpl(
            DccRegistrationCertificateQueryMapper queryMapper,
            MdmCompanyScopeApi companyScopeApi,
            MdmEnterpriseApi enterpriseApi,
            DccRegistrationCertificateReadAuditService readAuditService,
            DccRegistrationCertificateOperationAuditService operationAuditService,
            DccRegistrationCertificateAccessPolicyService accessPolicyService,
            DccRegistrationCertificateBusinessClock businessClock,
            DccRegistrationCertificateReminderService reminderService) {
        this.queryMapper = require(queryMapper, "queryMapper");
        this.companyScopeApi = require(companyScopeApi, "companyScopeApi");
        this.enterpriseApi = require(enterpriseApi, "enterpriseApi");
        this.readAuditService = require(readAuditService, "readAuditService");
        this.operationAuditService = require(operationAuditService, "operationAuditService");
        this.accessPolicyService = require(accessPolicyService, "accessPolicyService");
        this.businessClock = require(businessClock, "businessClock");
        this.reminderService = require(reminderService, "reminderService");
    }

    @Override
    public PageResult<DccRegistrationCertificatePageItem> getPage(
            Long tenantId, Long actorId, DccRegistrationCertificatePageQuery query,
            DccRequestAuditContext auditContext) {
        DccRegistrationCertificatePageQuery normalized = normalize(query);
        normalized.setBusinessDate(businessClock.businessDate());
        validateSort(normalized, CURRENT_SORT_FIELDS);
        validateCurrentReminderState(normalized);
        List<Long> scopedCompanyIds = scopedCompanyIds(actorId);
        long total = queryMapper.countPage(tenantId, scopedCompanyIds, normalized);
        List<DccRegistrationCertificateQueryRecord> rows = total == 0 ? List.of()
                : queryMapper.selectPage(tenantId, scopedCompanyIds, normalized,
                normalized.getPageSize(), offset(normalized));
        Map<Long, String> companyNames = companyNames(tenantId, rows);
        rows.forEach(row -> readAuditService.recordRepeatableListRead(successAudit(
                tenantId, actorId, row, "PAGE", auditContext, "page")));
        return new PageResult<>(rows.stream()
                .map(row -> pageItem(tenantId, row, companyNames.get(row.getOwnerCompanyId())))
                .toList(), total);
    }

    @Override
    public DccRegistrationCertificateDetail getDetail(
            Long tenantId, Long actorId, Long certificateId, Long versionId, DccRequestAuditContext auditContext) {
        List<Long> scopedCompanyIds = scopedCompanyIds(actorId);
        DccRegistrationCertificateQueryRecord row = queryMapper.selectDetail(
                tenantId, scopedCompanyIds, certificateId, versionId);
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
        DccRegistrationCertificateOperationAudit initialAudit =
                operationAuditService.getInitialAudit(tenantId, certificateId);
        return detail(tenantId, row, companyNames.get(row.getOwnerCompanyId()), initialAudit);
    }

    @Override
    public PageResult<DccRegistrationCertificateOldIndexItem> getOldIndexPage(
            Long tenantId, Long actorId, DccRegistrationCertificatePageQuery query,
            DccRequestAuditContext auditContext) {
        DccRegistrationCertificatePageQuery normalized = normalize(query);
        validateSort(normalized, OLD_INDEX_SORT_FIELDS);
        validateOldIndexReminderState(normalized);
        List<Long> scopedCompanyIds = scopedCompanyIds(actorId);
        long total = queryMapper.countOldIndex(tenantId, scopedCompanyIds, normalized);
        List<DccRegistrationCertificateQueryRecord> rows = total == 0 ? List.of()
                : queryMapper.selectOldIndexPage(tenantId, scopedCompanyIds, normalized,
                normalized.getPageSize(), offset(normalized));
        Map<Long, String> companyNames = companyNames(tenantId, rows);
        rows.forEach(row -> readAuditService.recordRepeatableListRead(successAudit(
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
                .requestTraceId(auditContext.requireRequestId("注册证读取：" + source))
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
                .requestTraceId(auditContext.requireRequestId("注册证详情"))
                .detailJson(JsonUtils.toJsonString(Map.of("reason", reason)))
                .build());
    }

    private DccRegistrationCertificatePageItem pageItem(
            Long tenantId, DccRegistrationCertificateQueryRecord row, String ownerCompanyName) {
        DccRegistrationCertificateReminderEvaluation reminder = reminderState(tenantId, row);
        return DccRegistrationCertificatePageItem.builder()
                .certificateId(row.getCertificateId())
                .rowVersion(row.getRowVersion())
                .versionId(row.getVersionId())
                .snapshotId(row.getSnapshotId())
                .ownerCompanyId(row.getOwnerCompanyId())
                .ownerCompanyName(ownerCompanyName)
                .productMasterId(row.getProductMasterId())
                .productName(row.getProductName())
                .projectCodeId(row.getProjectCodeId())
                .projectCode(row.getProjectCode())
                .certificateNo(row.getCertificateNo())
                .versionNo(row.getVersionNo())
                .status(row.getStatus())
                .classification(row.getClassification())
                .remark(row.getRemark())
                .hasPendingChange(Boolean.TRUE.equals(row.getHasPendingChange()))
                .hasProjectCode(row.getProjectCodeId() != null)
                .hasRegistrationFile(row.getRegistrationFileId() != null)
                .hasPendingRenewal(Boolean.TRUE.equals(row.getHasPendingRenewal()))
                .reminderColor(reminder.colorCode())
                .visualState(reminder.thresholdLevel())
                .firstObtainedDate(row.getFirstObtainedDate())
                .approvalDate(row.getApprovalDate())
                .effectiveDate(row.getEffectiveDate())
                .expiryDate(row.getExpiryDate())
                .build();
    }

    private DccRegistrationCertificateDetail detail(
            Long tenantId, DccRegistrationCertificateQueryRecord row, String ownerCompanyName,
            DccRegistrationCertificateOperationAudit initialAudit) {
        DccRegistrationCertificateReminderEvaluation reminder = reminderState(tenantId, row);
        return DccRegistrationCertificateDetail.builder()
                .certificateId(row.getCertificateId())
                .rowVersion(row.getRowVersion())
                .versionId(row.getVersionId())
                .snapshotId(row.getSnapshotId())
                .snapshotRevision(row.getSnapshotRevision())
                .ownerCompanyId(row.getOwnerCompanyId())
                .ownerCompanyName(ownerCompanyName)
                .productMasterId(row.getProductMasterId())
                .productName(row.getProductName())
                .projectCodeId(row.getProjectCodeId())
                .projectCode(row.getProjectCode())
                .certificateNo(row.getCertificateNo())
                .versionNo(row.getVersionNo())
                .status(row.getStatus())
                .firstObtainedDate(row.getFirstObtainedDate())
                .approvalDate(row.getApprovalDate())
                .effectiveDate(row.getEffectiveDate())
                .expiryDate(row.getExpiryDate())
                .classification(row.getClassification())
                .remark(row.getRemark())
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
                .registrationFileId(row.getRegistrationFileId())
                .registrationFileName(row.getRegistrationFileName())
                .uploadOperatorName(initialAudit.operatorName())
                .uploadedAt(initialAudit.operatedAt())
                .uploadApproverName(initialAudit.approverName())
                .uploadApprovedAt(initialAudit.approvedAt())
                .hasRegistrationFile(row.getRegistrationFileId() != null)
                .reminderColor(reminder.colorCode())
                .visualState(reminder.thresholdLevel())
                .build();
    }

    private DccRegistrationCertificateReminderEvaluation reminderState(
            Long tenantId, DccRegistrationCertificateQueryRecord row) {
        DccRegistrationCertificateReminderEvaluation evaluation = reminderService.evaluateThreshold(
                businessClock.businessDate(), row.getExpiryDate(), false);
        if (!"T_8".equals(evaluation.thresholdLevel())) {
            return evaluation;
        }
        boolean cleared = reminderService.isSupportingDocumentCleared(
                tenantId, row.getCertificateId(), "RENEWAL_ACCEPTANCE_RECEIPT")
                || reminderService.isSupportingDocumentCleared(
                tenantId, row.getCertificateId(), "RENEWAL_SUPPLEMENT_NOTICE");
        return cleared ? reminderService.evaluateThreshold(
                businessClock.businessDate(), row.getExpiryDate(), true) : evaluation;
    }

    private static DccRegistrationCertificateOldIndexItem oldIndexItem(
            DccRegistrationCertificateQueryRecord row, String ownerCompanyName) {
        return DccRegistrationCertificateOldIndexItem.builder()
                .certificateId(row.getCertificateId())
                .versionId(row.getVersionId())
                .ownerCompanyId(row.getOwnerCompanyId())
                .ownerCompanyName(ownerCompanyName)
                .productMasterId(row.getProductMasterId())
                .productName(row.getProductName())
                .projectCodeId(row.getProjectCodeId())
                .projectCode(row.getProjectCode())
                .certificateNo(row.getCertificateNo())
                .versionNo(row.getVersionNo())
                .classification(row.getClassification())
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

    private static void validateSort(DccRegistrationCertificatePageQuery query, Set<String> allowedFields) {
        String sortField = trimToNull(query.getSortField());
        String sortOrder = trimToNull(query.getSortOrder());
        query.setSortField(sortField);
        query.setSortOrder(sortOrder == null ? null : sortOrder.toLowerCase(Locale.ROOT));
        if (query.getSortField() == null && query.getSortOrder() == null) {
            return;
        }
        if (query.getSortField() == null || query.getSortOrder() == null
                || !allowedFields.contains(query.getSortField())
                || !SORT_ORDERS.contains(query.getSortOrder())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_SORT_INVALID);
        }
    }

    private static void validateCurrentReminderState(DccRegistrationCertificatePageQuery query) {
        String reminderState = trimToNull(query.getReminderState());
        query.setReminderState(reminderState);
        if (reminderState != null && !CURRENT_REMINDER_STATES.contains(reminderState)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_STATE_INVALID);
        }
    }

    private static void validateOldIndexReminderState(DccRegistrationCertificatePageQuery query) {
        String reminderState = trimToNull(query.getReminderState());
        query.setReminderState(reminderState);
        if (reminderState != null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_STATE_INVALID);
        }
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static int offset(DccRegistrationCertificatePageQuery query) {
        return (query.getPageNo() - 1) * query.getPageSize();
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + "不能为空");
        }
        return value;
    }
}
