package cn.iocoder.yudao.module.dcc.service.productcatalog.packageio;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.productcatalog.DccProductCatalogRowNumberAllocator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DccCatalogPackageServiceTest {
    JdbcTemplate jdbc;
    DccCatalogPackageService service;
    List<DccCatalogWorkbook.SheetData> schema;
    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        TenantContextHolder.setTenantId(10L);
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(ds);
        var target = new DccCatalogPackageService();
        ReflectionTestUtils.setField(target,"jdbc",jdbc); ReflectionTestUtils.setField(target,"json",new ObjectMapper());
        var transactions = new TransactionInterceptor(new DataSourceTransactionManager(ds),new AnnotationTransactionAttributeSource());
        var allocator = new DccProductCatalogRowNumberAllocator(); ReflectionTestUtils.setField(allocator,"jdbc",jdbc);
        var ap = new ProxyFactory(allocator); ap.setProxyTargetClass(true); ap.addAdvice(transactions);
        ReflectionTestUtils.setField(target,"rowNumbers",ap.getProxy());
        var factory = new ProxyFactory(target); factory.setProxyTargetClass(true); factory.addAdvice(transactions);
        service=(DccCatalogPackageService)factory.getProxy();
        schema=(List<DccCatalogWorkbook.SheetData>)ReflectionTestUtils.invokeMethod(target,"schema");
        for (String name:List.of("PROJECT","PRODUCT")) {
            var spec=(DccCatalogPackageService.Spec)ReflectionTestUtils.getField(DccCatalogPackageService.class,name);
            var ddl=new StringBuilder("CREATE TABLE "+spec.table()+" (id BIGINT AUTO_INCREMENT PRIMARY KEY");
            for(String field:spec.columns()) ddl.append(",").append(field).append(" VARCHAR(10000)");
            ddl.append(",tenant_id BIGINT,deleted INT DEFAULT 0,creator VARCHAR(64),updater VARCHAR(64),update_time TIMESTAMP");
            if(name.equals("PROJECT")) ddl.append(",library_scope VARCHAR(16) DEFAULT 'PROJECT',project_leader_user_id BIGINT,product_master_id BIGINT,last_import_batch_id BIGINT");
            else ddl.append(",project_name VARCHAR(255),project_code VARCHAR(64),UNIQUE(data_source,original_row_no)");
            jdbc.execute(ddl.append(")").toString());
        }
        jdbc.execute("CREATE TABLE dcc_data_relation(id BIGINT AUTO_INCREMENT PRIMARY KEY,product_catalog_id BIGINT,project_code_id BIGINT,registration_certificate_id BIGINT,relation_status VARCHAR(32),relation_source VARCHAR(32),confirmed_by BIGINT,confirmed_time TIMESTAMP,tenant_id BIGINT,creator VARCHAR(64),updater VARCHAR(64),deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE dcc_project_product_relation(id BIGINT AUTO_INCREMENT PRIMARY KEY,request_id BIGINT NOT NULL,product_catalog_id BIGINT,project_code_id BIGINT,relation_status VARCHAR(32),tenant_id BIGINT,deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE dcc_product_catalog_source_sequence(data_source VARCHAR(64) PRIMARY KEY,last_row_no BIGINT)");
        jdbc.update("INSERT INTO dcc_product_catalog_source_sequence VALUES('瑛泰产品',3),('子公司产品',1)");
        jdbc.update("INSERT INTO dcc_project_code(id,project_name,project_code,status,project_leader_user_id,product_master_id,last_import_batch_id,tenant_id) VALUES(1,'项目一','001','ENABLE',77,88,99,10)");
        jdbc.update("INSERT INTO dcc_product_catalog(id,data_source,original_row_no,product,product_code,registration_certificate_number,project_code,tenant_id) VALUES(1,'瑛泰产品','2','导管','00001','REG1','001',10)");
        jdbc.update("INSERT INTO dcc_data_relation(id,product_catalog_id,project_code_id,registration_certificate_id,relation_status,relation_source,tenant_id) VALUES(1,1,1,888,'CONFIRMED','MANUAL',10)");
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    List<DccCatalogWorkbook.SheetData> read(byte[] bytes)throws Exception{return DccCatalogWorkbook.read(bytes,schema);}
    Map<String,String> first(List<DccCatalogWorkbook.SheetData> data,int sheet){return data.get(sheet).rows().get(0);}
    int count(String table,long tenant){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE tenant_id=?",Integer.class,tenant);}
    @Test void exportsExactlyThreeReadableSheetsWithoutCertificateOrMasterTables()throws Exception{
        var data=read(service.exportWorkbook());
        assertEquals(List.of("项目代码","产品目录","产品与项目关系"),data.stream().map(DccCatalogWorkbook.SheetData::name).toList());
        assertEquals("001",first(data,2).get("项目代码")); assertEquals("导管",first(data,2).get("产品名称"));assertEquals("00001",first(data,2).get("产品编码"));
        for(var sheet:data)assertFalse(sheet.columns().contains("id"));
        assertEquals("REG1",first(data,1).get("注册证号"));
        // The fixture has no certificate, company, master-product or template tables.
    }
    @Test void roundTripUpdatesMetadataPreservesCertificateBindingAndLocalReferences()throws Exception{
        var data=read(service.exportWorkbook());first(data,0).put("存放位置","新位置");first(data,1).put("备注","新备注");
        assertEquals(0,service.importWorkbook(DccCatalogWorkbook.write(data)).get("created"));
        assertEquals("新位置",jdbc.queryForObject("SELECT storage_location FROM dcc_project_code WHERE id=1",String.class));
        assertEquals("新备注",jdbc.queryForObject("SELECT remark FROM dcc_product_catalog WHERE id=1",String.class));
        assertEquals(888L,jdbc.queryForObject("SELECT registration_certificate_id FROM dcc_data_relation WHERE id=1",Long.class));
        assertEquals("MANUAL",jdbc.queryForObject("SELECT relation_source FROM dcc_data_relation WHERE id=1",String.class));
        assertEquals(77L,jdbc.queryForObject("SELECT project_leader_user_id FROM dcc_project_code WHERE id=1",Long.class));
        assertEquals(88L,jdbc.queryForObject("SELECT product_master_id FROM dcc_project_code WHERE id=1",Long.class));
        assertEquals(99L,jdbc.queryForObject("SELECT last_import_batch_id FROM dcc_project_code WHERE id=1",Long.class));
    }
    @Test void crossTenantAllocatesIndependentRowsAndRepeatedImportIsIdempotent()throws Exception{
        byte[] bytes=service.exportWorkbook();TenantContextHolder.setTenantId(11L);
        assertEquals(3,service.importWorkbook(bytes).get("created"));assertEquals(0,service.importWorkbook(bytes).get("created"));
        assertEquals(1,count("dcc_project_code",11));assertEquals(1,count("dcc_product_catalog",11));assertEquals(1,count("dcc_data_relation",11));
        assertEquals("4",jdbc.queryForObject("SELECT original_row_no FROM dcc_product_catalog WHERE tenant_id=11",String.class));
        assertNull(jdbc.queryForObject("SELECT registration_certificate_id FROM dcc_data_relation WHERE tenant_id=11",Long.class));
        assertNull(jdbc.queryForObject("SELECT project_leader_user_id FROM dcc_project_code WHERE tenant_id=11",Long.class));
        assertEquals("001",jdbc.queryForObject("SELECT project_code FROM dcc_product_catalog WHERE tenant_id=11",String.class));
        assertEquals(1,read(service.exportWorkbook()).get(2).rows().size());
    }
    @Test void sameNameRequiresDisambiguationAndReadableCodeResolvesIt()throws Exception{
        jdbc.update("INSERT INTO dcc_product_catalog(id,data_source,original_row_no,product,product_code,tenant_id) VALUES(2,'瑛泰产品','3','导管','00002',10)");
        var data=read(service.exportWorkbook());for(String col:List.of("产品编码","数据来源","原行号"))first(data,2).put(col,null);
        assertTrue(assertThrows(IllegalArgumentException.class,()->service.importWorkbook(DccCatalogWorkbook.write(data))).getMessage().contains("重名"));
        first(data,2).put("产品编码","00001");assertEquals(0,service.importWorkbook(DccCatalogWorkbook.write(data)).get("created"));
    }
    @Test void uniqueNameBindsUsingOnlyProjectCodeAndProductName()throws Exception{
        var data=read(service.exportWorkbook());for(String col:List.of("产品编码","数据来源","原行号"))first(data,2).put(col,null);
        jdbc.update("DELETE FROM dcc_data_relation");assertEquals(1,service.importWorkbook(DccCatalogWorkbook.write(data)).get("created"));
    }
    @Test void missingOrMismatchedSelectorsFailBeforeChanges()throws Exception{
        for(String col:List.of("项目代码","产品名称","产品编码")){
            var data=read(service.exportWorkbook());first(data,0).put("存放位置","不得写入");first(data,2).put(col,"不存在");
            assertThrows(IllegalArgumentException.class,()->service.importWorkbook(DccCatalogWorkbook.write(data)));
            assertNull(jdbc.queryForObject("SELECT storage_location FROM dcc_project_code WHERE id=1",String.class));
        }
    }
    @Test void duplicateCodesAndRelationshipsAreRejected()throws Exception{
        var data=read(service.exportWorkbook());data.get(0).rows().add(new LinkedHashMap<>(first(data,0)));
        assertThrows(IllegalArgumentException.class,()->service.importWorkbook(DccCatalogWorkbook.write(data)));
        var relations=read(service.exportWorkbook());relations.get(2).rows().add(new LinkedHashMap<>(first(relations,2)));
        assertThrows(IllegalArgumentException.class,()->service.importWorkbook(DccCatalogWorkbook.write(relations)));
    }
    @Test void approvedRelationshipsAreExportedDeduplicatedAndPreserved()throws Exception{
        jdbc.update("INSERT INTO dcc_project_product_relation(request_id,product_catalog_id,project_code_id,relation_status,tenant_id) VALUES(123,1,1,'ACTIVE',10)");
        var data=read(service.exportWorkbook());assertEquals(1,data.get(2).rows().size());jdbc.update("DELETE FROM dcc_data_relation");
        assertEquals(0,service.importWorkbook(DccCatalogWorkbook.write(data)).get("created"));assertEquals(0,count("dcc_data_relation",10));
        assertEquals(123L,jdbc.queryForObject("SELECT request_id FROM dcc_project_product_relation",Long.class));
    }
    @Test void conflictingTargetRelationshipRollsBackWholeImport()throws Exception{
        byte[] bytes=service.exportWorkbook();
        jdbc.update("INSERT INTO dcc_project_code(id,project_name,project_code,status,tenant_id) VALUES(2,'其他项目','002','ENABLE',11)");
        jdbc.update("INSERT INTO dcc_product_catalog(id,data_source,original_row_no,product,product_code,tenant_id) VALUES(2,'瑛泰产品','3','导管','00001',11)");
        jdbc.update("INSERT INTO dcc_data_relation(product_catalog_id,project_code_id,relation_status,tenant_id) VALUES(2,2,'CONFIRMED',11)");
        TenantContextHolder.setTenantId(11L);assertThrows(IllegalArgumentException.class,()->service.importWorkbook(bytes));
        assertEquals(1,count("dcc_project_code",11));assertNull(jdbc.queryForObject("SELECT registration_certificate_number FROM dcc_product_catalog WHERE tenant_id=11",String.class));
    }
    @Test void missingSequenceFailsAndRollsBack()throws Exception{
        byte[] bytes=service.exportWorkbook();TenantContextHolder.setTenantId(11L);jdbc.update("DELETE FROM dcc_product_catalog_source_sequence");
        assertThrows(IllegalStateException.class,()->service.importWorkbook(bytes));assertEquals(0,count("dcc_project_code",11));
    }
    @Test void omittedRelationshipsPreserveBindingsAndNoRelationIsInferredFromText()throws Exception{
        var data=read(service.exportWorkbook());data.get(2).rows().clear();service.importWorkbook(DccCatalogWorkbook.write(data));assertEquals(1,count("dcc_data_relation",10));
        jdbc.update("DELETE FROM dcc_data_relation");assertTrue(read(service.exportWorkbook()).get(2).rows().isEmpty());
    }
    @Test void excludesOtherTenantsAndLibraries()throws Exception{
        jdbc.update("INSERT INTO dcc_project_code(project_name,project_code,status,library_scope,tenant_id) VALUES('公共库','','ENABLE','GENERAL',10),('其他租户','OTHER','ENABLE','PROJECT',11)");
        assertEquals(1,read(service.exportWorkbook()).get(0).rows().size());
    }
    @Test void oldWorkbookIsRejectedBeforeWrites()throws Exception{
        var old=List.of(new DccCatalogWorkbook.SheetData("导入说明",List.of("format"),List.of(Map.of("format","v8"))));
        assertThrows(IllegalArgumentException.class,()->service.importWorkbook(DccCatalogWorkbook.write(old)));assertEquals(1,count("dcc_project_code",10));
    }
    @Test void invalidConfigurationAndDisabledProjectCannotBeImportedAsBindings()throws Exception{
        for(String value:List.of("{", "[]", "null")){
            var data=read(service.exportWorkbook());first(data,0).put("默认属性配置",value);
            assertThrows(IllegalArgumentException.class,()->service.importWorkbook(DccCatalogWorkbook.write(data)));
        }
        var disabled=read(service.exportWorkbook());first(disabled,0).put("状态","DISABLE");
        assertThrows(IllegalArgumentException.class,()->service.importWorkbook(DccCatalogWorkbook.write(disabled)));
        assertEquals("ENABLE",jdbc.queryForObject("SELECT status FROM dcc_project_code WHERE id=1",String.class));
    }
    @Test void inactiveTargetLinkCannotBeSilentlyReactivated()throws Exception{
        byte[] bytes=service.exportWorkbook();jdbc.update("UPDATE dcc_data_relation SET relation_status='PENDING' WHERE id=1");
        assertThrows(IllegalArgumentException.class,()->service.importWorkbook(bytes));
        assertEquals("PENDING",jdbc.queryForObject("SELECT relation_status FROM dcc_data_relation WHERE id=1",String.class));
    }
    @Test void sequenceAllocationIsRolledBackWhenLaterRelationshipConflicts()throws Exception{
        jdbc.update("INSERT INTO dcc_product_catalog(id,data_source,original_row_no,product,product_code,tenant_id) VALUES(2,'瑛泰产品','3','新产品','00002',10)");
        byte[] bytes=service.exportWorkbook();
        jdbc.update("INSERT INTO dcc_project_code(id,project_name,project_code,status,tenant_id) VALUES(2,'其他项目','002','ENABLE',11)");
        jdbc.update("INSERT INTO dcc_product_catalog(id,data_source,original_row_no,product,product_code,tenant_id) VALUES(3,'瑛泰产品','4','导管','00001',11)");
        jdbc.update("UPDATE dcc_product_catalog_source_sequence SET last_row_no=4 WHERE data_source='瑛泰产品'");
        jdbc.update("INSERT INTO dcc_data_relation(product_catalog_id,project_code_id,relation_status,tenant_id) VALUES(3,2,'CONFIRMED',11)");
        TenantContextHolder.setTenantId(11L);assertThrows(IllegalArgumentException.class,()->service.importWorkbook(bytes));
        assertEquals(4L,jdbc.queryForObject("SELECT last_row_no FROM dcc_product_catalog_source_sequence WHERE data_source='瑛泰产品'",Long.class));
        assertEquals(1,count("dcc_product_catalog",11));assertEquals(1,count("dcc_project_code",11));
    }

    @Test void productsWithoutCodesKeepDistinctCatalogFactsAcrossRepeatedImports()throws Exception{
        jdbc.update("UPDATE dcc_product_catalog SET product_code=NULL WHERE id=1");
        jdbc.update("INSERT INTO dcc_product_catalog(id,data_source,original_row_no,product,registration_certificate_number,tenant_id) VALUES(2,'瑛泰产品','3','导管','REG2',10)");
        byte[] bytes=service.exportWorkbook();TenantContextHolder.setTenantId(11L);
        assertEquals(4,service.importWorkbook(bytes).get("created"));
        assertEquals(0,service.importWorkbook(bytes).get("created"));
        assertEquals(2,count("dcc_product_catalog",11));
        assertEquals("REG1",jdbc.queryForObject("SELECT p.registration_certificate_number FROM dcc_product_catalog p JOIN dcc_data_relation r ON r.product_catalog_id=p.id AND r.tenant_id=p.tenant_id WHERE p.tenant_id=11",String.class));
    }

    @Test void projectCodeMatchingUsesDatabaseCaseInsensitiveBusinessIdentity()throws Exception{
        jdbc.update("UPDATE dcc_project_code SET project_code='AbC' WHERE id=1");
        var data=read(service.exportWorkbook());first(data,0).put("项目代码","ABC");first(data,2).put("项目代码","abc");
        assertEquals(0,service.importWorkbook(DccCatalogWorkbook.write(data)).get("created"));
        assertEquals(1,count("dcc_project_code",10));
        var duplicate=new LinkedHashMap<>(first(data,0));duplicate.put("项目代码","abc");data.get(0).rows().add(duplicate);
        assertThrows(IllegalArgumentException.class,()->service.importWorkbook(DccCatalogWorkbook.write(data)));
    }

}
