package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.file.DccApplicationRoundService;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessServiceImpl;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.*;
import cn.iocoder.yudao.module.system.api.dept.*;
import cn.iocoder.yudao.module.system.api.permission.*;
import cn.iocoder.yudao.module.system.api.user.*;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({DccProjectAttributesService.class,DccProjectApplicationSnapshotService.class,DccApplicationRoundService.class,
        DccProjectFilePlacementService.class,DccProjectLeaderService.class,DccFolderTemplateService.class,
        DccProjectConfigurationAuditService.class,DccProjectAccessServiceImpl.class,
        GxpAuditServiceImpl.class,DccProjectFormalCombinationTest.JdbcFixture.class,
        cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectApplicationSnapshotController.class,
        cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectAttributesController.class,
        DccProjectFolderMaintenanceService.class,
        cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectFolderMaintenanceController.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts={"/sql/dcc_b_combination_tables.sql","/sql/dcc_b_gxp_audit_tables.sql"},executionPhase=Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts={"/sql/dcc_b_combination_clean.sql","/sql/dcc_b_gxp_audit_clean.sql"},executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccProjectFormalCombinationTest extends BaseDbUnitTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    static class JdbcFixture {
        @org.springframework.context.annotation.Bean
        JdbcTemplate jdbcTemplate(javax.sql.DataSource dataSource) { return new JdbcTemplate(dataSource); }
        @org.springframework.context.annotation.Bean("ss")
        cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService ss(PermissionApi permissions) {
            return new cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkServiceImpl(permissions);
        }
    }
    @Resource DccProjectApplicationSnapshotService snapshots;
    @Resource DccProjectAttributesService attributes;
    @Resource DccApplicationRoundService rounds;
    @Resource DccProjectFilePlacementService placements;
    @Resource DccProjectCodeMapper projects;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean DccProjectFolderMapper folders;
    @Resource DccProjectFolderMaintenanceService folderMaintenance;
    @Resource cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectFolderMaintenanceController folderMaintenanceController;
    @Resource DccProjectAccessRuleMapper rules;
    @Resource DccControlledFileMapper files;
    @Resource DccControlledFileMasterMapper masters;
    @Resource GxpAuditPolicyOperationMapper policies;
    @Resource GxpAuditEventMapper events;
    @Resource JdbcTemplate jdbc;
    @Resource PlatformTransactionManager transactions;
    @Resource cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectApplicationSnapshotController snapshotController;
    @Resource cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectAttributesController projectController;
    @MockitoBean AdminUserApi users;
    @MockitoBean PermissionApi permissions;
    @MockitoBean DeptApi departments;
    @MockitoBean PostApi posts;
    @MockitoBean RoleApi roles;
    @MockitoBean cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi apiErrorLogs;
    // Source-file visibility is D/Query's distinct boundary; target-project authority/resolver/audit are real.
    @MockitoBean DccRelationAccessPolicy sourceAccess;

    @BeforeEach void accounts() {
        for (long id : new long[]{1,7,8,9}) {
            when(users.getUser(id)).thenReturn(new AdminUserRespDTO().setId(id).setStatus(0).setNickname("账号"+id));
            when(permissions.getUserRoleIdListByUserId(id)).thenReturn(Set.of());
        }
        when(permissions.hasAnyPermissions(7L,"dcc:project-code:query","dcc:controlled-file:submit","dcc:controlled-file:query")).thenReturn(true);
        when(permissions.hasAnyPermissions(7L,"dcc:project-code:update")).thenReturn(true);
        login(7L);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    void login(Long id) {
        var login=new LoginUser();login.setId(id);login.setTenantId(1L);login.setUserType(2);
        login.setInfo(Map.of("username","b-combination-"+id,LoginUser.INFO_KEY_NICKNAME,"正式账号"+id));
        SecurityFrameworkUtils.setLoginUser(login,new MockHttpServletRequest());
    }
    DccProjectAttributes value(String market) {return new DccProjectAttributes(List.of(market),null,"Y","N","N",null);}
    DccProjectCodeDO project(String code,Long leader) {
        var row=DccProjectCodeDO.builder().projectName(code).projectCode(code).projectLeaderUserId(leader)
                .defaultAttributesJson(attributes.encode(value("NMPA"))).status("ENABLE").build();
        row.setTenantId(1L);projects.insert(row);
        var rule=new DccProjectAccessRuleDO();rule.setTenantId(1L);rule.setDccProjectCodeId(row.getId());
        rule.setSubjectType("USER");rule.setSubjectId(leader);rule.setAccessLevel("EDIT");rule.setActive(true);
        rule.setChangeReason("真实组合隔离测试");rules.insert(rule);return row;
    }
    DccProjectFolderDO folder(Long project,String key) {
        var row=new DccProjectFolderDO();row.setTenantId(1L);row.setProjectCodeId(project);row.setParentId(0L);
        row.setName(key);row.setSortOrder(0);row.setActive(true);row.setSourceTemplateId(1L);row.setSourceNodeKey(key);
        folders.insert(row);return row;
    }
    DccControlledFileDO file(Long project,long directory,String name) {
        var master=DccControlledFileMasterDO.builder().categoryId(1L).directoryId(directory).tenantId(1L)
                .dccProjectCodeId(project).fileName(name).fileNumber(name).status("ACTIVE").build();masters.insert(master);
        var row=DccControlledFileDO.builder().masterId(master.getId()).tenantId(1L).categoryId(1L).directoryId(directory)
                .dccProjectCodeId(project).sourceFileId(1L).originalFileId(1L).fileName(name).title(name).fileNumber(name)
                .versionNo("A/1").status("ACTIVE").submitterId(7L).requesterId(7L)
                .controlledTime(LocalDateTime.of(2026,10,1,8,0)).activatedTime(LocalDateTime.of(2026,10,1,8,0)).build();
        files.insert(row);master.setLatestControlledFileId(row.getId());master.setCurrentActiveControlledFileId(row.getId());
        masters.updateById(master);return row;
    }
    void directory(long id,long tenant,boolean active) {
        jdbc.update("INSERT INTO dcc_file_directory(id,tenant_id,code,name,active,sort) VALUES(?,?,?,?,?,0)",
                id,tenant,"STORAGE-"+id,"原NAS目录"+id,active?1:0);
    }
    void policy(String operation) {
        var p=new GxpAuditPolicyOperationDO();p.setTenantId(1L);p.setOperationId(operation);p.setPolicyVersion("B-I05-TEST");
        p.setSourceType("SERVICE_METHOD");p.setSourceLocator("ISOLATED_TEST");p.setDomain("DCC");p.setSubjectType("REFERENCE");
        p.setActionType("UPDATE");p.setReasonPolicy("REQUIRED");p.setSignaturePolicy("NOT_REQUIRED");p.setStatePolicy("BEFORE_AFTER");
        p.setRetentionClass("ISOLATED_TEST");p.setTestIds("B-I05");p.setOwner("B-test");p.setApplicability("GXP");p.setActive(true);
        policies.insert(p);
    }
    <T> T tx(java.util.function.Supplier<T> operation) {return new TransactionTemplate(transactions).execute(status->operation.get());}

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"UPLOAD","REVISION","OBSOLETE"})
    void formalThreeActionSubmissionFreezesIndependentSourceAndActualWithoutWritingOriginalFile(String action) {
        var project=project("ROUND-"+action,7L);var file=file(project.getId(),101L,action+".pdf");
        String before=JsonUtils.toJsonString(files.selectById(file.getId()));
        var saved=tx(()->snapshots.submit(7L,project.getId(),action,file.getId(),"opaque-bpm-"+action,value("CE")));
        assertEquals(1,saved.getApplicationRound());assertTrue(saved.getSubmitted());
        assertEquals(value("NMPA"),attributes.readValue(saved.getDefaultSourceJson()));
        assertEquals(value("CE"),attributes.readValue(saved.getActualAttributesJson()));
        project.setDefaultAttributesJson(attributes.encode(value("FDA")));projects.updateById(project);
        assertEquals(saved.getId(),tx(()->snapshots.submit(7L,project.getId(),action,file.getId(),"opaque-bpm-"+action,value("CE"))).getId());
        assertThrows(RuntimeException.class,()->tx(()->snapshots.submit(7L,project.getId(),action,file.getId(),"opaque-bpm-"+action,value("FDA"))));
        assertEquals(value("NMPA"),attributes.readValue(snapshots.read(7L,project.getId(),action,file.getId(),"opaque-bpm-"+action).getDefaultSourceJson()));
        assertEquals(before,JsonUtils.toJsonString(files.selectById(file.getId())));
    }
    @Test void applicationFailureRollsBackRoundAndSnapshotAndUnknownRoundIsNeverGuessed() {
        var project=project("ROLLBACK",7L);var file=file(project.getId(),101L,"rollback.pdf");
        assertThrows(RuntimeException.class,()->tx(()->{snapshots.submit(7L,project.getId(),"UPLOAD",file.getId(),"failed-round",value("CE"));throw new IllegalStateException("BPM提交失败");}));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes",Integer.class));
        assertThrows(RuntimeException.class,()->snapshots.read(7L,project.getId(),"UPLOAD",file.getId(),"unknown"));
        assertThrows(RuntimeException.class,()->snapshots.submit(7L,project.getId(),"UPLOAD",file.getId(),"outside-tx",value("CE")));
    }
    @Test void reworkUsesMappedNextRoundAndPreservesOriginalSourceAfterProjectDefaultsChange() {
        var project=project("REWORK",7L);var file=file(project.getId(),101L,"rework.pdf");
        tx(()->snapshots.submit(7L,project.getId(),"REVISION",file.getId(),"original-bpm",value("CE")));
        project.setDefaultAttributesJson(attributes.encode(value("FDA")));projects.updateById(project);
        var next=tx(()->snapshots.rework(7L,project.getId(),"REVISION",file.getId(),"original-bpm","rework-bpm"));
        assertEquals(2,next.getApplicationRound());assertFalse(next.getSubmitted());
        assertEquals(value("NMPA"),attributes.readValue(next.getDefaultSourceJson()));
        assertEquals(value("CE"),attributes.readValue(next.getActualAttributesJson()));
        assertEquals(2,rounds.require("REVISION",file.getId(),"rework-bpm"));
    }
    @Test void savedFormalBpmDraftKeepsInitialSourceEvenIfProjectChangesBeforeSubmit() {
        var project=project("DRAFT-SOURCE",7L);var file=file(project.getId(),101L,"draft-source.pdf");
        var initial=tx(()->snapshots.begin(7L,project.getId(),"UPLOAD",file.getId(),"draft-process"));
        project.setDefaultAttributesJson(attributes.encode(value("FDA")));projects.updateById(project);
        var frozen=tx(()->snapshots.submit(7L,project.getId(),"UPLOAD",file.getId(),"draft-process",value("CE")));
        assertEquals(initial.getId(),frozen.getId());
        assertEquals(value("NMPA"),attributes.readValue(frozen.getDefaultSourceJson()));
        assertEquals(value("CE"),attributes.readValue(frozen.getActualAttributesJson()));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"UPLOAD","REVISION","OBSOLETE"})
    void formalBpmDraftSavesManualActualAndRestoresItWithoutChangingSourceOrOtherActions(String action) {
        var project=project("DRAFT-"+action,7L);var file=file(project.getId(),101L,"draft-"+action+".pdf");
        String bpm="draft-bpm-"+action;
        String otherAction="UPLOAD".equals(action)?"OBSOLETE":"UPLOAD";
        String fileBefore=JsonUtils.toJsonString(files.selectById(file.getId()));
        var initial=tx(()->snapshots.begin(7L,project.getId(),action,file.getId(),bpm));
        var other=tx(()->snapshots.submit(7L,project.getId(),otherAction,file.getId(),"other-bpm",value("NMPA")));
        project.setDefaultAttributesJson(attributes.encode(value("FDA")));projects.updateById(project);
        var saved=tx(()->snapshots.saveDraft(7L,project.getId(),action,file.getId(),bpm,value("CE")));
        assertEquals(initial.getId(),saved.getId());assertFalse(saved.getSubmitted());
        var reopened=snapshots.read(7L,project.getId(),action,file.getId(),bpm);
        assertEquals(value("NMPA"),attributes.readValue(reopened.getDefaultSourceJson()));
        assertEquals(value("CE"),attributes.readValue(reopened.getActualAttributesJson()));
        assertThrows(RuntimeException.class,()->tx(()->{
            snapshots.saveDraft(7L,project.getId(),action,file.getId(),bpm,value("FDA"));
            throw new IllegalStateException("草稿业务事务失败");
        }));
        assertEquals(value("CE"),attributes.readValue(snapshots.read(7L,project.getId(),action,file.getId(),bpm).getActualAttributesJson()));
        assertEquals(other.getActualAttributesJson(),snapshots.read(7L,project.getId(),otherAction,file.getId(),"other-bpm").getActualAttributesJson());
        assertEquals(fileBefore,JsonUtils.toJsonString(files.selectById(file.getId())));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"UPLOAD","REVISION","OBSOLETE"})
    void formalBpmRestoreOnlyUsesConfirmedCurrentDefaultsAndCannotOverwriteFrozenApplication(String action) {
        var project=project("RESTORE-"+action,7L);var file=file(project.getId(),101L,"restore-"+action+".pdf");
        String bpm="restore-bpm-"+action;
        String fileBefore=JsonUtils.toJsonString(files.selectById(file.getId()));
        var initial=tx(()->snapshots.begin(7L,project.getId(),action,file.getId(),bpm));
        tx(()->attributes.saveDraft(7L,project.getId(),action,file.getId(),initial.getApplicationRound(),value("CE")));
        project.setDefaultAttributesJson(attributes.encode(value("FDA")));projects.updateById(project);
        var restored=tx(()->snapshots.restoreDraftDefaults(7L,project.getId(),action,file.getId(),bpm,value("FDA")));
        assertEquals(initial.getId(),restored.getId());assertFalse(restored.getSubmitted());
        assertEquals(value("FDA"),attributes.readValue(restored.getDefaultSourceJson()));
        assertEquals(value("FDA"),attributes.readValue(restored.getActualAttributesJson()));
        project.setDefaultAttributesJson(attributes.encode(value("MADSAP")));projects.updateById(project);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->tx(()->
                snapshots.restoreDraftDefaults(7L,project.getId(),action,file.getId(),bpm,value("FDA"))));
        assertEquals(value("FDA"),attributes.readValue(snapshots.read(7L,project.getId(),action,file.getId(),bpm).getActualAttributesJson()));
        tx(()->snapshots.submit(7L,project.getId(),action,file.getId(),bpm,value("CE")));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->tx(()->
                snapshots.restoreDraftDefaults(7L,project.getId(),action,file.getId(),bpm,value("MADSAP"))));
        assertEquals(value("CE"),attributes.readValue(snapshots.read(7L,project.getId(),action,file.getId(),bpm).getActualAttributesJson()));
        assertEquals(fileBefore,JsonUtils.toJsonString(files.selectById(file.getId())));
    }
    @Test void savedApplicationHttpReadsMappedRoundAndBothSnapshotsAsFormalStringIds() throws Exception {
        var project=project("HTTP-ROUND",7L);var file=file(project.getId(),101L,"http-round.pdf");
        tx(()->snapshots.submit(7L,project.getId(),"OBSOLETE",file.getId(),"opaque-obsolete-process",value("CE")));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(snapshotController).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/dcc/project-codes/{projectId}/applications/{fileId}/attributes",project.getId(),file.getId())
                .param("applicationType","OBSOLETE").param("bpmRound","opaque-obsolete-process"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.applicationId").value(String.valueOf(file.getId())))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.applicationRound").value(1))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.defaultSourceJson").value(attributes.encode(value("NMPA"))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.actualAttributesJson").value(attributes.encode(value("CE"))));
    }
    @Test void formalDraftDoesNotCreateMissingSourceOrBorrowAnotherProjectUserOrTenant() {
        var project=project("DRAFT-GUARDS",7L);var file=file(project.getId(),101L,"draft-guards.pdf");
        assertThrows(RuntimeException.class,()->tx(()->snapshots.saveDraft(7L,project.getId(),"UPLOAD",file.getId(),"missing-bpm",value("CE"))));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link",Integer.class));
        tx(()->rounds.bind(project.getId(),"UPLOAD",file.getId(),"mapped-without-source"));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->tx(()->
                snapshots.saveDraft(7L,project.getId(),"UPLOAD",file.getId(),"mapped-without-source",value("CE"))));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->tx(()->
                snapshots.restoreDraftDefaults(7L,project.getId(),"UPLOAD",file.getId(),"mapped-without-source",value("NMPA"))));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_application_attributes",Integer.class));
        tx(()->snapshots.begin(7L,project.getId(),"UPLOAD",file.getId(),"mapped-without-source"));
        var other=project("OTHER-DRAFT-PROJECT",7L);
        String savedBefore=JsonUtils.toJsonString(snapshots.read(7L,project.getId(),"UPLOAD",file.getId(),"mapped-without-source"));
        assertThrows(RuntimeException.class,()->tx(()->snapshots.saveDraft(7L,other.getId(),"UPLOAD",file.getId(),"mapped-without-source",value("CE"))));
        assertThrows(RuntimeException.class,()->tx(()->snapshots.saveDraft(8L,project.getId(),"UPLOAD",file.getId(),"mapped-without-source",value("CE"))));
        assertThrows(RuntimeException.class,()->tx(()->snapshots.saveDraft(7L,project.getId(),"UPLOAD",file.getId(),"mapped-without-source",
                new DccProjectAttributes(List.of("NA","CE"),null,"Y","N","N",null))));
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,()->
                snapshots.saveDraft(7L,project.getId(),"UPLOAD",file.getId(),"mapped-without-source",value("CE")));
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,()->
                snapshots.restoreDraftDefaults(7L,project.getId(),"UPLOAD",file.getId(),"mapped-without-source",value("NMPA")));
        TenantContextHolder.setTenantId(2L);
        try {
            assertThrows(RuntimeException.class,()->tx(()->snapshots.saveDraft(7L,project.getId(),"UPLOAD",file.getId(),"mapped-without-source",value("CE"))));
        } finally { TenantContextHolder.setTenantId(1L); }
        assertEquals(savedBefore,JsonUtils.toJsonString(snapshots.read(7L,project.getId(),"UPLOAD",file.getId(),"mapped-without-source")));
    }
    @Test void confirmedDefaultRestoreRollsBackWithParentTransactionAndFrozenDraftRejectsFurtherSave() {
        var project=project("RESTORE-ROLLBACK",7L);var file=file(project.getId(),101L,"restore-rollback.pdf");
        tx(()->snapshots.begin(7L,project.getId(),"OBSOLETE",file.getId(),"restore-rollback-bpm"));
        tx(()->snapshots.saveDraft(7L,project.getId(),"OBSOLETE",file.getId(),"restore-rollback-bpm",value("CE")));
        project.setDefaultAttributesJson(attributes.encode(value("FDA")));projects.updateById(project);
        assertThrows(RuntimeException.class,()->tx(()->{
            snapshots.restoreDraftDefaults(7L,project.getId(),"OBSOLETE",file.getId(),"restore-rollback-bpm",value("FDA"));
            throw new IllegalStateException("恢复业务事务失败");
        }));
        var saved=snapshots.read(7L,project.getId(),"OBSOLETE",file.getId(),"restore-rollback-bpm");
        assertEquals(value("NMPA"),attributes.readValue(saved.getDefaultSourceJson()));
        assertEquals(value("CE"),attributes.readValue(saved.getActualAttributesJson()));
        tx(()->snapshots.submit(7L,project.getId(),"OBSOLETE",file.getId(),"restore-rollback-bpm",value("CE")));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->tx(()->
                snapshots.saveDraft(7L,project.getId(),"OBSOLETE",file.getId(),"restore-rollback-bpm",value("FDA"))));
        assertEquals(value("CE"),attributes.readValue(snapshots.read(7L,project.getId(),"OBSOLETE",file.getId(),"restore-rollback-bpm").getActualAttributesJson()));
    }
    @Test void physicalDirectoryAndProjectFolderCoincidenceNeverCreatesImplicitPlacement() {
        policy("dcc.project-file-placement.bind");
        var project=project("LOCATION",7L);var folder=folder(project.getId(),"F");
        directory(folder.getId(),1L,true);directory(1001L,1L,true);
        var file=file(project.getId(),1001L,"location.pdf");
        assertThrows(RuntimeException.class,()->placements.require(7L,project.getId(),file.getId()));
        assertThrows(RuntimeException.class,()->tx(()->placements.bind(7L,project.getId(),folder.getId(),file.getId(),folder.getId(),"确认本文件项目目录")));
        var saved=tx(()->placements.bind(7L,project.getId(),folder.getId(),file.getId(),1001L,"确认本文件项目目录"));
        assertEquals(folder.getId(),saved.getProjectFolderId());assertEquals(1001L,saved.getStorageDirectoryId());
        assertEquals(saved.getId(),tx(()->placements.bind(7L,project.getId(),folder.getId(),file.getId(),1001L,"确认本文件项目目录")).getId());
        var other=folder(project.getId(),"OTHER");
        assertThrows(RuntimeException.class,()->tx(()->placements.bind(7L,project.getId(),other.getId(),file.getId(),1001L,"确认本文件项目目录")));
        assertEquals(1001L,files.selectById(file.getId()).getDirectoryId());
    }
    @Test void placementRejectsWrongProjectTenantInactiveStorageAndRollsBackWithFileApplication() {
        policy("dcc.project-file-placement.bind");
        var project=project("LOCATION-REJECT",7L);var folder=folder(project.getId(),"F");
        directory(101L,2L,true);var file=file(project.getId(),101L,"invalid-location.pdf");
        assertThrows(RuntimeException.class,()->tx(()->placements.bind(7L,project.getId(),folder.getId(),file.getId(),101L,"确认本文件项目目录")));
        jdbc.update("UPDATE dcc_file_directory SET tenant_id=1,active=0 WHERE id=101");
        assertThrows(RuntimeException.class,()->tx(()->placements.bind(7L,project.getId(),folder.getId(),file.getId(),101L,"确认本文件项目目录")));
        jdbc.update("UPDATE dcc_file_directory SET active=1 WHERE id=101");
        assertThrows(RuntimeException.class,()->tx(()->{placements.bind(7L,project.getId(),folder.getId(),file.getId(),101L,"确认本文件项目目录");throw new IllegalStateException("申请失败");}));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_placement",Integer.class));
    }
    @Test void missingPlacementAuditPolicyCannotLeaveLocationFactOrTouchFileHistory() {
        var project=project("NO-POLICY",7L);var folder=folder(project.getId(),"F");
        directory(101L,1L,true);var file=file(project.getId(),101L,"no-policy.pdf");
        String before=JsonUtils.toJsonString(files.selectById(file.getId()));
        assertThrows(RuntimeException.class,()->tx(()->placements.bind(7L,project.getId(),folder.getId(),file.getId(),101L,"真实选择目录")));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_placement",Integer.class));
        assertEquals(0,events.selectList().size());
        assertEquals(before,JsonUtils.toJsonString(files.selectById(file.getId())));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"UPLOAD","REVISION","OBSOLETE"})
    void snapshotHttpAppliesRealMethodPermissionAndProjectScopeBeforeReturningSavedValues(String action) throws Exception {
        var project=project("HTTP-AUTH-"+action,7L);var file=file(project.getId(),101L,"http-auth-"+action+".pdf");
        tx(()->snapshots.submit(7L,project.getId(),action,file.getId(),"http-auth-bpm",value("CE")));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(snapshotController)
                .setControllerAdvice(new cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler("dcc-b-combination",apiErrorLogs)).build();
        when(permissions.hasAnyPermissions(7L,"dcc:project-code:query","dcc:controlled-file:submit","dcc:controlled-file:query")).thenReturn(false);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/dcc/project-codes/{projectId}/applications/{fileId}/attributes",project.getId(),file.getId())
                .param("applicationType",action).param("bpmRound","http-auth-bpm"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(403))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data").doesNotExist());
        when(permissions.hasAnyPermissions(7L,"dcc:project-code:query","dcc:controlled-file:submit","dcc:controlled-file:query")).thenReturn(true);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/dcc/project-codes/{projectId}/applications/{fileId}/attributes",project.getId(),file.getId())
                .param("applicationType",action).param("bpmRound","http-auth-bpm"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.applicationId").value(String.valueOf(file.getId())))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.actualAttributesJson").value(attributes.encode(value("CE"))));
        login(8L);
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:query","dcc:controlled-file:submit","dcc:controlled-file:query")).thenReturn(true);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/dcc/project-codes/{projectId}/applications/{fileId}/attributes",project.getId(),file.getId())
                .param("applicationType",action).param("bpmRound","http-auth-bpm"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code")
                        .value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data").doesNotExist());
    }
    @Test void placementHttpKeepsDistinctStringDirectoryIdsAndDeniesMenuOnlyAccess() throws Exception {
        policy("dcc.project-file-placement.bind");
        var project=project("HTTP-PLACEMENT",7L);var folder=folder(project.getId(),"F");
        directory(1001L,1L,true);var file=file(project.getId(),1001L,"http-placement.pdf");
        tx(()->placements.bind(7L,project.getId(),folder.getId(),file.getId(),1001L,"确认文件所在项目目录"));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(projectController)
                .setControllerAdvice(new cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler("dcc-b-combination",apiErrorLogs)).build();
        when(permissions.hasAnyPermissions(7L,"dcc:project-code:query","dcc:controlled-file:query")).thenReturn(false);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/dcc/project-codes/{projectId}/folders/{folderId}/file-placements",project.getId(),folder.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(403));
        when(permissions.hasAnyPermissions(7L,"dcc:project-code:query","dcc:controlled-file:query")).thenReturn(true);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/dcc/project-codes/{projectId}/folders/{folderId}/file-placements",project.getId(),folder.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].projectFolderId").value(String.valueOf(folder.getId())))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].storageDirectoryId").value("1001"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].controlledFileId").value(String.valueOf(file.getId())));
        login(8L);when(permissions.hasAnyPermissions(8L,"dcc:project-code:query","dcc:controlled-file:query")).thenReturn(true);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/dcc/project-codes/{projectId}/folders/{folderId}/file-placements",project.getId(),folder.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code")
                        .value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data").doesNotExist());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_project_file_placement",Integer.class));
        assertEquals(1,events.selectList().size());
    }
    @Test void manualProjectFoldersPersistWithoutInventingTemplateSourceAndEditKeepsStableVersionPlacement() {
        policy("dcc.project-folder.create");policy("dcc.project-folder.update");policy("dcc.project-file-placement.bind");
        var project=project("MANUAL-FOLDER",7L);var templateFolder=folder(project.getId(),"原模板目录");
        var created=folderMaintenance.save(7L,project.getId(),new DccProjectFolderMaintenanceService.Save(null,templateFolder.getId(),"人工子目录",2,"新增项目目录"));
        assertNotNull(created.getId());assertNull(created.getSourceTemplateId());assertNull(created.getSourceNodeKey());
        assertEquals(templateFolder.getId(),created.getParentId());
        directory(1001L,1L,true);var file=file(project.getId(),1001L,"directory-edit.pdf");
        var placement=tx(()->placements.bind(7L,project.getId(),created.getId(),file.getId(),1001L,"登记位置"));
        jdbc.update("INSERT INTO dcc_project_file_reference(tenant_id,project_id,folder_id,master_id,selected_controlled_file_id,created_by) VALUES(?,?,?,?,?,?)",
                1L,project.getId(),created.getId(),file.getMasterId(),file.getId(),7L);
        String fileBefore=JsonUtils.toJsonString(files.selectById(file.getId()));
        var changed=folderMaintenance.save(7L,project.getId(),new DccProjectFolderMaintenanceService.Save(created.getId(),0L,"人工目录新名称",3,"显式修改项目目录"));
        assertEquals(created.getId(),changed.getId());assertEquals(0L,changed.getParentId());assertEquals("人工目录新名称",changed.getName());
        assertEquals(placement.getId(),placements.require(7L,project.getId(),file.getId()).getId());
        assertEquals(created.getId(),placements.require(7L,project.getId(),file.getId()).getProjectFolderId());
        assertEquals(created.getId(),jdbc.queryForObject("SELECT folder_id FROM dcc_project_file_reference",Long.class));
        assertEquals(fileBefore,JsonUtils.toJsonString(files.selectById(file.getId())));
        assertEquals("原模板目录",folders.selectById(templateFolder.getId()).getName());assertEquals(3,events.selectList().size());
        assertTrue(events.selectList().stream().anyMatch(event->event.getAfterStateJson().contains("人工目录新名称")));
        var renamed=folderMaintenance.save(7L,project.getId(),new DccProjectFolderMaintenanceService.Save(templateFolder.getId(),0L,"模板生成目录显式改名",0,"只改本项目目录"));
        assertEquals(templateFolder.getSourceTemplateId(),renamed.getSourceTemplateId());
        assertEquals(templateFolder.getSourceNodeKey(),renamed.getSourceNodeKey());
    }
    @Test void projectFolderEditRejectsForeignParentCyclesDuplicateNamesAndUnprivilegedActorWithoutWrites() {
        policy("dcc.project-folder.create");policy("dcc.project-folder.update");
        var project=project("FOLDER-GUARDS",7L);var root=folder(project.getId(),"根");
        var child=folderMaintenance.save(7L,project.getId(),new DccProjectFolderMaintenanceService.Save(null,root.getId(),"子",0,"新增目录"));
        var other=project("FOLDER-OTHER",7L);var foreign=folder(other.getId(),"外部目录");
        int eventCount=events.selectList().size();
        for(var invalid:List.of(
                new DccProjectFolderMaintenanceService.Save(root.getId(),child.getId(),"循环",0,"测试循环"),
                new DccProjectFolderMaintenanceService.Save(null,foreign.getId(),"跨项目",0,"测试归属"),
                new DccProjectFolderMaintenanceService.Save(null,root.getId(),"子",0,"测试重名"),
                new DccProjectFolderMaintenanceService.Save(null,0L," ",0,"测试空名"),
                new DccProjectFolderMaintenanceService.Save(null,0L,"非法排序",-1,"测试排序"))) {
            assertThrows(RuntimeException.class,()->folderMaintenance.save(7L,project.getId(),invalid));
        }
        when(permissions.hasAnyPermissions(8L,"dcc:project-code:update")).thenReturn(true);
        assertThrows(RuntimeException.class,()->folderMaintenance.save(8L,project.getId(),new DccProjectFolderMaintenanceService.Save(null,0L,"无范围",0,"无范围测试")));
        when(permissions.hasAnyPermissions(7L,"dcc:project-code:update")).thenReturn(false);
        assertThrows(RuntimeException.class,()->folderMaintenance.save(7L,project.getId(),new DccProjectFolderMaintenanceService.Save(null,0L,"无菜单",0,"无菜单测试")));
        assertEquals(2,folders.listByProject(project.getId()).size());assertEquals(root.getId(),folders.selectById(child.getId()).getParentId());
        assertEquals(eventCount,events.selectList().size());
    }
    @Test void projectFolderMissingAuditPolicyRollsBackCreateAndEdit() {
        var project=project("FOLDER-NO-POLICY",7L);var root=folder(project.getId(),"原名称");
        assertThrows(RuntimeException.class,()->folderMaintenance.save(7L,project.getId(),new DccProjectFolderMaintenanceService.Save(null,0L,"新目录",0,"新增目录")));
        assertThrows(RuntimeException.class,()->folderMaintenance.save(7L,project.getId(),new DccProjectFolderMaintenanceService.Save(root.getId(),0L,"未保存名称",0,"改名目录")));
        assertEquals(1,folders.listByProject(project.getId()).size());assertEquals("原名称",folders.selectById(root.getId()).getName());
        assertTrue(events.selectList().isEmpty());
    }
    @Test void projectFolderHttpUsesRealPermissionScopeAndReturnsPersistedManualSource() throws Exception {
        policy("dcc.project-folder.create");
        var project=project("FOLDER-HTTP",7L);
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(folderMaintenanceController)
                .setControllerAdvice(new cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler("dcc-b-combination",apiErrorLogs)).build();
        String payload=JsonUtils.toJsonString(new DccProjectFolderMaintenanceService.Save(null,0L,"人工目录",0,"新增项目文件夹"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/dcc/project-codes/{id}/folders",project.getId())
                .contentType("application/json").content(payload))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(0))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.name").value("人工目录"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.sourceTemplateId").doesNotExist());
        when(permissions.hasAnyPermissions(7L,"dcc:project-code:update")).thenReturn(false);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/dcc/project-codes/{id}/folders",project.getId())
                .contentType("application/json").content(payload))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(403));
        assertEquals(1,folders.listByProject(project.getId()).size());assertEquals(1,events.selectList().size());
    }
    @Test void projectFolderZeroRowWritesAndWrongTenantCannotLeaveSavedOrAuditedFacts() {
        policy("dcc.project-folder.create");policy("dcc.project-folder.update");
        var project=project("FOLDER-ZERO-TENANT",7L);var root=folder(project.getId(),"原名称");
        doReturn(0).when(folders).insert(any(DccProjectFolderDO.class));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->folderMaintenance.save(7L,project.getId(),
                new DccProjectFolderMaintenanceService.Save(null,0L,"零行新增",0,"新增目录测试")));
        doReturn(0).when(folders).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->folderMaintenance.save(7L,project.getId(),
                new DccProjectFolderMaintenanceService.Save(root.getId(),0L,"零行修改",0,"修改目录测试")));
        assertEquals(1,folders.listByProject(project.getId()).size());assertEquals("原名称",folders.selectById(root.getId()).getName());
        assertTrue(events.selectList().isEmpty());reset(folders);
        project.setTenantId(2L);projects.updateById(project);
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->folderMaintenance.save(7L,project.getId(),
                new DccProjectFolderMaintenanceService.Save(null,0L,"错租户",0,"租户检查")));
        assertTrue(events.selectList().isEmpty());
    }
}
