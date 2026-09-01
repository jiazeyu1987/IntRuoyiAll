package cn.iocoder.yudao.module.dcc.registrationcertificate.service.migration;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateAuditDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotEntrustedDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateAuditMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotEntrustedMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentKey;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentProjectionSnapshot;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentRegistrationProjectionService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_TENANT_MISMATCH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_REGISTRATION_CERTIFICATE;

@Service
public class DccRegistrationCertificateMigrationService {

    private final DccRegistrationCertificateMapper certificateMapper;
    private final DccRegistrationCertificateVersionMapper versionMapper;
    private final DccRegistrationCertificateSnapshotMapper snapshotMapper;
    private final DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper;
    private final DccRegistrationCertificateAuditMapper auditMapper;
    private final ControlledContentRegistrationProjectionService projectionService;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateMigrationService(
            DccRegistrationCertificateMapper certificateMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateSnapshotMapper snapshotMapper,
            DccRegistrationCertificateSnapshotEntrustedMapper entrustedMapper,
            DccRegistrationCertificateAuditMapper auditMapper,
            ControlledContentRegistrationProjectionService projectionService,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.certificateMapper = require(certificateMapper, "certificateMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.snapshotMapper = require(snapshotMapper, "snapshotMapper");
        this.entrustedMapper = require(entrustedMapper, "entrustedMapper");
        this.auditMapper = require(auditMapper, "auditMapper");
        this.projectionService = require(projectionService, "projectionService");
        this.businessClock = require(businessClock, "businessClock");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result commitHistoricalBatch(BatchCommand command) {
        validateBatch(command);
        for (RowCommand row : command.rows()) {
            validateRow(row);
        }
        int committed = 0;
        int replayed = 0;
        int restricted = 0;
        for (RowCommand row : command.rows()) {
            String payloadHash = payloadHash(command, row);
            String eventKey = eventKey(command.sourceHash(), row.sourceRow());
            DccRegistrationCertificateAuditDO existing =
                    auditMapper.selectByTenantIdAndEventKey(command.tenantId(), eventKey);
            if (existing != null) {
                HistoricalImportAuditDetail detail = parseDetail(existing);
                if (!Objects.equals(payloadHash, detail.getPayloadHash())) {
                    throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
                }
                replayed++;
                if (detail.getRestrictedReasons() != null && !detail.getRestrictedReasons().isEmpty()) {
                    restricted++;
                }
                continue;
            }
            CommittedRow committedRow = insertRow(command, row, payloadHash, eventKey);
            committed++;
            if (!committedRow.detail().getRestrictedReasons().isEmpty()) {
                restricted++;
            }
        }
        return new Result(committed, replayed, restricted);
    }

    private CommittedRow insertRow(BatchCommand command, RowCommand row, String payloadHash, String eventKey) {
        boolean immediate = !row.effectiveDate().isAfter(businessClock.businessDate());
        String masterStatus = immediate ? "ACTIVE" : "PENDING_FIRST_EFFECTIVE";
        String versionStatus = immediate ? "CURRENT" : "PENDING_EFFECTIVE";

        DccRegistrationCertificateDO certificate = DccRegistrationCertificateDO.builder()
                .ownerCompanyId(row.ownerCompanyId())
                .productMasterId(row.productMasterId())
                .projectCodeId(row.projectCodeId())
                .firstObtainedDate(row.firstObtainedDate())
                .status(masterStatus)
                .rowVersion(1)
                .build();
        certificate.setTenantId(command.tenantId());
        requireSingle("新增迁移注册证", certificateMapper.insert(certificate));

        DccRegistrationCertificateVersionDO version = DccRegistrationCertificateVersionDO.builder()
                .certificateId(certificate.getId())
                .versionNo(1)
                .versionType("INITIAL_CERTIFICATE")
                .certificateNo(trim(row.certificateNo()))
                .approvalDate(row.approvalDate())
                .effectiveDate(row.effectiveDate())
                .expiryDate(row.expiryDate())
                .classification(trim(row.classification()))
                .categoryChanged(false)
                .status(versionStatus)
                .formalizedAt(businessClock.now())
                .formalizedBy(command.actorId())
                .build();
        version.setTenantId(command.tenantId());
        requireSingle("新增迁移注册证版本", versionMapper.insert(version));

        DccRegistrationCertificateSnapshotDO snapshot = DccRegistrationCertificateSnapshotDO.builder()
                .versionId(version.getId())
                .revisionNo(1)
                .productName(trim(row.productName()))
                .registrantName(trim(row.registrantName()))
                .modelSpecification(blankToEmpty(row.modelSpecification()))
                .structureComposition(blankToEmpty(row.structureComposition()))
                .intendedUse(blankToEmpty(row.intendedUse()))
                .technicalRequirements(blankToEmpty(row.technicalRequirements()))
                .residenceAddress(blankToEmpty(row.residenceAddress()))
                .productionAddress(blankToEmpty(row.productionAddress()))
                .entrustedProduction(Boolean.TRUE.equals(row.entrustedProduction()))
                .selfProduction(Boolean.TRUE.equals(row.selfProduction()))
                .entrustedEnterprisesJson(entrustedJson(row.entrustedEnterprises()))
                .effectiveAt(row.effectiveDate().atStartOfDay())
                .build();
        snapshot.setTenantId(command.tenantId());
        requireSingle("新增迁移注册证快照", snapshotMapper.insert(snapshot));

        int sort = 1;
        for (EntrustedEnterpriseCommand enterprise : safeList(row.entrustedEnterprises())) {
            DccRegistrationCertificateSnapshotEntrustedDO entrusted =
                    DccRegistrationCertificateSnapshotEntrustedDO.builder()
                            .snapshotId(snapshot.getId())
                            .enterpriseId(enterprise.enterpriseId())
                            .enterpriseNameSnapshot(trim(enterprise.enterpriseNameSnapshot()))
                            .sortOrder(sort++)
                            .build();
            entrusted.setTenantId(command.tenantId());
            requireSingle("新增迁移受托企业", entrustedMapper.insert(entrusted));
        }

        requireSingle("更新迁移注册证关联",
                certificateMapper.update(null, new LambdaUpdateWrapper<DccRegistrationCertificateDO>()
                        .eq(DccRegistrationCertificateDO::getId, certificate.getId())
                        .eq(DccRegistrationCertificateDO::getTenantId, command.tenantId())
                        .set(DccRegistrationCertificateDO::getCurrentVersionId,
                                immediate ? version.getId() : null)
                        .set(DccRegistrationCertificateDO::getPendingVersionId,
                                immediate ? null : version.getId())
                        .set(DccRegistrationCertificateDO::getCurrentSnapshotId,
                                immediate ? snapshot.getId() : null)));

        ControlledContentKey key = ControlledContentKey.of(
                command.tenantId(), DCC_REGISTRATION_CERTIFICATE, String.valueOf(certificate.getId()));
        ControlledContentProjectionSnapshot before = ControlledContentProjectionSnapshot.of(key, null, null);
        ControlledContentProjectionSnapshot after = immediate
                ? ControlledContentProjectionSnapshot.of(key, version.getId(), null)
                : ControlledContentProjectionSnapshot.of(key, null, version.getId());
        if (immediate) {
            projectionService.registerActive(key, before, after,
                    certificate.getId(), version.getId(), "1", versionStatus, command.actorId(),
                    "注册证历史数据导入");
        } else {
            projectionService.registerReadyCandidate(key, before, after,
                    certificate.getId(), version.getId(), "1", versionStatus, command.actorId(),
                    "注册证历史数据导入后等待生效日期");
        }

        HistoricalImportAuditDetail detail = new HistoricalImportAuditDetail(
                command.sourceHash().toUpperCase(Locale.ROOT), row.sourceRow(), payloadHash,
                certificate.getId(), version.getId(), snapshot.getId(),
                restrictedReasons(row.restrictedReasons()));
        DccRegistrationCertificateAuditDO audit = DccRegistrationCertificateAuditDO.builder()
                .tenantId(command.tenantId())
                .ownerCompanyId(row.ownerCompanyId())
                .certificateId(certificate.getId())
                .versionId(version.getId())
                .snapshotId(snapshot.getId())
                .eventKey(eventKey)
                .eventType("HISTORICAL_IMPORT")
                .actorId(command.actorId())
                .result("SUCCESS")
                .requestTraceId(trim(command.requestTraceId()))
                .detailJson(JsonUtils.toJsonString(detail))
                .occurredAt(businessClock.now())
                .creator(String.valueOf(command.actorId()))
                .build();
        requireSingle("新增迁移审计记录", auditMapper.insert(audit));
        return new CommittedRow(detail);
    }

    private void validateBatch(BatchCommand command) {
        if (command == null || command.tenantId() == null || command.tenantId() <= 0
                || command.actorId() == null || command.actorId() <= 0
                || isBlank(command.sourceHash()) || isBlank(command.requestTraceId())
                || command.rows() == null || command.rows().isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        Long currentTenantId = TenantContextHolder.getRequiredTenantId();
        if (!Objects.equals(currentTenantId, command.tenantId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_TENANT_MISMATCH);
        }
    }

    private void validateRow(RowCommand row) {
        if (row == null || row.sourceRow() == null || row.sourceRow() <= 1
                || row.ownerCompanyId() == null || row.ownerCompanyId() <= 0
                || row.productMasterId() == null || row.productMasterId() <= 0
                || (row.projectCodeId() != null && row.projectCodeId() <= 0)
                || isBlank(row.productName()) || isBlank(row.registrantName())
                || isBlank(row.certificateNo()) || row.firstObtainedDate() == null
                || row.approvalDate() == null || row.effectiveDate() == null
                || row.expiryDate() == null || row.approvalDate().isAfter(row.effectiveDate())
                || !row.effectiveDate().isBefore(row.expiryDate())
                || isBlank(row.classification())
                || (!Boolean.TRUE.equals(row.entrustedProduction()) && !Boolean.TRUE.equals(row.selfProduction()))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        List<EntrustedEnterpriseCommand> entrustedEnterprises = safeList(row.entrustedEnterprises());
        if (Boolean.TRUE.equals(row.entrustedProduction()) && entrustedEnterprises.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        if (!Boolean.TRUE.equals(row.entrustedProduction()) && !entrustedEnterprises.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        for (EntrustedEnterpriseCommand enterprise : entrustedEnterprises) {
            if (enterprise.enterpriseId() == null || enterprise.enterpriseId() <= 0
                    || isBlank(enterprise.enterpriseNameSnapshot())) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
            }
        }
    }

    private HistoricalImportAuditDetail parseDetail(DccRegistrationCertificateAuditDO existing) {
        try {
            HistoricalImportAuditDetail detail =
                    JsonUtils.parseObject(existing.getDetailJson(), HistoricalImportAuditDetail.class);
            if (detail == null || isBlank(detail.getPayloadHash())) {
                throw new IllegalArgumentException("注册证迁移载荷校验值缺失");
            }
            return detail;
        } catch (RuntimeException ex) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_IDEMPOTENCY_CONFLICT);
        }
    }

    private String payloadHash(BatchCommand command, RowCommand row) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", command.tenantId());
        payload.put("sourceHash", command.sourceHash().toUpperCase(Locale.ROOT));
        payload.put("sourceRow", row.sourceRow());
        payload.put("ownerCompanyId", row.ownerCompanyId());
        payload.put("productMasterId", row.productMasterId());
        payload.put("projectCodeId", row.projectCodeId());
        payload.put("productName", trim(row.productName()));
        payload.put("registrantName", trim(row.registrantName()));
        payload.put("certificateNo", trim(row.certificateNo()));
        payload.put("firstObtainedDate", row.firstObtainedDate());
        payload.put("approvalDate", row.approvalDate());
        payload.put("effectiveDate", row.effectiveDate());
        payload.put("expiryDate", row.expiryDate());
        payload.put("classification", trim(row.classification()));
        payload.put("entrustedProduction", Boolean.TRUE.equals(row.entrustedProduction()));
        payload.put("selfProduction", Boolean.TRUE.equals(row.selfProduction()));
        payload.put("entrustedEnterprises", safeList(row.entrustedEnterprises()).stream()
                .sorted(Comparator.comparing(EntrustedEnterpriseCommand::enterpriseId))
                .map(enterprise -> List.of(enterprise.enterpriseId(), trim(enterprise.enterpriseNameSnapshot())))
                .toList());
        payload.put("restrictedReasons", restrictedReasons(row.restrictedReasons()));
        return sha256(JsonUtils.toJsonString(payload));
    }

    private String eventKey(String sourceHash, Integer sourceRow) {
        return "HISTORICAL_IMPORT:" + sourceHash.toUpperCase(Locale.ROOT) + ":" + sourceRow;
    }

    private String entrustedJson(List<EntrustedEnterpriseCommand> enterprises) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (EntrustedEnterpriseCommand enterprise : safeList(enterprises)) {
            rows.add(Map.of(
                    "enterpriseId", enterprise.enterpriseId(),
                    "enterpriseName", trim(enterprise.enterpriseNameSnapshot())));
        }
        return JsonUtils.toJsonString(rows);
    }

    private List<String> restrictedReasons(List<String> reasons) {
        return safeList(reasons).stream()
                .filter(reason -> !isBlank(reason))
                .map(reason -> reason.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();
    }

    private void requireSingle(String action, int updated) {
        if (updated != 1) {
            throw new IllegalStateException(action + "应影响一行，实际影响" + updated + "行");
        }
    }

    private String sha256(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 算法不可用", e);
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String blankToEmpty(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }

    private record CommittedRow(HistoricalImportAuditDetail detail) {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalImportAuditDetail {
        private String sourceHash;
        private Integer sourceRow;
        private String payloadHash;
        private Long outcomeCertificateId;
        private Long outcomeVersionId;
        private Long outcomeSnapshotId;
        private List<String> restrictedReasons;
    }

    public record BatchCommand(
            Long tenantId,
            Long actorId,
            String sourceHash,
            String requestTraceId,
            List<RowCommand> rows) {
    }

    public record RowCommand(
            Integer sourceRow,
            Long ownerCompanyId,
            Long productMasterId,
            Long projectCodeId,
            String productName,
            String registrantName,
            String certificateNo,
            LocalDate firstObtainedDate,
            LocalDate approvalDate,
            LocalDate effectiveDate,
            LocalDate expiryDate,
            String classification,
            String modelSpecification,
            String structureComposition,
            String intendedUse,
            String technicalRequirements,
            String residenceAddress,
            String productionAddress,
            Boolean entrustedProduction,
            Boolean selfProduction,
            List<EntrustedEnterpriseCommand> entrustedEnterprises,
            List<String> restrictedReasons) {
    }

    public record EntrustedEnterpriseCommand(
            Long enterpriseId,
            String enterpriseNameSnapshot) {
    }

    public record Result(
            int committedCount,
            int replayedCount,
            int restrictedCount) {
    }
}
