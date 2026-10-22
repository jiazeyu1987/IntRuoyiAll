package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.iocoder.yudao.module.bpm.service.task.BpmProcessInstanceService;
import cn.iocoder.yudao.module.bpm.service.definition.BpmProcessDefinitionService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Tests the lifecycle consumer only. The sole official bound resolver is an external port here. */
class DccLifecycleBoundContractConsumptionTest {
    @Mock private DccBoundWorkflowContractResolver bound;
    @Mock private BpmProcessInstanceService processes;
    @Mock private BpmProcessDefinitionService definitions;
    @InjectMocks private DccLifecycleEffectiveContractResolver resolver;
    private AutoCloseable mocks;
    private DccControlledFileDO file;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        mocks = MockitoAnnotations.openMocks(this);
        file = DccControlledFileDO.builder().id(20L).tenantId(1L).processInstanceId("real-round")
                .processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD).build();
    }

    @AfterEach void cleanup() throws Exception { mocks.close(); TenantContextHolder.clear(); }

    @Test void publicExactBoundIdentityMapsToExplicitModeWithoutMutatingSavedFile() {
        var before = new java.util.TreeMap<>(cn.iocoder.yudao.framework.common.util.json.JsonUtils
                .parseObject(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(file), Map.class));
        when(bound.resolve(file)).thenReturn(execution(7, DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL));
        var result = resolver.resolve(file);
        assertEquals(DccLifecycleEffectiveContract.Rule.NEW_EXPLICIT_MODE, result.rule());
        assertEquals(1L, result.tenantId()); assertEquals(20L, result.controlledFileId());
        assertEquals("real-round", result.processInstanceId()); assertEquals("exact-definition", result.processDefinitionId());
        assertEquals("exact-deployment", result.deploymentId()); assertEquals(file.getProcessDefinitionKey(), result.processKey());
        assertEquals("a".repeat(64), result.definitionDigest());
        assertEquals(before, cn.iocoder.yudao.framework.common.util.json.JsonUtils
                .parseObject(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(file), Map.class));
        verify(bound).resolve(file); verifyNoMoreInteractions(bound);
        verifyNoInteractions(processes, definitions);
    }

    @Test void provenLegacyHistoryKeepsAbsentNumericRoundAndNullNewFacts() {
        when(bound.resolve(file)).thenReturn(execution(null, DccWorkflowDefinitionCatalog.FlowRule.LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL));
        assertEquals(DccLifecycleEffectiveContract.Rule.LEGACY_V4_PRESET_DATE, resolver.resolve(file).rule());
        assertNull(file.getEffectiveMode()); assertNull(file.getEffectiveDate());
        assertNull(file.getPreparedWorkflowContract()); assertNull(file.getPreparedProcessDefinitionId());
        assertNull(file.getPreparedDefinitionDigest()); assertNull(file.getEffectiveTime());
    }

    @Test void officialPortMismatchIsPropagatedWithoutAlternateIdentityRead() {
        var failure = new IllegalStateException("EXECUTION_CONTRACT_BOUND_PREPARED_DEFINITION_MISMATCH");
        when(bound.resolve(file)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> resolver.resolve(file)));
        verify(bound).resolve(file); verifyNoMoreInteractions(bound);
    }

    @Test void publicRequiresActualPositiveNumericRound() {
        for (Integer round : new Integer[] {null, 0, -1}) {
            when(bound.resolve(file)).thenReturn(execution(round, DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL));
            assertThrows(IllegalStateException.class, () -> resolver.resolve(file));
        }
    }

    @Test void wrongBoundTenantFileOrProcessCannotSelectAnEffectiveRule() {
        for (var identity : new Object[][] {{2L, 20L, "real-round"}, {1L, 21L, "real-round"}, {1L, 20L, "other-round"}}) {
            when(bound.resolve(file)).thenReturn(new DccBoundWorkflowContractResolver.BoundExecution(
                    (Long) identity[0], (Long) identity[1], (String) identity[2], 7, file.getProcessDefinitionKey(),
                    "exact-definition", "exact-deployment", "a".repeat(64), "PUBLIC_MATRIX_NATIVE_V1",
                    DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_TRAINING_AFTER_DOCUMENT_CONTROL, true));
            assertThrows(IllegalStateException.class, () -> resolver.resolve(file));
        }
    }

    @Test void missingOrObsoleteContractCannotBeConsumedAsNativeEffectiveness() {
        when(bound.resolve(file)).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> resolver.resolve(file));
        when(bound.resolve(file)).thenReturn(execution(7, DccWorkflowDefinitionCatalog.FlowRule.PUBLIC_V5_OBSOLETE_APPROVAL_END));
        assertThrows(IllegalStateException.class, () -> resolver.resolve(file));
    }

    private DccBoundWorkflowContractResolver.BoundExecution execution(Integer round, DccWorkflowDefinitionCatalog.FlowRule flow) {
        return new DccBoundWorkflowContractResolver.BoundExecution(1L, 20L, "real-round", round,
                file.getProcessDefinitionKey(), "exact-definition", "exact-deployment", "a".repeat(64),
                flow == DccWorkflowDefinitionCatalog.FlowRule.LEGACY_V4_TRAINING_BEFORE_DOCUMENT_CONTROL
                        ? "LEGACY_V4_ACTION" : "PUBLIC_MATRIX_NATIVE_V1", flow, true);
    }
}
