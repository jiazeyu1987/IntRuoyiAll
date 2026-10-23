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
        when(autoCodeRecordService.generateAutoCode("PRO_PROCESS_GX_CODE", null))
                .thenReturn("GX-0001", "GX-0002");
        MesProProcessDO first = MesProProcessDO.builder().code("ER-OLD").name("A").build();
        MesProProcessDO second = MesProProcessDO.builder().code("GX-9999").name("B").build();
        service.create(first);
        service.create(second);
        assertEquals("GX-0001", first.getCode());
        assertEquals("GX-0002", second.getCode());
        verify(processMapper).insert(first);
        verify(processMapper).insert(second);
    }

    @Test void invalidRuleStopsInsert() {
        when(autoCodeRecordService.generateAutoCode("PRO_PROCESS_GX_CODE", null)).thenReturn("ER0001");
        assertThrows(IllegalStateException.class, () -> service.create(new MesProProcessDO()));
        verifyNoInteractions(processMapper);
    }

    @Test void missingRuleStopsInsert() {
        when(autoCodeRecordService.generateAutoCode("PRO_PROCESS_GX_CODE", null))
                .thenThrow(new IllegalStateException("missing rule"));
        assertThrows(IllegalStateException.class, () -> service.create(new MesProProcessDO()));
        verifyNoInteractions(processMapper);
    }

    @Test void missingTenantStopsAllocation() {
        TenantContextHolder.clear();
        assertThrows(NullPointerException.class, () -> service.create(new MesProProcessDO()));
        verifyNoInteractions(autoCodeRecordService, processMapper);
    }

    @Test void existingIdIsNotAnInsert() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create(MesProProcessDO.builder().id(12L).build()));
        verifyNoInteractions(autoCodeRecordService, processMapper);
    }
}
