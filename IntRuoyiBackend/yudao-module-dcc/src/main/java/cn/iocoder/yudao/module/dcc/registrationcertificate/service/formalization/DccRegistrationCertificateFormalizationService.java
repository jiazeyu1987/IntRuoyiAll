package cn.iocoder.yudao.module.dcc.registrationcertificate.service.formalization;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateEntrustedEnterprise;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateDraftState;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateResolvedDraft;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentKey;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentProjectionSnapshot;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentRegistrationProjectionService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_NOT_STAGED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_REQUIRED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_TENANT_MISMATCH;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_PROJECTION_MISMATCH;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_REGISTRATION_CERTIFICATE;

@Service
public class DccRegistrationCertificateFormalizationService {

    private final DccRegistrationCertificateMapper certificateMapper;
    private final DccRegistrationCertificateVersionMapper versionMapper;
    private final DccRegistrationCertificateFileMapper fileMapper;
    private final ControlledContentRegistrationProjectionService projectionService;
    private final DccRegistrationCertificateBusinessClock businessClock;

    public DccRegistrationCertificateFormalizationService(
            DccRegistrationCertificateMapper certificateMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateFileMapper fileMapper,
            ControlledContentRegistrationProjectionService projectionService,
            DccRegistrationCertificateBusinessClock businessClock) {
        this.certificateMapper = require(certificateMapper, "certificateMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.fileMapper = require(fileMapper, "fileMapper");
        this.projectionService = require(projectionService, "projectionService");
        this.businessClock = require(businessClock, "businessClock");
    }

    public DccRegistrationCertificateFormalizationResult formalize(
            DccRegistrationCertificateDraftState state,
            DccRegistrationCertificateResolvedDraft resolved,
            Long tenantId, Long actorId, Integer expectedRowVersion, Long businessFileId) {
        assertResolvedProjection(state, resolved);
        DccRegistrationCertificateFileDO file = requireStagedFile(
                tenantId, state.version().getId(), businessFileId);
        boolean immediate = !state.version().getEffectiveDate().isAfter(businessClock.businessDate());
        String versionStatus = immediate ? "CURRENT" : "PENDING_EFFECTIVE";
        String masterStatus = immediate ? "ACTIVE" : "PENDING_FIRST_EFFECTIVE";

        int versionUpdated = versionMapper.update(null,
                new LambdaUpdateWrapper<DccRegistrationCertificateVersionDO>()
                        .eq(DccRegistrationCertificateVersionDO::getId, state.version().getId())
                        .eq(DccRegistrationCertificateVersionDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateVersionDO::getCertificateId, state.certificate().getId())
                        .eq(DccRegistrationCertificateVersionDO::getStatus, "DRAFT")
                        .set(DccRegistrationCertificateVersionDO::getStatus, versionStatus)
                        .set(DccRegistrationCertificateVersionDO::getFormalizedAt, businessClock.now())
                        .set(DccRegistrationCertificateVersionDO::getFormalizedBy, actorId));
        requireSingle(versionUpdated, REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);

        LambdaUpdateWrapper<DccRegistrationCertificateDO> masterUpdate =
                new LambdaUpdateWrapper<DccRegistrationCertificateDO>()
                        .eq(DccRegistrationCertificateDO::getId, state.certificate().getId())
                        .eq(DccRegistrationCertificateDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateDO::getStatus, "DRAFT")
                        .eq(DccRegistrationCertificateDO::getRowVersion, expectedRowVersion)
                        .set(DccRegistrationCertificateDO::getStatus, masterStatus)
                        .set(DccRegistrationCertificateDO::getCurrentVersionId,
                                immediate ? state.version().getId() : null)
                        .set(DccRegistrationCertificateDO::getPendingVersionId,
                                immediate ? null : state.version().getId())
                        .set(DccRegistrationCertificateDO::getCurrentSnapshotId,
                                immediate ? state.snapshot().getId() : null)
                        .setSql("row_version = row_version + 1");
        requireSingle(certificateMapper.update(null, masterUpdate),
                REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);

        int fileUpdated = fileMapper.update(null, new LambdaUpdateWrapper<DccRegistrationCertificateFileDO>()
                .eq(DccRegistrationCertificateFileDO::getId, file.getId())
                .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateFileDO::getOwnerType, "VERSION")
                .eq(DccRegistrationCertificateFileDO::getOwnerId, state.version().getId())
                .eq(DccRegistrationCertificateFileDO::getFileKind, "REGISTRATION_CERTIFICATE")
                .eq(DccRegistrationCertificateFileDO::getStatus, "STAGED")
                .set(DccRegistrationCertificateFileDO::getStatus, "BOUND")
                .set(DccRegistrationCertificateFileDO::getBoundAt, businessClock.now())
                .set(DccRegistrationCertificateFileDO::getBoundBy, actorId));
        requireSingle(fileUpdated, REGISTRATION_CERTIFICATE_FILE_NOT_STAGED);

        ControlledContentKey key = ControlledContentKey.of(
                tenantId, DCC_REGISTRATION_CERTIFICATE, String.valueOf(state.certificate().getId()));
        ControlledContentProjectionSnapshot before = ControlledContentProjectionSnapshot.of(key, null, null);
        try {
            if (immediate) {
                projectionService.registerActive(key, before,
                        ControlledContentProjectionSnapshot.of(key, state.version().getId(), null),
                        state.certificate().getId(), state.version().getId(),
                        String.valueOf(state.version().getVersionNo()), versionStatus, actorId,
                        "首份注册证正式化后立即生效");
            } else {
                projectionService.registerReadyCandidate(key, before,
                        ControlledContentProjectionSnapshot.of(key, null, state.version().getId()),
                        state.certificate().getId(), state.version().getId(),
                        String.valueOf(state.version().getVersionNo()), versionStatus, actorId,
                        "首份注册证正式化后等待生效日期");
            }
        } catch (RuntimeException exception) {
            ServiceException mapped = new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
            mapped.initCause(exception);
            throw mapped;
        }
        return new DccRegistrationCertificateFormalizationResult(
                state.certificate().getId(), state.version().getId(), state.snapshot().getId(), file.getId());
    }

    private DccRegistrationCertificateFileDO requireStagedFile(Long tenantId, Long versionId, Long businessFileId) {
        DccRegistrationCertificateFileDO file;
        if (businessFileId == null) {
            List<DccRegistrationCertificateFileDO> candidates = fileMapper.selectList(
                    new LambdaQueryWrapperX<DccRegistrationCertificateFileDO>()
                            .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                            .eq(DccRegistrationCertificateFileDO::getOwnerType, "VERSION")
                            .eq(DccRegistrationCertificateFileDO::getOwnerId, versionId)
                            .eq(DccRegistrationCertificateFileDO::getFileKind, "REGISTRATION_CERTIFICATE")
                            .eq(DccRegistrationCertificateFileDO::getStatus, "STAGED"));
            if (candidates == null || candidates.isEmpty()) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
            }
            if (candidates.size() != 1) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
            }
            file = candidates.get(0);
        } else {
            if (businessFileId <= 0) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
            }
            file = fileMapper.selectById(businessFileId);
        }
        if (file == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_REQUIRED);
        }
        if (!tenantId.equals(file.getTenantId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_TENANT_MISMATCH);
        }
        if (!"VERSION".equals(file.getOwnerType()) || !versionId.equals(file.getOwnerId())
                || !"REGISTRATION_CERTIFICATE".equals(file.getFileKind())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_OWNER_CONFLICT);
        }
        if (!"STAGED".equals(file.getStatus())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_NOT_STAGED);
        }
        return file;
    }

    private void assertResolvedProjection(DccRegistrationCertificateDraftState state,
                                          DccRegistrationCertificateResolvedDraft resolved) {
        if (!resolved.productName().equals(state.snapshot().getProductName())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_PROJECTION_MISMATCH);
        }
        List<DccRegistrationCertificateEntrustedEnterprise> stored = state.entrustedProjection().stream()
                .map(row -> new DccRegistrationCertificateEntrustedEnterprise(
                        row.getEnterpriseId(), row.getEnterpriseNameSnapshot()))
                .toList();
        try {
            resolved.productionRelation().assertProjectionMatches(stored);
        } catch (IllegalArgumentException exception) {
            ServiceException mapped = new ServiceException(REGISTRATION_CERTIFICATE_PROJECTION_MISMATCH);
            mapped.initCause(exception);
            throw mapped;
        }
    }

    private static void requireSingle(int affected, cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        if (affected != 1) {
            throw new ServiceException(errorCode);
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }
}
