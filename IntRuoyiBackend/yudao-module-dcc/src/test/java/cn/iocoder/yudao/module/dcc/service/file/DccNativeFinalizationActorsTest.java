package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentCanonicalStatus;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentTransitionAction;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentKey;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentLifecycleCoreService;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual adapter calls; platform core is the explicit downstream port, not a fake GxP kernel. */
class DccNativeFinalizationActorsTest {
    private DccControlledContentAdapter adapter;
    private ControlledContentLifecycleCoreService core;
    private DccControlledFileDO file;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        adapter=new DccControlledContentAdapter();core=mock(ControlledContentLifecycleCoreService.class);
        ReflectionTestUtils.setField(adapter,"lifecycleCoreService",core);
        file=DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(3L).versionNo("B/1")
                .processInstanceId("real-round").build();
    }
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    @Test void approvalAAndOperationBRemainDifferentInTheirActualTransitionCommands() throws Exception {
        separated(88L,99L);
        verifyTransitions(88L,99L);
    }
    @Test void existingThreeArgumentCallRetainsBothOriginalActorsAndEventKeys() {
        adapter.recordFinalizationStarted(file,88L,"real-round");
        verifyTransitions(88L,88L);
    }
    @Test void newExplicitActorsCannotBeMissingOrSystemDefaults() throws Exception {
        for(var actors:new Long[][]{{null,99L},{88L,null},{-1L,99L},{88L,0L}}) {
            var failure=assertThrows(java.lang.reflect.InvocationTargetException.class,()->separated(actors[0],actors[1]));
            assertInstanceOf(IllegalArgumentException.class,failure.getCause());
        }
        verifyNoInteractions(core);
    }
    private void separated(Long approval,Long operation) throws Exception {
        // Reflective invocation makes the absent authorized overload a real RED without test-only production methods.
        adapter.getClass().getMethod("recordFinalizationStarted",DccControlledFileDO.class,Long.class,Long.class,String.class)
                .invoke(adapter,file,approval,operation,"real-round");
    }
    private void verifyTransitions(Long approval,Long operation) {
        var key=ControlledContentKey.of(1L,ControlledContentType.DCC_CONTROLLED_FILE,"3");
        verify(core).transitionVersionRefByDomainEvent(key,20L,ControlledContentCanonicalStatus.IN_REVIEW,
                ControlledContentCanonicalStatus.READY_TO_PUBLISH,"FINALIZING",ControlledContentTransitionAction.APPROVE,
                approval,"dcc controlled file approved","real-round","real-round:approve");
        verify(core).transitionVersionRefByDomainEvent(key,20L,ControlledContentCanonicalStatus.READY_TO_PUBLISH,
                ControlledContentCanonicalStatus.FINALIZING,"FINALIZING",ControlledContentTransitionAction.START_FINALIZATION,
                operation,"dcc controlled file finalization started","real-round","real-round:start-finalization");
        verifyNoMoreInteractions(core);
    }
}
