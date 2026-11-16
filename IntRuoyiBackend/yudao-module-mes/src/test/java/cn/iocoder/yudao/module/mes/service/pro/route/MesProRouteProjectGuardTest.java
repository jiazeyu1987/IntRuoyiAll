package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class MesProRouteProjectGuardTest {
    @InjectMocks private MesProRouteProjectGuard guard;
    @Mock private DccProjectCodeMapper projectMapper;
    @Mock private MesRouteDccProjectBindingMapper bindingMapper;
    @Mock private MesProRouteMapper routeMapper;
    @Mock private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccess;

    private void enabledProject() {
        DccProjectCodeDO project = new DccProjectCodeDO();
        project.setId(10L); project.setProjectCode("P10"); project.setProjectName("项目10"); project.setStatus("ENABLE");
        when(projectMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(project);
    }
    @Test void missingProjectIsRejectedBeforeAnyWrite() {
        assertThrows(ServiceException.class, () -> guard.bindNewRoute(20L, null));
        verifyNoInteractions(bindingMapper, routeMapper);
    }
    @Test void disabledProjectIsRejected() {
        DccProjectCodeDO project = new DccProjectCodeDO(); project.setStatus("DISABLE");
        when(projectMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(project);
        assertThrows(ServiceException.class, () -> guard.requireAvailable(10L, null));
        verifyNoInteractions(bindingMapper);
    }
    @Test void disabledRouteStillOccupiesProject() {
        enabledProject();
        when(bindingMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(
                MesRouteDccProjectBindingDO.builder().routeId(99L).dccProjectCodeId(10L).build()));
        when(routeMapper.selectById(99L)).thenReturn(MesProRouteDO.builder().id(99L).name("已存在流程").status(1).build());
        ServiceException error = assertThrows(ServiceException.class, () -> guard.bindNewRoute(20L, 10L));
        assertTrue(error.getMessage().contains("已存在流程"));
        verify(bindingMapper, never()).insert(any(MesRouteDccProjectBindingDO.class));
    }
    @Test void ownRouteCanContinueMaintainingProject() {
        enabledProject();
        when(bindingMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of(
                MesRouteDccProjectBindingDO.builder().routeId(20L).dccProjectCodeId(10L).build()));
        assertDoesNotThrow(() -> guard.requireAvailable(10L, 20L));
        verifyNoInteractions(routeMapper);
    }
    @Test void userWithoutReadableProjectsGetsNoProjectOptions() {
        when(projectAccess.listReadableProjectIds(any())).thenReturn(List.of());
        assertTrue(guard.options("").isEmpty());
        verifyNoInteractions(projectMapper, bindingMapper, routeMapper);
    }
    @Test void reattachedRouteUsesNextHistoricalBindingVersion() {
        enabledProject();
        when(bindingMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
        when(bindingMapper.selectMaxVersionByRouteIdIncludeDeleted(20L)).thenReturn(4L);
        guard.bindNewRoute(20L, 10L);
        var binding = org.mockito.ArgumentCaptor.forClass(MesRouteDccProjectBindingDO.class);
        verify(bindingMapper).insert(binding.capture());
        assertEquals(5L, binding.getValue().getVersion());
        assertEquals(false, binding.getValue().getDeleted());
    }
    @Test void emptyProjectCreatesExactlyOneFormalBinding() {
        enabledProject();
        when(bindingMapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(List.of());
        guard.bindNewRoute(20L, 10L);
        var binding = org.mockito.ArgumentCaptor.forClass(MesRouteDccProjectBindingDO.class);
        verify(bindingMapper).insert(binding.capture());
        assertEquals(20L, binding.getValue().getRouteId());
        assertEquals(10L, binding.getValue().getDccProjectCodeId());
        assertEquals(1L, binding.getValue().getVersion());
    }
}
