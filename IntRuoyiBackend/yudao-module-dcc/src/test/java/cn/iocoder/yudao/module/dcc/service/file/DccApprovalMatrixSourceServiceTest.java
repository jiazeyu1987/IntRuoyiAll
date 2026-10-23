package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

/** Actual MyBatis SQL and immutable raw source selection in the existing private H2 fixture. */
@Import({DccApprovalMatrixSourceService.class,DccWorkflowDatePolicy.class})
class DccApprovalMatrixSourceServiceTest extends BaseDbUnitTest {
    @Resource DccApprovalMatrixSourceService source;
    @Resource DccWorkflowDatePolicy dates;
    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager transactions;
    JdbcTemplate jdbc;
    @BeforeEach void setup()throws Exception{
        TenantContextHolder.setTenantId(1L);dates.setZoneId("Asia/Shanghai");jdbc=new JdbcTemplate(dataSource);
        try(var c=dataSource.getConnection()){assertTrue(c.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        jdbc.update("INSERT INTO dcc_file_category(id,code,name,source,lifecycle_stage,file_type_taxonomy_id,tenant_id) VALUES(7,'W05-M','矩阵','LOCAL','DEVELOPMENT',9007199254740993,1)");
        jdbc.update("INSERT INTO dcc_category_approval_route(id,category_id,action_type,version_no,effective_time,remark,tenant_id) VALUES(30,7,'LEGACY',2,?,'  原备注  ',1)",LocalDateTime.now().minusDays(1).withNano(0));
        jdbc.update("INSERT INTO dcc_category_approval_route(id,category_id,action_type,version_no,effective_time,tenant_id) VALUES(31,7,'NEW',99,?,1),(32,7,'LEGACY',3,?,1)",LocalDateTime.now().minusDays(1).withNano(0),LocalDateTime.now().plusDays(1).withNano(0));
        jdbc.update("INSERT INTO dcc_category_approval_route_node(id,route_id,stage_no,stage_code,stage_name,stage_order,candidate_source_type,candidate_source_id,candidate_source_ids,approve_method,approve_ratio,require_all_approvals,stage_type,marker,subject_type,subject_id,rule_remark,tenant_id) VALUES(40,30,2,'MATRIX_REVIEW','会签',2,'DEPT',51,'51, 52','ALL',100,1,'SIGNOFF','●','DEPT',51,?,1)","原文\r\n备注");
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    @Test void currentPublicSourceIsExactLegacyNotTypedNewOrFutureAndRawBytesRemainOriginal(){var selected=source.read(7L);assertEquals(30L,selected.baseline().getRouteId());assertEquals(2,selected.baseline().getRouteVersionNo());assertEquals("LEGACY",selected.baseline().getActionType());assertEquals("51, 52",selected.baseline().getNodes().get(0).getCandidateSourceIds());assertEquals("原文\r\n备注",selected.baseline().getNodes().get(0).getRuleRemark());String digest=selected.baseline().getMatrixDigest();jdbc.update("UPDATE dcc_category_approval_route SET active=0 WHERE id=30");var exact=new TransactionTemplate(transactions).execute(status->source.readExactForUpdate(7L,30L));assertEquals(digest,exact.baseline().getMatrixDigest());assertThrows(IllegalStateException.class,()->source.read(7L));}
    @Test void missingPublicMatrixCannotFallBackToTheTypedActionRoute(){jdbc.update("DELETE FROM dcc_category_approval_route WHERE action_type='LEGACY'");assertThrows(IllegalStateException.class,()->source.read(7L));}
}
