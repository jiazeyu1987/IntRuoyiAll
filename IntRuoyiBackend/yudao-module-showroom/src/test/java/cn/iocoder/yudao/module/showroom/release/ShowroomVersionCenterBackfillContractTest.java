package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.asset.ShowroomPreviewAssetVersionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomCompanyDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomCompanyRevisionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductRevisionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.narration.ShowroomNarrationVersionDO;
import cn.iocoder.yudao.module.showroom.dal.dataobject.version.ShowroomVersionBundleDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.asset.ShowroomPreviewAssetVersionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomCompanyMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomCompanyRevisionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.narration.ShowroomNarrationVersionMapper;
import cn.iocoder.yudao.module.showroom.dal.mysql.version.ShowroomVersionBundleMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ShowroomVersionCenterBackfillContractTest extends BaseDbUnitTest {

    @Resource
    private DataSource dataSource;
    @Resource
    private ShowroomCompanyMapper companyMapper;
    @Resource
    private ShowroomCompanyRevisionMapper companyRevisionMapper;
    @Resource
    private ShowroomProductMapper productMapper;
    @Resource
    private ShowroomProductRevisionMapper productRevisionMapper;
    @Resource
    private ShowroomNarrationVersionMapper narrationVersionMapper;
    @Resource
    private ShowroomPreviewAssetVersionMapper previewAssetVersionMapper;
    @Resource
    private ShowroomVersionBundleMapper versionBundleMapper;

    @Test
    void backfillShouldInsertReadableBundlesAndSkipSnapshotlessOrAmbiguousRows() throws Exception {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 5, 23, 10, 15);

        ShowroomCompanyRevisionDO readableCompany = insertCompanyRevision("OWNER-READABLE", "可回填公司", "Readable Company",
                "MAIN", 1, true, publishedAt);
        insertNarration("COMPANY", readableCompany.getCompanyId(), readableCompany.getId(), "ZH", 1, publishedAt);
        insertNarration("COMPANY", readableCompany.getCompanyId(), readableCompany.getId(), "EN", 1, publishedAt);

        ShowroomCompanyRevisionDO snapshotMissingCompany = insertCompanyRevision("OWNER-NO-SNAPSHOT", "缺快照公司",
                "Snapshot Missing Company", "MAIN", 1, false, publishedAt);
        insertNarration("COMPANY", snapshotMissingCompany.getCompanyId(), snapshotMissingCompany.getId(), "ZH", 1, publishedAt);
        insertNarration("COMPANY", snapshotMissingCompany.getCompanyId(), snapshotMissingCompany.getId(), "EN", 1, publishedAt);

        ShowroomCompanyRevisionDO duplicateNarrationCompany = insertCompanyRevision("OWNER-DUP-NARRATION", "多讲解公司",
                "Duplicate Narration Company", "MAIN", 1, true, publishedAt);
        insertNarration("COMPANY", duplicateNarrationCompany.getCompanyId(), duplicateNarrationCompany.getId(), "ZH", 1, publishedAt);
        insertNarration("COMPANY", duplicateNarrationCompany.getCompanyId(), duplicateNarrationCompany.getId(), "ZH", 2, publishedAt);
        insertNarration("COMPANY", duplicateNarrationCompany.getCompanyId(), duplicateNarrationCompany.getId(), "EN", 1, publishedAt);

        ShowroomProductRevisionDO readableProduct = insertProductRevision("PRODUCT-READABLE", "可回填产品",
                "Readable Product", 1, publishedAt);
        insertNarration("PRODUCT", readableProduct.getProductId(), readableProduct.getId(), "ZH", 1, publishedAt);
        insertNarration("PRODUCT", readableProduct.getProductId(), readableProduct.getId(), "EN", 1, publishedAt);
        insertPreview(readableProduct.getProductId(), readableProduct.getId(), 1, 91001L, publishedAt);

        ShowroomProductRevisionDO duplicatePreviewProduct = insertProductRevision("PRODUCT-DUP-PREVIEW", "多预览产品",
                "Duplicate Preview Product", 1, publishedAt);
        insertNarration("PRODUCT", duplicatePreviewProduct.getProductId(), duplicatePreviewProduct.getId(), "ZH", 1, publishedAt);
        insertNarration("PRODUCT", duplicatePreviewProduct.getProductId(), duplicatePreviewProduct.getId(), "EN", 1, publishedAt);
        insertPreview(duplicatePreviewProduct.getProductId(), duplicatePreviewProduct.getId(), 1, 92001L, publishedAt);
        insertPreview(duplicatePreviewProduct.getProductId(), duplicatePreviewProduct.getId(), 2, 92002L, publishedAt);

        executeNormalizedBackfill();

        List<ShowroomVersionBundleDO> readableCompanyBundles = versionBundleMapper.selectListByTarget("COMPANY",
                readableCompany.getCompanyId());
        assertEquals(1, readableCompanyBundles.size());
        assertNull(readableCompanyBundles.get(0).getReleasePreviewAssetVersionId());

        List<ShowroomVersionBundleDO> snapshotMissingCompanyBundles = versionBundleMapper.selectListByTarget("COMPANY",
                snapshotMissingCompany.getCompanyId());
        assertEquals(0, snapshotMissingCompanyBundles.size());

        List<ShowroomVersionBundleDO> duplicateNarrationCompanyBundles = versionBundleMapper.selectListByTarget("COMPANY",
                duplicateNarrationCompany.getCompanyId());
        assertEquals(0, duplicateNarrationCompanyBundles.size());

        List<ShowroomVersionBundleDO> readableProductBundles = versionBundleMapper.selectListByTarget("PRODUCT",
                readableProduct.getProductId());
        assertEquals(1, readableProductBundles.size());
        assertNotNull(readableProductBundles.get(0).getReleasePreviewAssetVersionId());

        List<ShowroomVersionBundleDO> duplicatePreviewProductBundles = versionBundleMapper.selectListByTarget("PRODUCT",
                duplicatePreviewProduct.getProductId());
        assertEquals(0, duplicatePreviewProductBundles.size());
    }

    private void executeNormalizedBackfill() throws Exception {
        String sql = Files.readString(Path.of("..", "sql", "showroom",
                        "20260523_showroom_version_center_backfill.sql"))
                .replace('`', '"')
                .replace("b'0'", "FALSE");
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String rawStatement : sql.split(";")) {
                String normalized = rawStatement.trim();
                if (normalized.isBlank()) {
                    continue;
                }
                statement.execute(normalized);
            }
        }
    }

    private ShowroomCompanyRevisionDO insertCompanyRevision(String companyCode, String displayName, String displayNameEn,
                                                            String companyType, int revisionNo, boolean withSnapshot,
                                                            LocalDateTime publishedAt) {
        ShowroomCompanyDO company = ShowroomCompanyDO.builder()
                .companyCode(companyCode)
                .displayName(displayName)
                .displayNameEn(displayNameEn)
                .companyType(companyType)
                .currentRevisionNo(revisionNo)
                .incompleteFlag(Boolean.FALSE)
                .status("LIVE")
                .build();
        company.setTenantId(TenantContextHolder.getRequiredTenantId());
        companyMapper.insert(company);
        ShowroomCompanyRevisionDO revision = ShowroomCompanyRevisionDO.builder()
                .companyId(company.getId())
                .revisionNo(revisionNo)
                .status("PUBLISHED")
                .developmentHistory("history-" + companyCode)
                .displayNameSnapshot(withSnapshot ? displayName : null)
                .displayNameEnSnapshot(withSnapshot ? displayNameEn : null)
                .companyTypeSnapshot(withSnapshot ? companyType : null)
                .approvedBy(900L)
                .publishedAt(publishedAt)
                .build();
        revision.setTenantId(TenantContextHolder.getRequiredTenantId());
        companyRevisionMapper.insert(revision);
        company.setCurrentRevisionId(revision.getId());
        companyMapper.updateById(company);
        return revision;
    }

    private ShowroomProductRevisionDO insertProductRevision(String productCode, String nameCn, String nameEn,
                                                            int revisionNo, LocalDateTime publishedAt) {
        ShowroomProductDO product = ShowroomProductDO.builder()
                .productCode(productCode)
                .currentRevisionNo(revisionNo)
                .incompleteFlag(Boolean.FALSE)
                .status("LIVE")
                .build();
        product.setTenantId(TenantContextHolder.getRequiredTenantId());
        productMapper.insert(product);
        ShowroomProductRevisionDO revision = ShowroomProductRevisionDO.builder()
                .productId(product.getId())
                .revisionNo(revisionNo)
                .status("PUBLISHED")
                .nameCn(nameCn)
                .nameEn(nameEn)
                .targetMarket("market-" + productCode)
                .targetMarketEn("market-en-" + productCode)
                .approvedBy(901L)
                .publishedAt(publishedAt)
                .build();
        revision.setTenantId(TenantContextHolder.getRequiredTenantId());
        productRevisionMapper.insert(revision);
        product.setCurrentRevisionId(revision.getId());
        productMapper.updateById(product);
        return revision;
    }

    private void insertNarration(String targetType, Long targetId, Long revisionId, String language, int versionNo,
                                 LocalDateTime publishedAt) {
        ShowroomNarrationVersionDO narration = ShowroomNarrationVersionDO.builder()
                .targetType(targetType)
                .targetId(targetId)
                .sourceRevisionId(revisionId)
                .audienceType("PUBLIC")
                .language(language)
                .versionNo(versionNo)
                .scriptText(targetType + "-" + language + "-" + versionNo)
                .audioFileId(80000L + versionNo)
                .audioDurationSeconds(60)
                .voice("ruoxi")
                .generationStatus("AUDIO_GENERATED")
                .status("PUBLISHED")
                .generatedByAi(Boolean.FALSE)
                .publishedAt(publishedAt)
                .build();
        narration.setTenantId(TenantContextHolder.getRequiredTenantId());
        narrationVersionMapper.insert(narration);
    }

    private void insertPreview(Long targetId, Long revisionId, int versionNo, Long imageFileId,
                               LocalDateTime publishedAt) {
        ShowroomPreviewAssetVersionDO previewAsset = ShowroomPreviewAssetVersionDO.builder()
                .targetType("PRODUCT")
                .targetId(targetId)
                .sourceRevisionId(revisionId)
                .versionNo(versionNo)
                .imageFileId(imageFileId)
                .status("PUBLISHED")
                .generatedByAi(Boolean.FALSE)
                .publishedAt(publishedAt)
                .build();
        previewAsset.setTenantId(TenantContextHolder.getRequiredTenantId());
        previewAssetVersionMapper.insert(previewAsset);
    }
}
