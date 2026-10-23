package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentLifecycleCoreService;
import cn.iocoder.yudao.module.system.service.controlledcontent.ControlledContentKey;
import cn.iocoder.yudao.module.system.enums.controlledcontent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual domain adapter with the platform core as an explicit command boundary, not proof of DB transition. */
class DccNativeActivationReasonTest {
    @BeforeEach void tenant(){TenantContextHolder.setTenantId(1L);}
    @AfterEach void clear(){TenantContextHolder.clear();}
    @ParameterizedTest @NullSource
    @ValueSource(strings={"AFTER_STANDARDIZATION_SIGNATURE","PRESET_DATE"})
    void reasonExpressesDailyActivationConditionsWhileIdentityAndSignedFactsStayUntouched(String mode){
        var core=mock(ControlledContentLifecycleCoreService.class);var adapter=new DccControlledContentAdapter();
        ReflectionTestUtils.setField(adapter,"lifecycleCoreService",core);
        var file=file(mode);var original=file.toString();adapter.recordNativeActivation(file,-1L,"DCC:1:20:round-1:ACTIVATED");
        var key=ControlledContentKey.of(1L,ControlledContentType.DCC_CONTROLLED_FILE,"3");
        String expected="AFTER_STANDARDIZATION_SIGNATURE".equals(mode)
                ?"DCC已签名实际生效且正式受控完成后日常启用"
                :"PRESET_DATE".equals(mode)?"DCC正式受控且预设日期到期后日常启用":"DCC已受控生效版本日常启用";
        verify(core).transitionDccControlledRef(key,20L,"B/1","round-1",ControlledContentCanonicalStatus.CONTROLLED_PENDING_EFFECTIVE,
                ControlledContentCanonicalStatus.ACTIVE,ControlledContentTransitionAction.ACTIVATE_CONTROLLED,-1L,
                expected,"DCC:1:20:round-1:ACTIVATED",null);
        verifyNoMoreInteractions(core);assertEquals(original,file.toString());
        if(mode==null){assertNull(file.getEffectiveMode());assertNull(file.getEffectiveTime());assertNull(file.getEffectiveDate());}
    }
    @Test void reasonNeverSubstitutesForMissingActualControlledOrActiveFacts(){
        var core=mock(ControlledContentLifecycleCoreService.class);var adapter=new DccControlledContentAdapter();
        ReflectionTestUtils.setField(adapter,"lifecycleCoreService",core);var file=file("AFTER_STANDARDIZATION_SIGNATURE");
        file.setActivatedTime(null);assertThrows(IllegalStateException.class,()->adapter.recordNativeActivation(file,-1L,"event"));
        file.setActivatedTime(LocalDateTime.of(2026,10,11,12,0));file.setControlledTime(null);
        assertThrows(IllegalStateException.class,()->adapter.recordNativeActivation(file,-1L,"event"));verifyNoInteractions(core);
    }
    private DccControlledFileDO file(String mode){var file=DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(3L).versionNo("B/1")
            .processInstanceId("round-1").controlledTime(LocalDateTime.of(2026,10,11,11,0)).activatedTime(LocalDateTime.of(2026,10,11,12,0))
            .publishedFileId(211L).stampedFileId(211L).status("ACTIVE").effectiveMode(mode).build();
        if("AFTER_STANDARDIZATION_SIGNATURE".equals(mode)){file.setEffectiveTime(LocalDateTime.of(2026,10,11,10,0));file.setEffectiveSignatureId(700L);file.setEffectiveProcessInstanceId("round-1");}
        if("PRESET_DATE".equals(mode)){file.setEffectiveDate(java.time.LocalDate.of(2026,10,11));file.setEffectiveTime(LocalDateTime.of(2026,10,11,12,0));file.setEffectiveProcessInstanceId("round-1");}
        return file;}
}
