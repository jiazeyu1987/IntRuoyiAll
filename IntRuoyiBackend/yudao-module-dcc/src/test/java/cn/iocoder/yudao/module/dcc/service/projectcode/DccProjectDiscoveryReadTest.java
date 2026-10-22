package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectCodeController;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectAccessRuleDO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAssignmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Actual public project Controller / project service / access / folders / assignment / isolated H2. */
@Import({DccProjectCodeServiceImpl.class,DccProjectCodeController.class,DccProjectFolderViewReadTest.WriteObservation.class})
class DccProjectDiscoveryReadTest extends DccProjectFormalCombinationTest {
    @Resource DccProjectCodeController discoveryController;
    @Resource DccProjectCodeService projectCodes;
    @Resource DccProjectCodeAssignmentMapper assignments;
    @Resource DccProjectFolderViewReadTest.WriteCounter writeCounter;
    @MockitoBean cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService fileQuery;
    @MockitoBean cn.iocoder.yudao.module.dcc.registrationcertificate.service.association.DccRegistrationCertificateProjectCodeFileAssociationService certificateFiles;
    @MockitoBean cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService taxonomy;
    @MockitoBean cn.iocoder.yudao.module.dcc.api.projectcode.DccProjectCodeConfigurationStatusApi configuration;
    @MockitoBean cn.iocoder.yudao.module.mdm.api.product.MdmProductApi products;
    @org.junit.jupiter.api.BeforeEach void discoveryAccounts() {
        when(certificateFiles.countAssociatedFilesByProjectCodeIds(any())).thenReturn(Map.of());
        when(certificateFiles.listAssociatedRows(any(),any(),any())).thenReturn(List.of());
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:query")).thenReturn(true);
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:query","dcc:controlled-file:query")).thenReturn(true);
        login(8L);
    }
    MockMvc discoveryMvc() {
        return org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(discoveryController,projectController,folderMaintenanceController)
                .setControllerAdvice(new cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler("dcc-b-project-discovery",apiErrorLogs)).build();
    }
    void view(Long project) {
        var rule=new DccProjectAccessRuleDO();rule.setTenantId(1L);rule.setDccProjectCodeId(project);rule.setSubjectType("USER");
        rule.setSubjectId(8L);rule.setAccessLevel("VIEW");rule.setActive(true);rule.setChangeReason("real discovery scope");rules.insert(rule);
    }
    @Test void discoveryPageFindsAllowedEmptyProjectWithoutLeakingUnscopedProject() throws Exception {
        var allowed=project("PUBLIC-EMPTY",7L);var hidden=project("PRIVATE-EMPTY",7L);view(allowed.getId());folder(allowed.getId(),"empty-directory");
        discoveryMvc().perform(get("/dcc/project-codes/page").param("pageNo","1").param("pageSize","1"))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(String.valueOf(allowed.getId())))
                .andExpect(jsonPath("$.data.list[0].projectName").value("PUBLIC-EMPTY"));
    }
    @Test void discoveryDetailRejectsProjectWithoutFormalRuleEvenWithQueryMenu() throws Exception {
        var hidden=project("DISCOVERY-PRIVATE",7L);
        discoveryMvc().perform(get("/dcc/project-codes/{id}",hidden.getId()))
                .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
    @Test void discoveryControlledFileQueryMenuReadsProjectAndEmptyDirectoriesWithoutConfigMenu() throws Exception {
        var allowed=project("DISCOVERY-FILE-MENU",7L);view(allowed.getId());
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:query")).thenReturn(false);
        discoveryMvc().perform(get("/dcc/project-codes/page").param("pageNo","1").param("pageSize","10"))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.total").value(1));
        discoveryMvc().perform(get("/dcc/project-codes/{id}",allowed.getId())).andExpect(jsonPath("$.code").value(0));
        discoveryMvc().perform(get("/dcc/project-codes/{id}/folders",allowed.getId()))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.length()").value(0));
    }

    @ParameterizedTest @ValueSource(strings={"VIEW","EDIT","OWNER"})
    void discoveryFormalLevelsFindEmptyProjectsAndIndependentChildDirectories(String level) throws Exception {
        var allowed=project("DISCOVERY-LEVEL-"+level,7L);view(allowed.getId());
        jdbc.update("UPDATE dcc_project_access_rule SET access_level=? WHERE dcc_project_code_id=? AND subject_id=8",level,allowed.getId());
        var parent=folder(allowed.getId(),"empty-parent");var child=folder(allowed.getId(),"empty-child");
        child.setParentId(parent.getId());folders.updateById(child);
        discoveryMvc().perform(get("/dcc/project-codes/page").param("keyword","DISCOVERY-LEVEL"))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(String.valueOf(allowed.getId())));
        discoveryMvc().perform(get("/dcc/project-codes/{id}/folders",allowed.getId()))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].parentId").value(String.valueOf(parent.getId())));
        assertTrue(files.selectList().isEmpty());
    }

    @ParameterizedTest @ValueSource(strings={"NO_RULE","FOREIGN_RULE","EXPIRED_RULE","FUTURE_RULE","INACTIVE_RULE",
            "FOREIGN_PROJECT","DISABLED_PROJECT","DELETED_PROJECT","CALLER_TENANT","ADMIN_NO_RULE"})
    void discoveryScopeCannotLeakUnauthorizedProjectsOrDetailsAndIdChangedDirectory(String violation) throws Exception {
        var project=project("DISCOVERY-DENIED",7L);view(project.getId());folder(project.getId(),"private-empty");
        var rule=rules.selectActiveRules(project.getId(),java.time.LocalDateTime.now()).stream()
                .filter(r->Long.valueOf(8).equals(r.getSubjectId())).findFirst().orElseThrow();
        switch(violation) {
            case "NO_RULE" -> rules.deleteById(rule.getId());
            case "FOREIGN_RULE" -> jdbc.update("UPDATE dcc_project_access_rule SET tenant_id=2 WHERE id=?",rule.getId());
            case "EXPIRED_RULE" -> {rule.setExpireTime(java.time.LocalDateTime.now().minusDays(1));rules.updateById(rule);}
            case "FUTURE_RULE" -> {rule.setValidFrom(java.time.LocalDateTime.now().plusDays(1));rules.updateById(rule);}
            case "INACTIVE_RULE" -> {rule.setActive(false);rules.updateById(rule);}
            case "FOREIGN_PROJECT" -> jdbc.update("UPDATE dcc_project_code SET tenant_id=2 WHERE id=?",project.getId());
            case "DISABLED_PROJECT" -> {project.setStatus("DISABLE");projects.updateById(project);}
            case "DELETED_PROJECT" -> projects.deleteById(project.getId());
            case "CALLER_TENANT" -> TenantContextHolder.setTenantId(2L);
            case "ADMIN_NO_RULE" -> {
                when(permissions.hasAnyPermissions(1L,"dcc:project-code:query","dcc:controlled-file:query")).thenReturn(true);
                when(permissions.hasAnyPermissions(1L,"dcc:project-code:scope:all")).thenReturn(true);login(1L);
            }
            default -> throw new IllegalArgumentException(violation);
        }
        try {
            discoveryMvc().perform(get("/dcc/project-codes/page"))
                    .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.total").value(0));
            discoveryMvc().perform(get("/dcc/project-codes/{id}",project.getId()))
                    .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()))
                    .andExpect(jsonPath("$.data").doesNotExist());
            discoveryMvc().perform(get("/dcc/project-codes/{id}/folders",project.getId()))
                    .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()));
        } finally {TenantContextHolder.setTenantId(1L);}
    }

    @ParameterizedTest @ValueSource(strings={"DISABLED","MISSING"})
    void discoveryDisabledOrMissingAccountCannotDiscoverEvenWithValidScope(String kind) throws Exception {
        var project=project("DISCOVERY-INACTIVE-ACCOUNT",7L);view(project.getId());
        when(users.getUser(8L)).thenReturn("MISSING".equals(kind)?null:new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(8L).setStatus(1));
        for(String url:new String[]{"/dcc/project-codes/page","/dcc/project-codes/"+project.getId(),"/dcc/project-codes/"+project.getId()+"/folders"})
            discoveryMvc().perform(get(url)).andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()));
    }

    @Test void discoveryMenuIsRequiredSeparatelyFromProjectRule() throws Exception {
        var project=project("DISCOVERY-NO-MENU",7L);view(project.getId());
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:query")).thenReturn(false);
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:query","dcc:controlled-file:query")).thenReturn(false);
        discoveryMvc().perform(get("/dcc/project-codes/page")).andExpect(jsonPath("$.code").value(403));
        discoveryMvc().perform(get("/dcc/project-codes/{id}",project.getId())).andExpect(jsonPath("$.code").value(403));
    }

    void assign(Long projectId,String status,long tenant,Long actor,java.time.LocalDateTime expires) {
        var assignment=DccProjectCodeAssignmentDO.builder().assignmentNo("DISCOVERY-"+java.util.UUID.randomUUID())
                .projectCodeId(projectId).scopeMode("PROJECT").assigneeUserId(actor).assignedBy(7L)
                .assignedTime(java.time.LocalDateTime.now()).expireTime(expires).status(status).fileCount(0)
                .changedFileCount(0).changedFieldCount(0).assignmentReason("formal hard scope test").build();
        assignment.setTenantId(tenant);assertEquals(1,assignments.insert(assignment));
    }
    @ParameterizedTest @ValueSource(strings={"NONE","REVOKED","EXPIRED","FOREIGN_TENANT","OTHER_ASSIGNEE"})
    void discoveryAssignmentHardScopeCannotBeBypassedThroughProjectOrFolderId(String kind) throws Exception {
        var project=project("DISCOVERY-HARD-SCOPE",7L);view(project.getId());folder(project.getId(),"empty-hard-scope");
        when(permissions.hasAnyPermissions(8L,"dcc:project-code-assignment:execute")).thenReturn(true);
        if(!"NONE".equals(kind)) assign(project.getId(),"REVOKED".equals(kind)?"REVOKED":"ACTIVE",
                "FOREIGN_TENANT".equals(kind)?2L:1L,"OTHER_ASSIGNEE".equals(kind)?9L:8L,
                java.time.LocalDateTime.now().plusDays("EXPIRED".equals(kind)?-1:1));
        discoveryMvc().perform(get("/dcc/project-codes/page")).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(0));
        for(String suffix:new String[]{"","/folders"}) discoveryMvc().perform(get("/dcc/project-codes/"+project.getId()+suffix))
                .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()));
    }
    @Test void discoveryAssignedScopeIsIntersectedWithFormalRulesAndExplicitFullOnlyRemovesAssignmentRestriction() throws Exception {
        var allowed=project("DISCOVERY-ASSIGNED",7L);view(allowed.getId());assign(allowed.getId(),"ACTIVE",1L,8L,java.time.LocalDateTime.now().plusDays(1));
        var notAssigned=project("DISCOVERY-VIEW-ONLY",7L);view(notAssigned.getId());
        var noRule=project("DISCOVERY-ASSIGNED-NO-RULE",7L);assign(noRule.getId(),"ACTIVE",1L,8L,java.time.LocalDateTime.now().plusDays(1));
        when(permissions.hasAnyPermissions(8L,"dcc:project-code-assignment:execute")).thenReturn(true);
        discoveryMvc().perform(get("/dcc/project-codes/page")).andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(String.valueOf(allowed.getId())));
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:scope:all")).thenReturn(true);
        discoveryMvc().perform(get("/dcc/project-codes/page")).andExpect(jsonPath("$.data.total").value(2));
        discoveryMvc().perform(get("/dcc/project-codes/{id}",noRule.getId()))
                .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()));
    }

    @Test void discoveryServerPaginationFiltersAndFileCountSortingKeepAuthorizedTotalAndEmptyProjects() throws Exception {
        var empty=project("DISCOVERY-EMPTY",7L);empty.setDocControlNo("1");empty.setCategory("wanted");projects.updateById(empty);view(empty.getId());
        var one=project("DISCOVERY-ONE",7L);one.setDocControlNo("2");one.setCategory("wanted");projects.updateById(one);view(one.getId());
        var two=project("DISCOVERY-TWO",7L);two.setDocControlNo("3");two.setCategory("wanted");projects.updateById(two);view(two.getId());
        file(one.getId(),101L,"one-file.pdf");file(two.getId(),101L,"two-one.pdf");file(two.getId(),101L,"two-two.pdf");
        var hidden=project("DISCOVERY-HIDDEN-SORT",7L);hidden.setDocControlNo("0");hidden.setCategory("wanted");projects.updateById(hidden);
        discoveryMvc().perform(get("/dcc/project-codes/page").param("category","wanted").param("pageNo","2").param("pageSize","1"))
                .andExpect(jsonPath("$.data.total").value(3)).andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(String.valueOf(one.getId())));
        discoveryMvc().perform(get("/dcc/project-codes/page").param("fileCountSort","desc").param("pageSize","1"))
                .andExpect(jsonPath("$.data.total").value(3)).andExpect(jsonPath("$.data.list[0].id").value(String.valueOf(two.getId())))
                .andExpect(jsonPath("$.data.list[0].associatedFileCount").value(2));
        discoveryMvc().perform(get("/dcc/project-codes/page").param("fileCountSort","asc").param("pageSize","1"))
                .andExpect(jsonPath("$.data.total").value(3)).andExpect(jsonPath("$.data.list[0].id").value(String.valueOf(empty.getId())));
        discoveryMvc().perform(get("/dcc/project-codes/page").param("keyword","DISCOVERY-EMPTY"))
                .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.list[0].id").value(String.valueOf(empty.getId())));
        discoveryMvc().perform(get("/dcc/project-codes/page").param("pageNo","4").param("pageSize","1"))
                .andExpect(jsonPath("$.data.total").value(3)).andExpect(jsonPath("$.data.list.length()").value(0));
    }

    @Test void discoveryProductConfigurationFiltersAreStillAppliedBeforeAuthorizedPagination() throws Exception {
        var matching=project("DISCOVERY-FILTER-MATCH",7L);matching.setProductMasterId(20L);projects.updateById(matching);view(matching.getId());
        var invalidProduct=project("DISCOVERY-FILTER-PRODUCT",7L);invalidProduct.setProductMasterId(21L);projects.updateById(invalidProduct);view(invalidProduct.getId());
        var noRoute=project("DISCOVERY-FILTER-ROUTE",7L);noRoute.setProductMasterId(20L);projects.updateById(noRoute);view(noRoute.getId());
        when(products.listSimpleProducts(cn.iocoder.yudao.module.mdm.enums.MdmProductStatusConstants.ENABLE,true,null))
                .thenReturn(List.of(cn.iocoder.yudao.module.mdm.api.product.dto.MdmProductRespDTO.builder().id(20L).build()));
        when(configuration.getStatus(any())).thenReturn(Map.of(
                matching.getId(),new cn.iocoder.yudao.module.dcc.api.projectcode.DccProjectCodeConfigurationStatus(matching.getId(),true,true,true),
                noRoute.getId(),new cn.iocoder.yudao.module.dcc.api.projectcode.DccProjectCodeConfigurationStatus(noRoute.getId(),false,true,true)));
        discoveryMvc().perform(get("/dcc/project-codes/page").param("requireDccProductCode","true").param("routeConfigured","true")
                        .param("mainBatchRecordConfigured","true").param("qaRegulationConfigured","true"))
                .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.list[0].id").value(String.valueOf(matching.getId())));
    }

    @Test void discoveryFormalLeaderLongIdentityIsIndependentOfDisplayNameAndBinaryAccess() throws Exception {
        when(users.getUser(9223372036854775000L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO()
                .setId(9223372036854775000L).setStatus(0).setNickname("same name"));
        when(users.getUser(9223372036854775001L)).thenReturn(new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO()
                .setId(9223372036854775001L).setStatus(0).setNickname("same name"));
        var first=project("DISCOVERY-LEADER-ONE",9223372036854775000L);first.setProjectLeader("same name");projects.updateById(first);view(first.getId());
        var second=project("DISCOVERY-LEADER-TWO",9223372036854775001L);second.setProjectLeader("same name");projects.updateById(second);view(second.getId());
        assertTrue(first.getId()>9007199254740991L);
        discoveryMvc().perform(get("/dcc/project-codes/{id}",first.getId()))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.id").value(String.valueOf(first.getId())))
                .andExpect(jsonPath("$.data.projectLeaderUserId").value("9223372036854775000"));
        discoveryMvc().perform(get("/dcc/project-codes/{id}",second.getId()))
                .andExpect(jsonPath("$.data.projectLeaderUserId").value("9223372036854775001"));
        verifyNoInteractions(fileQuery,sourceAccess);
        var noLeader=project("DISCOVERY-NO-LEADER",7L);jdbc.update("UPDATE dcc_project_code SET project_leader_user_id=NULL,project_leader='same name' WHERE id=?",noLeader.getId());view(noLeader.getId());
        discoveryMvc().perform(get("/dcc/project-codes/{id}",noLeader.getId())).andExpect(jsonPath("$.data.projectLeaderUserId").doesNotExist());
    }

    @Test void discoveryRepeatedReadsDoNotWriteBusinessOrGrantConfiguration() throws Exception {
        var project=project("DISCOVERY-READ-ONLY",7L);view(project.getId());folder(project.getId(),"empty-read-only");
        String before=discoveryBusinessState();
        writeCounter.writes.set(0);
        for(int i=0;i<2;i++) {
            discoveryMvc().perform(get("/dcc/project-codes/page")).andExpect(jsonPath("$.code").value(0));
            discoveryMvc().perform(get("/dcc/project-codes/{id}",project.getId())).andExpect(jsonPath("$.code").value(0));
            discoveryMvc().perform(get("/dcc/project-codes/{id}/folders",project.getId())).andExpect(jsonPath("$.code").value(0));
        }
        assertEquals(before,discoveryBusinessState());assertEquals(0,writeCounter.writes.get());verifyNoInteractions(fileQuery);
    }
    String discoveryBusinessState() {
        var state=new java.util.LinkedHashMap<String,Object>();
        for(String table:new String[]{"dcc_project_code","dcc_project_access_rule","dcc_project_folder","dcc_project_code_assignment",
                "dcc_controlled_file","dcc_controlled_file_master","dcc_project_application_attributes","dcc_application_round_link","gxp_audit_event","gxp_audit_ledger_sequence"})
            state.put(table,jdbc.queryForList("SELECT * FROM "+table));
        return state.toString();
    }
}
