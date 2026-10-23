package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceOwnershipMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectConfigurationAuditService;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.*;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.*;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real public A create, ownership, B placement and GXP on H2; storage/network/identity are explicit test ports. */
@Import({DccPublicUploadPlacementService.class,DccProjectFilePlacementService.class,DccFolderTemplateService.class,
    DccProjectConfigurationAuditService.class,GxpAuditServiceImpl.class,DccPublicCreationDatabaseTest.JdbcBean.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts="/sql/dcc_b_gxp_audit_tables.sql",executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts="/sql/dcc_b_gxp_audit_clean.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccPublicCreationDatabaseTest extends DccWorkflowAttributesIntegrationTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class JdbcBean {@org.springframework.context.annotation.Bean org.springframework.jdbc.core.JdbcTemplate jdbc(javax.sql.DataSource ds){return new org.springframework.jdbc.core.JdbcTemplate(ds);}}
    @Resource DccPublicUploadPlacementService publicUpload;
    @Resource DccProjectFilePlacementService placement;
    @Resource DccProjectFolderMapper folders;
    @Resource DccControlledFileSourceOwnershipMapper owners;
    @Resource FileMapper infraFiles;
    @Resource GxpAuditPolicyOperationMapper policies;
    @MockitoBean DccProjectAccessService access;
    @MockitoBean PermissionApi permissionApi;
    final Map<String,byte[]> bytes=new HashMap<>();
    final AtomicLong sequence=new AtomicLong(1000);
    Long folderId;
    @BeforeEach void realCreation() throws Exception {
        configurePublicCreation();
        var login=new LoginUser();login.setId(99L);login.setTenantId(1L);login.setUserType(2);
        login.setInfo(Map.of("username","public-test",LoginUser.INFO_KEY_NICKNAME,"Public test"));
        SecurityFrameworkUtils.setLoginUser(login,new MockHttpServletRequest());
        jdbc.execute("CREATE TABLE IF NOT EXISTS infra_file(id BIGINT PRIMARY KEY,config_id BIGINT,name VARCHAR(256),path VARCHAR(1024),url VARCHAR(1024),type VARCHAR(128),size BIGINT,creator VARCHAR(64),updater VARCHAR(64),create_time TIMESTAMP,update_time TIMESTAMP,deleted BIT DEFAULT 0)");
        jdbc.update("DELETE FROM infra_file");jdbc.update("DELETE FROM dcc_controlled_file_source_ownership");
        var source=FileDO.builder().id(100L).configId(1L).name("SOP.pdf").path("original/SOP.pdf").url("isolated-test").type("application/pdf").size(4L).build();infraFiles.insert(source);bytes.clear();bytes.put(source.getPath(),new byte[]{1,2,3,4});
        var storage=mock(FileService.class);
        when(storage.getFileContent(anyLong(),anyString())).thenAnswer(c->bytes.get(c.getArgument(1)));
        when(storage.createFileAndReturnId(any(),any(),any(),any())).thenAnswer(c->{long id=sequence.incrementAndGet();String path=c.getArgument(2);byte[] body=c.getArgument(0);
            infraFiles.insert(FileDO.builder().id(id).configId(1L).name(c.getArgument(1)).path(path).url("isolated").type(c.getArgument(3)).size((long)body.length).build());bytes.put(path,body.clone());return id;});
        // Exactly like real FileService, deletion by ID requires metadata which vanishes after rollback.
        doAnswer(c->{var row=infraFiles.selectById((Long)c.getArgument(0));if(row==null)throw new IllegalStateException("physical delete requires existing infra metadata");bytes.remove(row.getPath());infraFiles.deleteById(row.getId());return null;}).when(storage).deleteFile(anyLong());
        var sources=new DccControlledFileSourceOwnershipService();
        wire(sources,"controlledFileMapper",files,"ownershipMapper",owners,"fileMapper",infraFiles,"fileService",storage);
        var config=mock(FileConfigService.class);var client=mock(FileClient.class);when(config.getFileClient(1L)).thenReturn(client);
        doAnswer(c->{bytes.remove((String)c.getArgument(0));return null;}).when(client).delete(anyString());
        if(Arrays.stream(sources.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals("fileConfigService")))wire(sources,"fileConfigService",config);
        wire(workflow,"sourceOwnershipService",sources,"fileMapper",infraFiles);
        jdbc.update("INSERT INTO dcc_file_directory(id,tenant_id,code,name,active,sort) VALUES(3,1,'STORAGE-3','Actual NAS',1,0)");
        var folder=new DccProjectFolderDO();folder.setTenantId(1L);folder.setProjectCodeId(5L);folder.setParentId(0L);folder.setName("Logic location");folder.setSortOrder(0);folder.setActive(true);folder.setSourceTemplateId(1L);folder.setSourceNodeKey("quality");folders.insert(folder);folderId=folder.getId();
    }
    @AfterEach void clearPhysicalFixture(){SecurityContextHolder.clearContext();jdbc.update("DELETE FROM infra_file");jdbc.update("DELETE FROM dcc_controlled_file_source_ownership");}
    void policy(){var p=new GxpAuditPolicyOperationDO();p.setTenantId(1L);p.setOperationId("dcc.project-file-placement.bind");p.setPolicyVersion("PUBLIC-TEST");p.setSourceType("SERVICE_METHOD");p.setSourceLocator("ISOLATED_TEST");p.setDomain("DCC");p.setSubjectType("PLACEMENT");p.setActionType("CREATE");p.setReasonPolicy("REQUIRED");p.setSignaturePolicy("NOT_REQUIRED");p.setStatePolicy("BEFORE_AFTER");p.setRetentionClass("TEST");p.setTestIds("P02");p.setOwner("Root");p.setApplicability("GXP");p.setActive(true);policies.insert(p);}
    Long create(){var r=creationRequest();r.setProjectFolderId(folderId);r.setProjectFolderChangeReason("Selected exact logical folder");return publicUpload.create(99L,r,()->workflow.createWorkingControlledFile(99L,r));}
    @Test void publicUploadRealSuccessKeepsOwnershipAndPlacementAndExactReplay() {
        policy();long id=new TransactionTemplate(manager).execute(s->create());
        assertEquals(folderId,placement.require(99L,5L,id).getProjectFolderId());
        assertEquals(3L,placement.require(99L,5L,id).getStorageDirectoryId());
        assertNotEquals(100L,files.selectById(id).getSourceFileId());
        assertNotNull(owners.selectByControlledFileId(1L,id));assertEquals(2,bytes.size());
        assertEquals(Long.valueOf(id),new TransactionTemplate(manager).execute(s->create()));assertEquals(2,bytes.size());
        var changed=creationRequest();changed.setProjectFolderId(folderId);changed.setProjectFolderChangeReason("Different placement reason");
        assertThrows(RuntimeException.class,()->new TransactionTemplate(manager).execute(s->publicUpload.create(99L,changed,()->workflow.createWorkingControlledFile(99L,changed))));
    }
    @ParameterizedTest @ValueSource(strings={"AUDIT","OUTER"})
    void publicUploadLateFailureRollsBackRealOwnershipAndPhysicalCopy(String stage){
        if(stage.equals("OUTER"))policy();
        assertThrows(RuntimeException.class,()->new TransactionTemplate(manager).execute(s->{var id=create();if(stage.equals("OUTER"))throw new IllegalStateException("late caller");return id;}));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file",Integer.class));
        for(String table:List.of("dcc_controlled_file_name_claim","dcc_controlled_file_source_ownership","dcc_application_round_link","dcc_project_application_attributes","dcc_project_file_placement","gxp_audit_event"))assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class),table);
        assertEquals(1,bytes.size(),"isolated source copy must be removed after outer transaction rollback");
    }
    @ParameterizedTest @ValueSource(strings={"FOLDER","ACTUAL"})
    void publicUploadReplayCannotChangeFolderOrActual(String change){
        policy();long id=new TransactionTemplate(manager).execute(s->create());var changed=creationRequest();
        Long requestedFolder=folderId;
        if(change.equals("FOLDER")){var other=folders.selectById(folderId);other.setId(null);other.setName("Other exact folder");other.setSourceNodeKey("other");folders.insert(other);requestedFolder=other.getId();}
        else changed.setProjectAttributes(new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(List.of("NMPA"),null,"Y","N","N",null));
        changed.setProjectFolderId(requestedFolder);changed.setProjectFolderChangeReason("Selected exact logical folder");
        assertThrows(RuntimeException.class,()->new TransactionTemplate(manager).execute(s->publicUpload.create(99L,changed,()->workflow.createWorkingControlledFile(99L,changed))));
        assertEquals(folderId,placement.require(99L,5L,id).getProjectFolderId());assertEquals(2,bytes.size());
    }
    static void wire(Object target,Object... pairs){for(int i=0;i<pairs.length;i+=2)ReflectionTestUtils.setField(target,(String)pairs[i],pairs[i+1]);}
}
