package cn.iocoder.yudao.module.mes.service.pro.process;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.service.md.autocode.MesMdAutoCodeRecordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MesProProcessCreationServiceTest {
    @Mock private MesMdAutoCodeRecordService autoCodeRecordService;
    @Mock private MesProProcessMapper processMapper;
    @InjectMocks private MesProProcessCreationService service;

    @BeforeEach void setTenant() { TenantContextHolder.setTenantId(1L); }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void ignoresIncomingCodeForEveryNewProcess() {
        when(autoCodeRecordService.generateProjectCode("PRO_PROCESS_GX_CODE", 42L))
                .thenReturn("P42-GX-0001", "P42-GX-0002");
        MesProProcessDO first = MesProProcessDO.builder().code("ER-OLD").name("A").build();
        MesProProcessDO second = MesProProcessDO.builder().code("GX-9999").name("B").build();
        service.create(first, 42L);
        service.create(second, 42L);
        assertEquals("P42-GX-0001", first.getCode());
        assertEquals("P42-GX-0002", second.getCode());
        verify(processMapper).insert(first);
        verify(processMapper).insert(second);
    }

    @Test void invalidRuleStopsInsert() {
        when(autoCodeRecordService.generateProjectCode("PRO_PROCESS_GX_CODE", 42L)).thenReturn("");
        assertThrows(IllegalStateException.class, () -> service.create(new MesProProcessDO(), 42L));
        verifyNoInteractions(processMapper);
    }

    @Test void missingRuleStopsInsert() {
        when(autoCodeRecordService.generateProjectCode("PRO_PROCESS_GX_CODE", 42L))
                .thenThrow(new IllegalStateException("missing rule"));
        assertThrows(IllegalStateException.class, () -> service.create(new MesProProcessDO(), 42L));
        verifyNoInteractions(processMapper);
    }

    @Test void missingTenantStopsAllocation() {
        TenantContextHolder.clear();
        assertThrows(NullPointerException.class, () -> service.create(new MesProProcessDO(), 42L));
        verifyNoInteractions(autoCodeRecordService, processMapper);
    }

    @Test void existingIdIsNotAnInsert() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(MesProProcessDO.builder().id(12L).build(), 42L));
        verifyNoInteractions(autoCodeRecordService, processMapper);
    }
}
