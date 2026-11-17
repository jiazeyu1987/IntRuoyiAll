package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.DccRegistrationCertificateProjectProductMapper;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateProjectProductService;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DccRegistrationCertificateProjectProductTest {
    private SqlSession session;
    private JdbcTemplate jdbc;
    private DccRegistrationCertificateProjectProductService service;
    private DccProjectCodeDO project;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(10L);
        var datasource = new JdbcDataSource();
        datasource.setURL("jdbc:h2:mem:registration_product_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(datasource);
        jdbc.execute("CREATE TABLE dcc_product_catalog(id BIGINT, tenant_id BIGINT, product_code VARCHAR(64), product VARCHAR(255), deleted INT)");
        jdbc.execute("CREATE TABLE dcc_data_relation(id BIGINT, tenant_id BIGINT, project_code_id BIGINT, product_catalog_id BIGINT, relation_status VARCHAR(32), deleted INT)");
        jdbc.execute("CREATE TABLE dcc_project_product_relation(id BIGINT, tenant_id BIGINT, project_code_id BIGINT, product_catalog_id BIGINT, request_id BIGINT, relation_status VARCHAR(32), deleted INT)");
        jdbc.execute("CREATE TABLE dcc_project_product_create_request(id BIGINT, tenant_id BIGINT, generated_project_code_id BIGINT, generated_product_catalog_id BIGINT, relation_id BIGINT, status VARCHAR(32), deleted INT)");
        jdbc.update("INSERT INTO dcc_product_catalog VALUES (663,10,'0313170202','按压式压力泵',0)");
        jdbc.update("INSERT INTO dcc_data_relation VALUES (56,10,501,663,'CONFIRMED',0)");
        Configuration configuration = new Configuration(new Environment("test", new JdbcTransactionFactory(), datasource));
        configuration.addMapper(DccRegistrationCertificateProjectProductMapper.class);
        session = new SqlSessionFactoryBuilder().build(configuration).openSession(true);
        service = new DccRegistrationCertificateProjectProductService(session.getMapper(DccRegistrationCertificateProjectProductMapper.class));
        project = DccProjectCodeDO.builder().id(501L).projectCode("IDI").projectName("按压式球囊扩充压力泵")
                .productMasterId(990820294L).status("ENABLE").build();
        project.setTenantId(10L);
    }

    @AfterEach
    void tearDown() {
        session.close();
        jdbc.execute("DROP ALL OBJECTS");
        TenantContextHolder.clear();
    }

    @Test
    void resolvesFormalTenDigitDccProductEvenWhenProjectHasShowroomReference() {
        var product = service.resolve(project);
        assertEquals(663L, product.productCatalogId());
        assertEquals("0313170202", product.productCode());
        assertEquals("按压式压力泵", product.productName());
        assertEquals(56L, product.bindings().get(0).relationId());
    }

    private void approvedProduct() {
        jdbc.update("INSERT INTO dcc_project_product_relation VALUES (57,10,501,663,61,'ACTIVE',0)");
        jdbc.update("INSERT INTO dcc_project_product_create_request VALUES (61,10,501,663,57,'COMPLETED',0)");
    }

    @Test
    void acceptsCompletedProjectProductApprovalWithoutManualBinding() {
        jdbc.update("DELETE FROM dcc_data_relation");
        approvedProduct();
        assertEquals("APPROVED", service.resolve(project).bindings().get(0).source());
    }

    @Test
    void sameProductConfirmedAndApprovedIsOneProductWithBothEvidenceLinks() {
        approvedProduct();
        assertEquals(2, service.resolve(project).bindings().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "DELETE FROM dcc_data_relation",
        "UPDATE dcc_data_relation SET tenant_id=20",
        "UPDATE dcc_data_relation SET project_code_id=502",
        "UPDATE dcc_data_relation SET relation_status='REVOKED'",
        "UPDATE dcc_data_relation SET deleted=1",
        "UPDATE dcc_product_catalog SET tenant_id=20",
        "UPDATE dcc_product_catalog SET deleted=1",
        "UPDATE dcc_product_catalog SET product_code=''",
        "UPDATE dcc_product_catalog SET product=NULL"
    })
    void rejectsMissingInvalidDeletedOrForeignBinding(String sql) {
        jdbc.update(sql);
        assertThrows(ServiceException.class, () -> service.resolve(project));
    }

    @Test
    void neverChoosesTheFirstOfTwoBoundProducts() {
        jdbc.update("INSERT INTO dcc_product_catalog VALUES (664,10,'0313170201','另一产品',0)");
        jdbc.update("INSERT INTO dcc_data_relation VALUES (58,10,501,664,'CONFIRMED',0)");
        assertThrows(ServiceException.class, () -> service.resolve(project));
    }

    @Test
    void doesNotIgnoreAnInvalidSecondBindingWhenOneProductIsValid() {
        jdbc.update("INSERT INTO dcc_data_relation VALUES (58,10,501,664,'CONFIRMED',0)");
        assertThrows(ServiceException.class, () -> service.resolve(project));
    }

    @Test
    void rejectsDuplicateConfirmedRelationsEvenForTheSameProduct() {
        jdbc.update("INSERT INTO dcc_data_relation VALUES (58,10,501,663,'CONFIRMED',0)");
        assertThrows(ServiceException.class, () -> service.resolve(project));
    }

    @ParameterizedTest
    @ValueSource(strings = {"status='REJECTED'", "tenant_id=20", "relation_id=99", "generated_product_catalog_id=664", "deleted=1"})
    void requiresCompletedMatchingApprovalEvidence(String assignment) {
        jdbc.update("DELETE FROM dcc_data_relation");
        approvedProduct();
        jdbc.update("UPDATE dcc_project_product_create_request SET " + assignment);
        assertThrows(ServiceException.class, () -> service.resolve(project));
    }

    @Test
    void refusesForeignProjectOrIgnoredTenantContext() {
        project.setTenantId(20L);
        assertThrows(ServiceException.class, () -> service.resolve(project));
        project.setTenantId(10L);
        TenantContextHolder.setIgnore(true);
        assertThrows(ServiceException.class, () -> service.resolve(project));
    }
}
