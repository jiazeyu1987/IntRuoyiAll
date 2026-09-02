package cn.iocoder.yudao.module.dcc.registrationcertificate.service.reference;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_NOT_EXISTS;

@Service
public class DccRegistrationCertificateFileReferenceServiceImpl
        implements DccRegistrationCertificateFileReferenceService {

    private static final String OWNER_TYPE_VERSION = "VERSION";
    private static final String FILE_KIND_REGISTRATION_CERTIFICATE = "REGISTRATION_CERTIFICATE";
    private static final String FILE_STATUS_BOUND = "BOUND";
    private static final String MASTER_STATUS_ACTIVE = "ACTIVE";
    private static final String VERSION_STATUS_CURRENT = "CURRENT";

    private final DccRegistrationCertificateFileMapper fileMapper;
    private final DccRegistrationCertificateVersionMapper versionMapper;
    private final DccRegistrationCertificateMapper certificateMapper;

    public DccRegistrationCertificateFileReferenceServiceImpl(
            DccRegistrationCertificateFileMapper fileMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateMapper certificateMapper) {
        this.fileMapper = require(fileMapper, "fileMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.certificateMapper = require(certificateMapper, "certificateMapper");
    }

    @Override
    public Optional<DccRegistrationCertificateFileReference> resolveByInfraFileId(Long infraFileId) {
        if (infraFileId == null || infraFileId <= 0) {
            return Optional.empty();
        }
        return executeTenantNeutral(() -> {
            List<DccRegistrationCertificateFileDO> files = fileMapper.selectList(
                    new LambdaQueryWrapperX<DccRegistrationCertificateFileDO>()
                            .eq(DccRegistrationCertificateFileDO::getInfraFileId, infraFileId)
                            .eq(DccRegistrationCertificateFileDO::getOwnerType, OWNER_TYPE_VERSION)
                            .eq(DccRegistrationCertificateFileDO::getFileKind, FILE_KIND_REGISTRATION_CERTIFICATE));
            if (files.isEmpty()) {
                return Optional.empty();
            }
            if (files.size() != 1) {
                throw new IllegalStateException("注册证文件引用不唯一：文件 ID="
                        + infraFileId + ", count=" + files.size());
            }
            return Optional.of(toReference(files.get(0), infraFileId));
        });
    }

    @Override
    public Optional<DccRegistrationCertificateFileReference> resolveByBusinessFileId(Long tenantId,
                                                                                    Long businessFileId) {
        if (tenantId == null || businessFileId == null || businessFileId <= 0) {
            return Optional.empty();
        }
        return executeTenantNeutral(() -> {
            DccRegistrationCertificateFileDO file = fileMapper.selectById(businessFileId);
            if (file == null || !Objects.equals(file.getTenantId(), tenantId)) {
                return Optional.empty();
            }
            return Optional.of(toReference(file, file.getInfraFileId()));
        });
    }

    @Override
    public DccRegistrationCertificateFileReference requireBoundByBusinessFileId(
            Long tenantId, Long businessFileId) {
        DccRegistrationCertificateFileDO file = fileMapper.selectById(businessFileId);
        if (file == null || !Objects.equals(file.getTenantId(), tenantId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        DccRegistrationCertificateFileReference reference = toReference(file, file.getInfraFileId());
        if (!FILE_STATUS_BOUND.equals(file.getStatus())) {
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return reference;
    }

    @Override
    public DccRegistrationCertificateFileReference requireCurrentByBusinessFileId(Long tenantId, Long businessFileId) {
        DccRegistrationCertificateFileDO file = fileMapper.selectById(businessFileId);
        if (file == null || !Objects.equals(file.getTenantId(), tenantId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        return toCurrentReference(file, file.getInfraFileId());
    }

    @Override
    public DccRegistrationCertificateFileReference requireCurrentByReference(Long tenantId, Long businessFileId,
                                                                             Long expectedInfraFileId) {
        DccRegistrationCertificateFileReference reference = executeTenantNeutral(() -> {
            DccRegistrationCertificateFileDO file = fileMapper.selectById(businessFileId);
            if (file == null) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
            }
            return toCurrentReference(file, expectedInfraFileId);
        });
        if (!Objects.equals(reference.tenantId(), tenantId)) {
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return reference;
    }

    @Override
    public DccRegistrationCertificateFileReference requireBoundByReference(
            Long tenantId, Long businessFileId, Long expectedInfraFileId) {
        DccRegistrationCertificateFileReference reference = executeTenantNeutral(() -> {
            DccRegistrationCertificateFileDO file = fileMapper.selectById(businessFileId);
            if (file == null) {
                throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
            }
            DccRegistrationCertificateFileReference resolved = toReference(file, expectedInfraFileId);
            if (!FILE_STATUS_BOUND.equals(file.getStatus())) {
                throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
            }
            return resolved;
        });
        if (!Objects.equals(reference.tenantId(), tenantId)) {
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return reference;
    }

    private DccRegistrationCertificateFileReference toCurrentReference(DccRegistrationCertificateFileDO file,
                                                                       Long expectedInfraFileId) {
        DccRegistrationCertificateFileReference reference = toReference(file, expectedInfraFileId);
        if (!FILE_STATUS_BOUND.equals(file.getStatus())) {
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(file.getOwnerId());
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(version.getCertificateId());
        if (!VERSION_STATUS_CURRENT.equals(version.getStatus())
                || !Objects.equals(certificate.getCurrentVersionId(), version.getId())
                || !MASTER_STATUS_ACTIVE.equals(certificate.getStatus())) {
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return reference;
    }

    private DccRegistrationCertificateFileReference toReference(DccRegistrationCertificateFileDO file,
                                                               Long expectedInfraFileId) {
        if (file.getId() == null || file.getTenantId() == null || file.getOwnerId() == null
                || file.getInfraFileId() == null || !Objects.equals(file.getInfraFileId(), expectedInfraFileId)
                || !OWNER_TYPE_VERSION.equals(file.getOwnerType())
                || !FILE_KIND_REGISTRATION_CERTIFICATE.equals(file.getFileKind())) {
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(file.getOwnerId());
        if (version == null || !Objects.equals(version.getTenantId(), file.getTenantId())) {
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(version.getCertificateId());
        if (certificate == null || !Objects.equals(certificate.getTenantId(), file.getTenantId())
                || certificate.getOwnerCompanyId() == null) {
            throw new ServiceException(CONTROLLED_FILE_ACCESS_DENIED);
        }
        return new DccRegistrationCertificateFileReference(file.getTenantId(), certificate.getOwnerCompanyId(),
                certificate.getId(), version.getId(), version.getVersionNo(), file.getId(), file.getInfraFileId(),
                version.getStatus(), file.getOriginalName(), file.getMimeType());
    }

    private <T> T executeTenantNeutral(Supplier<T> action) {
        boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setIgnore(true);
            return action.get();
        } finally {
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + "不能为空");
        }
        return value;
    }
}
