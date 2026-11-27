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
    @Resource private cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateProductBindingService productBindings;

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

    /** Read the current approved snapshot, including projects created through project/product approval. */
    public void enrichCurrentCertificate(cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO row) {
        if (row.getProjectCodeId() == null) return;
        Long certificateId = row.getRegistrationCertificateId();
        Long tenantId = cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        var summaries = new java.util.ArrayList<cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO.RegistrationCertificateSummary>();
        for (var id : productBindings.currentCertificateIds(tenantId, row.getId())) {
            var current = loadProjection(id);
            var currentFacts = cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(current.snapshot().getFactsJson());
            summaries.add(new cn.iocoder.yudao.module.dcc.controller.admin.productcatalog.vo.DccProductCatalogRespVO.RegistrationCertificateSummary(
                    id, current.snapshot().getProductName(), certificateNumber(currentFacts)));
        }
        row.setRegistrationCertificates(List.copyOf(summaries));
        if (certificateId == null) {
            if (summaries.size() != 1) return;
            certificateId = summaries.get(0).certificateId();
        }
        if (!productBindings.containsProduct(tenantId, certificateId, row.getId())) throw exception(DCC_DATA_RELATION_TARGET_INVALID);
        var certificate = certificateMapper.selectById(certificateId);
        var projection = loadProjection(certificate);
        var snapshot = projection.snapshot();
        var facts = cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(snapshot.getFactsJson());
        row.setRegistrationCertificateId(certificateId);
        row.setRegistrationCertificateName(snapshot.getProductName());
        row.setRegistrationCertificateNumber(certificateNumber(facts));
        row.setCertificateHolder(facts.getCertificateHolder());
        row.setRegistrationPlace(snapshot.getResidenceAddress());
        row.setEffectiveDate(formatDate(facts.getEffectiveDate()));
        row.setExpiryDate(formatDate(facts.getExpiryDate()));
        row.setClassification(facts.getClassification());
        row.setRegistrationInfoLink(facts.getRegistrationLink());
    }

    @Transactional(rollbackFor = Exception.class)
    public void syncRelation(DccDataRelationDO relation) {
        if (relation.getRegistrationCertificateId() == null) {
            DccProductCatalogDO catalog = productCatalogMapper.selectById(relation.getProductCatalogId());
            DccProjectCodeDO projectCode = projectCodeMapper.selectById(relation.getProjectCodeId());
            if (catalog == null || projectCode == null) {
                throw exception(DCC_DATA_RELATION_TARGET_INVALID);
            }
            productCatalogMapper.update(null, new LambdaUpdateWrapper<DccProductCatalogDO>()
                    .eq(DccProductCatalogDO::getId, catalog.getId())
                    .set(DccProductCatalogDO::getProjectName, projectCode.getProjectName())
                    .set(DccProductCatalogDO::getProjectCode, projectCode.getProjectCode()));
            return;
        }
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
                || !productBindings.containsProduct(projection.certificate().getTenantId(),projection.certificate().getId(),catalog.getId())
                || !productBindings.containsProject(projection.certificate().getTenantId(),projection.certificate().getId(),projectCode.getId())
                || !Objects.equals(relation.getRegistrationCertificateId(), projection.certificate().getId())) {
            throw exception(DCC_DATA_RELATION_TARGET_INVALID);
        }
        var facts = cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.parse(projection.snapshot().getFactsJson());
        productCatalogMapper.update(null, new LambdaUpdateWrapper<DccProductCatalogDO>()
                .eq(DccProductCatalogDO::getId, catalog.getId())
                .set(DccProductCatalogDO::getProjectName, projectCode.getProjectName())
                .set(DccProductCatalogDO::getProjectCode, projectCode.getProjectCode())
                .set(DccProductCatalogDO::getRegistrationCertificateName, projection.snapshot().getProductName())
                .set(DccProductCatalogDO::getRegistrationCertificateNumber, certificateNumber(facts))
                .set(DccProductCatalogDO::getCertificateHolder, facts.getCertificateHolder())
                .set(DccProductCatalogDO::getRegistrationInfoLink, facts.getRegistrationLink())
                .set(DccProductCatalogDO::getRegistrationPlace, projection.snapshot().getResidenceAddress())
                .set(DccProductCatalogDO::getEffectiveDate, formatDate(facts.getEffectiveDate()))
                .set(DccProductCatalogDO::getExpiryDate, formatDate(facts.getExpiryDate()))
                .set(DccProductCatalogDO::getClassification, facts.getClassification()));
    }

    private RegistrationProjection loadProjection(Long registrationCertificateId) {
        return loadProjection(certificateMapper.selectById(registrationCertificateId));
    }

    private RegistrationProjection loadProjection(DccRegistrationCertificateDO certificate) {
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
                || !Objects.equals(snapshot.getTenantId(), certificate.getTenantId())) {
            throw exception(DCC_DATA_RELATION_TARGET_INVALID);
        }
        return new RegistrationProjection(certificate, version, snapshot);
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.toString();
    }

    private String certificateNumber(cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts facts) {
        if ("CE".equals(facts.getCertificateType())) {
            return facts.getCeCertificates() == null ? null : facts.getCeCertificates().stream()
                    .map(cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts.CeCertificate::getCertificateNo)
                    .filter(value -> value != null && !value.isBlank()).distinct()
                    .collect(java.util.stream.Collectors.joining("、"));
        }
        if (facts.isFda()) return facts.getListingNumber();
        return facts.getCertificateNo();
    }

    private record RegistrationProjection(DccRegistrationCertificateDO certificate,
                                          DccRegistrationCertificateVersionDO version,
                                          DccRegistrationCertificateSnapshotDO snapshot) {
    }
}
