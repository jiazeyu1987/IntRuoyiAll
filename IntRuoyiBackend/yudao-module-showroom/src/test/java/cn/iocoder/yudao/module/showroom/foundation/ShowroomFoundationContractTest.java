package cn.iocoder.yudao.module.showroom.foundation;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomApprovalRouteContract;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomBackendRootContract;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomNotifyPrerequisiteChecker;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomPublishContract;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomRoleModelContract;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomV1ScopeContract;
import cn.iocoder.yudao.module.showroom.foundation.enums.ShowroomFieldTierEnum;
import cn.iocoder.yudao.module.showroom.foundation.meta.ShowroomFieldCatalog;
import cn.iocoder.yudao.module.showroom.foundation.meta.ShowroomFieldDisplaySupport;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowroomFoundationContractTest {

    @Test
    void backendRootShouldBeFrozenAndReadable() {
        assertEquals("yudao-module-showroom", ShowroomBackendRootContract.moduleName());
        assertTrue(ShowroomBackendRootContract.moduleRoot().endsWith("ruoyi-vue-pro/yudao-module-showroom"));
    }

    @Test
    void schemaBaselineShouldDeclareV1ShowroomTablesAndConstraintsOnly() throws Exception {
        String schema = Files.readString(findProjectDir().resolve("sql/showroom/20260519_showroom_v1_schema.sql"));
        List<String> requiredTables = List.of(
                "showroom_company",
                "showroom_company_revision",
                "showroom_product",
                "showroom_product_revision",
                "showroom_version_bundle",
                "showroom_product_revision_relation",
                "showroom_hall",
                "showroom_hall_product",
                "showroom_change_request",
                "showroom_change_request_item",
                "showroom_change_request_signature",
                "showroom_version_audit",
                "showroom_field_assignment",
                "showroom_product_comment",
                "showroom_narration_version",
                "showroom_preview_asset_version"
        );
        for (String table : requiredTables) {
            assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS `" + table + "`"), "Missing table " + table);
        }
        assertTrue(schema.contains("`voice` varchar(64) DEFAULT NULL"),
                "showroom_narration_version.voice must exist in schema baseline");
        assertTrue(schema.contains("`display_name_en` varchar(255) DEFAULT NULL"),
                "showroom_company.display_name_en must exist in schema baseline");
        assertTrue(schema.contains("`development_history_en` text DEFAULT NULL"),
                "showroom_company_revision.development_history_en must exist in schema baseline");
        assertTrue(schema.contains("`park_introduction_en` text DEFAULT NULL"),
                "showroom_company_revision.park_introduction_en must exist in schema baseline");
        assertTrue(schema.contains("`incubation_platform_en` text DEFAULT NULL"),
                "showroom_company_revision.incubation_platform_en must exist in schema baseline");
        assertTrue(schema.contains("`subsidiary_overview_en` text DEFAULT NULL"),
                "showroom_company_revision.subsidiary_overview_en must exist in schema baseline");
        assertTrue(schema.contains("`stock_info_en` text DEFAULT NULL"),
                "showroom_company_revision.stock_info_en must exist in schema baseline");
        assertTrue(schema.contains("`core_manufacturing_capability_en` text DEFAULT NULL"),
                "showroom_company_revision.core_manufacturing_capability_en must exist in schema baseline");
        assertTrue(schema.contains("`honors_awards_en` text DEFAULT NULL"),
                "showroom_company_revision.honors_awards_en must exist in schema baseline");
        assertTrue(schema.contains("`display_name_snapshot` varchar(255) DEFAULT NULL"),
                "showroom_company_revision.display_name_snapshot must exist in schema baseline");
        assertTrue(schema.contains("`display_name_en_snapshot` varchar(255) DEFAULT NULL"),
                "showroom_company_revision.display_name_en_snapshot must exist in schema baseline");
        assertTrue(schema.contains("`company_type_snapshot` varchar(32) DEFAULT NULL"),
                "showroom_company_revision.company_type_snapshot must exist in schema baseline");
        assertTrue(schema.contains("`name_en` varchar(255) DEFAULT NULL"),
                "showroom_hall.name_en must exist in schema baseline");
        assertTrue(schema.contains("`description_en` text DEFAULT NULL"),
                "showroom_hall.description_en must exist in schema baseline");
        assertTrue(schema.contains("`canvas_background_image_url` varchar(1024) DEFAULT NULL"),
                "showroom_hall.canvas_background_image_url must exist in schema baseline");
        assertTrue(schema.contains("`target_market_en` text DEFAULT NULL"),
                "showroom_product_revision.target_market_en must exist in schema baseline");
        assertTrue(schema.contains("`pipeline_layout_en` text DEFAULT NULL"),
                "showroom_product_revision.pipeline_layout_en must exist in schema baseline");
        assertTrue(schema.contains("`indication_content_en` text DEFAULT NULL"),
                "showroom_product_revision.indication_content_en must exist in schema baseline");
        assertTrue(schema.contains("`core_selling_points_en` text DEFAULT NULL"),
                "showroom_product_revision.core_selling_points_en must exist in schema baseline");
        assertTrue(schema.contains("`model_specification_en` text DEFAULT NULL"),
                "showroom_product_revision.model_specification_en must exist in schema baseline");
        assertTrue(schema.contains("`registration_certificate_en` text DEFAULT NULL"),
                "showroom_product_revision.registration_certificate_en must exist in schema baseline");
        assertTrue(schema.contains("`clinical_effect_en` text DEFAULT NULL"),
                "showroom_product_revision.clinical_effect_en must exist in schema baseline");
        assertTrue(schema.contains("`fim_status_en` varchar(255) DEFAULT NULL"),
                "showroom_product_revision.fim_status_en must exist in schema baseline");
        assertTrue(schema.contains("UNIQUE KEY `uk_showroom_product_code` (`tenant_id`, `product_code`)"));
        assertTrue(schema.contains("UNIQUE KEY `uk_showroom_hall_code` (`tenant_id`, `hall_code`)"));
        assertTrue(schema.contains("UNIQUE KEY `uk_showroom_version_bundle_revision` (`tenant_id`, `target_type`, `target_id`, `revision_id`)"));
        assertTrue(schema.contains("UNIQUE KEY `uk_showroom_version_bundle_no` (`tenant_id`, `target_type`, `target_id`, `revision_no`)"));
        assertTrue(schema.contains("UNIQUE KEY `uk_showroom_hall_product` (`tenant_id`, `hall_id`, `product_id`)"));
        assertTrue(schema.contains("KEY `idx_showroom_change_request_process` (`process_instance_id`)"));
        assertFalse(schema.contains("CREATE TABLE IF NOT EXISTS `system_notify_"));
        assertFalse(schema.contains("CREATE TABLE IF NOT EXISTS `bpm_"));
        assertFalse(schema.contains("CREATE TABLE IF NOT EXISTS `infra_file"));
        assertFalse(schema.contains("CREATE TABLE IF NOT EXISTS `ai_knowledge"));
    }

    @Test
    void mysqlReleaseMigrationsShouldWidenShowroomProductTargetMarket() throws Exception {
        Path mysqlSqlDir = findProjectDir().resolve("sql/mysql");
        String migrations = String.join("\n", Files.list(mysqlSqlDir)
                .filter(path -> path.getFileName().toString().endsWith(".sql"))
                .map(path -> {
                    try {
                        return Files.readString(path);
                    } catch (Exception ex) {
                        throw new IllegalStateException("Failed to read migration " + path, ex);
                    }
                })
                .toList());

        assertTrue(migrations.contains("MODIFY COLUMN `target_market` text DEFAULT NULL"),
                "sql/mysql release migrations must widen showroom_product_revision.target_market for product package imports");
    }

    @Test
    void unitTestSchemaShouldCoverSchemaRemediationTables() throws Exception {
        String schema = Files.readString(findProjectDir()
                .resolve("yudao-module-showroom/src/test/resources/sql/create_tables.sql"));
        List<String> requiredTables = List.of(
                "showroom_version_bundle",
                "showroom_product_revision_relation",
                "showroom_product_cover_batch_task",
                "showroom_product_cover_batch_task_item",
                "showroom_change_request",
                "showroom_change_request_item",
                "showroom_change_request_signature",
                "showroom_field_assignment",
                "showroom_product_comment",
                "showroom_narration_version",
                "showroom_preview_asset_version"
        );
        for (String table : requiredTables) {
            assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS \"" + table + "\""),
                    "Missing test table " + table);
        }
        assertTrue(schema.contains("\"voice\" varchar(64) DEFAULT NULL"),
                "showroom_narration_version.voice must exist in unit-test schema");
        assertTrue(schema.contains("\"display_name_en\" varchar(255) DEFAULT NULL"),
                "showroom_company.display_name_en must exist in unit-test schema");
        assertTrue(schema.contains("\"development_history_en\" clob DEFAULT NULL"),
                "showroom_company_revision.development_history_en must exist in unit-test schema");
        assertTrue(schema.contains("\"park_introduction_en\" clob DEFAULT NULL"),
                "showroom_company_revision.park_introduction_en must exist in unit-test schema");
        assertTrue(schema.contains("\"incubation_platform_en\" clob DEFAULT NULL"),
                "showroom_company_revision.incubation_platform_en must exist in unit-test schema");
        assertTrue(schema.contains("\"subsidiary_overview_en\" clob DEFAULT NULL"),
                "showroom_company_revision.subsidiary_overview_en must exist in unit-test schema");
        assertTrue(schema.contains("\"stock_info_en\" clob DEFAULT NULL"),
                "showroom_company_revision.stock_info_en must exist in unit-test schema");
        assertTrue(schema.contains("\"core_manufacturing_capability_en\" clob DEFAULT NULL"),
                "showroom_company_revision.core_manufacturing_capability_en must exist in unit-test schema");
        assertTrue(schema.contains("\"honors_awards_en\" clob DEFAULT NULL"),
                "showroom_company_revision.honors_awards_en must exist in unit-test schema");
        assertTrue(schema.contains("\"display_name_snapshot\" varchar(255) DEFAULT NULL"),
                "showroom_company_revision.display_name_snapshot must exist in unit-test schema");
        assertTrue(schema.contains("\"display_name_en_snapshot\" varchar(255) DEFAULT NULL"),
                "showroom_company_revision.display_name_en_snapshot must exist in unit-test schema");
        assertTrue(schema.contains("\"company_type_snapshot\" varchar(32) DEFAULT NULL"),
                "showroom_company_revision.company_type_snapshot must exist in unit-test schema");
        assertTrue(schema.contains("\"name_en\" varchar(255) DEFAULT NULL"),
                "showroom_hall.name_en must exist in unit-test schema");
        assertTrue(schema.contains("\"description_en\" clob DEFAULT NULL"),
                "showroom_hall.description_en must exist in unit-test schema");
        assertTrue(schema.contains("\"canvas_background_image_url\" varchar(1024) DEFAULT NULL"),
                "showroom_hall.canvas_background_image_url must exist in unit-test schema");
        assertTrue(schema.contains("\"target_market_en\" clob DEFAULT NULL"),
                "showroom_product_revision.target_market_en must exist in unit-test schema");
        assertTrue(schema.contains("\"pipeline_layout_en\" clob DEFAULT NULL"),
                "showroom_product_revision.pipeline_layout_en must exist in unit-test schema");
        assertTrue(schema.contains("\"indication_content_en\" clob DEFAULT NULL"),
                "showroom_product_revision.indication_content_en must exist in unit-test schema");
        assertTrue(schema.contains("\"core_selling_points_en\" clob DEFAULT NULL"),
                "showroom_product_revision.core_selling_points_en must exist in unit-test schema");
        assertTrue(schema.contains("\"model_specification_en\" clob DEFAULT NULL"),
                "showroom_product_revision.model_specification_en must exist in unit-test schema");
        assertTrue(schema.contains("\"registration_certificate_en\" clob DEFAULT NULL"),
                "showroom_product_revision.registration_certificate_en must exist in unit-test schema");
        assertTrue(schema.contains("\"clinical_effect_en\" clob DEFAULT NULL"),
                "showroom_product_revision.clinical_effect_en must exist in unit-test schema");
        assertTrue(schema.contains("\"fim_status_en\" varchar(255) DEFAULT NULL"),
                "showroom_product_revision.fim_status_en must exist in unit-test schema");
        assertTrue(schema.contains("\"password_update_time\" timestamp DEFAULT NULL"),
                "system_users.password_update_time must exist in unit-test schema preflight");
        assertTrue(schema.contains("CREATE TABLE IF NOT EXISTS \"showroom_public_site_binding\""),
                "public site binding table must exist for siteKey/stage release scope");
        assertTrue(schema.contains("\"site_key\" varchar(64) NOT NULL"),
                "release schema must declare site_key for scoped public release");
        assertTrue(schema.contains("\"stage\" varchar(16) NOT NULL"),
                "release schema must declare stage for scoped public release");
        assertTrue(schema.contains("CONSTRAINT \"uk_showroom_release_pointer_scope\" UNIQUE (\"tenant_id\", \"site_key\", \"stage\", \"pointer_key\")"),
                "release pointer current uniqueness must be tenant + site_key + stage scoped");
        assertTrue(schema.contains("CONSTRAINT \"uk_showroom_product_revision_relation\" UNIQUE (\"tenant_id\", \"product_revision_id\", \"related_product_id\", \"relation_type\")"));
        assertTrue(schema.contains("CONSTRAINT \"uk_showroom_cover_batch_task_item\" UNIQUE (\"tenant_id\", \"task_id\", \"product_id\")"));
        assertTrue(schema.contains("CONSTRAINT \"uk_showroom_version_bundle_revision\" UNIQUE (\"tenant_id\", \"target_type\", \"target_id\", \"revision_id\")"));
        assertTrue(schema.contains("CONSTRAINT \"uk_showroom_version_bundle_no\" UNIQUE (\"tenant_id\", \"target_type\", \"target_id\", \"revision_no\")"));
        assertTrue(schema.contains("CONSTRAINT \"uk_showroom_preview_asset_version_no\" UNIQUE (\"tenant_id\", \"target_type\", \"target_id\", \"version_no\")"));
        assertTrue(schema.contains("CONSTRAINT \"uk_showroom_narration_version_no\" UNIQUE (\"tenant_id\", \"target_type\", \"target_id\", \"audience_type\", \"language\", \"version_no\")"));
    }

    @Test
    void remediationPersistenceContractsShouldExposeDoAndMapperPairs() throws Exception {
        List<PersistenceContract> contracts = List.of(
                new PersistenceContract(
                        "showroom_version_bundle",
                        "cn.iocoder.yudao.module.showroom.dal.dataobject.version.ShowroomVersionBundleDO",
                        "cn.iocoder.yudao.module.showroom.dal.mysql.version.ShowroomVersionBundleMapper"
                ),
                new PersistenceContract(
                        "showroom_product_revision_relation",
                        "cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductRevisionRelationDO",
                        "cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductRevisionRelationMapper"
                ),
                new PersistenceContract(
                        "showroom_product_cover_batch_task",
                        "cn.iocoder.yudao.module.showroom.dal.dataobject.cover.ShowroomProductCoverBatchTaskDO",
                        "cn.iocoder.yudao.module.showroom.dal.mysql.cover.ShowroomProductCoverBatchTaskMapper"
                ),
                new PersistenceContract(
                        "showroom_product_cover_batch_task_item",
                        "cn.iocoder.yudao.module.showroom.dal.dataobject.cover.ShowroomProductCoverBatchTaskItemDO",
                        "cn.iocoder.yudao.module.showroom.dal.mysql.cover.ShowroomProductCoverBatchTaskItemMapper"
                ),
                new PersistenceContract(
                        "showroom_change_request",
                        "cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestDO",
                        "cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestMapper"
                ),
                new PersistenceContract(
                        "showroom_change_request_item",
                        "cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestItemDO",
                        "cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestItemMapper"
                ),
                new PersistenceContract(
                        "showroom_change_request_signature",
                        "cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestSignatureDO",
                        "cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomChangeRequestSignatureMapper"
                ),
                new PersistenceContract(
                        "showroom_field_assignment",
                        "cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomFieldAssignmentDO",
                        "cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomFieldAssignmentMapper"
                ),
                new PersistenceContract(
                        "showroom_product_comment",
                        "cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductCommentDO",
                        "cn.iocoder.yudao.module.showroom.dal.mysql.content.ShowroomProductCommentMapper"
                ),
                new PersistenceContract(
                        "showroom_narration_version",
                        "cn.iocoder.yudao.module.showroom.dal.dataobject.narration.ShowroomNarrationVersionDO",
                        "cn.iocoder.yudao.module.showroom.dal.mysql.narration.ShowroomNarrationVersionMapper"
                ),
                new PersistenceContract(
                        "showroom_preview_asset_version",
                        "cn.iocoder.yudao.module.showroom.dal.dataobject.asset.ShowroomPreviewAssetVersionDO",
                        "cn.iocoder.yudao.module.showroom.dal.mysql.asset.ShowroomPreviewAssetVersionMapper"
                )
        );
        for (PersistenceContract contract : contracts) {
            assertDataObjectContract(contract);
            assertMapperContract(contract);
        }
    }

    @Test
    void productFieldCatalogShouldExposeBasicAndAdvancedPartitions() {
        assertEquals(ShowroomFieldTierEnum.BASIC, ShowroomFieldCatalog.productField("cover_image").tier());
        assertEquals(ShowroomFieldTierEnum.ADVANCED,
                ShowroomFieldCatalog.productField("registration_certificate").tier());
        assertEquals(ShowroomFieldTierEnum.ADVANCED,
                ShowroomFieldCatalog.productField("clinical_effect").tier());
        assertEquals(ShowroomFieldTierEnum.ADVANCED,
                ShowroomFieldCatalog.productField("fim_status").tier());
        assertEquals(ShowroomFieldTierEnum.BASIC, ShowroomFieldCatalog.productField("name_cn").tier());
        assertEquals(ShowroomFieldTierEnum.BASIC, ShowroomFieldCatalog.productField("name_en").tier());
        assertEquals(ShowroomFieldTierEnum.BASIC, ShowroomFieldCatalog.productField("target_market_en").tier());
        assertEquals(ShowroomFieldTierEnum.BASIC, ShowroomFieldCatalog.productField("pipeline_layout_en").tier());
        assertEquals(ShowroomFieldTierEnum.BASIC, ShowroomFieldCatalog.productField("indication_content_en").tier());
        assertEquals(ShowroomFieldTierEnum.BASIC, ShowroomFieldCatalog.productField("core_selling_points_en").tier());
        assertEquals(ShowroomFieldTierEnum.BASIC, ShowroomFieldCatalog.productField("model_specification_en").tier());
        assertEquals(ShowroomFieldTierEnum.ADVANCED,
                ShowroomFieldCatalog.productField("registration_certificate_en").tier());
        assertEquals(ShowroomFieldTierEnum.ADVANCED,
                ShowroomFieldCatalog.productField("clinical_effect_en").tier());
        assertEquals(ShowroomFieldTierEnum.ADVANCED,
                ShowroomFieldCatalog.productField("fim_status_en").tier());
    }

    @Test
    void productFieldDisplayLabelsShouldUseSalesCountryAndBuSemantics() {
        assertEquals("BU", ShowroomFieldDisplaySupport.fieldLabel("PRODUCT", "pipeline_layout"));
        assertEquals("BU(英文)", ShowroomFieldDisplaySupport.fieldLabel("PRODUCT", "pipeline_layout_en"));
        assertEquals("BU", ShowroomFieldDisplaySupport.fieldLabelEn("PRODUCT", "pipeline_layout"));
        assertEquals("BU (English)", ShowroomFieldDisplaySupport.fieldLabelEn("PRODUCT", "pipeline_layout_en"));
        assertEquals("在售国家", ShowroomFieldDisplaySupport.fieldLabel("PRODUCT", "target_market"));
        assertEquals("在售国家(英文)", ShowroomFieldDisplaySupport.fieldLabel("PRODUCT", "target_market_en"));
        assertEquals("Countries on Sale", ShowroomFieldDisplaySupport.fieldLabelEn("PRODUCT", "target_market"));
        assertEquals("Countries on Sale (English)",
                ShowroomFieldDisplaySupport.fieldLabelEn("PRODUCT", "target_market_en"));
        assertEquals("卖点文案", ShowroomFieldDisplaySupport.fieldLabel("PRODUCT", "core_selling_points"));
        assertEquals("Selling Points Copy",
                ShowroomFieldDisplaySupport.fieldLabelEn("PRODUCT", "core_selling_points"));
    }

    @Test
    void publishContractShouldRequireEnglishProductName() {
        assertEquals(Set.of("name_en"), ShowroomPublishContract.requiredProductPublishFields());
    }

    @Test
    void approvalRouteShouldBeFixedAndFailWhenRequiredActorsAreMissing() {
        assertEquals(List.of("EDITOR", "DEPARTMENT_SUPERVISOR", "showroom_publicity", "FRONTSTAGE_VIEWER"),
                ShowroomRoleModelContract.fixedRoleModel());
        assertEquals(List.of("EDITOR", "DEPARTMENT_SUPERVISOR", "showroom_publicity"),
                ShowroomApprovalRouteContract.fixedRoute());
        ShowroomApprovalRouteContract.validatePrerequisites(10L, null, null, 30L);
        ShowroomApprovalRouteContract.validatePrerequisites(10L, 8L, null, 30L);
        assertThrows(IllegalStateException.class,
                () -> ShowroomApprovalRouteContract.validatePrerequisites(10L, 8L, 20L, null));
    }

    @Test
    void notifyPrerequisitesShouldFailBeforeSendWhenMessagePersistenceIsMissing() {
        assertThrows(IllegalStateException.class,
                () -> ShowroomNotifyPrerequisiteChecker.validateBeforeSend(1L, "SHOWROOM_ASSIGNMENT", null));
        assertThrows(IllegalStateException.class,
                () -> ShowroomNotifyPrerequisiteChecker.validateBeforeSend(1L, "", 100L));
    }

    @Test
    void v1ScopeShouldExcludeKnowledgeEntrypoints() {
        assertTrue(ShowroomV1ScopeContract.excludedEntrypoints().containsAll(
                Set.of("knowledge-base", "q-and-a", "knowledge-graph")));
        assertFalse(ShowroomV1ScopeContract.v1Entrypoints().contains("knowledge-base"));
        assertFalse(ShowroomV1ScopeContract.v1Entrypoints().contains("q-and-a"));
        assertFalse(ShowroomV1ScopeContract.v1Entrypoints().contains("knowledge-graph"));
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        return "yudao-module-showroom".equals(currentDir.getFileName().toString())
                ? currentDir.getParent()
                : currentDir;
    }

    private static void assertDataObjectContract(PersistenceContract contract) throws ClassNotFoundException {
        Class<?> dataObjectClass = Class.forName(contract.dataObjectClassName());
        assertTrue(BaseDO.class.isAssignableFrom(dataObjectClass),
                () -> "Data object must extend BaseDO: " + contract.dataObjectClassName());
        assertTrue(TenantBaseDO.class.isAssignableFrom(dataObjectClass),
                () -> "Mutable showroom data object must be tenant managed: " + contract.dataObjectClassName());
        assertTrue(dataObjectClass.isAnnotationPresent(TenantIgnore.class),
                () -> "Data object keeps @TenantIgnore only for compatibility; TenantBaseDO takes precedence: "
                        + contract.dataObjectClassName());
        TableName tableName = dataObjectClass.getAnnotation(TableName.class);
        assertNotNull(tableName, () -> "Missing @TableName on " + contract.dataObjectClassName());
        assertEquals(contract.tableName(), tableName.value());
    }

    private static void assertMapperContract(PersistenceContract contract) throws ClassNotFoundException {
        Class<?> mapperClass = Class.forName(contract.mapperClassName());
        assertTrue(BaseMapperX.class.isAssignableFrom(mapperClass),
                () -> "Mapper must extend BaseMapperX: " + contract.mapperClassName());
        assertTrue(mapperClass.isAnnotationPresent(Mapper.class),
                () -> "Mapper must declare @Mapper: " + contract.mapperClassName());
    }

    private record PersistenceContract(String tableName, String dataObjectClassName, String mapperClassName) {
    }

}
