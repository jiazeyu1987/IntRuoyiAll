package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccWorkflowDistributionReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccWorkflowDistributionTransactionTest extends BaseDbUnitTest {
    @Resource private DataSource dataSource;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private DccControlledFileMapper controlledFileMapper;
    @Resource private DccControlledFileDistributionMapper distributionMapper;
    @Resource private DccControlledFileDistributionRecipientMapper recipientMapper;
    @Resource(name = "dccControlledFileMessageJobMapper") private DccControlledFileMessageJobMapper jobMapper;
    private DccWorkflowDistributionService distribution;
    private DccControlledFileFinalizationServiceImpl finalization;
    private JdbcTemplate jdbc;
    private AdminUserApi users;

    @BeforeEach void fixture() {
        jdbc=new JdbcTemplate(dataSource); users=mock(AdminUserApi.class);
        var permissions=mock(DccControlledFileCategoryPermissionSupport.class);
        var roles=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);
        when(roles.hasAnyRoles(99L,"doc_control")).thenReturn(true);
        when(permissions.hasCategoryPermission(any(),any(),any())).thenReturn(true);
        when(users.getUserList(List.of(99L))).thenReturn(List.of(new AdminUserRespDTO().setId(99L).setDeptId(51L).setStatus(0)));
        distribution=new DccWorkflowDistributionService(); finalization=new DccControlledFileFinalizationServiceImpl();
        for(Object service:List.of(distribution,finalization)) {
            ReflectionTestUtils.setField(service,"controlledFileMapper",controlledFileMapper);
            ReflectionTestUtils.setField(service,"distributionMapper",distributionMapper);
            ReflectionTestUtils.setField(service,"permissionSupport",permissions);
            ReflectionTestUtils.setField(service,"adminUserApi",users);
            ReflectionTestUtils.setField(service,"permissionApi",roles);
        }
        ReflectionTestUtils.setField(distribution,"recipientMapper",recipientMapper);
        ReflectionTestUtils.setField(distribution,"deptApi",mock(DeptApi.class));
        ReflectionTestUtils.setField(distribution,"finalizationService",finalization);
        ReflectionTestUtils.setField(finalization,"distributionRecipientMapper",recipientMapper);
        ReflectionTestUtils.setField(finalization,"messageJobMapper",jobMapper);
        // External message delivery is not exercised; real outbox rows remain pending.
        ReflectionTestUtils.setField(finalization,"messageDeliveryService",mock(DccControlledFileMessageDeliveryService.class));
        ReflectionTestUtils.setField(finalization,"transactionTemplate",new TransactionTemplate(transactionManager));
        var category=mock(DccFileCategoryMapper.class);
        when(category.selectById(10L)).thenReturn(DccFileCategoryDO.builder().id(10L).distributionRequired(true).build());
        ReflectionTestUtils.setField(finalization,"categoryMapper",category);
        var lifecycle=new DccControlledFileLifecycleService(); var dates=new DccWorkflowDatePolicy(); dates.setZoneId("Asia/Singapore");
        ReflectionTestUtils.setField(lifecycle,"datePolicy",dates);ReflectionTestUtils.setField(finalization,"lifecycleService",lifecycle);
        jdbc.update("""
                INSERT INTO dcc_controlled_file(id,master_id,category_id,directory_id,source_file_id,original_file_id,
                  file_name,title,file_number,version_no,status,submitter_id,requester_id,tenant_id,deleted,
                  published_file_id,stamped_file_id,effective_date,process_instance_id,process_definition_key,controlled_time)
                VALUES(42,10,10,20,100,100,'SOP.pdf','SOP','A-SOP','B/1','CONTROLLED_PENDING_EFFECTIVE',99,99,1,0,
                  100,100,?,'approval-42','dcc-controlled-file-upload',CURRENT_TIMESTAMP)
                """,LocalDate.now().plusDays(10));
    }
    @Test void savesSelectedRecipientsAndFutureDistributionWithoutReapprovalOrDateChange() {
        distribute(request());
        var file=controlledFileMapper.selectById(42L);
        assertEquals("CONTROLLED_PENDING_EFFECTIVE",file.getStatus());assertNotNull(file.getDistributedTime());
        assertNull(file.getActivatedTime());assertEquals(LocalDate.now().plusDays(10),file.getEffectiveDate());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution_recipient WHERE message_job_id IS NOT NULL",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_message_job WHERE status='PENDING'",Integer.class));
        distribute(request());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution",Integer.class));
        var conflicting=request();conflicting.getScopes().get(0).setDistributionMedium("PAPER");
        assertThrows(IllegalArgumentException.class,()->distribute(conflicting));
    }
    @Test void deliveryPersistenceFailureRollsBackRecipientRowsCompletionAndRequestHash() {
        var failure=mock(DccControlledFileMessageJobMapper.class);
        when(failure.insert(any(cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMessageJobDO.class)))
                .thenThrow(new IllegalStateException("test-owned outbox failure"));
        ReflectionTestUtils.setField(finalization,"messageJobMapper",failure);
        assertThrows(IllegalStateException.class,()->distribute(request()));
        assertNull(controlledFileMapper.selectById(42L).getDistributedTime());
        assertNull(controlledFileMapper.selectById(42L).getDistributionPayloadHash());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution_recipient",Integer.class));
    }
    @Test void disabledRecipientFailsBeforeAnyDistributionWrite() {
        when(users.getUserList(List.of(99L))).thenReturn(List.of(new AdminUserRespDTO().setId(99L).setDeptId(51L).setStatus(1)));
        assertThrows(IllegalArgumentException.class,()->distribute(request()));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution",Integer.class));
    }
    @Test void duplicateDirectoryRowsCannotStandInForTheOtherSelectedPaperRecipient() {
        var request=request();
        request.getScopes().get(0).setDistributionMedium("PAPER");
        request.getScopes().get(0).setRecipientUserIds(List.of(99L,100L));
        when(users.getUserList(List.of(99L,100L))).thenReturn(List.of(
                new AdminUserRespDTO().setId(99L).setDeptId(51L).setStatus(0),
                new AdminUserRespDTO().setId(99L).setDeptId(51L).setStatus(0)));
        assertThrows(IllegalArgumentException.class,()->distribute(request));
        assertNull(controlledFileMapper.selectById(42L).getDistributedTime());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_distribution_recipient",Integer.class));
    }
    private void distribute(DccWorkflowDistributionReqVO req) {
        new TransactionTemplate(transactionManager).executeWithoutResult(ignored->distribution.distribute(99L,42L,req));
    }
    private DccWorkflowDistributionReqVO request() {
        var scope=new DccWorkflowDistributionReqVO.Scope();scope.setDepartmentId(51L);
        scope.setDistributionMedium("PUBLIC_FOLDER");scope.setRecipientUserIds(List.of(99L));
        var req=new DccWorkflowDistributionReqVO();req.setScopes(List.of(scope));return req;
    }
}
