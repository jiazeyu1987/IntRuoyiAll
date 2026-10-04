package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFolderDO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** VDIR: real project rules / folders / Controller method authorization persisted in isolated H2. */
@org.springframework.context.annotation.Import(DccProjectFolderViewReadTest.WriteObservation.class)
class DccProjectFolderViewReadTest extends DccProjectFormalCombinationTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class WriteObservation {
        @org.springframework.context.annotation.Bean
        WriteCounter writeCounter() {return new WriteCounter();}
    }
    @org.apache.ibatis.plugin.Intercepts(@org.apache.ibatis.plugin.Signature(
            type=org.apache.ibatis.executor.Executor.class,method="update",args={org.apache.ibatis.mapping.MappedStatement.class,Object.class}))
    static class WriteCounter implements org.apache.ibatis.plugin.Interceptor {
        final java.util.concurrent.atomic.AtomicInteger writes=new java.util.concurrent.atomic.AtomicInteger();
        @Override public Object intercept(org.apache.ibatis.plugin.Invocation invocation) throws Throwable {
            writes.incrementAndGet();return invocation.proceed();
        }
    }
    @jakarta.annotation.Resource WriteCounter writeCounter;
    @jakarta.annotation.Resource cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService folderService;
    MockMvc mvc() {
        return org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(projectController,folderMaintenanceController)
                .setControllerAdvice(new cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler("dcc-b-view-directory",apiErrorLogs)).build();
    }
    void grant(Long projectId,Long actor,String level) {
        var rule=new DccProjectAccessRuleDO();rule.setTenantId(1L);rule.setDccProjectCodeId(projectId);
        rule.setSubjectType("USER");rule.setSubjectId(actor);rule.setAccessLevel(level);rule.setActive(true);
        rule.setChangeReason("isolated VIEW directory fixture");assertEquals(1,rules.insert(rule));
    }
    void readerMenu() {
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:query")).thenReturn(true);
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:query","dcc:controlled-file:query")).thenReturn(true);
        login(8L);
    }
    @ParameterizedTest @ValueSource(strings={"VIEW","EDIT","OWNER"})
    void viewDirectoryReaderGetsRealEmptyFoldersWithoutAnyControlledFiles(String level) throws Exception {
        var project=project("VDIR-"+level,7L);grant(project.getId(),8L,level);readerMenu();
        var parent=folder(project.getId(),"empty-parent");var child=folder(project.getId(),"empty-child");
        child.setParentId(parent.getId());assertEquals(1,folders.updateById(child));
        assertTrue(files.selectList().isEmpty());
        mvc().perform(get("/dcc/project-codes/{project}/folders",project.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(String.valueOf(parent.getId())))
                .andExpect(jsonPath("$.data[1].parentId").value(String.valueOf(parent.getId())))
                .andExpect(jsonPath("$.data[1].projectCodeId").value(String.valueOf(project.getId())))
                .andExpect(jsonPath("$.data[1].name").value("empty-child"));
    }
    @org.junit.jupiter.api.Test
    void viewDirectoryControlledFileQueryMenuDoesNotRequireConfigurationMenu() throws Exception {
        var project=project("VDIR-QUERY-MENU",7L);grant(project.getId(),8L,"VIEW");readerMenu();
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:query")).thenReturn(false);
        mvc().perform(get("/dcc/project-codes/{project}/folders",project.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @ParameterizedTest @ValueSource(strings={"NO_RULE","OTHER_PROJECT","RULE_FOREIGN_TENANT","RULE_INACTIVE","RULE_EXPIRED","RULE_FUTURE",
            "USER_DISABLED","USER_MISSING","PROJECT_DISABLED","PROJECT_MISSING","PROJECT_DELETED","PROJECT_FOREIGN_TENANT","CALLER_FOREIGN_TENANT","ADMIN_WITHOUT_SCOPE"})
    void viewDirectoryRejectsForeignScopeInactiveIdentityAndAdminWithoutRule(String violation) throws Exception {
        var project=project("VDIR-DENIED",7L);grant(project.getId(),8L,"VIEW");readerMenu();folder(project.getId(),"private-empty");
        Long requested=project.getId();
        var rule=rules.selectActiveRules(project.getId(),java.time.LocalDateTime.now()).stream()
                .filter(row->Long.valueOf(8).equals(row.getSubjectId())).findFirst().orElseThrow();
        switch(violation) {
            case "NO_RULE" -> rules.deleteById(rule.getId());
            case "OTHER_PROJECT" -> requested=project("VDIR-OTHER",7L).getId();
            case "RULE_FOREIGN_TENANT" -> jdbc.update("UPDATE dcc_project_access_rule SET tenant_id=2 WHERE id=?",rule.getId());
            case "RULE_INACTIVE" -> jdbc.update("UPDATE dcc_project_access_rule SET active=0 WHERE id=?",rule.getId());
            case "RULE_EXPIRED" -> {rule.setExpireTime(java.time.LocalDateTime.now().minusDays(1));rules.updateById(rule);}
            case "RULE_FUTURE" -> {rule.setValidFrom(java.time.LocalDateTime.now().plusDays(1));rules.updateById(rule);}
            case "USER_DISABLED" -> when(users.getUser(8L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(8L).setStatus(1));
            case "USER_MISSING" -> when(users.getUser(8L)).thenReturn(null);
            case "PROJECT_DISABLED" -> {project.setStatus("DISABLE");projects.updateById(project);}
            case "PROJECT_MISSING" -> requested=999L;
            case "PROJECT_DELETED" -> projects.deleteById(project.getId());
            case "PROJECT_FOREIGN_TENANT" -> jdbc.update("UPDATE dcc_project_code SET tenant_id=2 WHERE id=?",project.getId());
            case "CALLER_FOREIGN_TENANT" -> TenantContextHolder.setTenantId(2L);
            case "ADMIN_WITHOUT_SCOPE" -> {
                when(permissions.hasAnyPermissions(1L,"dcc:project-code:query")).thenReturn(true);
                when(permissions.hasAnyPermissions(1L,"dcc:project-code:query","dcc:controlled-file:query")).thenReturn(true);
                login(1L);
            }
            default -> throw new IllegalArgumentException(violation);
        }
        String before=businessState();writeCounter.writes.set(0);
        try {
            mvc().perform(get("/dcc/project-codes/{project}/folders",requested))
                    .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()))
                    .andExpect(jsonPath("$.data").doesNotExist());
            assertEquals(0,writeCounter.writes.get());assertEquals(before,businessState());
        } finally {TenantContextHolder.setTenantId(1L);}
    }

    @org.junit.jupiter.api.Test
    void viewDirectoryRequiresARealQueryMenuEvenWithProjectViewRule() throws Exception {
        var project=project("VDIR-NO-MENU",7L);grant(project.getId(),8L,"VIEW");readerMenu();
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:query")).thenReturn(false);
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:query","dcc:controlled-file:query")).thenReturn(false);
        mvc().perform(get("/dcc/project-codes/{project}/folders",project.getId()))
                .andExpect(jsonPath("$.code").value(403)).andExpect(jsonPath("$.data").doesNotExist());
    }

    @ParameterizedTest @ValueSource(strings={"USER","DEPT","ROLE","POSITION"})
    void viewDirectoryUsesTheExistingFormalSubjectMatchingRules(String subjectType) throws Exception {
        var project=project("VDIR-SUBJECT",7L);readerMenu();
        var user=new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(8L).setStatus(0).setDeptId(88L).setPostIds(java.util.Set.of(88L));
        when(users.getUser(8L)).thenReturn(user);when(permissions.getUserRoleIdListByUserId(8L)).thenReturn(java.util.Set.of(88L));
        var rule=new DccProjectAccessRuleDO();rule.setTenantId(1L);rule.setDccProjectCodeId(project.getId());
        rule.setSubjectType(subjectType);rule.setSubjectId("USER".equals(subjectType)?8L:88L);
        rule.setAccessLevel("VIEW");rule.setActive(true);rule.setChangeReason("formal subject read scope");rules.insert(rule);
        folder(project.getId(),"subject-empty");
        mvc().perform(get("/dcc/project-codes/{project}/folders",project.getId()))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.length()").value(1));
    }

    @org.junit.jupiter.api.Test
    void viewDirectoryMaintainsExactLongProjectFolderAndParentIdsAndIncludesEmptyRoots() throws Exception {
        var project=project("VDIR-LONG",7L);grant(project.getId(),8L,"VIEW");readerMenu();
        var parent=folder(project.getId(),"one-empty-root");var child=folder(project.getId(),"empty-child");
        child.setParentId(parent.getId());folders.updateById(child);var other=folder(project.getId(),"another-empty-root");
        assertTrue(project.getId()>9007199254740991L);assertTrue(parent.getId()>9007199254740991L);
        var foreign=new DccProjectFolderDO();foreign.setTenantId(2L);foreign.setProjectCodeId(project.getId());foreign.setParentId(0L);
        foreign.setName("foreign-tenant-folder");foreign.setSortOrder(0);foreign.setActive(true);folders.insert(foreign);
        var deleted=folder(project.getId(),"logically-deleted");folders.deleteById(deleted.getId());
        var response=mvc().perform(get("/dcc/project-codes/{project}/folders",project.getId()))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].id").value(String.valueOf(parent.getId())))
                .andExpect(jsonPath("$.data[1].id").value(String.valueOf(child.getId())))
                .andExpect(jsonPath("$.data[1].parentId").value(String.valueOf(parent.getId())))
                .andExpect(jsonPath("$.data[1].projectCodeId").value(String.valueOf(project.getId())))
                .andExpect(jsonPath("$.data[2].id").value(String.valueOf(other.getId()))).andReturn();
        assertFalse(response.getResponse().getContentAsString().contains("storageDirectoryId"));
        assertFalse(response.getResponse().getContentAsString().contains("canPreview"));
        assertTrue(files.selectList().isEmpty());
    }

    @org.junit.jupiter.api.Test
    void viewDirectoryCannotCreateEditDeletePlaceOrSaveAttributesEvenWithUpdateMenu() throws Exception {
        var project=project("VDIR-NO-WRITE",7L);grant(project.getId(),8L,"VIEW");readerMenu();
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:update")).thenReturn(true);
        var folder=folder(project.getId(),"stable-empty");var file=file(project.getId(),101L,"stable-original.pdf");
        String before=businessState();writeCounter.writes.set(0);
        var denied=cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode();
        var mvc=mvc();
        mvc.perform(put("/dcc/project-codes/{project}/folders",project.getId()).contentType("application/json")
                        .content("{\"parentId\":0,\"name\":\"not-created\",\"sortOrder\":0,\"changeReason\":\"explicit denied create\"}"))
                .andExpect(jsonPath("$.code").value(denied));
        mvc.perform(put("/dcc/project-codes/{project}/folders",project.getId()).contentType("application/json")
                        .content("{\"id\":\""+folder.getId()+"\",\"parentId\":0,\"name\":\"not-renamed\",\"sortOrder\":0,\"changeReason\":\"explicit denied edit\"}"))
                .andExpect(jsonPath("$.code").value(denied));
        mvc.perform(delete("/dcc/project-codes/{project}/folders/{folder}",project.getId(),folder.getId()).contentType("application/json")
                        .content("{\"confirmed\":true,\"changeReason\":\"explicit denied delete\"}"))
                .andExpect(jsonPath("$.code").value(denied));
        assertThrows(RuntimeException.class,()->tx(()->placements.bind(8L,project.getId(),folder.getId(),file.getId(),101L,"denied position")));
        assertThrows(RuntimeException.class,()->attributes.saveDraft(8L,project.getId(),"UPLOAD",file.getId(),1,value("CE")));
        assertEquals(0,writeCounter.writes.get());assertEquals(before,businessState());
    }

    @org.junit.jupiter.api.Test
    void viewDirectoryRepeatedHttpAndServiceReadsLeaveEveryBusinessRowAndLedgerUnchanged() throws Exception {
        var project=project("VDIR-NO-DML",7L);grant(project.getId(),8L,"VIEW");readerMenu();
        var folder=folder(project.getId(),"persisted-folder");var file=file(project.getId(),101L,"existing-history.pdf");
        directory(101L,1L,true);policy("dcc.project-file-placement.bind");login(7L);
        tx(()->placements.bind(7L,project.getId(),folder.getId(),file.getId(),101L,"existing saved location"));
        tx(()->snapshots.submit(7L,project.getId(),"UPLOAD",file.getId(),"existing-real-bpm",value("CE")));readerMenu();
        String before=businessState();writeCounter.writes.set(0);
        for(int i=0;i<3;i++) {
            mvc().perform(get("/dcc/project-codes/{project}/folders",project.getId())).andExpect(jsonPath("$.code").value(0));
            assertEquals(folder.getId(),folderService.readProjectFolders(8L,project.getId()).get(0).getId());
        }
        assertEquals(0,writeCounter.writes.get(),"real MyBatis Executor must not execute business writes on directory reads");
        assertEquals(before,businessState(),"includes existing placement, File, snapshots, round and unified ledger facts");
    }

    String businessState() {
        var states=new java.util.LinkedHashMap<String,Object>();
        for(String table:new String[]{"dcc_project_code","dcc_project_access_rule","dcc_project_folder","dcc_folder_template",
                "dcc_folder_template_history","dcc_controlled_file","dcc_controlled_file_master","dcc_file_directory",
                "dcc_project_file_placement","dcc_project_application_attributes","dcc_application_round_link","dcc_project_file_reference",
                "gxp_audit_event","gxp_audit_ledger_sequence","gxp_audit_policy_operation"})
            states.put(table,jdbc.queryForList("SELECT * FROM "+table));
        return states.toString();
    }
}
