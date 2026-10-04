package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import static org.junit.jupiter.api.Assertions.*;

class DccApplicationRoundMappingTest extends BaseDbUnitTest {
    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager transactions;
    @Resource DccProjectCodeMapper projects;
    @Resource(name="dccControlledFileMapper") DccControlledFileMapper files;
    JdbcTemplate jdbc;
    DccApplicationRoundService rounds;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE IF NOT EXISTS dcc_application_round_link(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,project_id BIGINT,application_type VARCHAR(16),application_id BIGINT,bpm_round VARCHAR(64),attribute_round INT,UNIQUE(tenant_id,application_type,application_id,bpm_round),UNIQUE(tenant_id,application_type,application_id,attribute_round))");
        jdbc.update("DELETE FROM dcc_application_round_link");
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,status,deleted) VALUES(5,1,'round-project','ENABLE',0)");
        jdbc.update("INSERT INTO dcc_controlled_file(id,tenant_id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,submitter_id,requester_id,process_type,change_type,status,dcc_project_code_id,deleted) VALUES(20,1,10,2,3,100,100,'SOP.pdf','SOP','SOP-1','A/1',99,99,'CONTROLLED_FILE','NEW','WORKING',5,0)");
        rounds=new DccApplicationRoundService();ReflectionTestUtils.setField(rounds,"jdbc",jdbc);
        ReflectionTestUtils.setField(rounds,"projects",projects);ReflectionTestUtils.setField(rounds,"files",files);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    int bind(String round) { return new TransactionTemplate(transactions).execute(s->rounds.bind(5L,"UPLOAD",20L,round)); }
    @Test void opaqueProcessIdentityMapsToStableSnapshotNumber() {
        assertEquals(1,bind("opaque-bpm-id"));assertEquals(1,bind("opaque-bpm-id"));
        assertEquals(2,bind("next-bpm-id"));assertEquals(2,rounds.require("UPLOAD",20L,"next-bpm-id"));
    }
    @Test void unknownProcessCannotBeGuessedAsRoundOne() {
        assertThrows(IllegalStateException.class,()->rounds.require("UPLOAD",20L,"not-bound"));
    }
    @Test void failedOuterApplicationTransactionRollsBackMapping() {
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).execute(s->{rounds.bind(5L,"UPLOAD",20L,"failed-bpm");throw new IllegalStateException("BPM failure");}));
        assertThrows(IllegalStateException.class,()->rounds.require("UPLOAD",20L,"failed-bpm"));
    }
}
