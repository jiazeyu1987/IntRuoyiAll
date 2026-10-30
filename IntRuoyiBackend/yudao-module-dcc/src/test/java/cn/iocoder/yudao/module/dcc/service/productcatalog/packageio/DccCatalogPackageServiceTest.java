package cn.iocoder.yudao.module.dcc.service.productcatalog.packageio;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentRegistrationProjectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccCatalogPackageServiceTest {
    JdbcTemplate jdbc;
    DccCatalogPackageService service;
    List<DccCatalogPackageService.Spec> specs;
    MdmCompanyScopeApi scope;
    cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService security;
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        TenantContextHolder.setTenantId(10L);
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        var target = new DccCatalogPackageService();
        specs = (List<DccCatalogPackageService.Spec>) ReflectionTestUtils.getField(DccCatalogPackageService.class, "SPECS");
        Set<String> tables = new HashSet<>();
        for (var spec : specs) {
            if (!tables.add(spec.table())) continue;
            StringBuilder ddl = new StringBuilder("CREATE TABLE " + spec.table() + " (id BIGINT AUTO_INCREMENT PRIMARY KEY");
            for (var field : spec.columns()) if (!field.equals("id") && !Set.of("certificate_key", "origin_tenant_id", "origin_row_no", "deleted").contains(field)) ddl.append(",").append(field).append(" VARCHAR(10000)");
            ddl.append(",tenant_id BIGINT,deleted INT DEFAULT 0,creator VARCHAR(64),updater VARCHAR(64),update_time TIMESTAMP)");
            jdbc.execute(ddl.toString());
        }
        jdbc.execute("CREATE TABLE dcc_catalog_workbook_origin(tenant_id BIGINT,product_catalog_id BIGINT,data_source VARCHAR(64),origin_tenant_id BIGINT,origin_row_no INT,PRIMARY KEY(tenant_id,product_catalog_id),UNIQUE(tenant_id,data_source,origin_tenant_id,origin_row_no))");
        jdbc.execute("CREATE UNIQUE INDEX catalog_source_row ON dcc_product_catalog(data_source,original_row_no)");
        jdbc.execute("CREATE TABLE system_users(id BIGINT,tenant_id BIGINT,deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE dcc_product_catalog_source_sequence(data_source VARCHAR(64) PRIMARY KEY,last_row_no INT)");
        jdbc.update("INSERT INTO dcc_product_catalog_source_sequence VALUES('W01',2)");
        ReflectionTestUtils.setField(target, "jdbc", jdbc);
        var allocator = new cn.iocoder.yudao.module.dcc.service.productcatalog.DccProductCatalogRowNumberAllocator();
        ReflectionTestUtils.setField(allocator, "jdbc", jdbc);
        ReflectionTestUtils.setField(target, "rowNumberAllocator", allocator);
        scope = mock(MdmCompanyScopeApi.class);
        security = mock(cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService.class);
        ReflectionTestUtils.setField(target, "security", security);
        ReflectionTestUtils.setField(target, "companyScope", scope);
        ReflectionTestUtils.setField(target, "json", new ObjectMapper());
        ReflectionTestUtils.setField(target, "catalogSync", mock(cn.iocoder.yudao.module.dcc.service.productcatalog.DccProductCatalogRegistrationSyncService.class));
        ReflectionTestUtils.setField(target, "projection", mock(ControlledContentRegistrationProjectionService.class));
        var factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(ds), new AnnotationTransactionAttributeSource()));
        service = (DccCatalogPackageService) factory.getProxy();
        jdbc.update("INSERT INTO mdm_enterprise(id,enterprise_code,name,type,status,tenant_id) VALUES(1,'C1','关联公司','ENTRUSTED_PARTY','ENABLE',10),(2,'C2','授权公司','OWNED_COMPANY','ENABLE',10),(3,'OTHER','其他租户','OWNED_COMPANY','ENABLE',11)");
        jdbc.update("INSERT INTO mdm_product(id,product_code,tenant_id) VALUES(1,'P1',10)");
        jdbc.update("INSERT INTO dcc_project_code(id,project_name,project_code,product_master_id,tenant_id) VALUES(1,'项目一','PR1','1',10)");
        jdbc.update("INSERT INTO dcc_product_catalog(id,data_source,original_row_no,project_code,product,tenant_id) VALUES(1,'W01','2','PR1','产品',10)");
        jdbc.update("INSERT INTO dcc_registration_certificate(id,owner_company_id,product_master_id,project_code_id,current_version_id,current_snapshot_id,status,tenant_id) VALUES(1,'2','1','1','1','1','ACTIVE',10)");
        jdbc.update("INSERT INTO dcc_registration_certificate_version(id,certificate_id,version_no,certificate_no,status,tenant_id) VALUES(1,'1','1','REG001','CURRENT',10)");
        jdbc.update("INSERT INTO dcc_registration_certificate_snapshot(id,version_id,revision_no,entrusted_enterprises_json,tenant_id) VALUES(1,'1','1','[{\"enterpriseId\":1,\"enterpriseName\":\"关联公司\"}]',10)");
        jdbc.update("INSERT INTO dcc_registration_certificate_snapshot_entrusted(id,snapshot_id,enterprise_id,tenant_id) VALUES(1,'1','1',10)");
        jdbc.update("INSERT INTO dcc_data_relation(id,product_catalog_id,project_code_id,registration_certificate_id,tenant_id) VALUES(1,'1','1','1',10)");
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @SuppressWarnings("unchecked") List<DccCatalogWorkbook.SheetData> read(byte[] data) throws Exception {
        var schema = (List<DccCatalogWorkbook.SheetData>) ReflectionTestUtils.invokeMethod(new DccCatalogPackageService(), "schema", 10L);
        return DccCatalogWorkbook.read(data, schema);
    }
    Map<String,String> row(List<DccCatalogWorkbook.SheetData> sheets, String name) {
        return sheets.stream().filter(s -> s.name().equals(name)).findFirst().orElseThrow().rows().get(0);
    }
    @Test void exportsTenantWithNoCertificatesWithoutCallingNonEmptyCompanyValidator() throws Exception {
        TenantContextHolder.setTenantId(11L);
        doThrow(new IllegalArgumentException("companyIds不能为空")).when(scope).validateUserCompanyAccessBatch(any(), any());
        byte[] workbook = service.exportWorkbook();
        assertTrue(workbook.length > 0);
        verifyNoInteractions(scope);
    }
    @Test void exportStillEnforcesCompanyScopeWhenCertificatesExist() {
        doThrow(new IllegalArgumentException("公司无权访问")).when(scope).validateUserCompanyAccessBatch(any(), eq(List.of(2L)));
        assertThrows(IllegalArgumentException.class, () -> service.exportWorkbook());
    }
    @Test void administratorCanExportAndImportWithoutCompanyGrantsWithinCurrentTenant() throws Exception {
        when(security.hasAnyRoles("super_admin", "tenant_admin")).thenReturn(true);
        doThrow(new IllegalArgumentException("未授权公司")).when(scope).validateUserCompanyAccessBatch(any(), any());
        doThrow(new IllegalArgumentException("未授权公司")).when(scope).validateUserCompanyAccess(any(), any());
        byte[] workbook = service.exportWorkbook();
        var data = read(workbook);
        assertEquals(1, data.stream().filter(s -> s.name().equals("授权公司")).findFirst().orElseThrow().rows().size());
        assertEquals(10, service.importWorkbook(workbook).get("updated"));
        assertEquals("其他租户", jdbc.queryForObject("SELECT name FROM mdm_enterprise WHERE id=3", String.class));
        verifyNoInteractions(scope);
    }
    @Test void roundTripMergesFiveDatasetsAndPreservesBindingsAndOtherTenants() throws Exception {
        var data = read(service.exportWorkbook());
        row(data,"关联公司").put("name", "更新公司");
        assertEquals(10, service.importWorkbook(DccCatalogWorkbook.write(data)).get("updated"));
        assertEquals("更新公司", jdbc.queryForObject("SELECT name FROM mdm_enterprise WHERE id=1", String.class));
        assertEquals("其他租户", jdbc.queryForObject("SELECT name FROM mdm_enterprise WHERE id=3", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_data_relation WHERE project_code_id='1' AND registration_certificate_id='1'", Integer.class));
        assertEquals(0, service.importWorkbook(service.exportWorkbook()).get("created"));
    }
    @Test void remapsNewProjectIdentityInsteadOfTrustingWorkbookId() throws Exception {
        var data = read(service.exportWorkbook());
        var projects = data.stream().filter(s -> s.name().equals("DCC项目代码")).findFirst().orElseThrow().rows();
        var added = new LinkedHashMap<>(projects.get(0)); added.put("id","999"); added.put("project_code","PR2"); projects.add(added);
        var result = service.importWorkbook(DccCatalogWorkbook.write(data));
        assertEquals(1, result.get("created"));
        assertNotEquals(999L, jdbc.queryForObject("SELECT id FROM dcc_project_code WHERE project_code='PR2'", Long.class));
        assertEquals(0, service.importWorkbook(service.exportWorkbook()).get("created"));
    }
    @Test void createsCompleteLinkedGraphAndRepeatedImportKeepsIdentity() throws Exception {
        var data = read(service.exportWorkbook());
        for (String name : List.of("产品主数据", "DCC项目代码", "DCC产品目录", "注册证", "注册证版本", "注册证快照", "受托公司绑定", "目录项目注册证绑定")) {
            var sheet = data.stream().filter(item -> item.name().equals(name)).findFirst().orElseThrow();
            var added = new LinkedHashMap<>(sheet.rows().get(0)); added.put("id", "999");
            for (String field : List.of("product_master_id", "project_code_id", "product_catalog_id", "registration_certificate_id", "certificate_id", "version_id", "snapshot_id", "current_version_id", "current_snapshot_id"))
                if (added.get(field) != null) added.put(field,"999");
            if (added.containsKey("product_code")) added.put("product_code", "P2");
            if (added.containsKey("project_code")) added.put("project_code", "PR2");
            if (added.containsKey("original_row_no")) { added.put("original_row_no", "3"); added.put("origin_row_no", "3"); }
            if (added.containsKey("certificate_key")) added.put("certificate_key", "REG002");
            if (added.containsKey("certificate_no")) added.put("certificate_no", "REG002");
            sheet.rows().add(added);
        }
        byte[] bytes = DccCatalogWorkbook.write(data);
        assertEquals(8, service.importWorkbook(bytes).get("created"));
        Long projectId = jdbc.queryForObject("SELECT id FROM dcc_project_code WHERE project_code='PR2'", Long.class);
        assertNotEquals(999L, projectId);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_data_relation r JOIN dcc_registration_certificate c ON r.registration_certificate_id=c.id WHERE r.project_code_id=? AND c.project_code_id=?", Integer.class, projectId, projectId));
        assertEquals(0, service.importWorkbook(bytes).get("created"));
    }
    @Test void administratorTransfersAcrossTenantsAndRepeatsWithoutDuplicatesOrForeignUserReferences() throws Exception {
        when(security.hasAnyRoles("super_admin", "tenant_admin")).thenReturn(true);
        jdbc.update("UPDATE dcc_project_code SET project_leader_user_id='77',last_import_batch_id='99' WHERE tenant_id=10");
        jdbc.update("UPDATE dcc_registration_certificate_version SET formalized_by='77' WHERE tenant_id=10");
        byte[] source = service.exportWorkbook();
        TenantContextHolder.setTenantId(11L);
        var first = service.importWorkbook(source);
        assertEquals(10, first.get("created"));
        assertEquals(3, first.get("localReferences"));
        assertNull(jdbc.queryForObject("SELECT project_leader_user_id FROM dcc_project_code WHERE tenant_id=11", String.class));
        assertEquals("3", jdbc.queryForObject("SELECT original_row_no FROM dcc_product_catalog WHERE tenant_id=11", String.class));
        assertEquals("2", jdbc.queryForObject("SELECT original_row_no FROM dcc_product_catalog WHERE tenant_id=10", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_data_relation r JOIN dcc_registration_certificate c ON r.registration_certificate_id=c.id JOIN dcc_project_code p ON p.id=r.project_code_id JOIN dcc_product_catalog d ON d.id=r.product_catalog_id WHERE r.tenant_id=11 AND c.tenant_id=11 AND p.tenant_id=11 AND d.tenant_id=11", Integer.class));
        assertEquals(0, service.importWorkbook(source).get("created"));
        assertEquals(0, service.importWorkbook(service.exportWorkbook()).get("created"));
        // An export from the destination retains origin, so reverse import also merges.
        byte[] destination = service.exportWorkbook();
        TenantContextHolder.setTenantId(10L);
        jdbc.update("INSERT INTO system_users(id,tenant_id) VALUES(77,10)");
        assertEquals(1, service.importWorkbook(destination).get("created")); // The destination-only company is new.
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_product_catalog WHERE tenant_id=10", Integer.class));
        assertEquals("77", jdbc.queryForObject("SELECT project_leader_user_id FROM dcc_project_code WHERE tenant_id=10", String.class));
    }
    @Test void transfersVoidedCertificateWithoutPublishingItAsActive() throws Exception {
        when(security.hasAnyRoles("super_admin", "tenant_admin")).thenReturn(true);
        jdbc.update("UPDATE dcc_registration_certificate SET status='VOIDED' WHERE tenant_id=10");
        jdbc.update("UPDATE dcc_registration_certificate_version SET status='VOIDED' WHERE tenant_id=10");
        byte[] workbook = service.exportWorkbook();
        TenantContextHolder.setTenantId(11L);
        assertEquals(10, service.importWorkbook(workbook).get("created"));
        assertEquals("VOIDED", jdbc.queryForObject("SELECT status FROM dcc_registration_certificate WHERE tenant_id=11", String.class));
        verifyNoInteractions((ControlledContentRegistrationProjectionService) ReflectionTestUtils.getField(service, "projection"));
    }
    @Test void distinctProjectsWithSameCodeKeepTheirOwnBindings() throws Exception {
        var data = read(service.exportWorkbook());
        var projects = data.stream().filter(s -> s.name().equals("DCC项目代码")).findFirst().orElseThrow().rows();
        var added = new LinkedHashMap<>(projects.get(0)); added.put("id", "999"); added.put("project_name", "项目二"); projects.add(added);
        assertEquals(1, service.importWorkbook(DccCatalogWorkbook.write(data)).get("created"));
        assertEquals(0, service.importWorkbook(service.exportWorkbook()).get("created"));
    }
    @Test void transfersArchivedDependenciesAndHistoricalBindingRowsExactly() throws Exception {
        when(security.hasAnyRoles("super_admin", "tenant_admin")).thenReturn(true);
        jdbc.update("UPDATE mdm_enterprise SET deleted=1 WHERE id=2");
        jdbc.update("UPDATE mdm_product SET deleted=1 WHERE id=1");
        jdbc.update("DELETE FROM dcc_registration_certificate_snapshot_entrusted WHERE tenant_id=10");
        jdbc.update("INSERT INTO mdm_enterprise(enterprise_code,name,type,deleted,tenant_id) VALUES('UNUSED','未引用历史公司','OWNED_COMPANY',1,10)");
        byte[] source = service.exportWorkbook();
        assertEquals("1", row(read(source), "授权公司").get("deleted"));
        assertEquals(1, read(source).stream().filter(v -> v.name().equals("授权公司")).findFirst().orElseThrow().rows().size());
        TenantContextHolder.setTenantId(11L);
        service.importWorkbook(source);
        assertEquals(1, jdbc.queryForObject("SELECT deleted FROM mdm_enterprise WHERE tenant_id=11 AND enterprise_code='C2'", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT deleted FROM mdm_product WHERE tenant_id=11 AND product_code='P1'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_certificate_snapshot_entrusted WHERE tenant_id=11", Integer.class));
        assertTrue(jdbc.queryForObject("SELECT entrusted_enterprises_json FROM dcc_registration_certificate_snapshot WHERE tenant_id=11", String.class).contains("enterpriseId"));
        assertEquals(0, service.importWorkbook(source).get("created"));
        assertEquals(0, service.importWorkbook(service.exportWorkbook()).get("created"));
    }
    @Test void preventsStateChangeAndRollsBackCompanyUpdate() throws Exception {
        var data = read(service.exportWorkbook()); row(data,"关联公司").put("name","不应保存");
        row(data,"注册证").put("status","VOIDED");
        assertThrows(IllegalArgumentException.class, () -> service.importWorkbook(DccCatalogWorkbook.write(data)));
        assertEquals("关联公司", jdbc.queryForObject("SELECT name FROM mdm_enterprise WHERE id=1", String.class));
    }
    @Test void rollsBackEarlierUpdatesWhenLaterBusinessKeyDuplicates() throws Exception {
        var data = read(service.exportWorkbook()); row(data,"关联公司").put("name","不应保存");
        var projects = data.stream().filter(s -> s.name().equals("DCC项目代码")).findFirst().orElseThrow().rows();
        var duplicate = new LinkedHashMap<>(projects.get(0)); duplicate.put("id","999"); projects.add(duplicate);
        assertThrows(IllegalArgumentException.class, () -> service.importWorkbook(DccCatalogWorkbook.write(data)));
        assertEquals("关联公司", jdbc.queryForObject("SELECT name FROM mdm_enterprise WHERE id=1", String.class));
    }
    @Test void rejectsDanglingRelationTenantMismatchAndCompanyDenial() throws Exception {
        var data = read(service.exportWorkbook()); row(data,"目录项目注册证绑定").put("project_code_id","999");
        assertThrows(IllegalArgumentException.class, () -> service.importWorkbook(DccCatalogWorkbook.write(data)));
        byte[] original = service.exportWorkbook(); TenantContextHolder.setTenantId(11L);
        assertThrows(IllegalArgumentException.class, () -> service.importWorkbook(original));
        TenantContextHolder.setTenantId(10L);
        doThrow(new IllegalArgumentException("公司无权访问")).when(scope).validateUserCompanyAccess(any(), eq(2L));
        assertThrows(IllegalArgumentException.class, () -> service.importWorkbook(original));
    }
}
