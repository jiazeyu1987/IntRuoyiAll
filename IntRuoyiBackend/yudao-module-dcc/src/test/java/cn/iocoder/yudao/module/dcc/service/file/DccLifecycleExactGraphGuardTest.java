package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Exact contract is the explicit external port; date policy and lifecycle preflight are actual implementations. */
class DccLifecycleExactGraphGuardTest {
    private DccControlledFileLifecycleService service;
    private DccLifecycleEffectiveContractResolver contracts;
    private DccControlledFileDO file;
    @BeforeEach void setup(){
        TenantContextHolder.setTenantId(1L);TransactionSynchronizationManager.setActualTransactionActive(true);
        service=new DccControlledFileLifecycleService();contracts=mock(DccLifecycleEffectiveContractResolver.class);
        ReflectionTestUtils.setField(service,"effectiveContractResolver",contracts);
        var dates=new DccWorkflowDatePolicy();ReflectionTestUtils.setField(dates,"zoneId","Asia/Singapore");
        ReflectionTestUtils.setField(service,"datePolicy",dates);
        file=DccControlledFileDO.builder().id(20L).tenantId(1L).processInstanceId("real-round")
                .processDefinitionKey(DccControlledFileProcessDefinitionKeys.UPLOAD)
                .effectiveMode(DccWorkflowDatePolicy.AFTER_STANDARDIZATION_SIGNATURE).build();
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();TransactionSynchronizationManager.clear();}
    @Test void explicitModeDoesNotBypassExactDefinitionRefusal(){
        var failure=new IllegalStateException("EXECUTION_CONTRACT_UNREGISTERED_DEFINITION");
        when(contracts.resolve(file)).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.requireReviewEffectiveSelection(file)));
        assertNull(file.getEffectiveTime());
    }
    @Test void publicCannotInferPresetFromMissingMode(){
        contract(DccLifecycleEffectiveContract.Rule.NEW_EXPLICIT_MODE);file.setEffectiveMode(null);
        file.setEffectiveDate(java.time.LocalDate.now().plusDays(1));
        assertThrows(RuntimeException.class,()->service.requireReviewEffectiveSelection(file));assertNull(file.getEffectiveMode());
    }
    @Test void publicExplicitSignatureModeConsumesExactContract(){
        contract(DccLifecycleEffectiveContract.Rule.NEW_EXPLICIT_MODE);
        assertDoesNotThrow(()->service.requireReviewEffectiveSelection(file));verify(contracts).resolve(file);
    }
    @Test void provenLegacyAcceptsSavedDateOnlyAndNeverNewMode(){
        contract(DccLifecycleEffectiveContract.Rule.LEGACY_V4_PRESET_DATE);
        file.setEffectiveMode(null);file.setEffectiveDate(java.time.LocalDate.now().plusDays(1));
        assertDoesNotThrow(()->service.requireReviewEffectiveSelection(file));assertNull(file.getEffectiveMode());
        file.setEffectiveMode(DccWorkflowDatePolicy.PRESET_DATE);
        assertThrows(RuntimeException.class,()->service.requireReviewEffectiveSelection(file));
    }
    private void contract(DccLifecycleEffectiveContract.Rule rule){
        when(contracts.resolve(file)).thenReturn(new DccLifecycleEffectiveContract(1L,20L,"real-round","exact-id",
                "exact-deployment",file.getProcessDefinitionKey(),"a".repeat(64),rule));
    }
}
