package cn.iocoder.yudao.module.dcc.service.productcatalog;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.productcatalog.DccProductCatalogDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.relation.DccDataRelationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.productcatalog.DccProductCatalogMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.relation.DccDataRelationMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateSnapshotDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateSnapshotMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DATA_RELATION_TARGET_INVALID;

@Service
public class DccProductCatalogRegistrationSyncService {

    private static final String CERTIFICATE_STATUS_ACTIVE = "ACTIVE";
    private static final String VERSION_STATUS_CURRENT = "CURRENT";

    @Resource
    private DccDataRelationMapper relationMapper;
    @Resource
    private DccProductCatalogMapper productCatalogMapper;
    @Resource
    private DccProjectCodeMapper projectCodeMapper;
    @Resource
    private DccRegistrationCertificateMapper certificateMapper;
    @Resource
    private DccRegistrationCertificateVersionMapper versionMapper;
    @Resource
    private DccRegistrationCertificateSnapshotMapper snapshotMapper;

    @Transactional(rollbackFor = Exception.class)
    public void syncRelation(DccDataRelationDO relation) {
        RegistrationProjection projection = loadProjection(relation.getRegistrationCertificateId());
        syncRelation(relation, projection);
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncByRegistrationCertificateId(Long registrationCertificateId) {
        RegistrationProjection projection = loadProjection(registrationCertificateId);
        List<DccDataRelationDO> relations = relationMapper.selectByRegistrationCertificateId(registrationCertificateId);
        for (DccDataRelationDO relation : relations) {
            syncRelation(relation, projection);
        }
    }

    private void syncRelation(DccDataRelationDO relation, RegistrationProjection projection) {
        DccProductCatalogDO catalog = productCatalogMapper.selectById(relation.getProductCatalogId());
        DccProjectCodeDO projectCode = projectCodeMapper.selectById(relation.getProjectCodeId());
        if (catalog == null || projectCode == null
                || !Objects.equals(projectCode.getId(), projection.certificate().getProjectCodeId())
                || !Objects.equals(relation.getRegistrationCertificateId(), projection.certificate().getId())) {
            throw exception(DCC_DATA_RELATION_TARGET_INVALID);
        }
        productCatalogMapper.update(null, new LambdaUpdateWrapper<DccProductCatalogDO>()
                .eq(DccProductCatalogDO::getId, catalog.getId())
                .set(DccProductCatalogDO::getProjectName, projectCode.getProjectName())
                .set(DccProductCatalogDO::getProjectCode, projectCode.getProjectCode())
                .set(DccProductCatalogDO::getRegistrationCertificateName, projection.snapshot().getProductName())
                .set(DccProductCatalogDO::getRegistrationCertificateNumber, projection.version().getCertificateNo())
                .set(DccProductCatalogDO::getCertificateHolder, projection.snapshot().getRegistrantName())
                .set(DccProductCatalogDO::getRegistrationPlace, projection.snapshot().getResidenceAddress())
                .set(DccProductCatalogDO::getEffectiveDate, formatDate(projection.version().getEffectiveDate()))
                .set(DccProductCatalogDO::getExpiryDate, formatDate(projection.version().getExpiryDate()))
                .set(DccProductCatalogDO::getClassification, projection.version().getClassification()));
    }

    private RegistrationProjection loadProjection(Long registrationCertificateId) {
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(registrationCertificateId);
        if (certificate == null || !CERTIFICATE_STATUS_ACTIVE.equals(certificate.getStatus())) {
            throw exception(DCC_DATA_RELATION_TARGET_INVALID);
        }
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(certificate.getCurrentVersionId());
        DccRegistrationCertificateSnapshotDO snapshot = snapshotMapper.selectById(certificate.getCurrentSnapshotId());
        if (version == null || snapshot == null
                || !Objects.equals(version.getCertificateId(), certificate.getId())
                || !Objects.equals(snapshot.getVersionId(), version.getId())
                || !VERSION_STATUS_CURRENT.equals(version.getStatus())
                || !Objects.equals(version.getTenantId(), certificate.getTenantId())
                || !Objects.equals(snapshot.getTenantId(), certificate.getTenantId())
                || StrUtil.isBlank(version.getCertificateNo())) {
            throw exception(DCC_DATA_RELATION_TARGET_INVALID);
        }
        return new RegistrationProjection(certificate, version, snapshot);
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private record RegistrationProjection(DccRegistrationCertificateDO certificate,
                                          DccRegistrationCertificateVersionDO version,
                                          DccRegistrationCertificateSnapshotDO snapshot) {
    }
}
