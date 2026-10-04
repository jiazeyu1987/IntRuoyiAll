package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStageCodeEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccApprovalFileOwnerTransactionTest extends BaseDbUnitTest {
    @Resource DataSource source;
    @Resource PlatformTransactionManager transactions;
    @Resource DccControlledFileMapper files;
    @Resource DccControlledFileSignatureMapper signatures;
    JdbcTemplate jdbc;
    DccApprovalFileOwnerSelectionService service;
    AdminUserApi users;
    @BeforeEach void init() throws Exception {
        try(var connection=source.getConnection()){assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        TenantContextHolder.setTenantId(1L);jdbc=new JdbcTemplate(source);users=mock(AdminUserApi.class);
        when(users.getUser(9007199254740993L)).thenReturn(new AdminUserRespDTO().setId(9007199254740993L).setTenantId(1L).setStatus(0).setUsername("owner").setNickname("原负责人"));
        service=new DccApprovalFileOwnerSelectionService();ReflectionTestUtils.setField(service,"users",users);ReflectionTestUtils.setField(service,"files",files);ReflectionTestUtils.setField(service,"signatures",signatures);
        files.insert(DccControlledFileDO.builder().id(10L).tenantId(1L).masterId(1L).categoryId(2L).directoryId(3L)
                .sourceFileId(4L).originalFileId(4L).fileName("SOP").title("SOP").fileNumber("N-1").versionNo("A/1")
                .submitterId(99L).requesterId(99L)
                .processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).processInstanceId("upload-10").status("PENDING_MATRIX_APPROVAL").build());
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    void signAndBind(String task,long id) {
        var file=files.selectById(10L);var selected=service.prepare(file,DccControlledFileStageCodeEnum.MATRIX_APPROVAL,9007199254740993L);
        String reason=service.signedReason("批准",selected);
        jdbc.update("INSERT INTO dcc_controlled_file_signature(id,controlled_file_id,task_id,actor_id,action_type,signature_mode,process_instance_id,version_no,comment,evidence_status,signed_at,tenant_id) VALUES(?,10,?,99,'APPROVE','FIXTURE','upload-10','A/1',?,'VALID',?,1)",id,task,reason,LocalDateTime.of(2026,10,3,9,0));
        jdbc.update("UPDATE dcc_controlled_file_signature SET meaning_code='MATRIX_APPROVAL_APPROVE',evidence_hash='isolated-owner-evidence' WHERE id=?",id);
        service.bind(file,task,99L,id,selected,reason);
    }
    @Test void actualOwnerAndSignatureSnapshotCommitTogetherAndLaterAccountRenameDoesNotRewriteHistory() {
        new TransactionTemplate(transactions).executeWithoutResult(ignored->signAndBind("approve-task",100L));
        var result=files.selectById(10L);assertEquals(9007199254740993L,result.getFileOwnerUserId());assertEquals(100L,result.getFileOwnerSignatureId());
        assertEquals("原负责人",result.getFileOwnerNicknameSnapshot());assertEquals("upload-10",result.getFileOwnerProcessInstanceId());
        when(users.getUser(9007199254740993L)).thenReturn(new AdminUserRespDTO().setId(9007199254740993L).setTenantId(1L).setStatus(0).setUsername("renamed").setNickname("新名字"));
        var selection=service.prepare(result,DccControlledFileStageCodeEnum.MATRIX_APPROVAL,9007199254740993L);
        assertEquals("新名字",selection.nickname());assertEquals("renamed",selection.username());
        assertEquals("原负责人",files.selectById(10L).getFileOwnerNicknameSnapshot());
    }
    @Test void lateEnclosingApprovalFailureRollsBackActualOwnerAndSignatureRows() {
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).executeWithoutResult(ignored->{signAndBind("approve-task",100L);throw new IllegalStateException("late BPM/audit failure");}));
        assertNull(files.selectById(10L).getFileOwnerUserId());assertNull(files.selectById(10L).getFileOwnerSignatureId());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_signature WHERE controlled_file_id=10",Integer.class));
    }
    @Test void laterCountersignerProjectsTheLatestSignedSelectionAndKeepsBothOriginalSignatures() {
        new TransactionTemplate(transactions).executeWithoutResult(ignored->signAndBind("first-task",100L));
        new TransactionTemplate(transactions).executeWithoutResult(ignored->signAndBind("second-task",101L));
        assertEquals(101L,files.selectById(10L).getFileOwnerSignatureId());
        assertEquals(2,signatures.selectListByControlledFileId(10L).size());
        assertTrue(signatures.selectById(100L).getComment().contains("原负责人"));
        when(users.getUser(8L)).thenReturn(new AdminUserRespDTO().setId(8L).setTenantId(1L).setStatus(0).setUsername("other").setNickname("其他"));
        assertEquals(8L,service.prepare(files.selectById(10L),DccControlledFileStageCodeEnum.MATRIX_APPROVAL,8L).userId());
    }
}
