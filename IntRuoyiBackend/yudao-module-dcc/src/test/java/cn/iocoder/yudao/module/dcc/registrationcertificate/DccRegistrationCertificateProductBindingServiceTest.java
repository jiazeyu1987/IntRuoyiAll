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
        jdbc.execute("CREATE TABLE dcc_registration_certificate(tenant_id BIGINT,id BIGINT,current_version_id BIGINT,pending_version_id BIGINT)");
        jdbc.execute("CREATE TABLE dcc_registration_certificate_version(tenant_id BIGINT,id BIGINT,certificate_id BIGINT,certificate_no VARCHAR(128),deleted INT)");
        jdbc.execute("CREATE TABLE dcc_registration_certificate_product(tenant_id BIGINT,product_catalog_id BIGINT,certificate_id BIGINT,project_code_id BIGINT,product_code VARCHAR(128),product_name VARCHAR(255),state VARCHAR(16),created_by BIGINT,PRIMARY KEY(tenant_id,product_catalog_id),UNIQUE(tenant_id,project_code_id))");
        for(long id:List.of(101L,102L)) {
            jdbc.update("INSERT INTO dcc_registration_certificate VALUES(1,?,?,NULL)",id,id);
            jdbc.update("INSERT INTO dcc_registration_certificate_version VALUES(1,?,?,?,0)",id,id,"证"+id);
        }
        service=new DccRegistrationCertificateProductBindingService(jdbc);
    }
    private DccRegistrationCertificateProjectProduct product(long id) {
        return new DccRegistrationCertificateProjectProduct(id+1000,id,"P"+id,"产品"+id,List.of());
    }
    @Test void multipleProductsShareOneCertificateButOneProductCannotShareTwo() {
        tx.executeWithoutResult(s->service.reserve(1L,101L,List.of(product(1),product(2)),9L));
        assertEquals(101L,service.occupancy(1L,2L).certificateId());
        var ex=assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->service.reserve(1L,102L,List.of(product(1)),9L)));
        assertTrue(ex.getMessage().contains("产品1"));assertTrue(ex.getMessage().contains("证101"));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_certificate_product",Integer.class));
    }
    @Test void rejectedOrWithdrawnUploadReleasesOnlyPendingProducts() {
        tx.executeWithoutResult(s->service.confirm(1L,101L,List.of(product(1)),9L));
        tx.executeWithoutResult(s->service.reserve(1L,102L,List.of(product(2)),9L));
        service.releasePending(1L,101L);service.releasePending(1L,102L);
        assertEquals("BOUND",service.occupancy(1L,1L).state());assertNull(service.occupancy(1L,2L));
        tx.executeWithoutResult(s->service.reserve(1L,101L,List.of(product(2)),9L));
        assertEquals(101L,service.occupancy(1L,2L).certificateId());
    }
    @Test void conflictRollsBackTheEntireProductSelection() {
        tx.executeWithoutResult(s->service.confirm(1L,101L,List.of(product(2)),9L));
        assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->service.reserve(1L,102L,List.of(product(1),product(2)),9L)));
        assertNull(service.occupancy(1L,1L));
    }
    @Test void systemProjectCannotBeClaimedByAnotherCertificateEvenIfItsProductMappingChanges() {
        tx.executeWithoutResult(s->service.confirm(1L,101L,List.of(product(1)),9L));
        var remapped=new DccRegistrationCertificateProjectProduct(1001L,2L,"P2","产品2",List.of());
        assertThrows(RuntimeException.class,()->tx.executeWithoutResult(s->service.reserve(1L,102L,List.of(remapped),9L)));
        assertNull(service.occupancy(1L,2L));assertEquals(101L,service.projectOccupancy(1L,1001L).certificateId());
    }
    @Test void tenantsHaveIndependentProductIdentity() {
        jdbc.update("INSERT INTO dcc_registration_certificate VALUES(2,102,102,NULL)");
        jdbc.update("INSERT INTO dcc_registration_certificate_version VALUES(2,102,102,'租户2证',0)");
        tx.executeWithoutResult(s->service.reserve(1L,101L,List.of(product(1)),9L));
        tx.executeWithoutResult(s->service.reserve(2L,102L,List.of(product(1)),9L));
        assertEquals(102L,service.occupancy(2L,1L).certificateId());
    }
    @Test void concurrentUploadsCannotClaimTheSameProduct() throws Exception {
        var executor=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try {
            var futures=List.of(101L,102L).stream().map(c->executor.submit(()->{
                ready.countDown();start.await();
                try { tx.executeWithoutResult(s->service.reserve(1L,c,List.of(product(1)),9L));return true; }
                catch(RuntimeException conflict) { return false; }
            })).toList();
            assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();
            int successful=0;for(var f:futures) if(f.get(10,TimeUnit.SECONDS))successful++;
            assertEquals(1,successful);
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_registration_certificate_product",Integer.class));
        } finally {executor.shutdownNow();}
    }
}
