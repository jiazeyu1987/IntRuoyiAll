package cn.iocoder.yudao.module.dcc.service.projectcode;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({DccProjectReferenceAuthorityImpl.class,DccProjectReferenceService.class,DccLatestControlledFileResolverImpl.class,DccRelationStore.class})
class DccProjectFolderDeletionCombinationTest extends DccProjectFormalCombinationTest {
    @Resource DccProjectReferenceService references;
    @Resource DccRelationStore relationStore;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl ledger;
    void referencePolicies(){policy("dcc.project-reference.create");policy("dcc.project-reference.cancel");policy("dcc.project-folder.delete");}
    @Test void cancelledReferenceAllowsLogicalDeletionAndRetainsFolderIdentityOriginalFileAndAuditHistory(){
        referencePolicies();var origin=project("DELETE-SOURCE",9L);var target=project("DELETE-TARGET",7L);
        var folder=folder(target.getId(),"REF");var file=file(origin.getId(),101L,"delete-source.pdf");
        String fileBefore=JsonUtils.toJsonString(files.selectById(file.getId()));
        var reference=references.create(7L,target.getId(),folder.getId(),file.getId(),"确认项目引用");
        assertThrows(RuntimeException.class,()->folderMaintenance.delete(7L,target.getId(),folder.getId(),true,"有引用不能删除"));
        references.cancel(7L,target.getId(),folder.getId(),file.getMasterId(),reference.reference().id(),true,"明确取消引用");
        String auditBefore=events.selectList().get(0).getCanonicalEventJson();
        folderMaintenance.delete(7L,target.getId(),folder.getId(),true,"取消引用后的空目录删除");
        assertNull(folders.selectById(folder.getId()));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder WHERE id=? AND deleted=1 AND active=0",Integer.class,folder.getId()));
        assertEquals(auditBefore,events.selectList().get(0).getCanonicalEventJson());assertEquals(3,events.selectList().size());
        assertEquals(fileBefore,JsonUtils.toJsonString(files.selectById(file.getId())));
        assertThrows(RuntimeException.class,()->references.create(7L,target.getId(),folder.getId(),file.getId(),"删除后不得新引用"));
    }
    @Test void childrenPlacementsWrongIdentityMissingConfirmationAndMissingPolicyRejectWithoutDeletion(){
        var project=project("DELETE-GUARDS",7L);var root=folder(project.getId(),"root");var child=folder(project.getId(),"child");
        child.setParentId(root.getId());folders.updateById(child);policy("dcc.project-folder.delete");
        assertThrows(RuntimeException.class,()->folderMaintenance.delete(7L,project.getId(),root.getId(),true,"有子目录"));
        assertThrows(RuntimeException.class,()->folderMaintenance.delete(7L,project.getId(),child.getId(),false,"未确认"));
        var other=project("DELETE-OTHER",7L);
        assertThrows(RuntimeException.class,()->folderMaintenance.delete(7L,other.getId(),child.getId(),true,"错项目"));
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:update")).thenReturn(true);
        assertThrows(RuntimeException.class,()->folderMaintenance.delete(8L,project.getId(),child.getId(),true,"无项目范围"));
        jdbc.update("INSERT INTO dcc_project_file_placement(tenant_id,project_code_id,project_folder_id,controlled_file_id,storage_directory_id,deleted) VALUES(1,?,?,?,?,0)",project.getId(),child.getId(),123L,456L);
        assertThrows(RuntimeException.class,()->folderMaintenance.delete(7L,project.getId(),child.getId(),true,"已有历史位置"));
        var empty=folder(project.getId(),"no-policy");policies.deleteById(policies.selectList().get(0).getId());
        assertThrows(RuntimeException.class,()->folderMaintenance.delete(7L,project.getId(),empty.getId(),true,"缺审计策略"));
        assertNotNull(folders.selectById(empty.getId()));assertTrue(events.selectList().isEmpty());
    }
    <T>Future<T> submit(ExecutorService pool,java.util.function.Supplier<T> action){
        return pool.submit(()->{TenantContextHolder.setTenantId(1L);login(7L);try{return action.get();}finally{TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}});
    }
    @Test void referenceCreationHoldingSharedLockMakesConcurrentDeleteWaitAndThenReject() throws Exception{
        referencePolicies();var source=project("CONCURRENT-SOURCE",9L);var target=project("CONCURRENT-TARGET",7L);
        var folder=folder(target.getId(),"R");var file=file(source.getId(),101L,"concurrent.pdf");
        var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{
            var creating=submit(pool,()->tx(()->{references.create(7L,target.getId(),folder.getId(),file.getId(),"引用先锁");locked.countDown();
                try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException e){throw new IllegalStateException(e);}return true;}));
            assertTrue(locked.await(10,TimeUnit.SECONDS));
            var deleting=submit(pool,()->{folderMaintenance.delete(7L,target.getId(),folder.getId(),true,"并发删除");return true;});
            assertThrows(TimeoutException.class,()->deleting.get(200,TimeUnit.MILLISECONDS));release.countDown();assertTrue(creating.get(10,TimeUnit.SECONDS));
            assertThrows(ExecutionException.class,()->deleting.get(10,TimeUnit.SECONDS));assertNotNull(folders.selectById(folder.getId()));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
        }finally{release.countDown();pool.shutdownNow();}
    }
    @Test void deleteHoldingSharedLockMakesConcurrentReferenceWaitAndRejectAfterCommit() throws Exception{
        referencePolicies();var source=project("DELETE-FIRST-SOURCE",9L);var target=project("DELETE-FIRST-TARGET",7L);
        var folder=folder(target.getId(),"R");var file=file(source.getId(),101L,"delete-first.pdf");
        var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{
            var deleting=submit(pool,()->tx(()->{folderMaintenance.delete(7L,target.getId(),folder.getId(),true,"删除先锁");locked.countDown();
                try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException e){throw new IllegalStateException(e);}return true;}));
            assertTrue(locked.await(10,TimeUnit.SECONDS));
            var creating=submit(pool,()->references.create(7L,target.getId(),folder.getId(),file.getId(),"等待后引用"));
            assertThrows(TimeoutException.class,()->creating.get(200,TimeUnit.MILLISECONDS));release.countDown();assertTrue(deleting.get(10,TimeUnit.SECONDS));
            assertThrows(ExecutionException.class,()->creating.get(10,TimeUnit.SECONDS));assertNull(folders.selectById(folder.getId()));
            assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_reference",Integer.class));
        }finally{release.countDown();pool.shutdownNow();}
    }
    @Test void auditAppendFailureRestoresActualFolderAndCreatesNoDeletionEvent(){
        policy("dcc.project-folder.delete");var project=project("DELETE-APPEND-FAIL",7L);var folder=folder(project.getId(),"empty");
        doThrow(new IllegalStateException("正式账本append失败")).when(ledger).append(argThat(command->command!=null && "dcc.project-folder.delete".equals(command.getOperationId())));
        assertThrows(RuntimeException.class,()->folderMaintenance.delete(7L,project.getId(),folder.getId(),true,"空目录删除"));
        assertNotNull(folders.selectById(folder.getId()));assertTrue(folders.selectById(folder.getId()).getActive());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder WHERE id=? AND deleted=1",Integer.class,folder.getId()));assertTrue(events.selectList().isEmpty());
    }
    @Test void cancellationAndDeleteShareOrderAndPreserveSourceWithoutDeadlock() throws Exception{
        referencePolicies();var source=project("CANCEL-SOURCE",9L);var target=project("CANCEL-TARGET",7L);var folder=folder(target.getId(),"R");
        var file=file(source.getId(),101L,"cancel-first.pdf");var ref=references.create(7L,target.getId(),folder.getId(),file.getId(),"正式引用");
        String sourceBefore=JsonUtils.toJsonString(files.selectById(file.getId()));
        var locked=new CountDownLatch(1);var release=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{
            var cancelling=submit(pool,()->tx(()->{references.cancel(7L,target.getId(),folder.getId(),file.getMasterId(),ref.reference().id(),true,"取消最后引用");locked.countDown();
                try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException e){throw new IllegalStateException(e);}return true;}));
            assertTrue(locked.await(10,TimeUnit.SECONDS));
            var deleting=submit(pool,()->{folderMaintenance.delete(7L,target.getId(),folder.getId(),true,"取消后的空目录");return true;});
            assertThrows(TimeoutException.class,()->deleting.get(200,TimeUnit.MILLISECONDS));release.countDown();
            assertTrue(cancelling.get(10,TimeUnit.SECONDS));assertTrue(deleting.get(10,TimeUnit.SECONDS));
            assertEquals(sourceBefore,JsonUtils.toJsonString(files.selectById(file.getId())));assertNull(folders.selectById(folder.getId()));
            assertEquals(3,events.selectList().size());
        }finally{release.countDown();pool.shutdownNow();}
    }
    @Test void deleteHttpRequiresExactConfirmationAndPreservesLogicalHistoricalRow() throws Exception{
        policy("dcc.project-folder.delete");var project=project("DELETE-HTTP",7L);var folder=folder(project.getId(),"empty");
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(folderMaintenanceController)
                .setControllerAdvice(new cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler("dcc-b-delete",apiErrorLogs)).build();
        String route="/dcc/project-codes/{projectId}/folders/{folderId}";
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(route,project.getId(),folder.getId())
                .contentType("application/json").content("{\"confirmed\":false,\"changeReason\":\"取消确认\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(1080090010));
        assertNotNull(folders.selectById(folder.getId()));assertTrue(events.selectList().isEmpty());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(route,project.getId(),folder.getId())
                .contentType("application/json").content("{\"confirmed\":true,\"changeReason\":\"删除空目录明确原因\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(0));
        assertNull(folders.selectById(folder.getId()));assertEquals(1,events.selectList().size());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_folder WHERE id=? AND deleted=1",Integer.class,folder.getId()));
    }
}
