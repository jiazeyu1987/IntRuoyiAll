package cn.iocoder.yudao.module.dcc.registrationcertificate.service.historicalimport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport.vo.DccRegistrationCertificateHistoricalImportPageReqVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.historicalimport.vo.DccRegistrationCertificateHistoricalImportRespVO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration.DccRegistrationCertificateMigrationService;
import cn.iocoder.yudao.module.mdm.api.enterprise.MdmEnterpriseApi;
import cn.iocoder.yudao.module.mdm.api.enterprise.dto.MdmEnterpriseRespDTO;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT;

@Service
@Validated
public class DccRegistrationCertificateHistoricalImportServiceImpl
        implements DccRegistrationCertificateHistoricalImportService {

    private static final List<String> OWNED_COMPANY = List.of("OWNED_COMPANY");

    private final DccRegistrationCertificateAuditMapper auditMapper;
    private final MdmEnterpriseApi enterpriseApi;

    public DccRegistrationCertificateHistoricalImportServiceImpl(
            DccRegistrationCertificateAuditMapper auditMapper,
            MdmEnterpriseApi enterpriseApi) {
        this.auditMapper = require(auditMapper, "auditMapper");
        this.enterpriseApi = require(enterpriseApi, "enterpriseApi");
    }

    @Override
    public PageResult<DccRegistrationCertificateHistoricalImportRespVO> getHistoricalImportPage(
            @Valid DccRegistrationCertificateHistoricalImportPageReqVO reqVO) {
        DccRegistrationCertificateHistoricalImportPageReqVO pageReqVO = normalize(reqVO);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String sourceHash = normalizeSourceHash(pageReqVO.getSourceHash());
        long total = auditMapper.countHistoricalImportPage(tenantId, sourceHash);
        List<DccRegistrationCertificateHistoricalImportRow> rows = total == 0
                ? List.of()
                : auditMapper.selectHistoricalImportPage(tenantId, sourceHash, pageReqVO.getPageSize(),
                offset(pageReqVO));
        Map<Long, MdmEnterpriseRespDTO> companyMap = companyMap(tenantId, rows);
        List<DccRegistrationCertificateHistoricalImportRespVO> result = rows.stream()
                .map(row -> toResp(row, companyMap.get(row.getOwnerCompanyId()), sourceHash))
                .toList();
        return new PageResult<>(result, total);
    }

    private Map<Long, MdmEnterpriseRespDTO> companyMap(Long tenantId,
                                                       List<DccRegistrationCertificateHistoricalImportRow> rows) {
        List<Long> companyIds = rows.stream()
                .map(DccRegistrationCertificateHistoricalImportRow::getOwnerCompanyId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        if (companyIds.isEmpty()) {
            return Map.of();
        }
        List<MdmEnterpriseRespDTO> companies = enterpriseApi.getEnabledEnterprises(companyIds, OWNED_COMPANY);
        if (companies == null || companies.size() != companyIds.size()) {
            throw exception(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT);
        }
        Map<Long, MdmEnterpriseRespDTO> result = new LinkedHashMap<>();
        for (MdmEnterpriseRespDTO company : companies) {
            if (company == null || company.getId() == null || !Objects.equals(tenantId, company.getTenantId())
                    || StrUtil.isBlank(company.getEnterpriseCode()) || StrUtil.isBlank(company.getName())
                    || !Objects.equals("OWNED_COMPANY", company.getType())
                    || result.putIfAbsent(company.getId(), company) != null
                    || !companyIds.contains(company.getId())) {
                throw exception(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT);
            }
        }
        if (result.size() != companyIds.size()) {
            throw exception(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT);
        }
        return result;
    }

    private DccRegistrationCertificateHistoricalImportRespVO toResp(
            DccRegistrationCertificateHistoricalImportRow row, MdmEnterpriseRespDTO company, String sourceHashFilter) {
        DccRegistrationCertificateMigrationService.HistoricalImportAuditDetail detail = parseDetail(row);
        if (sourceHashFilter != null && !Objects.equals(sourceHashFilter, detail.getSourceHash())) {
            throw exception(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT);
        }
        String result = normalizeResult(row);
        if (row.getOwnerCompanyId() != null && company == null) {
            throw exception(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT);
        }
        if ("SUCCESS".equals(result)) {
            if (row.getOwnerCompanyId() == null || row.getCertificateId() == null || row.getCertificateRecordId() == null
                    || row.getCertificateOwnerCompanyId() == null || row.getVersionId() == null
                    || row.getVersionRecordId() == null || row.getSnapshotId() == null
                    || row.getSnapshotRecordId() == null
                    || detail.getOutcomeCertificateId() == null || detail.getOutcomeVersionId() == null
                    || detail.getOutcomeSnapshotId() == null
                    || !Objects.equals(row.getCertificateId(), row.getCertificateRecordId())
                    || !Objects.equals(row.getVersionId(), row.getVersionRecordId())
                    || !Objects.equals(row.getSnapshotId(), row.getSnapshotRecordId())
                    || !Objects.equals(row.getOwnerCompanyId(), row.getCertificateOwnerCompanyId())
                    || !Objects.equals(row.getCertificateId(), detail.getOutcomeCertificateId())
                    || !Objects.equals(row.getVersionId(), detail.getOutcomeVersionId())
                    || !Objects.equals(row.getSnapshotId(), detail.getOutcomeSnapshotId())) {
                throw exception(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT);
            }
        }
        DccRegistrationCertificateHistoricalImportRespVO resp = new DccRegistrationCertificateHistoricalImportRespVO();
        resp.setId(row.getId());
        resp.setSourceHash(detail.getSourceHash());
        resp.setSourceRow(detail.getSourceRow());
        resp.setPayloadHash(detail.getPayloadHash());
        resp.setOutcomeCertificateId(detail.getOutcomeCertificateId());
        resp.setOutcomeVersionId(detail.getOutcomeVersionId());
        resp.setOutcomeSnapshotId(detail.getOutcomeSnapshotId());
        resp.setRestrictedReasons(List.copyOf(detail.getRestrictedReasons()));
        resp.setOwnerCompanyId(row.getOwnerCompanyId());
        if (company != null) {
            resp.setOwnerCompanyCode(company.getEnterpriseCode());
            resp.setOwnerCompanyName(company.getName());
        }
        resp.setCertificateId(row.getCertificateId());
        resp.setCertificateNo(row.getCertificateNo());
        resp.setVersionNo(row.getVersionNo());
        resp.setProductName(row.getProductName());
        resp.setActorId(row.getActorId());
        resp.setResult(result);
        resp.setResultCode(row.getResultCode());
        resp.setRequestTraceId(row.getRequestTraceId());
        resp.setOccurredAt(row.getOccurredAt());
        return resp;
    }

    private DccRegistrationCertificateMigrationService.HistoricalImportAuditDetail parseDetail(
            DccRegistrationCertificateHistoricalImportRow row) {
        if (row == null || row.getDetailJson() == null || row.getDetailJson().isBlank()) {
            throw exception(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT);
        }
        try {
            DccRegistrationCertificateMigrationService.HistoricalImportAuditDetail detail = JsonUtils.parseObject(
                    row.getDetailJson(), DccRegistrationCertificateMigrationService.HistoricalImportAuditDetail.class);
            if (detail == null || detail.getSourceHash() == null || detail.getSourceHash().isBlank()
                    || detail.getSourceRow() == null || detail.getSourceRow() <= 0
                    || detail.getPayloadHash() == null || detail.getPayloadHash().isBlank()
                    || detail.getRestrictedReasons() == null) {
                throw new IllegalArgumentException("invalid detail");
            }
            detail.setSourceHash(detail.getSourceHash().trim().toUpperCase(Locale.ROOT));
            return detail;
        } catch (RuntimeException ex) {
            throw exception(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT);
        }
    }

    private String normalizeResult(DccRegistrationCertificateHistoricalImportRow row) {
        String result = StrUtil.trimToNull(row == null ? null : row.getResult());
        if (result == null || !Set.of("SUCCESS", "FAILURE").contains(result)) {
            throw exception(REGISTRATION_CERTIFICATE_AUDIT_EVENT_CONFLICT);
        }
        return result;
    }

    private DccRegistrationCertificateHistoricalImportPageReqVO normalize(
            DccRegistrationCertificateHistoricalImportPageReqVO reqVO) {
        DccRegistrationCertificateHistoricalImportPageReqVO value = reqVO == null
                ? new DccRegistrationCertificateHistoricalImportPageReqVO() : reqVO;
        if (value.getPageNo() == null || value.getPageNo() <= 0) {
            value.setPageNo(1);
        }
        if (value.getPageSize() == null || value.getPageSize() <= 0) {
            value.setPageSize(10);
        }
        if (value.getPageSize() > 100) {
            value.setPageSize(100);
        }
        value.setSourceHash(normalizeSourceHash(value.getSourceHash()));
        return value;
    }

    private String normalizeSourceHash(String sourceHash) {
        String normalized = StrUtil.trimToNull(sourceHash);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private int offset(DccRegistrationCertificateHistoricalImportPageReqVO reqVO) {
        return (reqVO.getPageNo() - 1) * reqVO.getPageSize();
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
