package cn.iocoder.yudao.module.dcc.registrationcertificate.service.association;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomySaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileTypeTaxonomyMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateFileDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.DccRegistrationCertificateVersionDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateFileMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateVersionMapper;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyPath;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FILE_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_NOT_EXISTS;

@Service
public class DccRegistrationCertificateProjectCodeFileAssociationService {

    public static final String BUSINESS_SOURCE_TYPE = "DCC_REGISTRATION_CERTIFICATE";

    private static final String ROOT_PARENT_ID = "0";
    private static final String FILE_OWNER_VERSION = "VERSION";
    private static final String FILE_OWNER_CHANGE = "CHANGE";
    private static final String FILE_KIND_REGISTRATION_CERTIFICATE = "REGISTRATION_CERTIFICATE";
    private static final String FILE_KIND_CHANGE_APPROVAL = "CHANGE_APPROVAL";
    private static final String FILE_STATUS_BOUND = "BOUND";
    private static final String TECHNICAL_DOCUMENT_CODE = "TECHNICAL_DOCUMENT";
    private static final String TECHNICAL_DOCUMENT_NAME = "技术文档";
    private static final String REGISTRATION_DOSSIER_CODE = "REGISTRATION_DOSSIER";
    private static final String REGISTRATION_DOSSIER_NAME = "注册资料汇编";
    private static final String REGISTRATION_CERTIFICATE_CODE = "REGISTRATION_CERTIFICATE";
    private static final String REGISTRATION_CERTIFICATE_NAME = "注册证";

    private final DccRegistrationCertificateMapper certificateMapper;
    private final DccRegistrationCertificateVersionMapper versionMapper;
    private final DccRegistrationCertificateFileMapper fileMapper;
    private final DccFileTypeTaxonomyMapper taxonomyMapper;
    private final DccFileTypeTaxonomyAdminService taxonomyAdminService;
    private final JdbcTemplate jdbcTemplate;

    public DccRegistrationCertificateProjectCodeFileAssociationService(
            DccRegistrationCertificateMapper certificateMapper,
            DccRegistrationCertificateVersionMapper versionMapper,
            DccRegistrationCertificateFileMapper fileMapper,
            DccFileTypeTaxonomyMapper taxonomyMapper,
            DccFileTypeTaxonomyAdminService taxonomyAdminService,
            JdbcTemplate jdbcTemplate) {
        this.certificateMapper = require(certificateMapper, "certificateMapper");
        this.versionMapper = require(versionMapper, "versionMapper");
        this.fileMapper = require(fileMapper, "fileMapper");
        this.taxonomyMapper = require(taxonomyMapper, "taxonomyMapper");
        this.taxonomyAdminService = require(taxonomyAdminService, "taxonomyAdminService");
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindVersionRegistrationFile(Long tenantId, Long versionId, Long businessFileId, Long actorId) {
        VersionProjectBinding binding = resolveVersionProjectBinding(tenantId, versionId);
        if (binding.projectCodeId() == null) {
            return;
        }
        DccFileTypeTaxonomyPath path = ensureRegistrationCertificateTaxonomyPath();
        bindBusinessFile(tenantId, businessFileId, FILE_OWNER_VERSION, versionId,
                FILE_KIND_REGISTRATION_CERTIFICATE, binding.projectCodeId(), path);
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindVersionRegistrationFiles(Long tenantId, Long versionId, Long actorId) {
        VersionProjectBinding binding = resolveVersionProjectBinding(tenantId, versionId);
        if (binding.projectCodeId() == null) {
            return;
        }
        List<Long> businessFileIds = fileMapper.selectList(new LambdaQueryWrapperX<DccRegistrationCertificateFileDO>()
                        .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                        .eq(DccRegistrationCertificateFileDO::getOwnerType, FILE_OWNER_VERSION)
                        .eq(DccRegistrationCertificateFileDO::getOwnerId, versionId)
                        .eq(DccRegistrationCertificateFileDO::getFileKind, FILE_KIND_REGISTRATION_CERTIFICATE)
                        .eq(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_BOUND))
                .stream()
                .map(DccRegistrationCertificateFileDO::getId)
                .filter(Objects::nonNull)
                .toList();
        if (businessFileIds.isEmpty()) {
            return;
        }
        DccFileTypeTaxonomyPath path = ensureRegistrationCertificateTaxonomyPath();
        for (Long businessFileId : businessFileIds) {
            bindBusinessFile(tenantId, businessFileId, FILE_OWNER_VERSION, versionId,
                    FILE_KIND_REGISTRATION_CERTIFICATE, binding.projectCodeId(), path);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void bindChangeApprovalFile(Long tenantId, Long projectCodeId, Long changeId, Long businessFileId,
                                       Long actorId) {
        Long actualProjectCodeId = resolveChangeProjectCodeId(tenantId, changeId);
        if (actualProjectCodeId == null) {
            return;
        }
        if (projectCodeId != null && !Objects.equals(projectCodeId, actualProjectCodeId)) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
        DccFileTypeTaxonomyPath path = ensureRegistrationCertificateTaxonomyPath();
        bindBusinessFile(tenantId, businessFileId, FILE_OWNER_CHANGE, changeId,
                FILE_KIND_CHANGE_APPROVAL, actualProjectCodeId, path);
    }

    public List<DccControlledFileRespVO> listAssociatedRows(Long projectCodeId, String keyword, String status) {
        if (projectCodeId == null || projectCodeId <= 0) {
            return List.of();
        }
        String normalizedKeyword = StrUtil.trimToNull(keyword);
        String normalizedStatus = StrUtil.trimToNull(status);
        List<RegistrationAssociatedRow> rows = jdbcTemplate.query("""
                        SELECT f.id AS business_file_id,
                               f.dcc_project_code_id,
                               f.file_type_taxonomy_id,
                               f.file_type_level1,
                               f.file_type_level2,
                               f.file_type_level3,
                               f.file_type_level4,
                               f.file_type_level5,
                               f.original_name,
                               f.mime_type,
                               f.status AS file_status,
                               f.bound_at,
                               c.id AS certificate_id,
                               c.product_master_id,
                               v.id AS version_id,
                               v.version_no,
                               v.certificate_no,
                               v.effective_date,
                               s.product_name
                          FROM dcc_registration_certificate_file f
                          JOIN dcc_registration_certificate_version v
                            ON v.tenant_id = f.tenant_id
                           AND v.id = f.owner_id
                          JOIN dcc_registration_certificate c
                            ON c.tenant_id = f.tenant_id
                           AND c.id = v.certificate_id
                          LEFT JOIN dcc_registration_certificate_snapshot s
                            ON s.tenant_id = f.tenant_id
                           AND s.version_id = v.id
                         WHERE f.deleted = 0
                           AND f.owner_type = 'VERSION'
                           AND f.file_kind = 'REGISTRATION_CERTIFICATE'
                           AND f.status = 'BOUND'
                           AND f.dcc_project_code_id = ?
                        UNION ALL
                        SELECT f.id AS business_file_id,
                               f.dcc_project_code_id,
                               f.file_type_taxonomy_id,
                               f.file_type_level1,
                               f.file_type_level2,
                               f.file_type_level3,
                               f.file_type_level4,
                               f.file_type_level5,
                               f.original_name,
                               f.mime_type,
                               f.status AS file_status,
                               f.bound_at,
                               c.id AS certificate_id,
                               c.product_master_id,
                               v.id AS version_id,
                               v.version_no,
                               v.certificate_no,
                               v.effective_date,
                               s.product_name
                          FROM dcc_registration_certificate_file f
                          JOIN dcc_registration_certificate_change ch
                            ON ch.tenant_id = f.tenant_id
                           AND ch.id = f.owner_id
                           AND ch.deleted = 0
                          JOIN dcc_registration_certificate c
                            ON c.tenant_id = ch.tenant_id
                           AND c.id = ch.certificate_id
                          JOIN dcc_registration_certificate_version v
                            ON v.tenant_id = ch.tenant_id
                           AND v.id = ch.source_version_id
                          LEFT JOIN dcc_registration_certificate_snapshot s
                            ON s.tenant_id = ch.tenant_id
                           AND s.id = ch.resulting_snapshot_id
                         WHERE f.deleted = 0
                           AND f.owner_type = 'CHANGE'
                           AND f.file_kind = 'CHANGE_APPROVAL'
                           AND f.status = 'BOUND'
                           AND f.dcc_project_code_id = ?
                         ORDER BY business_file_id
                        """,
                (rs, rowNum) -> new RegistrationAssociatedRow(
                        rs.getLong("business_file_id"),
                        rs.getLong("dcc_project_code_id"),
                        rs.getObject("file_type_taxonomy_id", Long.class),
                        rs.getString("file_type_level1"),
                        rs.getString("file_type_level2"),
                        rs.getString("file_type_level3"),
                        rs.getString("file_type_level4"),
                        rs.getString("file_type_level5"),
                        rs.getString("original_name"),
                        rs.getString("mime_type"),
                        rs.getString("file_status"),
                        rs.getObject("bound_at", LocalDateTime.class),
                        rs.getLong("certificate_id"),
                        rs.getObject("product_master_id", Long.class),
                        rs.getLong("version_id"),
                        rs.getInt("version_no"),
                        rs.getString("certificate_no"),
                        rs.getObject("effective_date", LocalDate.class),
                        rs.getString("product_name")),
                projectCodeId, projectCodeId);
        return rows.stream()
                .filter(row -> normalizedStatus == null || Objects.equals(normalizedStatus, row.fileStatus()))
                .filter(row -> matchesKeyword(row, normalizedKeyword))
                .map(this::toRespVO)
                .toList();
    }

    public Map<Long, Long> countAssociatedFilesByProjectCodeIds(Set<Long> projectCodeIds) {
        if (projectCodeIds == null || projectCodeIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = projectCodeIds.stream().map(ignored -> "?").collect(Collectors.joining(","));
        Object[] args = projectCodeIds.toArray();
        return jdbcTemplate.query("""
                        SELECT dcc_project_code_id, COUNT(*) AS file_count
                          FROM dcc_registration_certificate_file
                         WHERE deleted = 0
                           AND status = 'BOUND'
                           AND dcc_project_code_id IN (%s)
                         GROUP BY dcc_project_code_id
                        """.formatted(placeholders), rs -> {
                    Map<Long, Long> result = new LinkedHashMap<>();
                    while (rs.next()) {
                        result.put(rs.getLong("dcc_project_code_id"), rs.getLong("file_count"));
                    }
                    return result;
                }, args);
    }

    private VersionProjectBinding resolveVersionProjectBinding(Long tenantId, Long versionId) {
        if (tenantId == null || tenantId <= 0 || versionId == null || versionId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FORMALIZATION_CONFLICT);
        }
        DccRegistrationCertificateVersionDO version = versionMapper.selectById(versionId);
        if (version == null || !Objects.equals(tenantId, version.getTenantId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        DccRegistrationCertificateDO certificate = certificateMapper.selectById(version.getCertificateId());
        if (certificate == null || !Objects.equals(tenantId, certificate.getTenantId())) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_NOT_EXISTS);
        }
        return new VersionProjectBinding(version.getCertificateId(), certificate.getProjectCodeId());
    }

    private Long resolveChangeProjectCodeId(Long tenantId, Long changeId) {
        if (tenantId == null || tenantId <= 0 || changeId == null || changeId <= 0) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
        List<Long> rows = jdbcTemplate.query("""
                        SELECT c.project_code_id
                          FROM dcc_registration_certificate_change ch
                          JOIN dcc_registration_certificate c
                            ON c.tenant_id = ch.tenant_id
                           AND c.id = ch.certificate_id
                         WHERE ch.tenant_id = ?
                           AND ch.id = ?
                           AND ch.deleted = 0
                        """,
                (rs, rowNum) -> rs.getObject("project_code_id", Long.class), tenantId, changeId);
        if (rows.isEmpty()) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
        return rows.get(0);
    }

    private void bindBusinessFile(Long tenantId, Long businessFileId, String ownerType, Long ownerId,
                                  String fileKind, Long projectCodeId, DccFileTypeTaxonomyPath path) {
        int affected = fileMapper.update(null, new LambdaUpdateWrapper<DccRegistrationCertificateFileDO>()
                .eq(DccRegistrationCertificateFileDO::getId, businessFileId)
                .eq(DccRegistrationCertificateFileDO::getTenantId, tenantId)
                .eq(DccRegistrationCertificateFileDO::getOwnerType, ownerType)
                .eq(DccRegistrationCertificateFileDO::getOwnerId, ownerId)
                .eq(DccRegistrationCertificateFileDO::getFileKind, fileKind)
                .eq(DccRegistrationCertificateFileDO::getStatus, FILE_STATUS_BOUND)
                .set(DccRegistrationCertificateFileDO::getDccProjectCodeId, projectCodeId)
                .set(DccRegistrationCertificateFileDO::getFileTypeTaxonomyId, path.id())
                .set(DccRegistrationCertificateFileDO::getFileTypeLevel1, path.level1())
                .set(DccRegistrationCertificateFileDO::getFileTypeLevel2, path.level2())
                .set(DccRegistrationCertificateFileDO::getFileTypeLevel3, path.level3())
                .set(DccRegistrationCertificateFileDO::getFileTypeLevel4, path.level4())
                .set(DccRegistrationCertificateFileDO::getFileTypeLevel5, path.level5()));
        if (affected != 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
    }

    private DccFileTypeTaxonomyPath ensureRegistrationCertificateTaxonomyPath() {
        DccFileTypeTaxonomyDO root = ensureTaxonomyNode(null, TECHNICAL_DOCUMENT_CODE, TECHNICAL_DOCUMENT_NAME, 1);
        DccFileTypeTaxonomyDO dossier = ensureTaxonomyNode(root.getId(), REGISTRATION_DOSSIER_CODE,
                REGISTRATION_DOSSIER_NAME, 10);
        DccFileTypeTaxonomyDO certificate = ensureTaxonomyNode(dossier.getId(), REGISTRATION_CERTIFICATE_CODE,
                REGISTRATION_CERTIFICATE_NAME, 1);
        return taxonomyAdminService.resolveActivePath(certificate.getId());
    }

    private DccFileTypeTaxonomyDO ensureTaxonomyNode(Long parentId, String code, String name, Integer sort) {
        Long normalizedParentId = parentId == null ? 0L : parentId;
        DccFileTypeTaxonomyDO existing = selectActiveTaxonomyNode(normalizedParentId, name);
        if (existing != null) {
            return existing;
        }
        DccFileTypeTaxonomySaveReqVO reqVO = new DccFileTypeTaxonomySaveReqVO();
        reqVO.setParentId(normalizedParentId);
        reqVO.setCode(code);
        reqVO.setName(name);
        reqVO.setActive(true);
        reqVO.setSort(sort);
        reqVO.setRemark("注册证模块自动归档分类");
        try {
            Long id = taxonomyAdminService.createTaxonomy(reqVO);
            return taxonomyMapper.selectById(id);
        } catch (ServiceException | DuplicateKeyException exception) {
            DccFileTypeTaxonomyDO createdByConcurrentRequest = selectActiveTaxonomyNode(normalizedParentId, name);
            if (createdByConcurrentRequest != null) {
                return createdByConcurrentRequest;
            }
            throw exception;
        }
    }

    private DccFileTypeTaxonomyDO selectActiveTaxonomyNode(Long parentId, String name) {
        List<DccFileTypeTaxonomyDO> rows = taxonomyMapper.selectList(
                new LambdaQueryWrapperX<DccFileTypeTaxonomyDO>()
                        .eq(DccFileTypeTaxonomyDO::getParentId, parentId)
                        .eq(DccFileTypeTaxonomyDO::getName, name)
                        .eq(DccFileTypeTaxonomyDO::getActive, true));
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        if (rows.size() > 1) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_FILE_CONFLICT);
        }
        return rows.get(0);
    }

    private DccControlledFileRespVO toRespVO(RegistrationAssociatedRow row) {
        DccControlledFileRespVO respVO = new DccControlledFileRespVO();
        respVO.setId(row.businessFileId());
        respVO.setBusinessSourceType(BUSINESS_SOURCE_TYPE);
        respVO.setRegistrationCertificateId(row.certificateId());
        respVO.setRegistrationCertificateVersionId(row.versionId());
        respVO.setRegistrationCertificateBusinessFileId(row.businessFileId());
        respVO.setTitle(row.originalName());
        respVO.setFileName(row.originalName());
        respVO.setContentType(row.mimeType());
        respVO.setFileNumber(row.certificateNo());
        respVO.setProductMasterId(row.productMasterId());
        respVO.setProductName(row.productName());
        respVO.setDccProjectCodeId(row.projectCodeId());
        respVO.setFileTypeTaxonomyId(row.fileTypeTaxonomyId());
        respVO.setFileTypeLevel1(row.fileTypeLevel1());
        respVO.setFileTypeLevel2(row.fileTypeLevel2());
        respVO.setFileTypeLevel3(row.fileTypeLevel3());
        respVO.setFileTypeLevel4(row.fileTypeLevel4());
        respVO.setFileTypeLevel5(row.fileTypeLevel5());
        respVO.setVersionNo(String.valueOf(row.versionNo()));
        respVO.setEffectiveDate(row.effectiveDate());
        respVO.setStatus(row.fileStatus());
        respVO.setPublishedTime(row.boundAt());
        respVO.setCanPreview(true);
        respVO.setCanDownload(true);
        respVO.setCanPrint(false);
        return respVO;
    }

    private boolean matchesKeyword(RegistrationAssociatedRow row, String keyword) {
        if (keyword == null) {
            return true;
        }
        return contains(row.originalName(), keyword)
                || contains(row.certificateNo(), keyword)
                || contains(row.productName(), keyword)
                || contains(row.fileTypeLevel2(), keyword)
                || contains(row.fileTypeLevel3(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }

    private record VersionProjectBinding(Long certificateId, Long projectCodeId) {
    }

    private record RegistrationAssociatedRow(
            Long businessFileId,
            Long projectCodeId,
            Long fileTypeTaxonomyId,
            String fileTypeLevel1,
            String fileTypeLevel2,
            String fileTypeLevel3,
            String fileTypeLevel4,
            String fileTypeLevel5,
            String originalName,
            String mimeType,
            String fileStatus,
            LocalDateTime boundAt,
            Long certificateId,
            Long productMasterId,
            Long versionId,
            Integer versionNo,
            String certificateNo,
            LocalDate effectiveDate,
            String productName) {
    }
}
