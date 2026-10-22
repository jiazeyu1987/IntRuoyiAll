package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.service.ApprovalIdentity;
import cn.iocoder.yudao.module.bpm.dal.mysql.approval.BpmApprovalReviewPrepareMapper;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalPrepareH2TransactionIntegrationTest {
    private static final String TABLE = "CREATE TABLE bpm_approval_review_prepare (id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, actor_id BIGINT NOT NULL, prepare_id VARCHAR(64) NOT NULL UNIQUE, owner_code VARCHAR(32), source_task_type VARCHAR(64), source_task_id VARCHAR(64), process_instance_id VARCHAR(64), subject_type VARCHAR(64), subject_id VARCHAR(64), content_policy_version VARCHAR(64), subject_version VARCHAR(128), preview_content_hash VARCHAR(64), scope_code VARCHAR(64), canonical_business_content_json CLOB, prepare_record_hash VARCHAR(64), prepared_at TIMESTAMP, expires_at TIMESTAMP)";
    @Test void actualMapperAndPrepareIntegrityReaderCommitAndRollbackOnSameSpringManagedConnection() throws Exception {
        var source=new DriverManagerDataSource("jdbc:h2:mem:approval_prepare_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        var jdbc=new JdbcTemplate(source);
        jdbc.execute(TABLE);
        var config=new Configuration();config.setMapUnderscoreToCamelCase(true);config.addMapper(BpmApprovalReviewPrepareMapper.class);
        var factory=new SqlSessionFactoryBean();factory.setDataSource(source);factory.setConfiguration(config);
        var mapper=new SqlSessionTemplate(factory.getObject()).getMapper(BpmApprovalReviewPrepareMapper.class);
        var service=new ApprovalReviewPrepareService(mapper,Clock.fixed(Instant.parse("2026-10-10T01:00:00Z"),ZoneOffset.UTC));
        var transaction=new TransactionTemplate(new DataSourceTransactionManager(source));
        var identity=new ApprovalIdentity(1L,"EDHR","EDHR_WORK_TASK","2",null,"EDHR_RELEASE","3");
        var content=new PreparedApprovalContent(identity,"approval-content-v1","EDHR1:"+"a".repeat(64),ApprovalContentJson.hash("{}"),"EDHR_RELEASE","scope","{}",List.of(),List.of("APPROVE"));
        try {
            TenantContextHolder.setTenantId(1L);
            var stored=transaction.execute(s -> service.insert(4L,content));
            assertNotNull(stored);assertEquals(stored,service.require(4L,stored.getPrepareId(),true));
            assertThrows(IllegalStateException.class,()->transaction.executeWithoutResult(s -> {
                service.insert(4L,content);
                throw new IllegalStateException("later transactional effect failed");
            }));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM bpm_approval_review_prepare",Integer.class));
            jdbc.update("UPDATE bpm_approval_review_prepare SET canonical_business_content_json=? WHERE id=?","{\"tampered\":true}",stored.getId());
            assertThrows(IllegalStateException.class,()->service.require(4L,stored.getPrepareId(),true));
            TenantContextHolder.setTenantId(5L);
            assertThrows(IllegalArgumentException.class,()->service.require(4L,stored.getPrepareId(),true));
        } finally {TenantContextHolder.clear();}
    }

    @Test void actualRequiresNewProxyCommitsPrepareWhileOuterBusinessTransactionRollsBack() throws Exception {
        var source=new DriverManagerDataSource("jdbc:h2:mem:approval_prepare_proxy_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        var jdbc=new JdbcTemplate(source);jdbc.execute(TABLE);jdbc.execute("CREATE TABLE outer_effect(id BIGINT PRIMARY KEY)");
        var config=new Configuration();config.setMapUnderscoreToCamelCase(true);config.addMapper(BpmApprovalReviewPrepareMapper.class);
        var factory=new SqlSessionFactoryBean();factory.setDataSource(source);factory.setConfiguration(config);
        var mapper=new SqlSessionTemplate(factory.getObject()).getMapper(BpmApprovalReviewPrepareMapper.class);
        var target=new ApprovalReviewPrepareService(mapper,Clock.fixed(Instant.parse("2026-10-10T01:00:00Z"),ZoneOffset.UTC));
        var manager=new DataSourceTransactionManager(source);
        var proxyFactory=new org.springframework.aop.framework.ProxyFactory(target);proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(manager,
                new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        var proxy=(ApprovalReviewPrepareService)proxyFactory.getProxy();
        var content=new PreparedApprovalContent(new ApprovalIdentity(1L,"EDHR","EDHR_WORK_TASK","2",null,"EDHR_RELEASE","3"),
                "approval-content-v1","EDHR1:"+"a".repeat(64),ApprovalContentJson.hash("{}"),"EDHR_RELEASE","scope","{}",List.of(),List.of("APPROVE"));
        var stored=new java.util.concurrent.atomic.AtomicReference<cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalReviewPrepareDO>();
        try {
            TenantContextHolder.setTenantId(1L);
            assertThrows(IllegalStateException.class,()->new TransactionTemplate(manager).executeWithoutResult(s -> {
                jdbc.update("INSERT INTO outer_effect VALUES (1)");
                stored.set(proxy.insert(4L,content));
                throw new IllegalStateException("outer business rollback");
            }));
            assertNotNull(stored.get());
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM outer_effect",Integer.class));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM bpm_approval_review_prepare",Integer.class));
            assertEquals(stored.get(),proxy.require(4L,stored.get().getPrepareId(),true));
        } finally {TenantContextHolder.clear();}
    }
}
