package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccWorkflowDistributionTransactionTest extends BaseDbUnitTest {
    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager transactionManager;
    @Resource DccControlledFileMapper controlledFileMapper;
    @Resource DccControlledFileDistributionMapper distributionMapper;
    @Resource DccControlledFileDistributionRecipientMapper recipientMapper;
    @Resource(name="dccControlledFileMessageJobMapper") DccControlledFileMessageJobMapper jobMapper;
    JdbcTemplate jdbc;
    DccWorkflowDistributionService service;
    final LocalDateTime now=LocalDateTime.of(2026,10,11,9,0);
    @BeforeEach void fixture() {
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(dataSource);service=new DccWorkflowDistributionService();
        ReflectionTestUtils.setField(service,"controlledFileMapper",controlledFileMapper);
        ReflectionTestUtils.setField(service,"distributionMapper",distributionMapper);
        ReflectionTestUtils.setField(service,"recipientMapper",recipientMapper);
        ReflectionTestUtils.setField(service,"messageJobMapper",jobMapper);
        ReflectionTestUtils.setField(service,"messageDeliveryService",mock(DccControlledFileMessageDeliveryService.class));
        jdbc.update("""
                INSERT INTO dcc_controlled_file(id,master_id,category_id,directory_id,source_file_id,original_file_id,
                  file_name,title,file_number,version_no,status,submitter_id,requester_id,tenant_id,deleted,
                  published_file_id,stamped_file_id,effective_date,process_instance_id,process_definition_key,controlled_time,activated_time)
                VALUES(42,10,10,20,100,100,'SOP.pdf','SOP','A-SOP','B/1','ACTIVE',99,99,1,0,
                  100,100,'2026-10-11','round-42','dcc-controlled-file-upload',?,?)
                """,now.minusDays(1),now);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    void dispatch() {
        var event=new DccControlledFileLifecycleEvent("event-42","ACTIVATED",1L,10L,42L,null,"B/1","round-42",now);
        var snapshot=new DccDistributionRosterSnapshot(1L,42L,"B/1","round-42",now,"formal-policy-input",
                List.of(new DccDistributionRosterSnapshot.Department(61L,List.of(99L,100L))));
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored->service.dispatchActivatedDistribution(event,snapshot));
    }
    @Test void activatedVersionDispatchIsIdempotentAndDoesNotChangeActivation() {
        dispatch();dispatch();
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution_recipient",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_message_job",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution WHERE tenant_id=1",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution_recipient WHERE tenant_id=1",Integer.class));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_message_job WHERE tenant_id=1",Integer.class));
        assertEquals(now,controlledFileMapper.selectById(42L).getActivatedTime());
        assertNotNull(controlledFileMapper.selectById(42L).getDistributedTime());
    }
    @Test void pendingEffectiveIsZeroDispatchAndZeroReceiptObligations() {
        jdbc.update("UPDATE dcc_controlled_file SET status='CONTROLLED_PENDING_EFFECTIVE',activated_time=NULL WHERE id=42");
        assertThrows(IllegalStateException.class,this::dispatch);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution_recipient",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_message_job",Integer.class));
    }
    @Test void outboxFailureRollsBackOnlyDispatchTransactionAndKeepsCommittedActivation() {
        var failure=mock(DccControlledFileMessageJobMapper.class);
        when(failure.insert(any(DccControlledFileMessageJobDO.class))).thenThrow(new IllegalStateException("owned outbox failure"));
        ReflectionTestUtils.setField(service,"messageJobMapper",failure);
        assertThrows(IllegalStateException.class,this::dispatch);
        assertNull(controlledFileMapper.selectById(42L).getDistributedTime());
        assertEquals(now,controlledFileMapper.selectById(42L).getActivatedTime());
        assertEquals("ACTIVE",controlledFileMapper.selectById(42L).getStatus());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution_recipient",Integer.class));
    }
}
