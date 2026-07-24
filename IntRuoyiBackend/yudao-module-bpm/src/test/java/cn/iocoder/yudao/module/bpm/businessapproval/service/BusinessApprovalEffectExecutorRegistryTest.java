package cn.iocoder.yudao.module.bpm.businessapproval.service;

import cn.iocoder.yudao.module.bpm.businessapproval.model.BusinessApprovalException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessApprovalEffectExecutorRegistryTest {

    @Test
    void requireExecutorReturnsRegisteredExecutor() {
        RecordingExecutor executor = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH");
        BusinessApprovalEffectExecutorRegistry registry = new BusinessApprovalEffectExecutorRegistry(List.of(executor));

        assertSame(executor, registry.requireExecutor("MES_ROUTE_VERSION_PUBLISH"));
    }

    @Test
    void requireExecutorFailsFastWhenMissing() {
        BusinessApprovalEffectExecutorRegistry registry = new BusinessApprovalEffectExecutorRegistry(List.of());

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> registry.requireExecutor("MES_ROUTE_VERSION_PUBLISH"));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_EXECUTOR_MISSING, ex.getErrorCode());
    }

    @Test
    void constructorFailsFastWhenExecutorCodeDuplicates() {
        RecordingExecutor left = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH");
        RecordingExecutor right = new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH");

        BusinessApprovalException ex = assertThrows(BusinessApprovalException.class,
                () -> new BusinessApprovalEffectExecutorRegistry(List.of(left, right)));

        assertEquals(BusinessApprovalErrorCode.BUSINESS_APPROVAL_EXECUTOR_CONFLICT, ex.getErrorCode());
    }

    @Test
    void requireBpmProcessDefinitionKeyUsesRegisteredBusinessActionContract() {
        BusinessApprovalEffectExecutorRegistry registry = new BusinessApprovalEffectExecutorRegistry(List.of(
                new RecordingExecutor("MES_ROUTE_VERSION_PUBLISH")));

        assertEquals("mes-route-version-approval-v1",
                registry.requireBpmProcessDefinitionKey("MES_ROUTE_VERSION_PUBLISH"));
    }

}
