package cn.iocoder.yudao.module.dcc.registrationcertificate;

import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateProjectProduct;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateProductBindingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class DccRegistrationCertificateProductBindingServiceTest {
    private JdbcTemplate jdbc;
    private DccRegistrationCertificateProductBindingService service;
    private TransactionTemplate tx;

    @BeforeEach void setup() {
        var ds=new DriverManagerDataSource("jdbc:h2:mem:reg_products_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("CREATE TABLE dcc_registration_certificate(tenant_id BIGINT,id BIGINT,status VARCHAR(32),deleted INT)");
        jdbc.execute("CREATE TABLE dcc_registration_certificate_product(tenant_id BIGINT,product_catalog_id BIGINT,certificate_id BIGINT,project_code_id BIGINT,product_code VARCHAR(128),product_name VARCHAR(255),state VARCHAR(16),created_by BIGINT,PRIMARY KEY(tenant_id,certificate_id,product_catalog_id),UNIQUE(tenant_id,certificate_id,project_code_id))");
        for(long id:List.of(101L,102L)) jdbc.update("INSERT INTO dcc_registration_certificate VALUES(1,?,'ACTIVE',0)",id);
        service=new DccRegistrationCertificateProductBindingService(jdbc);
    }
    private DccRegistrationCertificateProjectProduct product(long id) {
        return new DccRegistrationCertificateProjectProduct(id+1000,id,"P"+id,"产品"+id,List.of());
    }
    @Test void productSharesSeveralCertificatesAndCertificateSharesSeveralProducts() {
        tx.executeWithoutResult(s->service.confirm(1L,101L,List.of(product(1),product(2)),9L));
        tx.executeWithoutResult(s->service.confirm(1L,102L,List.of(product(1)),9L));
        assertEquals(List.of(101L,102L),service.currentCertificateIds(1L,1L));
        assertTrue(service.containsProduct(1L,101L,1L));assertTrue(service.containsProduct(1L,102L,1L));
        assertTrue(service.containsProject(1L,102L,1001L));assertFalse(service.containsProduct(1L,102L,2L));
        assertEquals(3,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_certificate_product",Integer.class));
    }
    @Test void rejectedUploadReleasesItsOwnReservationOnly() {
        tx.executeWithoutResult(s->service.confirm(1L,101L,List.of(product(1)),9L));
        tx.executeWithoutResult(s->service.reserve(1L,102L,List.of(product(1)),9L));
        assertFalse(service.containsProduct(1L,102L,1L));
        service.releasePending(1L,101L);service.releasePending(1L,102L);
        assertTrue(service.containsProduct(1L,101L,1L));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_certificate_product",Integer.class));
    }
    @Test void repeatedApprovalIsIdempotentWithinCertificate() {
        for(int i=0;i<2;i++) tx.executeWithoutResult(s->service.confirm(1L,101L,List.of(product(1)),9L));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_certificate_product",Integer.class));
        assertTrue(service.containsProduct(1L,101L,1L));
    }
    @Test void mismatchedProjectRollsBackTheWholeSelectionWithinCertificate() {
        tx.executeWithoutResult(s->service.confirm(1L,101L,List.of(product(2)),9L));
        var changedProject=new DccRegistrationCertificateProjectProduct(999L,2L,"P2","产品2",List.of());
        assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->service.reserve(1L,101L,List.of(product(1),changedProject),9L)));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_certificate_product",Integer.class));
        assertTrue(service.containsProject(1L,101L,1002L));
    }
    @Test void sameCertificateCannotBindTwoProductsForTheSameProject() {
        tx.executeWithoutResult(s->service.confirm(1L,101L,List.of(product(1)),9L));
        var remapped=new DccRegistrationCertificateProjectProduct(1001L,2L,"P2","产品2",List.of());
        assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->service.reserve(1L,101L,List.of(remapped),9L)));
        assertFalse(service.containsProduct(1L,101L,2L));
    }
    @Test void tenantAndCertificateStateRestrictCurrentCertificateList() {
        jdbc.update("INSERT INTO dcc_registration_certificate VALUES(2,101,'ACTIVE',0)");
        tx.executeWithoutResult(s->service.confirm(1L,101L,List.of(product(1)),9L));
        tx.executeWithoutResult(s->service.confirm(2L,101L,List.of(product(1)),9L));
        tx.executeWithoutResult(s->service.confirm(1L,102L,List.of(product(1)),9L));
        jdbc.update("UPDATE dcc_registration_certificate SET status='EXPIRED_UNRENEWED' WHERE tenant_id=1 AND id=102");
        assertEquals(List.of(101L),service.currentCertificateIds(1L,1L));
        assertEquals(List.of(101L),service.currentCertificateIds(2L,1L));
        jdbc.update("UPDATE dcc_registration_certificate SET deleted=1 WHERE tenant_id=1 AND id=101");
        assertTrue(service.currentCertificateIds(1L,1L).isEmpty());
        assertTrue(service.containsProduct(1L,102L,1L));
    }
    @Test void concurrentUploadsCanReserveSameProductForDifferentCertificates() throws Exception {
        var executor=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try {
            var futures=List.of(101L,102L).stream().map(c->executor.submit(()->{
                ready.countDown();start.await();
                tx.executeWithoutResult(s->service.reserve(1L,c,List.of(product(1)),9L));return true;
            })).toList();
            assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();
            for(var f:futures) assertTrue(f.get(10,TimeUnit.SECONDS));
            assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_certificate_product",Integer.class));
        } finally {executor.shutdownNow();}
    }
}
