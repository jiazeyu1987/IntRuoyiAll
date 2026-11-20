package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyTransferVO;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.RoleApprovalConfigPackageVO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.system.service.permission.RoleConfigPackageService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import org.flowable.engine.RepositoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Import({RoleApprovalConfigPackageService.class, BusinessApprovalPolicyTransferService.class,
        RoleApprovalConfigPackageServiceTest.Config.class})
class RoleApprovalConfigPackageServiceTest extends BaseDbUnitTest {
    private static final String ROLES = """
            {"packageVersion":"4","categories":[{"code":"ops","name":"运营","sort":0,"status":0}],
             "roles":[{"code":"reviewer","name":"审批人","sort":0,"status":0,"categoryCode":"ops",
                       "dataScope":1,"dataScopeDeptIds":[],"menuKeys":["permission:example:query"]}]}
            """;
    @Resource private RoleApprovalConfigPackageService service;
    @Resource private BusinessApprovalPolicyMapper policyMapper;
    @Resource private JdbcTemplate jdbc;
    @MockitoBean private RoleConfigPackageService roleConfigPackageService;
    @MockitoBean private BusinessApprovalProcessTransferService processTransferService;
    @MockitoBean private BusinessApprovalEffectExecutorRegistry executorRegistry;
    @MockitoBean private RepositoryService repositoryService;

    @BeforeEach
    void setup() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS role_approval_package_test (kind VARCHAR(30) PRIMARY KEY)");
        jdbc.update("DELETE FROM role_approval_package_test");
        when(roleConfigPackageService.exportAllPackage()).thenReturn(ROLES.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(processTransferService.exportProcesses(anyList())).thenReturn(List.of());
        doAnswer(invocation -> {
            assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
            jdbc.update("INSERT INTO role_approval_package_test VALUES ('role-grants')");
            return null;
        }).when(roleConfigPackageService).importPackage(any());
        doAnswer(invocation -> {
            assertEquals(1, markerCount(), "roles must be restored before deployment");
            jdbc.update("INSERT INTO role_approval_package_test VALUES ('deployment')");
            return null;
        }).when(processTransferService).importProcesses(any());
    }

    @Test
    void combinedFilePreservesCompleteRoleGrantsAndPolicyFields() {
        var transfer = pack();
        var policy = policy();
        policy.setRemark(" 原文\n保留 ");
        policy.setFormSlotsJson(" [{\"slot\":\"生产记录\"}] ");
        transfer.getApprovalPolicies().setPolicies(List.of(policy));
        assertEquals(1, service.importPackage(JsonUtils.toJsonByte(transfer)));
        assertEquals(2, markerCount());
        var exported = JsonUtils.parseObject(service.exportPackage(), RoleApprovalConfigPackageVO.class);
        assertEquals(transfer, exported);
        verify(roleConfigPackageService).importPackage(JsonUtils.toJsonByte(transfer.getRoleConfig()));
    }

    @Test
    void latePolicyDatabaseFailureRollsBackRoleGrantsAndDeployment() {
        var transfer = pack();
        transfer.getApprovalPolicies().setPolicies(List.of(policy()));
        jdbc.execute("ALTER TABLE bpm_business_approval_policy ADD CONSTRAINT combined_package_test_check CHECK (remark <> 'reject')");
        transfer.getApprovalPolicies().getPolicies().get(0).setRemark("reject");
        try {
            assertThrows(RuntimeException.class, () -> service.importPackage(JsonUtils.toJsonByte(transfer)));
            assertEquals(0, markerCount(), "both role grants and deployment must roll back");
            assertEquals(0L, policyMapper.selectCount());
        } finally {
            jdbc.execute("ALTER TABLE bpm_business_approval_policy DROP CONSTRAINT combined_package_test_check");
        }
    }

    @Test
    void emptyPolicySectionStillImportsRolesAndDoesNotInventPoliciesOrProcesses() {
        assertEquals(0, service.importPackage(JsonUtils.toJsonByte(pack())));
        assertEquals(1, markerCount());
        assertEquals(0L, policyMapper.selectCount());
        verifyNoInteractions(processTransferService);
    }

    @Test
    void standaloneRoleOrPolicyFilesCannotPartiallyImportThroughCombinedEntry() {
        assertThrows(RuntimeException.class, () -> service.importPackage(ROLES.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertThrows(RuntimeException.class, () -> service.importPackage(JsonUtils.toJsonByte(pack().getApprovalPolicies())));
        verify(roleConfigPackageService, never()).importPackage(any());
        assertEquals(0, markerCount());
    }

    @Test
    void missingOrDisabledApprovalRoleIsRejectedBeforeAnyWrites() {
        var transfer = pack();
        var process = JsonUtils.parseObject("""
                {"key":"review","name":"审批","category":{"code":"ops","name":"运营","status":0,"sort":0},
                 "bpmnXml":"<definitions/>","metaInfo":{},"startUsernames":[],
                 "roles":[{"code":"missing","name":"审批人","category":{"code":"ops","name":"运营","status":0,"sort":0}}]}
                """, BusinessApprovalPolicyTransferVO.Process.class);
        transfer.getApprovalPolicies().setProcesses(List.of(process));
        assertThrows(BusinessApprovalException.class, () -> service.importPackage(JsonUtils.toJsonByte(transfer)));
        process.getRoles().get(0).setCode("reviewer");
        ((com.fasterxml.jackson.databind.node.ObjectNode) transfer.getRoleConfig().path("roles").get(0)).put("status", 1);
        assertThrows(BusinessApprovalException.class, () -> service.importPackage(JsonUtils.toJsonByte(transfer)));
        verify(roleConfigPackageService, never()).importPackage(any());
        assertEquals(0, markerCount());
    }

    private int markerCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM role_approval_package_test", Integer.class);
    }

    private RoleApprovalConfigPackageVO pack() {
        var transfer = new RoleApprovalConfigPackageVO();
        transfer.setSchemaVersion(1);
        transfer.setFormat("ROLE_APPROVAL_CONFIGURATION");
        transfer.setRoleConfig(JsonUtils.parseObject(ROLES, JsonNode.class));
        var policies = new BusinessApprovalPolicyTransferVO();
        policies.setSchemaVersion(2);
        policies.setFormat("BUSINESS_APPROVAL_POLICIES");
        policies.setPolicies(List.of());
        policies.setProcesses(List.of());
        transfer.setApprovalPolicies(policies);
        return transfer;
    }

    private BusinessApprovalPolicyTransferVO.Item policy() {
        var policy = new BusinessApprovalPolicyTransferVO.Item();
        policy.setDataDomain("MES"); policy.setSystemCode("MES"); policy.setObjectType("ROUTE_VERSION");
        policy.setActionCode("PUBLISH"); policy.setObjectState("DRAFT"); policy.setPolicyMode("DIRECT");
        policy.setStatus("DRAFT"); policy.setEffectExecutorCode("MES_ROUTE_VERSION_PUBLISH");
        return policy;
    }

    @TestConfiguration
    static class Config {
        @Bean JdbcTemplate jdbcTemplate(javax.sql.DataSource dataSource) { return new JdbcTemplate(dataSource); }
    }
}
