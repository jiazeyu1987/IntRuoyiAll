package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.*;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeServiceImpl;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import javax.sql.DataSource;
import java.util.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual existing FormCenter.createInstance+SQL snapshots, form-specific numeric round and
 * protected attribute methods in the same private H2 transaction. Policy/permissions are external ports. */
@Import({DccApplicationRoundService.class,DccProjectAttributesService.class,DccProjectApplicationSnapshotService.class,DccObsoleteFormOpenDraftDatabaseTest.Beans.class})
@TestPropertySource(properties="spring.datasource.url=jdbc:h2:mem:dcc_ic29_open;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;LOCK_TIMEOUT=5000")
class DccObsoleteFormOpenDraftDatabaseTest extends BaseDbUnitTest {
    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods=false)
    static class Beans { @org.springframework.context.annotation.Bean JdbcTemplate jdbc(DataSource source){return new JdbcTemplate(source);} }
    @Resource DataSource dataSource;
    @Resource PlatformTransactionManager manager;
    @Resource org.mybatis.spring.SqlSessionTemplate sql;
    @Resource(name="dccControlledFileMapper") DccControlledFileMapper files;
    @Resource DccControlledFileMasterMapper masters;
    @Resource DccProjectCodeMapper projects;
    @Resource DccProjectApplicationAttributesMapper attributesRows;
    @Resource DccApplicationRoundService rounds;
    @Resource DccProjectAttributesService attributes;
    @MockitoBean DccProjectAccessService projectAccess;
    @MockitoBean DccControlledFileCategoryPermissionSupport permissions;
    JdbcTemplate jdbc;
    DccControlledFileObsoleteServiceImpl obsolete;
    FormActionInstanceMapper forms;
    FormActionSnapshotMapper formSnapshots;
    FormCenterRuntimeServiceImpl formCenter;
    BusinessApprovalPolicyMapper policies;
    DccObsoleteFormExecutionPreparationService source;
    DccApprovalMatrixSourceService matrixSources;
    DccApprovalMatrixTokenService tokens;
    org.springframework.beans.factory.ObjectProvider<DccDirectSupervisorSource> supervisorProvider;
    cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
    org.flowable.engine.ProcessEngine preparationEngine;
    cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService preparedDefinitions;
    final DccProjectAttributes d1=value("CE"),d2=value("FDA"),actual=value("NMPA");
    static DccProjectAttributes value(String market){return new DccProjectAttributes(List.of(market),null,"Y","N","N",null);}
    static void wire(Object bean,Object... fields){for(int i=0;i<fields.length;i+=2)ReflectionTestUtils.setField(bean,(String)fields[i],fields[i+1]);}
    TransactionTemplate tx(){return new TransactionTemplate(manager);}
    @BeforeEach void seed()throws Exception{
        TenantContextHolder.setTenantId(1L);SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(99L).setTenantId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());
        jdbc=new JdbcTemplate(dataSource);try(var connection=dataSource.getConnection()){assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();
        String schema=Files.readString(root.resolve("IntRuoyiBackend/yudao-module-bpm/src/test/resources/sql/create_tables.sql"));
        for(String table:List.of("bpm_form_action_instance","bpm_form_action_snapshot")){String marker="CREATE TABLE IF NOT EXISTS \""+table+"\"";int start=schema.indexOf(marker);int end=schema.indexOf(";",start);jdbc.execute(schema.substring(start,end));}
        jdbc.update("DELETE FROM bpm_form_action_snapshot WHERE instance_id IN (SELECT id FROM bpm_form_action_instance WHERE tenant_id=1 AND system_code='DCC' AND object_id='20')");
        jdbc.update("DELETE FROM bpm_form_action_instance WHERE tenant_id=1 AND system_code='DCC' AND object_id='20'");
        for(var type:List.of(FormActionInstanceMapper.class,FormActionSnapshotMapper.class))if(!sql.getConfiguration().hasMapper(type))sql.getConfiguration().addMapper(type);
        forms=sql.getMapper(FormActionInstanceMapper.class);formSnapshots=sql.getMapper(FormActionSnapshotMapper.class);
        var provider=mock(org.springframework.beans.factory.ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(forms);wire(rounds,"obsoleteForms",provider,"obsoleteMasters",masters);
        jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_name,project_code,status,default_attributes_json,deleted) VALUES(5,1,'真实open项目','OPEN-IC29','ENABLE',?,0)",attributes.encode(d1));
        jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number,status,current_active_controlled_file_id,latest_controlled_file_id,deleted) VALUES(10,1,2,3,'SOP','N-1',5,6,'N-1','ACTIVE_CHAIN',20,20,0)");
        var file=DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(10L).categoryId(2L).directoryId(3L).dccProjectCodeId(5L).fileTypeTaxonomyId(6L)
                .fileNumber("N-1").fileName("SOP").title("SOP").sourceOriginalFileName("SOP.pdf").sourceFileId(100L).originalFileId(100L).sourceSha256("a".repeat(64))
                .versionNo("A/1").changeType("NEW").processType("CONTROLLED_FILE").requesterId(99L).submitterId(99L).status("ACTIVE").controlledTime(java.time.LocalDateTime.of(2026,10,1,0,0))
                .processInstanceId("original-upload-round").processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).build();files.insert(file);
        when(permissions.hasCategoryPermission(any(),eq(99L),any())).thenReturn(true);
        policies=mock(BusinessApprovalPolicyMapper.class);var policy=BusinessApprovalPolicyDO.builder().id(81L).tenantId(1L).dataDomain("DCC").systemCode("DCC").objectType("CONTROLLED_FILE").actionCode("OBSOLETE").objectState("ACTIVE")
                .policyMode("BPM_REQUIRED").formPolicyType("NONE").formSlotsJson("[]").processDefinitionKey(DccControlledFileProcessDefinitionKeys.OBSOLETE).effectExecutorCode("DCC_OBSOLETE").status("PUBLISHED").build();
        when(policies.selectPublishedByAction(any(),any(),any(),any(),any(),any())).thenReturn(List.of(policy));
        when(policies.selectById(81L)).thenReturn(policy);
        formCenter=new FormCenterRuntimeServiceImpl();wire(formCenter,"actionInstanceMapper",forms,"actionSnapshotMapper",formSnapshots,"businessApprovalPolicyMapper",policies);
        var pending=new DccControlledFilePendingActionGuard();wire(pending,"formCenterRuntimeService",formCenter);
        obsolete=new DccControlledFileObsoleteServiceImpl();wire(obsolete,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"permissionSupport",permissions,"pendingActionGuard",pending,
                "formCenterRuntimeService",formCenter,"applicationRoundService",rounds,"projectAttributesService",attributes);
        wire(obsolete,"obsoleteForms",forms,"obsoletePolicies",policies);
        source=new DccObsoleteFormExecutionPreparationService();supervisorProvider=mock(org.springframework.beans.factory.ObjectProvider.class);
        users=mock(cn.iocoder.yudao.module.system.api.user.AdminUserApi.class);when(users.getUser(anyLong())).thenAnswer(c->new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO().setId(c.getArgument(0)).setTenantId(1L).setStatus(0).setUsername("actual-directory-account").setNickname("实际目录候选").setPostIds(Set.of(10L)));
        when(users.getUserList(any())).thenAnswer(c->c.<Collection<Long>>getArgument(0).stream().map(users::getUser).toList());
        var key=new DccApprovalMatrixTokenProperties();key.setSigningSecret("ic29-private-form-draft-HMAC-key-at-least-32-bytes");
        tokens=new DccApprovalMatrixTokenService();wire(tokens,"properties",key);var proof=new DccObsoleteFormPreparationProof();wire(proof,"signing",key);
        matrixSources=new DccApprovalMatrixSourceService();var dates=new DccWorkflowDatePolicy();dates.setZoneId("Asia/Shanghai");
        wire(matrixSources,"categories",sql.getMapper(cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper.class),"routes",sql.getMapper(cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper.class),"nodes",sql.getMapper(cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper.class),"datePolicy",dates);
        wire(source,"files",files,"masters",masters,"projects",projects,"projectAccess",projectAccess,"categoryPermissions",permissions,"rounds",rounds,"projectAttributes",attributes,
                "sources",matrixSources,"tokens",tokens,"signing",key,"proof",proof,"supervisors",supervisorProvider,"users",users);
        wire(obsolete,"executionPreparation",source);
        var adapter=new DccControlledFileObsoleteFormEffectExecutor(obsolete);wire(adapter,"executionPreparation",source);wire(formCenter,"lifecycleAdapters",List.of(adapter));
    }
    @AfterEach void cleanup(){jdbc.update("DELETE FROM bpm_form_action_snapshot WHERE instance_id IN (SELECT id FROM bpm_form_action_instance WHERE tenant_id=1 AND system_code='DCC' AND object_id='20')");jdbc.update("DELETE FROM bpm_form_action_instance WHERE tenant_id=1 AND system_code='DCC' AND object_id='20'");TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    @AfterEach void closePreparationEngine(){if(preparationEngine!=null)preparationEngine.close();}
    DccControlledFileObsoleteReqVO request(String key){var r=new DccControlledFileObsoleteReqVO();r.setPhase("OPEN");r.setIdempotencyKey(key);r.setReason("真实作废草稿原因");r.setSelectedSignoffDepartmentIds(List.of(51L));r.setProjectAttributes(actual);return r;}
    FormInstanceRespVO open(DccControlledFileObsoleteReqVO r){return tx().execute(s->obsolete.obsoleteControlledFile(99L,20L,r));}
    int ownRound(FormInstanceRespVO form){return tx().execute(s->rounds.requireObsoleteFormDraft(99L,20L,form.getId(),form.getInstanceCode()));}
    DccControlledFileObsoleteReqVO phaseRequest(FormInstanceRespVO form,String phase){var r=request("OPEN-SAVE");r.setPhase(phase);r.setFormInstanceId(form.getId());r.setFormInstanceCode(form.getInstanceCode());r.setAttributeRound(ownRound(form));r.setApplicationContextKey("DCC_OBSOLETE:"+form.getId()+":"+form.getInstanceCode());return r;}
    cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO getDraft(){return tx().execute(s->obsolete.getObsoleteApplicationDraft(99L,20L));}
    cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO phase(DccControlledFileObsoleteReqVO r){return (cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO)tx().execute(s->obsolete.obsoleteControlledFile(99L,20L,r));}
    void currentMatrix(int version)throws Exception{
        if(version==1){var category=cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO.builder().id(2L).code("IC29").name("真实作废类别").active(true).source("LOCAL").lifecycleStage("DEVELOPMENT").fileTypeTaxonomyId(6L).build();sql.getMapper(cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper.class).insert(category);jdbc.update("UPDATE dcc_file_category SET tenant_id=1 WHERE id=2");}
        jdbc.update("UPDATE dcc_category_approval_route SET active=0 WHERE category_id=2");var canonical=new DccPreparedExecutionInheritanceDatabaseTest().canonicalMatrix(version);var baseline=DccApprovalMatrixSourceService.readCanonical(canonical);
        var route=cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteDO.builder().id(baseline.getRouteId()).categoryId(2L).versionNo(version).actionType("LEGACY").active(true).effectiveTime(java.time.LocalDateTime.of(2026,10,1,12,0)).build();sql.getMapper(cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteMapper.class).insert(route);jdbc.update("UPDATE dcc_category_approval_route SET tenant_id=1 WHERE id=?",route.getId());
        var raw=JsonUtils.parseObject(canonical,Map.class);for(var node:JsonUtils.parseArray(JsonUtils.toJsonString(raw.get("nodes")),cn.iocoder.yudao.module.dcc.dal.dataobject.route.DccCategoryApprovalRouteNodeDO.class)){node.setId(version*10L+node.getId());node.setRouteId(route.getId());sql.getMapper(cn.iocoder.yudao.module.dcc.dal.mysql.route.DccCategoryApprovalRouteNodeMapper.class).insert(node);jdbc.update("UPDATE dcc_category_approval_route_node SET tenant_id=1 WHERE id=?",node.getId());}
    }
    void authoritativePreparationPorts()throws Exception{
        var organization=mock(DccDirectSupervisorSource.class);when(supervisorProvider.getIfAvailable()).thenReturn(organization);when(organization.require(any())).thenAnswer(c->{var input=c.<DccDirectSupervisorSource.Context>getArgument(0);return new DccDirectSupervisorSource.Evidence(input.tenantId(),input.requesterId(),99L,"ISOLATED_AUTHORITATIVE_PORT","ic29-private-source","v1");});
        var departments=mock(cn.iocoder.yudao.module.system.api.dept.DeptApi.class);when(departments.getDeptList(any())).thenReturn(List.of(new cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO().setId(51L).setStatus(0).setName("正式会签部门").setLeaderUserId(99L)));
        var resolver=new DccControlledFileApprovalRouteAssigneeResolver();wire(resolver,"deptApi",departments,"adminUserApi",users);
        var readiness=new DccControlledFileRouteReadinessService();var permissionsPort=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);when(permissionsPort.hasAnyPermissions(anyLong(),any(String[].class))).thenReturn(true);
        var authorization=mock(DccElectronicSignatureAuthorizationService.class);when(authorization.getAuthorizationMap(any())).thenAnswer(c->{var map=new HashMap<Long,Boolean>();c.<Collection<Long>>getArgument(0).forEach(id->map.put(id,true));return map;});
        var images=mock(DccElectronicSignatureImageService.class);when(images.requireActiveSnapshot(anyLong())).thenReturn(DccElectronicSignatureImageSnapshot.builder().imageId(10L).fileId(10L).verifiedStatus("VALID").build());wire(readiness,"adminUserApi",users,"permissionApi",permissionsPort,"signatureAuthorizationService",authorization,"signatureImageService",images);
        preparationEngine=org.flowable.engine.ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration().setJdbcUrl("jdbc:h2:mem:ic29_prepare_engine_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1").setDatabaseSchemaUpdate("true").setAsyncExecutorActivate(false).buildProcessEngine();
        Path root=Path.of("").toAbsolutePath();while(root!=null&&!Files.isRegularFile(root.resolve("AGENTS.md")))root=root.getParent();var deployment=preparationEngine.getRepositoryService().createDeployment().tenantId("1").addString("actual-obsolete.bpmn20.xml",Files.readString(root.resolve("doc/tasks/20261011-dcc-w05-approval-signatures/bpmn-v5/obsolete.bpmn20.xml"))).deploy();var definition=preparationEngine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        var definitions=mock(cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService.class);preparedDefinitions=definitions;when(definitions.getActiveProcessDefinition(DccControlledFileProcessDefinitionKeys.OBSOLETE)).thenReturn(definition);when(definitions.getProcessDefinition(anyString())).thenAnswer(c->preparationEngine.getRepositoryService().createProcessDefinitionQuery().processDefinitionId(c.getArgument(0)).singleResult());when(definitions.getProcessDefinitionBpmnModel(anyString())).thenAnswer(c->preparationEngine.getRepositoryService().getBpmnModel(c.getArgument(0)));
        var contracts=new DccApplicationExecutionContractService();wire(contracts,"definitions",definitions);wire(source,"routeResolver",resolver,"readiness",readiness,"executionContracts",contracts);
    }
    DccControlledFileObsoleteReqVO prepareRequest(FormInstanceRespVO form){var preview=phase(phaseRequest(form,"PREVIEW"));var r=phaseRequest(form,"SAVE");r.setPrepareExecution(true);r.setExpectedApprovalRouteId(preview.getApprovalMatrixAfter().getRouteId());r.setExpectedApprovalRouteVersionNo(preview.getApprovalMatrixAfter().getRouteVersionNo());r.setExpectedApprovalMatrixDigest(preview.getApprovalMatrixAfter().getMatrixDigest());r.setApprovalMatrixBaselineToken(preview.getApprovalMatrixBaselineToken());r.setApprovalMatrixConfirmationToken(preview.getApprovalMatrixConfirmationToken());r.setMatrixChangeConfirmed(preview.isApprovalMatrixChanged());return r;}
    @Test void preparedNumericDraftKeepsOriginalSourceAndExactProofAcrossSamePayloadTrueSave()throws Exception{
        currentMatrix(1);authoritativePreparationPorts();var form=open(request("OPEN-SAVE"));int number=ownRound(form);jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(d2));var r=prepareRequest(form);var prepared=phase(r);assertTrue(prepared.isPreparationReady());assertNull(prepared.getUnavailableReason());
        var json=JsonUtils.parseObject(forms.selectById(form.getId()).getFormDataJson(),Map.class);var proof=(Map<String,Object>)json.get("dccExecutionPreparation");assertEquals("DCC_OBSOLETE_FORM_PREPARATION_V2",proof.get("formatVersion"));assertEquals("RESERVED_FORM_DRAFT",proof.get("attributeState"));assertEquals(number,proof.get("attributeRound"));assertEquals(attributes.encode(d1),proof.get("defaultSourceJson"));assertEquals(attributes.encode(actual),proof.get("actualAttributesJson"));assertNotNull(proof.get("attributeDigest"));
        String proofBefore=JsonUtils.toJsonString(proof);currentMatrix(2);clearInvocations(preparedDefinitions);assertTrue(phase(r).isPreparationReady());assertEquals(proofBefore,JsonUtils.toJsonString(JsonUtils.parseObject(forms.selectById(form.getId()).getFormDataJson(),Map.class).get("dccExecutionPreparation")));verify(preparedDefinitions,never()).getActiveProcessDefinition(anyString());assertEquals(0,preparationEngine.getRuntimeService().createProcessInstanceQuery().count());assertEquals("original-upload-round",files.selectById(20L).getProcessInstanceId());
    }
    @Test void falseSavePreservesUnchangedProofButChangesInvalidateOnlyCurrentDraftProof()throws Exception{
        currentMatrix(1);authoritativePreparationPorts();var form=open(request("OPEN-SAVE"));phase(prepareRequest(form));var preparedForm=forms.selectById(form.getId());var before=JsonUtils.parseObject(preparedForm.getFormDataJson(),Map.class);String originalProof=JsonUtils.toJsonString(before.get("dccExecutionPreparation"));long count=formSnapshots.selectCount(null);
        var r=phaseRequest(form,"SAVE");r.setPrepareExecution(false);clearInvocations(preparedDefinitions);assertTrue(phase(r).isPreparationReady());assertEquals(originalProof,JsonUtils.toJsonString(JsonUtils.parseObject(forms.selectById(form.getId()).getFormDataJson(),Map.class).get("dccExecutionPreparation")));verify(preparedDefinitions,never()).getActiveProcessDefinition(anyString());
        r.setReason("显式变更后必须重新准备");r.setProjectAttributes(d2);var changed=phase(r);assertFalse(changed.isPreparationReady());assertEquals("DCC_OBSOLETE_EXECUTION_PREPARATION_REQUIRED",changed.getUnavailableReason());assertFalse(JsonUtils.parseObject(forms.selectById(form.getId()).getFormDataJson(),Map.class).containsKey("dccExecutionPreparation"));assertEquals(d1,changed.getDefaultSource());assertEquals(d2,changed.getActualAttributes());
        assertTrue(formSnapshots.selectList(null).stream().anyMatch(row->row.getFormDataJson()!=null&&row.getFormDataJson().equals(preparedForm.getFormDataJson())));assertEquals(count+2,formSnapshots.selectCount(null));assertEquals("original-upload-round",files.selectById(20L).getProcessInstanceId());
    }
    @Test void everyUnpreparedDraftResponseProjectsExplicitFalseAndActualSavedInputs(){
        var r=request("OPEN-SAVE");var opened=(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileObsoleteApplicationRespVO)open(r);
        for(var response:List.of(opened,getDraft())){assertEquals(Boolean.FALSE,response.isApprovalMatrixChanged());assertFalse(response.isPreparationReady());assertEquals("DCC_OBSOLETE_EXECUTION_PREPARATION_REQUIRED",response.getUnavailableReason());assertEquals(r.getReason(),response.getReason());assertEquals(r.getIdempotencyKey(),response.getIdempotencyKey());assertEquals(List.of(51L),response.getSelectedSignoffDepartmentIds());assertEquals(actual,response.getActualAttributes());}
    }
    @Test void explicitUnselectedDepartmentsAreSavedAsEmptyButMissingArrayIsRejected(){
        var r=request("OPEN-SAVE");r.setSelectedSignoffDepartmentIds(null);assertThrows(RuntimeException.class,()->open(r));assertEquals(0L,forms.selectCount(null));
        r.setSelectedSignoffDepartmentIds(List.of());open(r);assertEquals(List.of(),getDraft().getSelectedSignoffDepartmentIds());
    }
    @Test void publicOpenRejectsInitiallyActiveSourceThatIsActuallyObsoleteWhenReservedAndRollsEverythingBack(){
        var sourceBefore=files.selectById(20L);var actualCenter=spy(formCenter);
        doAnswer(c->{var created=c.callRealMethod();jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE' WHERE id=20 AND tenant_id=1");return created;}).when(actualCenter).createInstance(any(),eq(99L));
        wire(obsolete,"formCenterRuntimeService",actualCenter);var error=assertThrows(IllegalArgumentException.class,()->open(request("OPEN-LOCKED-STATE")));assertTrue(error.getMessage().contains("受控File"));
        assertEquals(sourceBefore,files.selectById(20L));assertEquals(0L,forms.selectCount(null));assertEquals(0L,formSnapshots.selectCount(null));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link WHERE form_instance_id IS NOT NULL",Integer.class));assertEquals(0L,attributesRows.selectCount(null));verify(actualCenter,never()).submitInstance(any(),any(),any());
    }
    @Test void previewReadsActualRawBeforeAfterTokensWithoutAnyFormAttributeOrSourceWrites()throws Exception{
        currentMatrix(1);var form=open(request("OPEN-SAVE"));var formBefore=forms.selectById(form.getId());var attrsBefore=attributesRows.find("OBSOLETE",20L,ownRound(form));var fileBefore=files.selectById(20L);var r=phaseRequest(form,"PREVIEW");
        var first=phase(r);assertEquals(Boolean.FALSE,first.isApprovalMatrixChanged());assertNotNull(first.getApprovalMatrixBaselineToken());assertEquals(31L,first.getApprovalMatrixBefore().getRouteId());assertFalse(first.isPreparationReady());assertEquals("DCC_OBSOLETE_SUPERVISOR_SOURCE_NOT_READY",first.getUnavailableReason());
        currentMatrix(2);r.setApprovalMatrixBaselineToken(first.getApprovalMatrixBaselineToken());var changed=phase(r);assertEquals(Boolean.TRUE,changed.isApprovalMatrixChanged());assertEquals(31L,changed.getApprovalMatrixBefore().getRouteId());assertEquals(32L,changed.getApprovalMatrixAfter().getRouteId());assertNotNull(changed.getApprovalMatrixConfirmationToken());
        assertEquals(formBefore,forms.selectById(form.getId()));assertEquals(attrsBefore,attributesRows.find("OBSOLETE",20L,ownRound(form)));assertEquals(fileBefore,files.selectById(20L));assertEquals(1L,formSnapshots.selectCount(null));verify(supervisorProvider,atLeastOnce()).getIfAvailable();
    }
    @Test void trueSaveWithAbsentSupervisorRollsBackActualDraftAttributeAndSnapshotWrites()throws Exception{
        currentMatrix(1);var form=open(request("OPEN-SAVE"));var formBefore=forms.selectById(form.getId());var attrsBefore=attributesRows.find("OBSOLETE",20L,ownRound(form));var r=phaseRequest(form,"SAVE");r.setPrepareExecution(true);r.setReason("不能丢掉原草稿的晚失败原因");r.setProjectAttributes(d2);
        var error=assertThrows(IllegalStateException.class,()->phase(r));assertEquals("DCC_OBSOLETE_SUPERVISOR_SOURCE_NOT_READY",error.getMessage());assertEquals(formBefore,forms.selectById(form.getId()));assertEquals(attrsBefore,attributesRows.find("OBSOLETE",20L,ownRound(form)));assertEquals(1L,formSnapshots.selectCount(null));
    }
    @Test void getDraftReturnsOnlyActualSavedFactsAcrossReopenWithoutWritingOrProvider(){
        assertNull(getDraft());var form=open(request("OPEN-SAVE"));var before=forms.selectById(form.getId());var saved=attributesRows.find("OBSOLETE",20L,ownRound(form));
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(d2));var reopened=getDraft();assertNotNull(reopened);
        assertEquals(before,forms.selectById(form.getId()));assertEquals(saved,attributesRows.find("OBSOLETE",20L,ownRound(form)));assertEquals(1L,formSnapshots.selectCount(null));
    }
    @Test void saveFalsePersistsRealReasonDepartmentsAndActualWithOriginalSourceAndNoProvider(){
        var form=open(request("OPEN-SAVE"));var sourceBefore=JsonUtils.toJsonString(files.selectById(20L));jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(d2));
        var r=phaseRequest(form,"SAVE");r.setReason("本次已修改真实原因");r.setSelectedSignoffDepartmentIds(List.of(52L,53L));r.setProjectAttributes(d2);
        r.setPrepareExecution(false);
        var saved=tx().execute(s->obsolete.obsoleteControlledFile(99L,20L,r));assertEquals(form.getId(),saved.getId());assertEquals("DRAFT",saved.getStatus());assertNull(saved.getBpmProcessInstanceId());
        var json=JsonUtils.parseObject(forms.selectById(form.getId()).getFormDataJson(),Map.class);assertEquals(r.getReason(),json.get("reason"));assertEquals(List.of(52,53),json.get("dccSignoffDepartmentIds"));
        var numeric=attributesRows.find("OBSOLETE",20L,ownRound(form));assertEquals(d1,attributes.readValue(numeric.getDefaultSourceJson()));assertEquals(d2,attributes.readValue(numeric.getActualAttributesJson()));assertFalse(numeric.getSubmitted());assertNotNull(getDraft());assertEquals(sourceBefore,JsonUtils.toJsonString(files.selectById(20L)));
    }
    @Test void getDoesNotChooseFirstOfTwoDraftsAndWrongActorCannotReadAnotherSavedForm(){
        var form=open(request("OPEN-SAVE"));var duplicate=forms.selectById(form.getId());duplicate.setId(null);duplicate.setInstanceCode("another-real-code");duplicate.setIdempotencyKey("another-key");forms.insert(duplicate);
        assertThrows(RuntimeException.class,this::getDraft);forms.deleteById(duplicate.getId());SecurityFrameworkUtils.setLoginUser(new LoginUser().setId(88L).setTenantId(1L).setUserType(2),new org.springframework.mock.web.MockHttpServletRequest());
        assertThrows(RuntimeException.class,this::getDraft);
    }
    @Test void openActualFormCapturesDefaultsAndActualThenReopenIgnoresChangedProjectDefaults(){
        var sourceBefore=JsonUtils.toJsonString(files.selectById(20L));var request=request("OPEN-ONE");var draft=open(request);assertEquals("DRAFT",draft.getStatus());assertNull(draft.getBpmProcessInstanceId());
        int number=ownRound(draft);var saved=attributes.readSaved(99L,5L,"OBSOLETE",20L,number);assertEquals(d1,attributes.readValue(saved.getDefaultSourceJson()));assertEquals(actual,attributes.readValue(saved.getActualAttributesJson()));assertFalse(saved.getSubmitted());
        jdbc.update("UPDATE dcc_project_code SET default_attributes_json=? WHERE id=5",attributes.encode(d2));String attrsBefore=JsonUtils.toJsonString(saved);var reopened=open(request);
        assertEquals(draft.getId(),reopened.getId());assertEquals(number,ownRound(reopened));assertEquals(attrsBefore,JsonUtils.toJsonString(attributesRows.find("OBSOLETE",20L,number)));assertEquals(1L,forms.selectCount(null));assertEquals(1L,formSnapshots.selectCount(null));
        assertEquals(sourceBefore,JsonUtils.toJsonString(files.selectById(20L)));
    }
    @Test void noProviderIsNeededToOpenAndNativeDraftRoundIsNeverReused(){
        int nativeNumber=tx().execute(s->rounds.reserveDraft(5L,"UPLOAD",20L));var form=open(request("OPEN-WITH-NATIVE"));int obsoleteNumber=ownRound(form);
        assertEquals(nativeNumber,rounds.requireDraft("UPLOAD",20L));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link WHERE form_instance_id=? AND application_type='OBSOLETE'",Integer.class,form.getId()));
        assertNull(forms.selectById(form.getId()).getBpmProcessInstanceId());assertNotNull(attributesRows.find("OBSOLETE",20L,obsoleteNumber));
    }
    @ParameterizedTest @ValueSource(strings={"ACTUAL","REASON","DEPARTMENTS","KEY"})
    void openIdempotentReplayCannotEditPreviousDraftOrBorrowDifferentPayload(String conflict){
        var draft=open(request("SAME-OPEN"));var formBefore=forms.selectById(draft.getId());var attrsBefore=attributesRows.find("OBSOLETE",20L,ownRound(draft));
        var changed=request("SAME-OPEN");switch(conflict){case "ACTUAL"->changed.setProjectAttributes(d2);case "REASON"->changed.setReason("另一原因");case "DEPARTMENTS"->changed.setSelectedSignoffDepartmentIds(List.of(52L));case "KEY"->changed.setIdempotencyKey("DIFFERENT-OPEN");}
        assertThrows(RuntimeException.class,()->open(changed));assertEquals(formBefore,forms.selectById(draft.getId()));assertEquals(attrsBefore,attributesRows.find("OBSOLETE",20L,ownRound(draft)));assertEquals(1L,forms.selectCount(null));
    }
    @ParameterizedTest @ValueSource(strings={"OUTER","ATTRIBUTES","RESERVATION"})
    void lateOpenFailureRollsRealFormSnapshotReservationAndSavedAttributesBack(String stage){
        var req=request("ROLLBACK-OPEN");if(stage.equals("ATTRIBUTES"))req.setProjectAttributes(new DccProjectAttributes(List.of(),null,"Y","N","N",null));
        if(stage.equals("RESERVATION"))jdbc.update("UPDATE dcc_controlled_file_master SET file_type_taxonomy_leaf_id=7 WHERE id=10");
        assertThrows(RuntimeException.class,()->tx().execute(s->{var created=ReflectionTestUtils.<FormInstanceRespVO>invokeMethod(obsolete,"openObsoleteDraft",99L,20L,req);if(stage.equals("OUTER"))throw new IllegalStateException("late actual caller failure");return created;}));
        assertEquals(0L,forms.selectCount(null));assertEquals(0L,formSnapshots.selectCount(null));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link WHERE form_instance_id IS NOT NULL",Integer.class));assertEquals(0L,attributesRows.selectCount(null));
    }
}
