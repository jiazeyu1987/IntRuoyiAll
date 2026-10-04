package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import(DccControlledFileRelatedFileServiceImpl.class)
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:dcc_d_submission_test;MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;NON_KEYWORDS=value")
class DccSubmissionRelationPersistenceTest extends BaseDbUnitTest {
    @Resource javax.sql.DataSource dataSource;
    @Resource DccControlledFileRelatedFileServiceImpl service;
    @Resource DccControlledFileRelatedFileMapper relatedMapper;
    @MockitoBean DccLatestControlledFileResolver files;
    @MockitoBean DccRelationAccessPolicy access;
    @MockitoBean DccRelationStore store;
    JdbcTemplate jdbc;
    @BeforeEach void setup() throws Exception {
        jdbc=new JdbcTemplate(dataSource);
        try(var connection=dataSource.getConnection()){assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        jdbc.update("DELETE FROM dcc_controlled_file_related_file WHERE controlled_file_id IN(100,101)");
        jdbc.update("INSERT INTO dcc_controlled_file_master (id,category_id,file_name,file_number,status,tenant_id) VALUES (10,1,'源.pdf','SOURCE','ACTIVE',1)");
        jdbc.update("INSERT INTO dcc_controlled_file (id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,status,submitter_id,requester_id,dcc_project_code_id,tenant_id) VALUES (100,10,1,1,1,1,'源.pdf','源','SOURCE','A/1','DRAFT',7,7,1,1)");
        var actor=new LoginUser();actor.setId(7L);actor.setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,List.of()));
        var first=new FileVersion(1L,201L,20L,2L,"N201","跨项目.pdf","B/1","ACTIVE",true,true,false);
        var second=new FileVersion(1L,301L,30L,3L,"N301","其他项目.pdf","A/1","ACTIVE",true,false,true);
        when(files.resolveSelected(201L)).thenReturn(first);when(files.resolveLatest(20L)).thenReturn(first);
        when(files.resolveSelected(301L)).thenReturn(second);when(files.resolveLatest(30L)).thenReturn(second);
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();if(jdbc!=null)jdbc.update("DELETE FROM dcc_controlled_file_related_file WHERE controlled_file_id IN(100,101)");}
    @Test void submissionPersistsCrossProjectFrozenVersionAndHistoryIgnoresLaterLatest() {
        service.validateAndBindRelatedFiles(100L,1L,List.of(201L,301L));
        var saved=relatedMapper.selectListByControlledFileId(100L);assertEquals(2,saved.size());
        assertEquals(List.of(20L,30L),saved.stream().map(row->row.getRelatedMasterId()).toList());
        assertEquals("B/1",saved.get(0).getRelatedVersionNoSnapshot());
        assertEquals(201L,saved.get(0).getRelatedControlledFileId());assertEquals(1L,saved.get(0).getTenantId());
        when(files.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,1L,"SOURCE","源.pdf","A/1","DRAFT",false,false,false));
        when(files.resolveLatest(20L)).thenReturn(new FileVersion(1L,202L,20L,2L,"N201","跨项目.pdf","B/2","ACTIVE",true,false,true));
        var history=service.listHistoricalRelatedFiles(7L,100L);assertEquals("B/1",history.get(0).getVersionNo());assertEquals(201L,history.get(0).getControlledFileId());
    }
    @Test void deniedSecondCandidateCausesZeroWritesBeforeInsert() {
        doThrow(new DccRelationFailure("DCC_NAME_PERMISSION_DENIED")).when(access).assertNameVisible(7L,301L);
        assertThrows(IllegalStateException.class,()->service.validateAndBindRelatedFiles(100L,1L,List.of(201L,301L)));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_related_file",Integer.class));
    }
    @Test void realUniqueConstraintFailureOnLaterCandidateRollsBackFirstCandidate() {
        service.validateAndBindRelatedFiles(100L,1L,List.of(301L));
        assertThrows(org.springframework.dao.DuplicateKeyException.class,()->service.validateAndBindRelatedFiles(100L,1L,List.of(201L,301L)));
        var saved=relatedMapper.selectListByControlledFileId(100L);assertEquals(1,saved.size());assertEquals(301L,saved.get(0).getRelatedControlledFileId());
    }
    @Test void checkinInheritancePersistsTheSameTenantAndExactFrozenVersionWithoutChangingSource() {
        service.validateAndBindRelatedFiles(100L,1L,List.of(201L));
        jdbc.update("INSERT INTO dcc_controlled_file (id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,status,submitter_id,requester_id,dcc_project_code_id,tenant_id) VALUES (101,10,1,1,2,2,'源.pdf','源','SOURCE','A/1-1','WORKING',7,7,1,1)");
        service.inheritRelatedFiles(100L,101L);
        var inherited=relatedMapper.selectListByControlledFileId(101L);assertEquals(1,inherited.size());
        assertEquals(1L,inherited.get(0).getTenantId());assertEquals("B/1",inherited.get(0).getRelatedVersionNoSnapshot());
        assertEquals(201L,inherited.get(0).getRelatedControlledFileId());assertEquals(1,relatedMapper.selectListByControlledFileId(100L).size());
        jdbc.update("DELETE FROM dcc_controlled_file_related_file WHERE controlled_file_id=101");
    }
    @Test void historicalReadExcludesForeignTenantRowsEvenWithoutTenantInterceptor(){
        service.validateAndBindRelatedFiles(100L,1L,List.of(201L));
        jdbc.update("INSERT INTO dcc_controlled_file_related_file (controlled_file_id,related_controlled_file_id,project_code_id,related_master_id,related_file_name_snapshot,related_version_no_snapshot,relation_source,tenant_id) VALUES (100,999,2,99,'外租户私密文件','Z/1','UPLOAD',2)");
        when(files.resolveSelected(100L)).thenReturn(new FileVersion(1L,100L,10L,1L,"SOURCE","源.pdf","A/1","DRAFT",false,false,false));
        var history=service.listHistoricalRelatedFiles(7L,100L);assertEquals(1,history.size());assertEquals(201L,history.get(0).getControlledFileId());
        verify(access,never()).assertNameVisible(7L,999L);
    }
    @ParameterizedTest
    @ValueSource(strings={"PROCESS","CONTROLLED_TIME","ACTIVATED_TIME","ACTIVE","CONTROLLED_PENDING_EFFECTIVE","OBSOLETE","SUPERSEDED","PENDING_MATRIX_REVIEW","WITHDRAWN","REJECTED"})
    void approvalOrControlledFactsFreezeBothBindingAndInheritanceTarget(String fact){
        service.validateAndBindRelatedFiles(100L,1L,List.of(201L));
        jdbc.update("INSERT INTO dcc_controlled_file (id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,status,submitter_id,requester_id,dcc_project_code_id,tenant_id) VALUES (101,10,1,1,2,2,'源.pdf','源','SOURCE','A/1-1','WORKING',7,7,1,1)");
        service.validateAndBindRelatedFiles(101L,1L,List.of(301L));
        switch(fact){
            case "PROCESS" -> jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='actual-approval-round' WHERE id=100");
            case "CONTROLLED_TIME" -> jdbc.update("UPDATE dcc_controlled_file SET controlled_time=CURRENT_TIMESTAMP WHERE id=100");
            case "ACTIVATED_TIME" -> jdbc.update("UPDATE dcc_controlled_file SET activated_time=CURRENT_TIMESTAMP WHERE id=100");
            default -> jdbc.update("UPDATE dcc_controlled_file SET status=? WHERE id=100",fact);
        }
        assertAll(
                ()->assertEquals("DCC_RELATION_APPROVAL_SNAPSHOT_FROZEN",assertThrows(DccRelationFailure.class,
                        ()->service.validateAndBindRelatedFiles(100L,1L,List.of(301L))).getMessage()),
                ()->assertEquals("DCC_RELATION_APPROVAL_SNAPSHOT_FROZEN",assertThrows(DccRelationFailure.class,
                        ()->service.inheritRelatedFiles(101L,100L)).getMessage()));
        var saved=relatedMapper.selectListByControlledFileId(100L);assertEquals(1,saved.size());assertEquals(201L,saved.get(0).getRelatedControlledFileId());
    }
}
