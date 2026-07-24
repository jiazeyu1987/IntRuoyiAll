package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrOperationAuditRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchRecordFormPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrProcessFormPermissionRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrProcessFormPermissionRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrProcessFormPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPermissionRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPermissionScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessBatchRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteFlowProcessConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrProcessFormPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPermissionRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrPermissionScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport.MesProBatchRecordReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessBatchRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteFlowProcessConfigMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.SystemEntitlementSyncReqDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteProcessService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_CANDIDATE_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_ROUTE_BINDING_MISSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Import({MesProEdhrProcessFormPermissionRuleServiceImpl.class, MesProEdhrPermissionScopeServiceImpl.class})
class MesProEdhrProcessFormPermissionRuleServiceImplTest extends BaseDbUnitTest {

    @Resource
    private MesProEdhrProcessFormPermissionRuleService processFormPermissionRuleService;
    @Resource
    private MesProEdhrProcessFormPermissionRuleMapper processFormPermissionRuleMapper;
    @Resource
    private MesProRouteFlowProcessBatchRecordMapper routeFlowProcessBatchRecordMapper;
    @Resource
    private MesProRouteFlowConfigMapper routeFlowConfigMapper;
    @Resource
    private MesProRouteFlowProcessConfigMapper routeFlowProcessConfigMapper;
    @Resource
    private MesProEdhrPermissionScopeMapper permissionScopeMapper;
    @Resource
    private MesProEdhrPermissionRuleMapper permissionRuleMapper;
    @Resource
    private MesProBatchRecordReportMapper batchRecordReportMapper;
    @Resource
    private DataSource dataSource;

    @MockitoBean
    private AdminUserApi adminUserApi;
    @MockitoBean
    private PermissionApi permissionApi;
    @MockitoBean
    private DeptApi deptApi;
    @MockitoBean
    private MesProEdhrOperationAuditService operationAuditService;
    @MockitoBean
    private MesProRouteProcessService routeProcessService;
    @MockitoBean
    private MesProEdhrWorkTaskService workTaskService;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(122L);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(
                "ALTER TABLE mes_pro_route_flow_process_batch_record ADD COLUMN IF NOT EXISTS form_slot_type VARCHAR(32)");
        jdbcTemplate.execute(
                "ALTER TABLE mes_pro_route_flow_process_batch_record ADD COLUMN IF NOT EXISTS required_policy VARCHAR(32)");
        jdbcTemplate.execute(
                "ALTER TABLE mes_pro_route_flow_process_batch_record ADD COLUMN IF NOT EXISTS required_condition_json CLOB");
        jdbcTemplate.execute(
                "ALTER TABLE mes_pro_route_flow_process_batch_record ADD COLUMN IF NOT EXISTS owner_role_key VARCHAR(64)");
        jdbcTemplate.execute(
                "ALTER TABLE mes_pro_route_flow_process_batch_record ADD COLUMN IF NOT EXISTS archive_visibility VARCHAR(32)");
        jdbcTemplate.execute(
                "ALTER TABLE mes_pro_route_flow_process_batch_record ADD COLUMN IF NOT EXISTS slot_config_snapshot_hash VARCHAR(128)");
        when(operationAuditService.record(any(MesProEdhrOperationAuditCommand.class)))
                .thenReturn(new MesProEdhrOperationAuditRespVO().setId(9001L));
    }

    @Test
    void saveRule_persistsSubmittedSignatureRulesForOrdinaryProcessForm() {
        insertRouteBatchRecord(8801L, 5101L, "REPORT-001", null, 88001L, 88011L);
        when(adminUserApi.getUserList(List.of(101L, 102L))).thenReturn(List.of(
                adminUser(101L, "张三", CommonStatusEnum.ENABLE.getStatus()),
                adminUser(102L, "李四", CommonStatusEnum.ENABLE.getStatus())));
        when(adminUserApi.getUserList(List.of(301L))).thenReturn(List.of(
                adminUser(301L, "批准人", CommonStatusEnum.ENABLE.getStatus())));
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(7001L))).thenReturn(Set.of(201L, 202L));
        when(adminUserApi.getUserList(Set.of(201L, 202L))).thenReturn(List.of(
                adminUser(201L, "审批甲", CommonStatusEnum.ENABLE.getStatus()),
                adminUser(202L, "审批乙", CommonStatusEnum.DISABLE.getStatus())));

        MesProEdhrProcessFormPermissionRuleRespVO saved;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            saved = processFormPermissionRuleService.saveRule(new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                    .setRouteProcessId(5101L)
                    .setBatchRecordReportId("REPORT-001")
                    .setFillRule(candidateRule("USERS", List.of(101L, 102L), "ANY_ONE", 120, true, "填写候选"))
                    .setSignatureRules(List.of(
                            signatureRule("R1C1", "APPROVAL", candidateRule("ROLE", List.of(7001L), "ANY_ONE", 60, true, "审批位")),
                            signatureRule("R2C1", "APPROVE", candidateRule("USERS", List.of(301L), "ALL", 90, true, "批准位")))));
        }

        assertEquals("CONFIGURED", saved.getFillRuleStatus());
        assertEquals("CONFIGURED", saved.getSignatureRuleStatus());
        assertEquals(2, saved.getSignatureRules().size());
        assertEquals("R1C1", saved.getSignatureRules().get(0).getSignatureCellKey());
        assertEquals("APPROVAL", saved.getSignatureRules().get(0).getSignatureRole());
        assertEquals(List.of(7001L), saved.getSignatureRules().get(0).getRule().getCandidateSourceIds());
        assertEquals(Integer.MAX_VALUE, saved.getSignatureRules().get(0).getRule().getDueMinutes());
        assertEquals(1, saved.getSignatureRules().get(0).getRule().getCandidateUsers().size());
        assertEquals("审批甲", saved.getSignatureRules().get(0).getRule().getCandidateUsers().get(0).getDisplayName());
        assertEquals("R2C1", saved.getSignatureRules().get(1).getSignatureCellKey());
        assertEquals("APPROVE", saved.getSignatureRules().get(1).getSignatureRole());
        assertEquals(List.of(301L), saved.getSignatureRules().get(1).getRule().getCandidateSourceIds());
        List<MesProEdhrProcessFormPermissionRuleDO> persisted =
                processFormPermissionRuleMapper.selectListByRouteProcessAndReport(5101L, "REPORT-001");
        assertEquals(3, persisted.size());
        assertTrue(persisted.stream().anyMatch(rule -> "FILL".equals(rule.getRuleType())));
        assertTrue(persisted.stream().anyMatch(rule -> "SIGNATURE".equals(rule.getRuleType())
                && "R1C1".equals(rule.getSignatureCellKey())
                && "APPROVAL".equals(rule.getSignatureRole())
                && "ROLE".equals(rule.getCandidateSourceType())
                && "7001".equals(rule.getCandidateSourceIds())));
        assertTrue(persisted.stream().anyMatch(rule -> "SIGNATURE".equals(rule.getRuleType())
                && "R2C1".equals(rule.getSignatureCellKey())
                && "APPROVE".equals(rule.getSignatureRole())
                && "USERS".equals(rule.getCandidateSourceType())
                && "301".equals(rule.getCandidateSourceIds())));

        MesProEdhrProcessFormPermissionRuleRespVO queried =
                processFormPermissionRuleService.getRule(5101L, "REPORT-001");

        assertEquals("CONFIGURED", queried.getFillRuleStatus());
        assertNotNull(queried.getFillRule());
        assertEquals(List.of(101L, 102L), queried.getFillRule().getCandidateSourceIds());
        assertEquals(2, queried.getFillRule().getCandidateUsers().size());
        assertEquals("张三", queried.getFillRule().getCandidateUsers().get(0).getDisplayName());
        assertEquals("CONFIGURED", queried.getSignatureRuleStatus());
        assertEquals(2, queried.getSignatureRules().size());
        assertEquals("R1C1", queried.getSignatureRules().get(0).getSignatureCellKey());
        assertEquals("审批甲", queried.getSignatureRules().get(0).getRule().getCandidateUsers().get(0).getDisplayName());
        assertNotNull(queried.getPermissionScopeId());
    }

    @Test
    void saveRule_autoCreatesPermissionScopeAndBindsRouteBatchRecord() {
        insertRouteBatchRecord(8802L, 5201L, "REPORT-010", null, 88002L, 88012L);
        when(adminUserApi.getUserList(List.of(101L))).thenReturn(List.of(
                adminUser(101L, "填写人", CommonStatusEnum.ENABLE.getStatus())));
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(7001L))).thenReturn(Set.of(201L));
        when(adminUserApi.getUserList(Set.of(201L))).thenReturn(List.of(
                adminUser(201L, "审批人", CommonStatusEnum.ENABLE.getStatus())));

        MesProEdhrProcessFormPermissionRuleRespVO saved;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            saved = processFormPermissionRuleService.saveRule(
                    new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                            .setRouteProcessId(5201L)
                            .setBatchRecordReportId("REPORT-010")
                            .setFillRule(candidateRule("USER", List.of(101L), "ANY_ONE", 120, true, "填写"))
                            .setSignatureRules(List.of(signatureRule("R1C1", "APPROVAL",
                                    candidateRule("ROLE", List.of(7001L), "ANY_ONE", 60, true, "审批")))));
        }

        assertNotNull(saved.getPermissionScopeId());
        MesProRouteFlowProcessBatchRecordDO bound = routeFlowProcessBatchRecordMapper.selectById(8802L);
        assertEquals(saved.getPermissionScopeId(), bound.getPermissionScopeId());
        MesProEdhrPermissionScopeDO scope = permissionScopeMapper.selectById(saved.getPermissionScopeId());
        assertEquals("ROUTE_PROCESS_BATCH_RECORD", scope.getObjectType());
        assertEquals("5201|REPORT-010", scope.getObjectId());
        List<MesProEdhrPermissionRuleDO> rules = permissionRuleMapper.selectListByScopeId(saved.getPermissionScopeId());
        assertTrue(rules.stream().anyMatch(rule -> "USER".equals(rule.getSubjectType())
                && Long.valueOf(101L).equals(rule.getSubjectId()) && "FILL".equals(rule.getAbility())));
        assertFalse(rules.stream().anyMatch(rule -> "SIGN".equals(rule.getAbility())
                || "APPROVE".equals(rule.getAbility())));
    }

    @Test
    void saveRule_persistsSingleBatchRecordFillRuleAndRemovesLegacyFillerColumns() {
        insertRouteBatchRecord(8803L, 5301L, "REPORT-020", null, 88003L, 88013L);
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(7101L))).thenReturn(Set.of(101L, 102L));
        when(adminUserApi.getUserList(Set.of(101L, 102L))).thenReturn(List.of(
                adminUser(101L, "角色填写甲", CommonStatusEnum.ENABLE.getStatus()),
                adminUser(102L, "角色填写乙", CommonStatusEnum.ENABLE.getStatus())));

        assertThrows(NoSuchMethodException.class,
                () -> MesProEdhrProcessFormPermissionRuleSaveReqVO.class.getDeclaredMethod("getEquipmentFillRule"));
        assertThrows(NoSuchMethodException.class,
                () -> MesProEdhrProcessFormPermissionRuleSaveReqVO.class.getDeclaredMethod("getQualityFillRule"));
        assertThrows(NoSuchMethodException.class,
                () -> MesProEdhrProcessFormPermissionRuleRespVO.class.getDeclaredMethod("getEquipmentFillRule"));
        assertThrows(NoSuchMethodException.class,
                () -> MesProEdhrProcessFormPermissionRuleRespVO.class.getDeclaredMethod("getQualityFillRule"));

        MesProEdhrProcessFormPermissionRuleRespVO saved;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            saved = processFormPermissionRuleService.saveRule(
                    new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                            .setRouteProcessId(5301L)
                            .setBatchRecordReportId("REPORT-020")
                            .setFillRule(candidateRule("ROLE", List.of(7101L), "ANY_ONE", 120, true, "填写人")));
        }

        assertEquals("CONFIGURED", saved.getFillRuleStatus());
        assertEquals(List.of(7101L), saved.getFillRule().getCandidateSourceIds());
        assertEquals("角色填写甲", saved.getFillRule().getCandidateUsers().get(0).getDisplayName());

        List<MesProEdhrProcessFormPermissionRuleDO> persisted =
                processFormPermissionRuleMapper.selectListByRouteProcessAndReport(5301L, "REPORT-020");
        assertEquals(1, persisted.size());
        assertTrue(persisted.stream().anyMatch(rule -> "FILL".equals(rule.getRuleType())));

        List<MesProEdhrPermissionRuleDO> permissionRules =
                permissionRuleMapper.selectListByScopeId(saved.getPermissionScopeId());
        assertTrue(permissionRules.stream().anyMatch(rule -> "ROLE".equals(rule.getSubjectType())
                && Long.valueOf(7101L).equals(rule.getSubjectId()) && "FILL".equals(rule.getAbility())));
        assertFalse(permissionRules.stream().anyMatch(rule -> "EQUIPMENT_FILL".equals(rule.getAbility())
                || "QUALITY_FILL".equals(rule.getAbility())));

        MesProEdhrProcessFormPermissionRuleRespVO queried =
                processFormPermissionRuleService.getRule(5301L, "REPORT-020");
        assertEquals("角色填写乙", queried.getFillRule().getCandidateUsers().get(1).getDisplayName());
    }

    @Test
    void saveRule_replacesExistingRulesWithoutSoftDeleteUniqueConflict() {
        insertRouteBatchRecord(8805L, 5501L, "REPORT-040", null, 88005L, 88015L);
        when(adminUserApi.getUserList(List.of(101L))).thenReturn(List.of(
                adminUser(101L, "生产旧", CommonStatusEnum.ENABLE.getStatus())));
        when(adminUserApi.getUserList(List.of(102L))).thenReturn(List.of(
                adminUser(102L, "生产新", CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            processFormPermissionRuleService.saveRule(
                    new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                            .setRouteProcessId(5501L)
                            .setBatchRecordReportId("REPORT-040")
                            .setFillRule(candidateRule("USER", List.of(101L), "ANY_ONE", 120, true, "旧填写人")));

            MesProEdhrProcessFormPermissionRuleRespVO saved = processFormPermissionRuleService.saveRule(
                    new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                            .setRouteProcessId(5501L)
                            .setBatchRecordReportId("REPORT-040")
                            .setFillRule(candidateRule("USER", List.of(102L), "ANY_ONE", 120, true, "新填写人")));

            assertEquals(List.of(102L), saved.getFillRule().getCandidateSourceIds());
        }

        List<MesProEdhrProcessFormPermissionRuleDO> persisted =
                processFormPermissionRuleMapper.selectListByRouteProcessAndReport(5501L, "REPORT-040");
        assertEquals(1, persisted.size());
        assertTrue(persisted.stream().allMatch(rule -> Boolean.TRUE.equals(rule.getEnabled())));
        assertTrue(persisted.stream().anyMatch(rule -> "FILL".equals(rule.getRuleType())
                && "102".equals(rule.getCandidateSourceIds())));
        assertEquals(1L, new JdbcTemplate(dataSource).queryForObject("""
                SELECT COUNT(*)
                FROM mes_pro_edhr_process_form_permission_rule
                WHERE route_process_id = 5501
                  AND batch_record_report_id = 'REPORT-040'
                """, Long.class));
    }

    @Test
    void saveRule_syncsRouteLevelFillerEntitlementWithStableSourceKey() {
        insertRouteBatchRecord(8821L, 5821L, "REPORT-ENT-ROUTE", null, 88001L, 88002L);
        when(adminUserApi.getUserList(List.of(501L, 502L))).thenReturn(List.of(
                adminUser(501L, "limin", CommonStatusEnum.ENABLE.getStatus()),
                adminUser(502L, "jiazeyu", CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            processFormPermissionRuleService.saveRule(
                    new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                            .setRouteProcessId(5821L)
                            .setBatchRecordReportId("REPORT-ENT-ROUTE")
                            .setFillRule(candidateRule("USERS", List.of(501L, 502L), "ANY_ONE", 120,
                                    true, "路线级填写人")));
        }

        ArgumentCaptor<SystemEntitlementSyncReqDTO> captor =
                ArgumentCaptor.forClass(SystemEntitlementSyncReqDTO.class);
        verify(permissionApi).syncEntitlementClaims(captor.capture());
        SystemEntitlementSyncReqDTO request = captor.getValue();
        assertEquals(122L, request.getTenantId());
        assertEquals("EDHR_PROCESS_FORM_FILLER", request.getSourceType());
        assertEquals("ROUTE|5821|REPORT-ENT-ROUTE|88002", request.getSourceKey());
        assertEquals("88002", request.getSourceVersion());
        assertEquals("MES_EDHR_FILLER_MINIMAL", request.getPolicyCode());
        assertEquals(Set.of(501L, 502L), request.getResolvedUserIds());
        assertEquals(113L, request.getOperatorUserId());
        assertEquals("aoteman", request.getOperatorUsername());
        assertTrue(request.getSourceDigest().contains("candidateSourceType=USERS"));
        assertTrue(request.getSourceDigest().contains("candidateSourceIds=[501, 502]"));
        assertTrue(request.getSourceDigest().contains("completionPolicy=ANY_ONE"));
    }

    @Test
    void saveRule_rejectsRouteLevelFillerEntitlementWhenBatchRecordVersionMissing() {
        insertRouteBatchRecord(8831L, 5831L, "REPORT-ENT-NO-VERSION", null);
        when(adminUserApi.getUserList(List.of(531L))).thenReturn(List.of(
                adminUser(531L, "缺版本填写人", CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            assertThrows(ServiceException.class, () -> processFormPermissionRuleService.saveRule(
                    new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                            .setRouteProcessId(5831L)
                            .setBatchRecordReportId("REPORT-ENT-NO-VERSION")
                            .setFillRule(candidateRule("USER", List.of(531L), "ANY_ONE", 120,
                                    true, "缺版本不得同步权益"))));
        }

        verify(permissionApi, never()).syncEntitlementClaims(any(SystemEntitlementSyncReqDTO.class));
        assertTrue(processFormPermissionRuleMapper.selectListByRouteProcessAndReport(
                5831L, "REPORT-ENT-NO-VERSION").isEmpty());
    }

    @Test
    void saveRule_reconcilesActiveWorkTaskOwnershipWithRouteSourceKey() {
        insertRouteBatchRecord(8824L, 5824L, "REPORT-OWN-ROUTE", null, 89201L, 89202L);
        when(adminUserApi.getUserList(List.of(511L))).thenReturn(List.of(
                adminUser(511L, "jiazeyu", CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            processFormPermissionRuleService.saveRule(
                    new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                            .setRouteProcessId(5824L)
                            .setBatchRecordReportId("REPORT-OWN-ROUTE")
                            .setFillRule(candidateRule("USERS", List.of(511L), "ANY_ONE", 120,
                                    true, "路线级填写人换人")));
        }

        ArgumentCaptor<MesProEdhrProcessFormPermissionRuleDO> ruleCaptor =
                ArgumentCaptor.forClass(MesProEdhrProcessFormPermissionRuleDO.class);
        verify(workTaskService).reconcileProcessFormFillTaskOwnership(
                eq("ROUTE|5824|REPORT-OWN-ROUTE|89202"),
                ruleCaptor.capture(),
                eq("填写人配置变更"));
        MesProEdhrProcessFormPermissionRuleDO rule = ruleCaptor.getValue();
        assertEquals(5824L, rule.getRouteProcessId());
        assertEquals("REPORT-OWN-ROUTE", rule.getBatchRecordReportId());
        assertEquals(89202L, rule.getBatchRecordVersionId());
        assertEquals("511", rule.getCandidateSourceIds());
    }

    @Test
    void saveRuleByReport_syncsFormLevelFillerEntitlementWithStableSourceKey() {
        insertRouteBatchRecord(8822L, 5822L, "REPORT-ENT-FORM", null, 89001L, 89002L);
        when(adminUserApi.getUserList(List.of(503L))).thenReturn(List.of(
                adminUser(503L, "表单填写人", CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            processFormPermissionRuleService.saveRuleByReport(
                    new MesProEdhrBatchRecordFormPermissionRuleSaveReqVO()
                            .setBatchRecordReportId("REPORT-ENT-FORM")
                            .setFillRule(candidateRule("USERS", List.of(503L), "ANY_ONE", null,
                                    true, "表单级填写人")));
        }

        ArgumentCaptor<SystemEntitlementSyncReqDTO> captor =
                ArgumentCaptor.forClass(SystemEntitlementSyncReqDTO.class);
        verify(permissionApi).syncEntitlementClaims(captor.capture());
        SystemEntitlementSyncReqDTO request = captor.getValue();
        assertEquals(122L, request.getTenantId());
        assertEquals("EDHR_PROCESS_FORM_FILLER", request.getSourceType());
        assertEquals("FORM|REPORT-ENT-FORM|89002", request.getSourceKey());
        assertEquals("89002", request.getSourceVersion());
        assertEquals("MES_EDHR_FILLER_MINIMAL", request.getPolicyCode());
        assertEquals(Set.of(503L), request.getResolvedUserIds());
        assertEquals(113L, request.getOperatorUserId());
        assertEquals("aoteman", request.getOperatorUsername());
    }

    @Test
    void saveRuleByReport_rejectsFormLevelFillerEntitlementWhenReportVersionMissing() {
        when(adminUserApi.getUserList(List.of(533L))).thenReturn(List.of(
                adminUser(533L, "表单缺版本填写人", CommonStatusEnum.ENABLE.getStatus())));

        assertThrows(ServiceException.class, () -> processFormPermissionRuleService.saveRuleByReport(
                new MesProEdhrBatchRecordFormPermissionRuleSaveReqVO()
                        .setBatchRecordReportId("REPORT-FORM-NO-VERSION")
                        .setFillRule(candidateRule("USERS", List.of(533L), "ANY_ONE", null,
                                true, "表单缺版本不得同步权益"))));

        verify(permissionApi, never()).syncEntitlementClaims(any(SystemEntitlementSyncReqDTO.class));
        assertTrue(processFormPermissionRuleMapper.selectListByRouteProcessAndReport(
                0L, "REPORT-FORM-NO-VERSION").isEmpty());
    }

    @Test
    void saveRuleByReport_reconcilesActiveWorkTaskOwnershipWithFormSourceKey() {
        insertRouteBatchRecord(8825L, 5825L, "REPORT-OWN-FORM", null, 89301L, 89302L);
        when(adminUserApi.getUserList(List.of(512L))).thenReturn(List.of(
                adminUser(512L, "jiazeyu", CommonStatusEnum.ENABLE.getStatus())));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            processFormPermissionRuleService.saveRuleByReport(
                    new MesProEdhrBatchRecordFormPermissionRuleSaveReqVO()
                            .setBatchRecordReportId("REPORT-OWN-FORM")
                            .setFillRule(candidateRule("USERS", List.of(512L), "ANY_ONE", null,
                                    true, "表单级填写人换人")));
        }

        ArgumentCaptor<MesProEdhrProcessFormPermissionRuleDO> ruleCaptor =
                ArgumentCaptor.forClass(MesProEdhrProcessFormPermissionRuleDO.class);
        verify(workTaskService).reconcileProcessFormFillTaskOwnership(
                eq("FORM|REPORT-OWN-FORM|89302"),
                ruleCaptor.capture(),
                eq("填写人配置变更"));
        MesProEdhrProcessFormPermissionRuleDO rule = ruleCaptor.getValue();
        assertEquals(0L, rule.getRouteProcessId());
        assertEquals("REPORT-OWN-FORM", rule.getBatchRecordReportId());
        assertEquals(89302L, rule.getBatchRecordVersionId());
        assertEquals("512", rule.getCandidateSourceIds());
    }

    @Test
    void saveRule_rollsBackBusinessRuleWhenEntitlementSyncFails() {
        insertRouteBatchRecord(8823L, 5823L, "REPORT-ENT-ROLLBACK", null, 89101L, 89102L);
        when(adminUserApi.getUserList(List.of(504L))).thenReturn(List.of(
                adminUser(504L, "回滚填写人", CommonStatusEnum.ENABLE.getStatus())));
        doThrow(new IllegalStateException("entitlement policy missing"))
                .when(permissionApi).syncEntitlementClaims(any(SystemEntitlementSyncReqDTO.class));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            assertThrows(IllegalStateException.class, () -> processFormPermissionRuleService.saveRule(
                    new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                            .setRouteProcessId(5823L)
                            .setBatchRecordReportId("REPORT-ENT-ROLLBACK")
                            .setFillRule(candidateRule("USER", List.of(504L), "ANY_ONE", 120,
                                    true, "同步失败回滚"))));
        }

        assertTrue(processFormPermissionRuleMapper.selectListByRouteProcessAndReport(
                5823L, "REPORT-ENT-ROLLBACK").isEmpty());
        assertNull(routeFlowProcessBatchRecordMapper.selectById(8823L).getPermissionScopeId());
    }

    @Test
    void saveRule_defaultsMissingDueMinutesToUnlimited() {
        insertRouteBatchRecord(8804L, 5401L, "REPORT-030", null, 88004L, 88014L);
        when(adminUserApi.getUserList(List.of(101L))).thenReturn(List.of(
                adminUser(101L, "生产填写", CommonStatusEnum.ENABLE.getStatus())));

        MesProEdhrProcessFormPermissionRuleRespVO saved;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            saved = processFormPermissionRuleService.saveRule(
                    new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                            .setRouteProcessId(5401L)
                            .setBatchRecordReportId("REPORT-030")
                            .setFillRule(candidateRule("USER", List.of(101L), "ANY_ONE", null, true, "生产")));
        }

        assertEquals(Integer.MAX_VALUE, saved.getFillRule().getDueMinutes());
        List<MesProEdhrProcessFormPermissionRuleDO> persisted =
                processFormPermissionRuleMapper.selectListByRouteProcessAndReport(5401L, "REPORT-030");
        assertEquals(1, persisted.size());
        assertEquals(Integer.MAX_VALUE, persisted.get(0).getDueMinutes());
    }

    @Test
    void saveRule_failsFastWhenRoleCandidatePoolIsEmpty() {
        when(permissionApi.getUserRoleIdListByRoleIds(List.of(7002L))).thenReturn(Set.of());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> processFormPermissionRuleService.saveRule(new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                        .setRouteProcessId(5102L)
                        .setBatchRecordReportId("REPORT-002")
                        .setFillRule(candidateRule("ROLE", List.of(7002L), "ANY_ONE", 120, true, "空角色"))));

        assertEquals(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_CANDIDATE_EMPTY.getCode(), exception.getCode());
        assertTrue(processFormPermissionRuleMapper.selectListByRouteProcessAndReport(5102L, "REPORT-002").isEmpty());
    }

    @Test
    void saveRule_rejectsRouteBatchRecordWhenBatchFlowConfigDisabled() {
        insertRouteBatchRecord(8806L, 5601L, "REPORT-050", null, false, true);
        when(adminUserApi.getUserList(List.of(101L))).thenReturn(List.of(
                adminUser(101L, "生产填写", CommonStatusEnum.ENABLE.getStatus())));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> processFormPermissionRuleService.saveRule(
                        new MesProEdhrProcessFormPermissionRuleSaveReqVO()
                                .setRouteProcessId(5601L)
                                .setBatchRecordReportId("REPORT-050")
                                .setFillRule(candidateRule("USER", List.of(101L), "ANY_ONE", 120,
                                        true, "生产"))));

        assertEquals(PRO_EDHR_PROCESS_FORM_PERMISSION_RULE_ROUTE_BINDING_MISSING.getCode(), exception.getCode());
        assertTrue(processFormPermissionRuleMapper
                .selectListByRouteProcessAndReport(5601L, "REPORT-050").isEmpty());
    }

    @Test
    void getRule_shouldNotRemapCurrentRouteProcessToHistoricalBinding() {
        Long routeId = 8807L;
        Long historicalRouteProcessId = 5699L;
        Long currentRouteProcessId = 5701L;
        Long permissionScopeId = 9907L;
        insertRouteBatchRecord(routeId, historicalRouteProcessId, "REPORT-060", permissionScopeId);

        MesProEdhrProcessFormPermissionRuleRespVO result =
                processFormPermissionRuleService.getRule(currentRouteProcessId, "REPORT-060");

        assertEquals(currentRouteProcessId, result.getRouteProcessId());
        assertNull(result.getPermissionScopeId());
        assertEquals(0, result.getAffectedRouteBindingCount());
        verify(routeProcessService, never()).resolveCurrentRouteProcess(currentRouteProcessId, null, null);
        verify(routeProcessService, never()).resolveCurrentRouteProcess(historicalRouteProcessId, routeId, null);
    }

    @Test
    void saveRuleByReport_savesFormLevelRuleAndSyncsEveryRouteBindingPermissionScope() {
        insertRouteBatchRecord(8811L, 5811L, "REPORT-FORM-LIST", null, 70001L, 70002L);
        insertRouteBatchRecord(8812L, 5812L, "REPORT-FORM-LIST", null, 70001L, 70002L);
        when(adminUserApi.getUserList(List.of(301L))).thenReturn(List.of(
                adminUser(301L, "批记录填写人", CommonStatusEnum.ENABLE.getStatus())));

        MesProEdhrProcessFormPermissionRuleRespVO saved;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            saved = processFormPermissionRuleService.saveRuleByReport(
                    new MesProEdhrBatchRecordFormPermissionRuleSaveReqVO()
                            .setBatchRecordReportId("REPORT-FORM-LIST")
                            .setFillRule(candidateRule("USERS", List.of(301L), "ANY_ONE", null, true, "表单页填写人")));
        }

        assertEquals("CONFIGURED", saved.getFillRuleStatus());
        assertEquals(2, saved.getAffectedRouteBindingCount());
        assertEquals(List.of(301L), saved.getFillRule().getCandidateSourceIds());
        assertEquals(1, processFormPermissionRuleMapper.selectListByRouteProcessAndReport(
                0L, "REPORT-FORM-LIST").size());
        assertTrue(processFormPermissionRuleMapper.selectListByRouteProcessAndReport(
                5811L, "REPORT-FORM-LIST").isEmpty());
        assertTrue(processFormPermissionRuleMapper.selectListByRouteProcessAndReport(
                5812L, "REPORT-FORM-LIST").isEmpty());
        assertNotNull(routeFlowProcessBatchRecordMapper.selectById(8811L).getPermissionScopeId());
        assertNotNull(routeFlowProcessBatchRecordMapper.selectById(8812L).getPermissionScopeId());
    }

    @Test
    void saveRuleByReport_allowsFormListConfigurationBeforeBatchRouteIsEnabled() {
        insertRouteBatchRecord(8814L, 5814L, "REPORT-FORM-LIST-DISABLED", null,
                false, true, 70011L, 70012L);
        when(adminUserApi.getUserList(List.of(302L))).thenReturn(List.of(
                adminUser(302L, "列表页填写人", CommonStatusEnum.ENABLE.getStatus())));

        MesProEdhrProcessFormPermissionRuleRespVO saved;
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(113L);
            security.when(SecurityFrameworkUtils::getLoginUserNickname).thenReturn("aoteman");
            saved = processFormPermissionRuleService.saveRuleByReport(
                    new MesProEdhrBatchRecordFormPermissionRuleSaveReqVO()
                            .setBatchRecordReportId("REPORT-FORM-LIST-DISABLED")
                            .setFillRule(candidateRule("USERS", List.of(302L), "ANY_ONE", null, true,
                                    "表单列表未启用路线预配置")));
        }

        assertEquals("CONFIGURED", saved.getFillRuleStatus());
        assertEquals(1, saved.getAffectedRouteBindingCount());
        assertEquals(List.of(302L), saved.getFillRule().getCandidateSourceIds());
        assertEquals(1, processFormPermissionRuleMapper.selectListByRouteProcessAndReport(
                0L, "REPORT-FORM-LIST-DISABLED").size());
        assertTrue(processFormPermissionRuleMapper.selectListByRouteProcessAndReport(
                5814L, "REPORT-FORM-LIST-DISABLED").isEmpty());
        assertNotNull(routeFlowProcessBatchRecordMapper.selectById(8814L).getPermissionScopeId());
    }

    @Test
    void saveRuleByReport_persistsFormLevelFillRuleWithoutRouteBinding() {
        insertReportVersion("REPORT-FORM-UNBOUND", 77101L, 77102L);
        when(adminUserApi.getUserList(List.of(303L))).thenReturn(List.of(
                adminUser(303L, "未绑定表单填写人", CommonStatusEnum.ENABLE.getStatus())));

        MesProEdhrProcessFormPermissionRuleRespVO saved =
                processFormPermissionRuleService.saveRuleByReport(
                        new MesProEdhrBatchRecordFormPermissionRuleSaveReqVO()
                                .setBatchRecordReportId("REPORT-FORM-UNBOUND")
                                .setFillRule(candidateRule("USERS", List.of(303L), "ANY_ONE", null, true,
                                        "未绑定路线表单填写人")));

        assertEquals("CONFIGURED", saved.getFillRuleStatus());
        assertEquals(0, saved.getAffectedRouteBindingCount());
        assertEquals(List.of(303L), saved.getFillRule().getCandidateSourceIds());
        assertEquals("未绑定表单填写人", saved.getFillRule().getCandidateUsers().get(0).getDisplayName());
        assertEquals(1, processFormPermissionRuleMapper.selectListByRouteProcessAndReport(
                0L, "REPORT-FORM-UNBOUND").size());

        MesProEdhrProcessFormPermissionRuleRespVO queried =
                processFormPermissionRuleService.getRuleByReport("REPORT-FORM-UNBOUND");
        assertEquals("CONFIGURED", queried.getFillRuleStatus());
        assertEquals(List.of(303L), queried.getFillRule().getCandidateSourceIds());
    }

    @Test
    void getRuleByReport_returnsConfiguredFillRuleForBatchRecordFormListColumn() {
        insertRouteBatchRecord(8813L, 5813L, "REPORT-FORM-QUERY", 9913L);
        processFormPermissionRuleMapper.insert(new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(5813L)
                .setBatchRecordReportId("REPORT-FORM-QUERY")
                .setRuleType("FILL")
                .setSignatureCellKey("")
                .setCandidateSourceType("USER")
                .setCandidateSourceIds("401")
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(Integer.MAX_VALUE)
                .setEnabled(true)
                .setRemark("表单列表回显"));
        when(adminUserApi.getUserList(List.of(401L))).thenReturn(List.of(
                adminUser(401L, "回显填写人", CommonStatusEnum.ENABLE.getStatus())));

        MesProEdhrProcessFormPermissionRuleRespVO result =
                processFormPermissionRuleService.getRuleByReport("REPORT-FORM-QUERY");

        assertEquals("CONFIGURED", result.getFillRuleStatus());
        assertEquals(1, result.getAffectedRouteBindingCount());
        assertEquals("回显填写人", result.getFillRule().getCandidateUsers().get(0).getDisplayName());
    }

    @Test
    void getRuleByReport_returnsVersionedFormLevelRuleCopiedDuringImportUpgrade() {
        insertRouteBatchRecord(8816L, 5816L, "REPORT-FORM-UPGRADE-V12", null,
                77001L, 77012L);
        processFormPermissionRuleMapper.insert(new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(0L)
                .setBatchRecordReportId("REPORT-FORM-UPGRADE-V12")
                .setBatchRecordDefinitionId(77001L)
                .setBatchRecordVersionId(77012L)
                .setRuleType("FILL")
                .setSignatureCellKey("")
                .setCandidateSourceType("USERS")
                .setCandidateSourceIds("403")
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(Integer.MAX_VALUE)
                .setEnabled(true)
                .setRemark("V12 升版复制填写人"));
        when(adminUserApi.getUserList(List.of(403L))).thenReturn(List.of(
                adminUser(403L, "升版填写人", CommonStatusEnum.ENABLE.getStatus())));

        MesProEdhrProcessFormPermissionRuleRespVO result =
                processFormPermissionRuleService.getRuleByReport("REPORT-FORM-UPGRADE-V12");

        assertEquals("CONFIGURED", result.getFillRuleStatus());
        assertEquals(1, result.getAffectedRouteBindingCount());
        assertEquals(List.of(403L), result.getFillRule().getCandidateSourceIds());
        assertEquals("升版填写人", result.getFillRule().getCandidateUsers().get(0).getDisplayName());
    }

    @Test
    void getRuleByReport_returnsVersionedFormLevelRuleForExtraFormUpgradeWithoutRouteBinding() {
        batchRecordReportMapper.insert(new MesProBatchRecordReportDO()
                .setSampleKey("LOSS-UPGRADE-V12")
                .setBatchRecordName("球囊扩张压力泵")
                .setProductName("球囊扩张压力泵")
                .setFormSlotType("LOSS")
                .setRouteKey("LOSS")
                .setBatchRecordDefinitionId(77002L)
                .setBatchRecordVersionId(77022L)
                .setSourceFileName("loss-v12.doc")
                .setSourceFileSha256("loss-v12-sha")
                .setSourceTableIndex(1)
                .setTableTitle("损耗单")
                .setReportId("REPORT-LOSS-UPGRADE-V12")
                .setReportCode("LOSS_UPGRADE_V12")
                .setReportName("损耗单")
                .setReportCategoryId("category-loss")
                .setLastImportTime(LocalDateTime.now()));
        processFormPermissionRuleMapper.insert(new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(0L)
                .setBatchRecordReportId("REPORT-LOSS-UPGRADE-V12")
                .setBatchRecordDefinitionId(77002L)
                .setBatchRecordVersionId(77022L)
                .setRuleType("FILL")
                .setSignatureCellKey("")
                .setCandidateSourceType("USERS")
                .setCandidateSourceIds("404")
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(Integer.MAX_VALUE)
                .setEnabled(true)
                .setRemark("损耗单升版复制填写人"));
        when(adminUserApi.getUserList(List.of(404L))).thenReturn(List.of(
                adminUser(404L, "损耗单填写人", CommonStatusEnum.ENABLE.getStatus())));

        MesProEdhrProcessFormPermissionRuleRespVO result =
                processFormPermissionRuleService.getRuleByReport("REPORT-LOSS-UPGRADE-V12");

        assertEquals("CONFIGURED", result.getFillRuleStatus());
        assertEquals(0, result.getAffectedRouteBindingCount());
        assertEquals(List.of(404L), result.getFillRule().getCandidateSourceIds());
        assertEquals("损耗单填写人", result.getFillRule().getCandidateUsers().get(0).getDisplayName());
    }

    @Test
    void getRule_returnsFormLevelFillRuleWhenRouteBindingHasNoRouteSpecificRule() {
        insertRouteBatchRecord(8815L, 5815L, "REPORT-FORM-FALLBACK", 9915L);
        processFormPermissionRuleMapper.insert(new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(0L)
                .setBatchRecordReportId("REPORT-FORM-FALLBACK")
                .setRuleType("FILL")
                .setSignatureCellKey("")
                .setCandidateSourceType("USER")
                .setCandidateSourceIds("402")
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(Integer.MAX_VALUE)
                .setEnabled(true)
                .setRemark("表单级填写人"));
        when(adminUserApi.getUserList(List.of(402L))).thenReturn(List.of(
                adminUser(402L, "表单级回显填写人", CommonStatusEnum.ENABLE.getStatus())));

        MesProEdhrProcessFormPermissionRuleRespVO result =
                processFormPermissionRuleService.getRule(5815L, "REPORT-FORM-FALLBACK");

        assertEquals("CONFIGURED", result.getFillRuleStatus());
        assertEquals(1, result.getAffectedRouteBindingCount());
        assertEquals("表单级回显填写人", result.getFillRule().getCandidateUsers().get(0).getDisplayName());
    }

    @Test
    void getRule_prefersVersionedRouteFillRuleOverLegacyNullVersionRule() {
        insertRouteBatchRecord(8817L, 5817L, "REPORT-VERSION-DETAIL", null, 77003L, 77032L);
        processFormPermissionRuleMapper.insert(permissionRule(5817L, "REPORT-VERSION-DETAIL", null,
                "FILL", "", "USER", "405", "旧版本填写人"));
        processFormPermissionRuleMapper.insert(permissionRule(5817L, "REPORT-VERSION-DETAIL", 77032L,
                "FILL", "", "USER", "406", "当前版本填写人"));
        when(adminUserApi.getUserList(List.of(405L))).thenReturn(List.of(
                adminUser(405L, "旧版本填写人", CommonStatusEnum.ENABLE.getStatus())));
        when(adminUserApi.getUserList(List.of(406L))).thenReturn(List.of(
                adminUser(406L, "当前版本填写人", CommonStatusEnum.ENABLE.getStatus())));

        MesProEdhrProcessFormPermissionRuleRespVO result =
                processFormPermissionRuleService.getRule(5817L, "REPORT-VERSION-DETAIL");

        assertEquals("CONFIGURED", result.getFillRuleStatus());
        assertEquals(List.of(406L), result.getFillRule().getCandidateSourceIds());
        assertEquals("当前版本填写人", result.getFillRule().getCandidateUsers().get(0).getDisplayName());
    }

    @Test
    void getRule_doesNotFallbackFromVersionedBindingToLegacyNullVersionRule() {
        insertRouteBatchRecord(8818L, 5818L, "REPORT-VERSION-NO-FALLBACK", null, 77004L, 77042L);
        processFormPermissionRuleMapper.insert(permissionRule(5818L, "REPORT-VERSION-NO-FALLBACK", null,
                "FILL", "", "USER", "407", "旧空版本填写人"));

        MesProEdhrProcessFormPermissionRuleRespVO result =
                processFormPermissionRuleService.getRule(5818L, "REPORT-VERSION-NO-FALLBACK");

        assertEquals("NOT_CONFIGURED", result.getFillRuleStatus());
        assertNull(result.getFillRule());
    }

    @Test
    void mapper_shouldResolveFillAndSignatureRulesByFrozenBatchRecordVersion() {
        Long routeProcessId = 5915L;
        String reportId = "REPORT-VERSIONED-RULE";
        processFormPermissionRuleMapper.insert(permissionRule(routeProcessId, reportId, 7701L,
                "FILL", "", "USER", "701", "V1 fill"));
        processFormPermissionRuleMapper.insert(permissionRule(routeProcessId, reportId, 7702L,
                "FILL", "", "USER", "702", "V2 fill"));
        processFormPermissionRuleMapper.insert(permissionRule(routeProcessId, reportId, 7701L,
                "SIGNATURE", "R1C1", "USER", "801", "V1 signature"));
        processFormPermissionRuleMapper.insert(permissionRule(routeProcessId, reportId, 7702L,
                "SIGNATURE", "R1C1", "USER", "802", "V2 signature"));

        MesProEdhrProcessFormPermissionRuleDO fillRule =
                processFormPermissionRuleMapper.selectEnabledFillRuleForRouteOrReport(
                        routeProcessId, reportId, 7701L);
        MesProEdhrProcessFormPermissionRuleDO signatureRule =
                processFormPermissionRuleMapper.selectEnabledSignatureRule(
                        routeProcessId, reportId, "R1C1", 7701L);

        assertEquals("701", fillRule.getCandidateSourceIds());
        assertEquals("801", signatureRule.getCandidateSourceIds());
        assertNull(processFormPermissionRuleMapper.selectEnabledFillRuleForRouteOrReport(
                routeProcessId, reportId, 7799L));
        assertNull(processFormPermissionRuleMapper.selectEnabledSignatureRule(
                routeProcessId, reportId, "R1C1", 7799L));
    }

    private MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule candidateRule(String sourceType,
                                                                                     List<Long> sourceIds,
                                                                                     String completionPolicy,
                                                                                     Integer dueMinutes,
                                                                                     Boolean enabled,
                                                                                     String remark) {
        return new MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule()
                .setCandidateSourceType(sourceType)
                .setCandidateSourceIds(sourceIds)
                .setCompletionPolicy(completionPolicy)
                .setDueMinutes(dueMinutes)
                .setEnabled(enabled)
                .setRemark(remark);
    }

    private MesProEdhrProcessFormPermissionRuleSaveReqVO.SignatureRule signatureRule(String cellKey,
                                                                                     String signatureRole,
                                                                                     MesProEdhrProcessFormPermissionRuleSaveReqVO.CandidateRule rule) {
        return new MesProEdhrProcessFormPermissionRuleSaveReqVO.SignatureRule()
                .setSignatureCellKey(cellKey)
                .setSignatureRole(signatureRole)
                .setRule(rule);
    }

    private AdminUserRespDTO adminUser(Long userId, String nickname, Integer status) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(userId);
        user.setNickname(nickname);
        user.setStatus(status);
        return user;
    }

    private MesProEdhrProcessFormPermissionRuleDO permissionRule(Long routeProcessId,
                                                                 String reportId,
                                                                 Long batchRecordVersionId,
                                                                 String ruleType,
                                                                 String signatureCellKey,
                                                                 String candidateSourceType,
                                                                 String candidateSourceIds,
                                                                 String remark) {
        return new MesProEdhrProcessFormPermissionRuleDO()
                .setRouteProcessId(routeProcessId)
                .setBatchRecordReportId(reportId)
                .setBatchRecordVersionId(batchRecordVersionId)
                .setRuleType(ruleType)
                .setSignatureCellKey(signatureCellKey)
                .setSignatureRole("REVIEW")
                .setCandidateSourceType(candidateSourceType)
                .setCandidateSourceIds(candidateSourceIds)
                .setCompletionPolicy("ANY_ONE")
                .setDueMinutes(30)
                .setEnabled(true)
                .setRemark(remark);
    }

    private void insertRouteBatchRecord(Long id, Long routeProcessId, String reportId, Long permissionScopeId) {
        insertRouteBatchRecord(id, routeProcessId, reportId, permissionScopeId, true, true);
    }

    private void insertRouteBatchRecord(Long id, Long routeProcessId, String reportId, Long permissionScopeId,
                                        boolean flowEnabled, boolean processEnabled) {
        insertRouteBatchRecord(id, routeProcessId, reportId, permissionScopeId, flowEnabled, processEnabled,
                null, null);
    }

    private void insertRouteBatchRecord(Long id, Long routeProcessId, String reportId, Long permissionScopeId,
                                        Long batchRecordDefinitionId, Long batchRecordVersionId) {
        insertRouteBatchRecord(id, routeProcessId, reportId, permissionScopeId, true, true,
                batchRecordDefinitionId, batchRecordVersionId);
    }

    private void insertRouteBatchRecord(Long id, Long routeProcessId, String reportId, Long permissionScopeId,
                                        boolean flowEnabled, boolean processEnabled,
                                        Long batchRecordDefinitionId, Long batchRecordVersionId) {
        MesProRouteFlowConfigDO flowConfig = MesProRouteFlowConfigDO.builder()
                .routeId(id)
                .useType("BATCH")
                .enabled(flowEnabled)
                .configVersion("TEST-BATCH-" + id)
                .build();
        routeFlowConfigMapper.insert(flowConfig);
        MesProRouteFlowProcessConfigDO processConfig = MesProRouteFlowProcessConfigDO.builder()
                .routeFlowConfigId(flowConfig.getId())
                .routeId(id)
                .routeProcessId(routeProcessId)
                .useType("BATCH")
                .enabled(processEnabled)
                .executionMode("SEQUENTIAL")
                .build();
        routeFlowProcessConfigMapper.insert(processConfig);
        routeFlowProcessBatchRecordMapper.insert(MesProRouteFlowProcessBatchRecordDO.builder()
                .id(id)
                .routeFlowProcessConfigId(processConfig.getId())
                .routeId(id)
                .routeProcessId(routeProcessId)
                .useType("BATCH")
                .batchRecordReportId(reportId)
                .batchRecordDefinitionId(batchRecordDefinitionId)
                .batchRecordVersionId(batchRecordVersionId)
                .recordCategory("BATCH_RECORD")
                .validationProfile("CONTROLLED_BATCH")
                .permissionScopeId(permissionScopeId)
                .recordCategorySnapshotHash("hash-" + id)
                .reportSort(1)
                .build());
    }

    private void insertReportVersion(String reportId, Long batchRecordDefinitionId, Long batchRecordVersionId) {
        batchRecordReportMapper.insert(new MesProBatchRecordReportDO()
                .setSampleKey("REPORT-VERSION-" + reportId)
                .setBatchRecordName("批记录")
                .setProductName("批记录产品")
                .setFormSlotType("BATCH_RECORD")
                .setRouteKey("BATCH_RECORD")
                .setBatchRecordDefinitionId(batchRecordDefinitionId)
                .setBatchRecordVersionId(batchRecordVersionId)
                .setSourceFileName(reportId + ".doc")
                .setSourceFileSha256("sha-" + reportId)
                .setSourceTableIndex(1)
                .setTableTitle("批记录表")
                .setReportId(reportId)
                .setReportCode(reportId)
                .setReportName(reportId)
                .setReportCategoryId("category-" + reportId)
                .setLastImportTime(LocalDateTime.now()));
    }
}
