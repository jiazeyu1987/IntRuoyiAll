package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkServiceImpl;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.web.core.handler.GlobalExceptionHandler;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccFolderTemplateController;
import cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectAttributesController;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessServiceImpl;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccFolderTemplateService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.PostApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.RoleApi;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 实际MVC绑定、方法授权、领域服务、H2 Mapper与统一审计落库；不启动服务/浏览器。 */
@Import({cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectReviewerConfigurationService.class,DccProjectAttributesController.class, DccFolderTemplateController.class,
        DccProjectAttributesService.class, DccProjectLeaderService.class, DccProjectConfigurationAuditService.class,
        cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductAuditService.class,
        DccFolderTemplateService.class, DccProjectAccessServiceImpl.class, GxpAuditServiceImpl.class,
        cn.iocoder.yudao.module.dcc.controller.admin.category.DccFileTypeTaxonomyController.class,
        cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminServiceImpl.class,
        cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectProductCreateController.class,
        cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateServiceImpl.class,
        cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateWriteService.class,
        cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateStateService.class,
        cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateFailureService.class,
        DccProjectConfigurationHttpPersistenceTest.MethodSecurityConfiguration.class})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(scripts = "/sql/dcc_b_gxp_audit_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/dcc_b_gxp_audit_clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DccProjectConfigurationHttpPersistenceTest extends BaseDbUnitTest {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductNotificationService projectNotifications;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductActorSupport projectActors;
    @Resource javax.sql.DataSource reviewerDataSource;
    @org.junit.jupiter.api.BeforeEach void configuredReviewerFixture() {
        new org.springframework.jdbc.core.JdbcTemplate(reviewerDataSource).update("INSERT INTO dcc_project_reviewer_config(tenant_id,reviewer_user_id,reviewer_username,reviewer_nickname,version_no,updated_by,change_reason) VALUES(1,1,'admin','显式配置测试审核人',1,1,'测试正式配置')");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
        @Bean("ss") SecurityFrameworkService ss(PermissionApi permissions) {
            return new SecurityFrameworkServiceImpl(permissions);
        }
    }
    @Resource private DccProjectAttributesController attributesController;
    @Resource private DccFolderTemplateController folderController;
    @Resource private DccProjectAttributesService attributes;
    @Resource private DccFolderTemplateService folderService;
    @Resource private cn.iocoder.yudao.module.dcc.controller.admin.category.DccFileTypeTaxonomyController taxonomyController;
    @Resource private cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminServiceImpl taxonomies;
    @Resource private cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper categories;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileTypeTaxonomyMapper taxonomyRows;
    @Resource private cn.iocoder.yudao.module.dcc.controller.admin.projectcode.DccProjectProductCreateController productController;
    @Resource private cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductCreateServiceImpl productService;
    @Resource private DccProjectProductCreateRequestMapper productRequests;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean private DccProjectCodeMapper projects;
    @Resource private DccProjectAccessRuleMapper accessRules;
    @Resource private DccFolderTemplateMapper templates;
    @Resource private DccFolderTemplateHistoryMapper history;
    @Resource private GxpAuditPolicyOperationMapper policies;
    @Resource private GxpAuditEventMapper events;
    @MockitoBean private AdminUserApi users;
    @MockitoBean private PermissionApi permissions;
    @MockitoBean private DeptApi departments;
    @MockitoBean private PostApi posts;
    @MockitoBean private RoleApi roles;
    @MockitoBean private ApiErrorLogCommonApi apiErrorLogs;
    @MockitoBean private cn.iocoder.yudao.module.dcc.service.projectcode.folder.DccProjectFilePlacementService placementService;
    private MockMvc mvc;

    @BeforeEach void setupHttp() {
        when(users.getUser(1L)).thenReturn(new AdminUserRespDTO().setId(1L).setTenantId(1L).setStatus(0).setUsername("admin").setNickname("显式配置测试审核人"));
        var user = new AdminUserRespDTO(); user.setId(7L); user.setTenantId(1L); user.setStatus(0); user.setNickname("正式负责人"); user.setUsername("dcc-b-test");
        when(users.getUser(7L)).thenReturn(user);
        when(permissions.hasAnyPermissions(7L, "dcc:project-code:query")).thenReturn(true);
        when(permissions.hasAnyPermissions(7L, "dcc:project-code:query", "dcc:controlled-file:query")).thenReturn(true);
        when(permissions.hasAnyPermissions(7L, "dcc:project-code:update")).thenReturn(true);
        when(permissions.hasAnyPermissions(7L, "dcc:project-code:query", "dcc:controlled-file:submit", "dcc:controlled-file:query")).thenReturn(true);
        when(permissions.getUserRoleIdListByUserId(7L)).thenReturn(Set.of());
        var login = new LoginUser(); login.setId(7L); login.setTenantId(1L);
        login.setUserType(UserTypeEnum.ADMIN.getValue());
        login.setInfo(Map.of("username", "dcc-b-test", LoginUser.INFO_KEY_NICKNAME, "正式负责人"));
        SecurityFrameworkUtils.setLoginUser(login, new MockHttpServletRequest());
        mvc = MockMvcBuilders.standaloneSetup(attributesController, folderController, taxonomyController, productController)
                .setControllerAdvice(new GlobalExceptionHandler("dcc-b-test", apiErrorLogs)).build();
    }
    @AfterEach void clearLogin() { SecurityContextHolder.clearContext(); }
    private DccProjectAttributes value(String market) {
        return new DccProjectAttributes(List.of(market), null, "Y", "N", "N", null);
    }
    private Long project() {
        var row = DccProjectCodeDO.builder().projectName("真实保存测试").projectCode("B-HTTP")
                .status("ENABLE").projectLeaderUserId(7L).defaultAttributesJson(attributes.encode(value("NMPA"))).build();
        row.setTenantId(1L); projects.insert(row);
        var rule = new DccProjectAccessRuleDO(); rule.setDccProjectCodeId(row.getId()); rule.setTenantId(1L);
        rule.setSubjectType("USER"); rule.setSubjectId(7L); rule.setAccessLevel("EDIT"); rule.setActive(true);
        rule.setChangeReason("B隔离测试正式编制权限");
        accessRules.insert(rule);
        return row.getId();
    }
    private void policy(String operation, String subjectType) {
        var policy = new GxpAuditPolicyOperationDO();
        policy.setTenantId(1L); policy.setOperationId(operation); policy.setPolicyVersion("BCC-ISOLATED-TEST");
        policy.setSourceType("SERVICE_METHOD"); policy.setSourceLocator("ISOLATED_TEST");
        policy.setDomain("DCC"); policy.setSubjectType(subjectType); policy.setActionType("UPDATE");
        policy.setReasonPolicy("REQUIRED"); policy.setSignaturePolicy("NOT_REQUIRED"); policy.setStatePolicy("BEFORE_AFTER");
        policy.setRetentionClass("ISOLATED_TEST"); policy.setTestIds("B-HTTP"); policy.setOwner("B-test");
        policy.setApplicability("GXP"); policy.setActive(true); policies.insert(policy);
    }
    private String configuration(String market) {
        return JsonUtils.toJsonString(Map.of("projectLeaderUserId", 7L, "defaultAttributes", value(market), "changeReason", "正式修改项目默认"));
    }
    @Test void reviewerConfigurationHttpRequiresRolePersistsAuditAndReturnsExactLongIdentity() throws Exception {
        new org.springframework.jdbc.core.JdbcTemplate(reviewerDataSource).update("DELETE FROM dcc_project_reviewer_config");
        mvc.perform(get("/dcc/project-product-requests/reviewer-config")).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.configured").value(false));
        var user=new AdminUserRespDTO().setId(9007199254740993L).setTenantId(1L).setStatus(0).setUsername("formal-reviewer").setNickname("正式配置审核人");
        when(users.getUser(user.getId())).thenReturn(user);
        String body="{\"reviewerUserId\":\"9007199254740993\",\"reason\":\"按批准职责配置正式审核人\"}";
        mvc.perform(put("/dcc/project-product-requests/reviewer-config").contentType("application/json").content(body))
                .andExpect(jsonPath("$.code").value(403));
        assertEquals(0,new org.springframework.jdbc.core.JdbcTemplate(reviewerDataSource).queryForObject("SELECT COUNT(*) FROM dcc_project_reviewer_config",Integer.class));
        when(permissions.hasAnyRoles(7L,"doc_control")).thenReturn(true);
        policy("dcc.project-product.reviewer-config","DCC_PROJECT_CONFIGURATION");
        mvc.perform(put("/dcc/project-product-requests/reviewer-config").contentType("application/json").content(body))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.reviewerUserId").value("9007199254740993"));
        mvc.perform(get("/dcc/project-product-requests/reviewer-config")).andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.reviewerNickname").value("正式配置审核人"));
        assertEquals(1,events.selectList().size());assertEquals("dcc.project-product.reviewer-config",events.selectList().get(0).getOperationId());
    }
    private String templatePayload(Long id, Object description) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("id", id); payload.put("name", "目录模板"); payload.put("description", description);
        payload.put("active", true); payload.put("changeReason", "目录模板修改");
        payload.put("structure", Map.of("nodes", List.of(Map.of("key", "root", "name", "根目录", "sortOrder", 0))));
        return JsonUtils.toJsonString(payload);
    }
    @Test void configurationHttpPersistsAndThreeApplicationInitializationReadsCurrentDefaults() throws Exception {
        policy("dcc.project-attributes.configure", "DCC_PROJECT_CONFIGURATION");
        Long project = project();
        mvc.perform(put("/dcc/project-codes/{id}/attributes/configuration", project)
                        .contentType("application/json").content(configuration("CE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data").value(true));
        assertEquals(value("CE"), attributes.defaults(projects.selectById(project)));
        for (String type : List.of("UPLOAD", "REVISION", "OBSOLETE")) {
            mvc.perform(get("/dcc/project-codes/{id}/attributes/defaults", project).param("applicationType", type))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.targetMarkets[0]").value("CE"))
                    .andExpect(jsonPath("$.data.licenseHolder").value("Y"))
                    .andExpect(jsonPath("$.data.actualManufacturer").value("N"));
        }
        var event = events.selectList().get(0);
        assertEquals(7L, event.getActorId());
        assertEquals("dcc-b-test", event.getActorUsername());
        assertNotNull(event.getEventHash());
        assertTrue(event.getAfterStateJson().contains("CE"));
    }
    @Test void missingFormalAuditPolicyRollsBackActualHttpConfiguration() throws Exception {
        Long project = project();
        mvc.perform(put("/dcc/project-codes/{id}/attributes/configuration", project)
                        .contentType("application/json").content(configuration("FDA")))
                .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_NOT_FOUND.getCode()));
        assertEquals(value("NMPA"), attributes.defaults(projects.selectById(project)));
        assertTrue(events.selectList().isEmpty());
    }
    @Test void templateHttpCreateEditAndClearDescriptionReadBackWithRealAuditRows() throws Exception {
        policy("dcc.folder-template.save", "DCC_FOLDER_TEMPLATE");
        mvc.perform(put("/dcc/folder-templates").contentType("application/json").content(templatePayload(null, "旧说明")))
                .andExpect(jsonPath("$.code").value(0));
        Long id = templates.selectList().get(0).getId();
        mvc.perform(put("/dcc/folder-templates").contentType("application/json").content(templatePayload(id, null)))
                .andExpect(jsonPath("$.code").value(0));
        mvc.perform(get("/dcc/folder-templates")).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].description").isEmpty());
        assertNull(templates.selectById(id).getDescription());
        assertEquals(2, history.selectList().size()); assertEquals(2, events.selectList().size());
        assertEquals(7L, events.selectList().get(1).getActorId());
    }
    @Test void deniedRealMethodPermissionAndIllegalFieldsHaveZeroDatabaseWrites() throws Exception {
        policy("dcc.project-attributes.configure", "DCC_PROJECT_CONFIGURATION");
        Long project = project();
        when(permissions.hasAnyPermissions(7L, "dcc:project-code:update")).thenReturn(false);
        mvc.perform(put("/dcc/project-codes/{id}/attributes/configuration", project)
                        .contentType("application/json").content(configuration("FDA")))
                .andExpect(jsonPath("$.code").value(403));
        assertEquals(value("NMPA"), attributes.defaults(projects.selectById(project)));
        assertTrue(events.selectList().isEmpty());
        when(permissions.hasAnyPermissions(7L, "dcc:project-code:update")).thenReturn(true);
        String invalid = configuration("FDA").replace("\"targetMarkets\":[\"FDA\"]", "\"targetMarkets\":[\"NA\",\"FDA\"]");
        mvc.perform(put("/dcc/project-codes/{id}/attributes/configuration", project)
                        .contentType("application/json").content(invalid))
                .andExpect(jsonPath("$.code").value(DccProjectAttributeErrors.INVALID.getCode()));
        assertEquals(value("NMPA"), attributes.defaults(projects.selectById(project)));
        assertTrue(events.selectList().isEmpty());
    }
    @Test void zeroConfigurationUpdateCannotReturnSavedOrAppendSuccessfulChangeAudit() throws Exception {
        policy("dcc.project-attributes.configure", "DCC_PROJECT_CONFIGURATION");
        Long project=project();
        String before=JsonUtils.toJsonString(projects.selectById(project));
        doReturn(0).when(projects).update(isNull(),any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        mvc.perform(put("/dcc/project-codes/{id}/attributes/configuration",project)
                        .contentType("application/json").content(configuration("FDA")))
                .andExpect(jsonPath("$.code").value(DccProjectAttributeErrors.WRITE_INCOMPLETE.getCode()))
                .andExpect(jsonPath("$.data").doesNotExist());
        assertEquals(before,JsonUtils.toJsonString(projects.selectById(project)));
        assertTrue(events.selectList().isEmpty(),"0行配置更新不能记录成功变更事实");
    }
    @Test void projectFolderHttpReadsGeneratedIndependentDirectoryIds() throws Exception {
        policy("dcc.folder-template.save", "DCC_FOLDER_TEMPLATE");
        Long project = project();
        mvc.perform(put("/dcc/folder-templates").contentType("application/json").content(templatePayload(null, null)))
                .andExpect(jsonPath("$.code").value(0));
        Long templateId = templates.selectList().get(0).getId();
        var generated = folderService.generate(project, templateId, folderService.captureForRequest(templateId));
        mvc.perform(get("/dcc/project-codes/{id}/folders", project))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(generated.get(0).getId()))
                .andExpect(jsonPath("$.data[0].projectCodeId").value(project))
                .andExpect(jsonPath("$.data[0].sourceNodeKey").value("root"))
                .andExpect(jsonPath("$.data[0].name").value("根目录"));
        var unrelated = DccProjectCodeDO.builder().projectName("其他项目").projectCode("OTHER").status("ENABLE").build();
        unrelated.setTenantId(1L); projects.insert(unrelated);
        mvc.perform(get("/dcc/project-codes/{id}/folders", unrelated.getId()))
                .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()));
    }
    @Test void taxonomyMaintenanceHttpChecksUniqueMappingWithManagementPermission() throws Exception {
        when(permissions.hasAnyPermissions(7L, "dcc:controlled-file:submit", "dcc:controlled-file:category:manage")).thenReturn(true);
        Long parent = null;
        for (int level = 1; level <= 3; level++) {
            var input = new cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomySaveReqVO();
            input.setParentId(parent); input.setCode("B-HTTP-MAP-" + level); input.setName("类别映射" + level);
            input.setActive(true); input.setSort(level);
            parent = taxonomies.createTaxonomy(input);
        }
        Long typeId = parent;
        var category = cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO.builder()
                .code("B-MAP-C1").name("正式类别1").parentId(0L).active(true).sort(0)
                .source("LOCAL").lifecycleStage("GENERAL").fileTypeTaxonomyId(typeId).build();
        categories.insert(category);
        mvc.perform(get("/dcc/file-type-taxonomies/{id}/active-category", typeId))
                .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data").value(category.getId()));
        var ambiguous = cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO.builder()
                .code("B-MAP-C2").name("正式类别2").parentId(0L).active(true).sort(0)
                .source("LOCAL").lifecycleStage("GENERAL").fileTypeTaxonomyId(typeId).build();
        categories.insert(ambiguous);
        mvc.perform(get("/dcc/file-type-taxonomies/{id}/active-category", typeId))
                .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.PROJECT_FILE_TEMPLATE_CATEGORY_INVALID.getCode()));
        when(permissions.hasAnyPermissions(7L, "dcc:controlled-file:submit", "dcc:controlled-file:category:manage")).thenReturn(false);
        mvc.perform(get("/dcc/file-type-taxonomies/{id}/active-category", typeId)).andExpect(jsonPath("$.code").value(403));
        assertEquals(2, categories.selectList().size(), "只读映射检查不能写回类别或矩阵");
    }
    @Test void applicationEntryPermissionsCanReadDefaultsWithoutProjectConfigurationMenu() throws Exception {
        Long project = project();
        when(permissions.hasAnyPermissions(7L, "dcc:project-code:query")).thenReturn(false);
        when(permissions.hasAnyPermissions(7L, "dcc:project-code:query", "dcc:controlled-file:submit", "dcc:controlled-file:query")).thenReturn(true);
        for (String type : List.of("UPLOAD", "REVISION", "OBSOLETE")) {
            mvc.perform(get("/dcc/project-codes/{id}/attributes/defaults", project).param("applicationType", type))
                    .andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data.targetMarkets[0]").value("NMPA"));
        }
        accessRules.deleteByProjectCodeId(project);
        mvc.perform(get("/dcc/project-codes/{id}/attributes/defaults", project).param("applicationType", "UPLOAD"))
                .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_PROJECT_ACCESS_DENIED.getCode()));
    }
    @Test void taxonomyWriteHttpPropagatesZeroRowFailureAndPreservesExistingType() throws Exception {
        when(permissions.hasAnyPermissions(7L,"dcc:controlled-file:category:manage")).thenReturn(true);
        var input=new cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomySaveReqVO();
        input.setCode("B-HTTP-WRITE");input.setName("正式文件类型");input.setActive(true);input.setSort(0);
        String payload=JsonUtils.toJsonString(input);
        doReturn(0).when(taxonomyRows).insert(any(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO.class));
        mvc.perform(post("/dcc/file-type-taxonomies").contentType("application/json").content(payload))
                .andExpect(jsonPath("$.code").value(1080090015)).andExpect(jsonPath("$.data").doesNotExist());
        assertTrue(taxonomyRows.selectList().isEmpty());
        reset(taxonomyRows);
        mvc.perform(post("/dcc/file-type-taxonomies").contentType("application/json").content(payload))
                .andExpect(jsonPath("$.code").value(0));
        Long id=taxonomyRows.selectList().get(0).getId();
        doReturn(0).when(taxonomyRows).updateById(any(cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO.class));
        input.setName("未实际保存的名称");
        mvc.perform(put("/dcc/file-type-taxonomies/{id}",id).contentType("application/json").content(JsonUtils.toJsonString(input)))
                .andExpect(jsonPath("$.code").value(1080090015)).andExpect(jsonPath("$.data").doesNotExist());
        assertEquals("正式文件类型",taxonomyRows.selectById(id).getName());
        reset(taxonomyRows);
        doReturn(0).when(taxonomyRows).deleteById(id);
        mvc.perform(delete("/dcc/file-type-taxonomies/{id}",id))
                .andExpect(jsonPath("$.code").value(1080090015)).andExpect(jsonPath("$.data").doesNotExist());
        assertNotNull(taxonomyRows.selectById(id));
        reset(taxonomyRows);
        mvc.perform(delete("/dcc/file-type-taxonomies/{id}",id)).andExpect(jsonPath("$.code").value(0));
        assertNull(taxonomyRows.selectById(id));
    }
    @Test void originalApplicantHttpResubmitsRejectedProjectProductAndReadsImmutablePredecessor() throws Exception {
        when(permissions.hasAnyPermissions(7L, "dcc:project-code:create")).thenReturn(true);
        policy("dcc.folder-template.save", "DCC_FOLDER_TEMPLATE");
        policy("dcc.project-product.create","DCC_PROJECT_PRODUCT_REQUEST");
        policy("dcc.project-product.review","DCC_PROJECT_PRODUCT_REQUEST");
        mvc.perform(put("/dcc/folder-templates").contentType("application/json").content(templatePayload(null, null)))
                .andExpect(jsonPath("$.code").value(0));
        Long templateId = templates.selectList().get(0).getId();
        var input = new cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateReqVO();
        input.setProjectName("重提HTTP项目"); input.setProjectCode("HTTP-REJECT-B");
        input.setProjectLeaderUserId(7L); input.setFolderTemplateId(templateId); input.setDefaultAttributes(value("CE"));
        input.setProductCode("HTTP-REJECT-PR"); input.setProductName("重提HTTP产品"); input.setClassification("一类");input.setCreationReason("正式新建申请原因");
        mvc.perform(post("/dcc/project-product-requests/create").contentType("application/json").content(JsonUtils.toJsonString(input)))
                .andExpect(jsonPath("$.code").value(0));
        Long originalId = productRequests.selectList().get(0).getId();
        var reviewer = new AdminUserRespDTO(); reviewer.setId(1L); reviewer.setStatus(0); reviewer.setUsername("admin"); reviewer.setTenantId(1L);reviewer.setNickname("显式配置测试审核人");
        when(users.getUser(1L)).thenReturn(reviewer);
        productService.review(1L, originalId, "原审批规则驳回", false);
        input.setDefaultAttributes(value("FDA")); input.setResubmissionReason("按驳回意见补齐后重提");
        mvc.perform(post("/dcc/project-product-requests/{id}/resubmit", originalId).contentType("application/json").content(JsonUtils.toJsonString(input)))
                .andExpect(jsonPath("$.code").value(cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.GXP_AUDIT_POLICY_NOT_FOUND.getCode()));
        assertEquals(1, productRequests.selectList().size());
        policy("dcc.project-product.resubmit", "DCC_PROJECT_PRODUCT_REQUEST");
        mvc.perform(post("/dcc/project-product-requests/{id}/resubmit", originalId).contentType("application/json").content(JsonUtils.toJsonString(input)))
                .andExpect(jsonPath("$.code").value(0));
        var next = productRequests.selectList().stream().filter(row -> originalId.equals(row.getPreviousRequestId())).findFirst().orElseThrow();
        assertEquals("PENDING_REVIEW", next.getStatus()); assertEquals(7L, next.getApplicantUserId());
        assertEquals(value("CE"), attributes.readValue(productRequests.selectById(originalId).getDefaultAttributesJson()));
        assertEquals(value("FDA"), attributes.readValue(next.getDefaultAttributesJson()));
        mvc.perform(get("/dcc/project-product-requests/pending")).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].previousRequestId").value(originalId))
                .andExpect(jsonPath("$.data[1].resubmittedRequestId").value(next.getId()))
                .andExpect(jsonPath("$.data[1].rejectReason").value("原审批规则驳回"));
        mvc.perform(post("/dcc/project-product-requests/{id}/resubmit", originalId).contentType("application/json").content(JsonUtils.toJsonString(input)))
                .andExpect(jsonPath("$.code").value(DccProjectAttributeErrors.REJECTED_REQUEST_REWORK_INVALID.getCode()));
        assertEquals(2, productRequests.selectList().size());
        var event = events.selectList().stream().filter(row -> "dcc.project-product.resubmit".equals(row.getOperationId())).findFirst().orElseThrow();
        assertEquals(7L, event.getActorId()); assertEquals("按驳回意见补齐后重提", event.getReason());
        assertTrue(event.getAfterStateJson().contains("rejectedPredecessor"));
    }
    @Test void initialCreateAndRetryHttpRequireTheirOwnReasonsAndPersistLedgerFact() throws Exception {
        when(permissions.hasAnyPermissions(7L,"dcc:project-code:create")).thenReturn(true);
        policy("dcc.folder-template.save","DCC_FOLDER_TEMPLATE");policy("dcc.project-product.create","DCC_PROJECT_PRODUCT_REQUEST");
        mvc.perform(put("/dcc/folder-templates").contentType("application/json").content(templatePayload(null,null)))
                .andExpect(jsonPath("$.code").value(0));
        var input=new cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.productcreate.DccProjectProductCreateReqVO();
        input.setProjectName("审计HTTP项目");input.setProjectCode("HTTP-AUDIT-P");input.setProjectLeaderUserId(7L);
        input.setFolderTemplateId(templates.selectList().get(0).getId());input.setDefaultAttributes(value("CE"));
        input.setProductCode("HTTP-AUDIT-PR");input.setProductName("审计HTTP产品");input.setClassification("一类");
        input.setRemark("只能是备注");input.setResubmissionReason("只能是重提原因");
        mvc.perform(post("/dcc/project-product-requests/create").contentType("application/json").content(JsonUtils.toJsonString(input)))
                .andExpect(jsonPath("$.code").value(DccProjectAttributeErrors.INVALID.getCode()));
        assertTrue(productRequests.selectList().isEmpty());
        input.setCreationReason("用户填写的独立新建原因");
        mvc.perform(post("/dcc/project-product-requests/create").contentType("application/json").content(JsonUtils.toJsonString(input)))
                .andExpect(jsonPath("$.code").value(0));
        Long id=productRequests.selectList().get(0).getId();
        assertEquals("用户填写的独立新建原因",productRequests.selectById(id).getCreationReason());
        var event=events.selectList().stream().filter(row->"dcc.project-product.create".equals(row.getOperationId())).findFirst().orElseThrow();
        assertEquals(7L,event.getActorId());assertEquals("用户填写的独立新建原因",event.getReason());
        int count=events.selectList().size();
        mvc.perform(post("/dcc/project-product-requests/{id}/retry-write",id).contentType("application/json").content("{\"reason\":\" \"}"))
                .andExpect(jsonPath("$.code").value(400));
        assertEquals(count,events.selectList().size());
    }
}
