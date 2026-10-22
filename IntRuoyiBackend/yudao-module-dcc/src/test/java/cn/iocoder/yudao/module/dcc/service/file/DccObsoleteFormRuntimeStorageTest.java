package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual shared H2 DDL and append-only JDBC repositories; no service, network or runtime DB. */
class DccObsoleteFormRuntimeStorageTest {
    JdbcTemplate jdbc;
    DccObsoleteFormRuntimeSnapshotMapper heads;
    DccObsoleteFormRuntimeObligationMapper obligations;
    TransactionTemplate tx;
    static final LocalDateTime TIME=LocalDateTime.of(2026,10,12,10,45,0,123000000);
    @BeforeEach void setup() throws Exception {
        var source=new JdbcDataSource();source.setURL("jdbc:h2:mem:obsolete_storage_"+UUID.randomUUID()+";MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1");
        jdbc=new JdbcTemplate(source);tx=new TransactionTemplate(new DataSourceTransactionManager(source));
        String schema;
        try(var resource=DccObsoleteFormRuntimeStorageTest.class.getResourceAsStream("/sql/create_tables.sql")) {
            assertNotNull(resource,"The actual DCC module schema resource is required");
            schema=new String(resource.readAllBytes(),StandardCharsets.UTF_8);
        }
        for(String table:List.of("dcc_obsolete_form_runtime_snapshot","dcc_obsolete_form_runtime_obligation")) {
            var match=Pattern.compile("CREATE TABLE IF NOT EXISTS `?"+table+"`?\\s*\\([\\s\\S]*?\\);",Pattern.CASE_INSENSITIVE).matcher(schema);
            assertTrue(match.find(),"formal H2 table exists: "+table);jdbc.execute(match.group());
        }
        heads=new DccObsoleteFormRuntimeSnapshotMapper(jdbc);obligations=new DccObsoleteFormRuntimeObligationMapper(jdbc);
    }
    @AfterEach void cleanup() { if(jdbc!=null) jdbc.execute("DROP ALL OBJECTS"); }
    DccObsoleteFormRuntimeSnapshotDO head(long tenant,long form,String bpm) {
        return new DccObsoleteFormRuntimeSnapshotDO(null,tenant,form,20L,5L,99L,"FORM-"+form,"A/1",bpm,2,
            "dcc-obsolete:5:formal","a".repeat(64),"PUBLIC_DCC_V5","{\"default\":\"CE\",\"actual\":\"FDA\",\"rules\":\"原始◎与比例\"}","b".repeat(64),TIME);
    }
    DccObsoleteFormRuntimeObligationDO obligation(long tenant,long head,long form,String bpm,String obligation,Long department) {
        return new DccObsoleteFormRuntimeObligationDO(null,tenant,head,form,20L,bpm,obligation,
            department==null?"SUPERVISOR_REVIEW":"MATRIX_REVIEW",department==null?"supervisor_review":"review_"+department,
            department==null?0:1,department==null?0:1,department,"USER","[99]","[99,100]","ALL",100,true,TIME);
    }
    @Test void generatedHeadAndCompleteCanonicalEvidenceRoundTrip() {
        var expected=head(1,901,"bpm-one");long id=heads.insert(expected);assertTrue(id>0);
        var actual=heads.listByForm(1L,901L).get(0);assertEquals(id,actual.id());
        assertEquals(expected.canonicalPreparationJson(),actual.canonicalPreparationJson());
        assertEquals(expected.preparedDefinitionDigest(),actual.preparedDefinitionDigest());assertEquals(TIME,actual.createdAt());
        assertEquals(actual,heads.listByBpm(1L,20L,"bpm-one").get(0));
    }
    @Test void exactTenantFileFormAndBpmQueriesNeverBorrowAnotherHead() {
        heads.insert(head(1,901,"same-bpm"));heads.insert(head(2,901,"same-bpm"));heads.insert(head(1,902,"other-bpm"));
        assertEquals(1,heads.listByForm(1L,901L).size());assertEquals(2L,heads.listByBpm(2L,20L,"same-bpm").get(0).tenantId());
        assertTrue(heads.listByBpm(1L,21L,"same-bpm").isEmpty());assertTrue(heads.listByForm(3L,901L).isEmpty());
    }
    @Test void duplicateFormOrBpmCannotCreateASecondHead() {
        heads.insert(head(1,901,"one"));
        assertThrows(RuntimeException.class,()->heads.insert(head(1,901,"two")));
        assertThrows(RuntimeException.class,()->heads.insert(head(1,902,"one")));
        assertEquals(1L,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_obsolete_form_runtime_snapshot",Long.class));
    }
    @Test void independentDepartmentsKeepObligationsDespiteTheSameFrozenUserAndSupervisorHasNoFakeDepartment() {
        long id=heads.insert(head(1,901,"one"));
        obligations.insert(obligation(1,id,901,"one","form-901:SUPERVISOR_REVIEW",null));
        obligations.insert(obligation(1,id,901,"one","form-901:MATRIX_REVIEW:51",51L));
        obligations.insert(obligation(1,id,901,"one","form-901:MATRIX_REVIEW:52",52L));
        var rows=obligations.listByHead(1L,id);assertEquals(3,rows.size());assertNull(rows.get(0).departmentId());
        assertEquals(List.of(51L,52L),rows.subList(1,3).stream().map(DccObsoleteFormRuntimeObligationDO::departmentId).toList());
        assertTrue(rows.stream().allMatch(row->"[99,100]".equals(row.frozenUserIdsJson())));
        assertEquals(rows,obligations.listByBpm(1L,20L,"one"));assertTrue(obligations.listByBpm(2L,20L,"one").isEmpty());
    }
    @Test void obligationUniquenessIsPerTenantAndExactBpm() {
        long id=heads.insert(head(1,901,"one"));var row=obligation(1,id,901,"one","form-901:MATRIX_REVIEW:51",51L);
        obligations.insert(row);assertThrows(RuntimeException.class,()->obligations.insert(row));
        obligations.insert(obligation(2,id,901,"one",row.obligationId(),51L));
        assertEquals(1,obligations.listByHead(1L,id).size());assertEquals(1,obligations.listByHead(2L,id).size());
    }
    @Test void lateObligationFailureRollsBackHeadAndAllEarlierObligations() {
        assertThrows(RuntimeException.class,()->tx.executeWithoutResult(status->{
            long id=heads.insert(head(1,901,"one"));var row=obligation(1,id,901,"one","same-obligation",51L);
            obligations.insert(row);obligations.insert(row);
        }));
        assertEquals(0L,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_obsolete_form_runtime_snapshot",Long.class));
        assertEquals(0L,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_obsolete_form_runtime_obligation",Long.class));
    }
}
