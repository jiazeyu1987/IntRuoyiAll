package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.vo.BusinessApprovalPolicyTransferVO;
import cn.iocoder.yudao.module.bpm.dal.dataobject.businessapproval.BusinessApprovalPolicyDO;
import cn.iocoder.yudao.module.bpm.dal.mysql.businessapproval.BusinessApprovalPolicyMapper;
import cn.iocoder.yudao.module.bpm.formcenter.service.FormBusinessEffectExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.validation.Validation;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Import({BusinessApprovalPolicyTransferService.class, BusinessApprovalPolicyTransferServiceTest.Config.class})
class BusinessApprovalPolicyTransferServiceTest extends BaseDbUnitTest {
    @Resource private BusinessApprovalPolicyTransferService service;
    @Resource private BusinessApprovalPolicyMapper mapper;
    @Resource private RepositoryService repository;
    @Resource private ProcessDefinitionQuery query;
    @Resource private JdbcTemplate jdbc;
    @org.springframework.boot.test.mock.mockito.MockBean
    private BusinessApprovalProcessTransferService processTransferService;

    @BeforeEach
    void setup() {
        TenantContextHolder.setTenantId(101L);
        when(processTransferService.exportProcesses(anyList())).thenReturn(List.of());
        reset(query);
        when(query.processDefinitionKey(anyString())).thenReturn(query);
        when(query.processDefinitionTenantId(anyString())).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.singleResult()).thenReturn(mock(ProcessDefinition.class));
    }

    @Test
    void exportImportExportPreservesEveryBusinessFieldAndReplacesOnlyMatchingScope() throws Exception {
        BusinessApprovalPolicyDO retired = row("ROUTE_VERSION", "DISABLED", "DIRECT");
        mapper.insert(retired);
        BusinessApprovalPolicyDO active = row("ROUTE_VERSION", "PUBLISHED", "BPM_REQUIRED");
        active.setFormPolicyType("CUSTOM");
        active.setFormSlotsJson(" [{\"slot\":\"生产记录\",\"value\":\"2116486795512508425\"}] ");
        active.setRemark(" 原配置：中文、空格及换行\n均保留 ");
        mapper.insert(active);
        mapper.insert(row("ROUTE_VERSION", "DRAFT", "SIGNATURE_REQUIRED"));
        mapper.insert(row("FORM_TEMPLATE", "DISABLED", "DIRECT"));
        BusinessApprovalPolicyTransferVO exported = service.exportPolicies();
        assertEquals(3, exported.getPolicies().size(), "retired audit history is not configuration");
        ObjectMapper json = new ObjectMapper();
        String file = json.writeValueAsString(exported);
        assertFalse(file.contains("tenantId"));
        assertFalse(file.contains("\"id\""));
        var imported = json.readValue(file, BusinessApprovalPolicyTransferVO.class);

        TenantContextHolder.setTenantId(202L);
        BusinessApprovalPolicyDO previous = row("ROUTE_VERSION", "PUBLISHED", "DIRECT");
        mapper.insert(previous);
        BusinessApprovalPolicyDO unrelated = row("UNRELATED", "PUBLISHED", "DIRECT");
        mapper.insert(unrelated);
        assertEquals(3, service.importPolicies(imported));
        assertEquals("DISABLED", mapper.selectById(previous.getId()).getStatus());
        assertEquals("PUBLISHED", mapper.selectById(unrelated.getId()).getStatus());
        var target = service.exportPolicies();
        target.setPolicies(target.getPolicies().stream()
                .filter(item -> !"UNRELATED".equals(item.getObjectType())).toList());
        assertEquals(exported, target, "exported configuration must round trip without any field changes");
        verify(query).processDefinitionTenantId("202");
        TenantContextHolder.setTenantId(101L);
        assertEquals(exported, service.exportPolicies(), "source tenant must be unchanged");
    }

    @Test
    void missingProcessInLaterRowLeavesExistingPoliciesAndAllNewRowsUnchanged() {
        TenantContextHolder.setTenantId(202L);
        BusinessApprovalPolicyDO existing = row("ROUTE_VERSION", "PUBLISHED", "DIRECT");
        mapper.insert(existing);
        var before = service.exportPolicies();
        var transfer = transfer(row("NEW_OBJECT", "PUBLISHED", "DIRECT"),
                row("ROUTE_VERSION", "PUBLISHED", "BPM_REQUIRED"));
        when(query.singleResult()).thenReturn(null);
        var ex = assertThrows(BusinessApprovalException.class, () -> service.importPolicies(transfer));
        assertTrue(ex.getMessage().contains("目标租户缺少"));
        assertEquals(before, service.exportPolicies());
        assertEquals("PUBLISHED", mapper.selectById(existing.getId()).getStatus());
    }

    @Test
    void wildcardConflictRejectsWholePackageWithoutDisablingUnrelatedState() {
        TenantContextHolder.setTenantId(202L);
        var existing = row("ROUTE_VERSION", "PUBLISHED", "DIRECT");
        existing.setObjectState("ALL");
        mapper.insert(existing);
        var before = service.exportPolicies();
        assertThrows(BusinessApprovalException.class,
                () -> service.importPolicies(transfer(row("ROUTE_VERSION", "PUBLISHED", "DIRECT"))));
        assertEquals(before, service.exportPolicies());
    }

    @Test
    void duplicatePublishedRowsInFileAreRejectedBeforeWriting() {
        var transfer = transfer(row("ROUTE_VERSION", "PUBLISHED", "DIRECT"),
                row("ROUTE_VERSION", "PUBLISHED", "DIRECT"));
        assertThrows(BusinessApprovalException.class, () -> service.importPolicies(transfer));
        assertTrue(service.exportPolicies().getPolicies().isEmpty());
    }

    @Test
    void registeredFormAndBusinessExecutorsCanBeImportedTogetherWithoutChangingFields() {
        var form = row("EDHR_ROUTE_FORM", "PUBLISHED", "DIRECT");
        form.setActionCode("EDHR_RF_310_FORM_BINDING_1784782156927_1");
        form.setObjectState("ACTIVE");
        form.setEffectExecutorCode("MES_EDHR_ROUTE_FORM_FILL");
        form.setProcessDefinitionKey(null);
        var transfer = transfer(form, row("ROUTE_VERSION", "PUBLISHED", "DIRECT"));
        assertEquals(2, service.importPolicies(transfer));
        assertEquals(transfer, service.exportPolicies());
    }

    @Test
    void unknownExecutorRejectsWholePackageBeforeProcessOrPolicyWrites() {
        var existing = row("ROUTE_VERSION", "PUBLISHED", "DIRECT");
        mapper.insert(existing);
        var before = service.exportPolicies();
        var unknown = row("EDHR_ROUTE_FORM", "PUBLISHED", "DIRECT");
        unknown.setEffectExecutorCode("MISSING_FORM_EXECUTOR");
        var error = assertThrows(BusinessApprovalException.class, () -> service.importPolicies(
                transfer(row("ROUTE_VERSION", "PUBLISHED", "DIRECT"), unknown)));
        assertTrue(error.getMessage().contains("MISSING_FORM_EXECUTOR"));
        verify(processTransferService, never()).importProcesses(any());
        assertEquals(before, service.exportPolicies());
        assertEquals("PUBLISHED", mapper.selectById(existing.getId()).getStatus());
    }

    @Test
    void databaseFailureOnLaterInsertRollsBackEarlierInsertAndReplacement() {
        TenantContextHolder.setTenantId(202L);
        var existing = row("ROUTE_VERSION", "PUBLISHED", "DIRECT");
        mapper.insert(existing);
        var before = service.exportPolicies();
        var first = row("ROUTE_VERSION", "PUBLISHED", "DIRECT");
        var second = row("NEW_OBJECT", "DRAFT", "DIRECT");
        second.setRemark("force-failure");
        jdbc.execute("ALTER TABLE bpm_business_approval_policy ADD CONSTRAINT policy_transfer_test_check "
                + "CHECK (remark IS NULL OR remark <> 'force-failure')");
        try {
            assertThrows(RuntimeException.class, () -> service.importPolicies(transfer(first, second)));
            assertEquals(before, service.exportPolicies());
            assertEquals("PUBLISHED", mapper.selectById(existing.getId()).getStatus());
        } finally {
            jdbc.execute("ALTER TABLE bpm_business_approval_policy DROP CONSTRAINT policy_transfer_test_check");
        }
    }

    @Test
    void suspendedTargetProcessIsRejectedWithoutImporting() {
        var definition = mock(ProcessDefinition.class);
        when(definition.isSuspended()).thenReturn(true);
        when(query.singleResult()).thenReturn(definition);
        assertThrows(BusinessApprovalException.class,
                () -> service.importPolicies(transfer(row("ROUTE_VERSION", "PUBLISHED", "BPM_REQUIRED"))));
        assertTrue(service.exportPolicies().getPolicies().isEmpty());
    }

    @Test
    void unknownTenantFieldsAndUnsupportedSchemaOrStatusAreRejected() {
        ObjectMapper json = new ObjectMapper();
        assertThrows(Exception.class, () -> json.readValue(
                "{\"schemaVersion\":1,\"format\":\"BUSINESS_APPROVAL_POLICIES\",\"policies\":[],\"tenantId\":101}",
                BusinessApprovalPolicyTransferVO.class));
        assertThrows(Exception.class, () -> json.readValue(
                "{\"schemaVersion\":1,\"format\":\"BUSINESS_APPROVAL_POLICIES\",\"policies\":[{\"tenantId\":101}]}",
                BusinessApprovalPolicyTransferVO.class));
        var transfer = transfer(row("ROUTE_VERSION", "UNKNOWN", "DIRECT"));
        transfer.setSchemaVersion(3);
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var errors = factory.getValidator().validate(transfer);
            assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("schemaVersion")));
            assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().contains("status")));
        }
    }

    private BusinessApprovalPolicyTransferVO transfer(BusinessApprovalPolicyDO... rows) {
        var transfer = new BusinessApprovalPolicyTransferVO();
        transfer.setSchemaVersion(2);
        transfer.setProcesses(List.of());
        transfer.setFormat("BUSINESS_APPROVAL_POLICIES");
        transfer.setPolicies(List.of(rows).stream()
                .map(row -> BeanUtils.toBean(row, BusinessApprovalPolicyTransferVO.Item.class)).toList());
        return transfer;
    }

    private BusinessApprovalPolicyDO row(String object, String status, String mode) {
        return BusinessApprovalPolicyDO.builder().tenantId(TenantContextHolder.getRequiredTenantId())
                .dataDomain("MES").systemCode("MES").objectType(object).actionCode("PUBLISH").objectState("DRAFT")
                .policyMode(mode).processDefinitionKey("mes-route-version-approval-v1")
                .effectExecutorCode("MES_ROUTE_VERSION_PUBLISH").status(status).build();
    }

    @TestConfiguration
    static class Config {
        @Bean JdbcTemplate jdbc(javax.sql.DataSource dataSource) { return new JdbcTemplate(dataSource); }
        @Bean BusinessApprovalEffectExecutorRegistry registry() {
            var executor = mock(BusinessApprovalEffectExecutor.class);
            when(executor.getExecutorCode()).thenReturn("MES_ROUTE_VERSION_PUBLISH");
            return new BusinessApprovalEffectExecutorRegistry(List.of(executor));
        }
        @Bean FormBusinessEffectExecutor formExecutor() {
            var executor = mock(FormBusinessEffectExecutor.class);
            when(executor.getExecutorCode()).thenReturn("MES_EDHR_ROUTE_FORM_FILL");
            return executor;
        }
        @Bean ProcessDefinitionQuery query() { return mock(ProcessDefinitionQuery.class); }
        @Bean RepositoryService repository(ProcessDefinitionQuery query) {
            var repository = mock(RepositoryService.class);
            when(repository.createProcessDefinitionQuery()).thenReturn(query);
            return repository;
        }
    }
}
