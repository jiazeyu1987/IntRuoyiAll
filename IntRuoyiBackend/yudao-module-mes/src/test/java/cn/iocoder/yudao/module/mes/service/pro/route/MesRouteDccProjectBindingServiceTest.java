package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.dcc.MesRouteDccProjectBindingSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_PROJECT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_BINDING_VERSION_CONFLICT;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesRouteDccProjectBindingServiceTest {

    @InjectMocks
    private MesRouteDccProjectBindingServiceImpl service;

    @Mock
    private MesRouteDccProjectBindingMapper bindingMapper;
    @Mock
    private MesProRouteMapper routeMapper;
    @Mock
    private DccProjectCodeMapper dccProjectCodeMapper;

    @Test
    void bindInitialRoute_createsVersionOneFromExpectedZero() {
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L)).thenReturn(null);
        when(bindingMapper.selectMaxVersionByRouteIdIncludeDeleted(100L)).thenReturn(null);
        when(dccProjectCodeMapper.selectById(200L)).thenReturn(dccProjectCode(200L, DccProjectCodeStatusConstants.ENABLE));

        MesRouteDccProjectBindingRespVO result = service.saveBinding(new MesRouteDccProjectBindingSaveReqVO()
                .setRouteId(100L)
                .setDccProjectCodeId(200L)
                .setExpectedVersion(0L));

        ArgumentCaptor<MesRouteDccProjectBindingDO> captor =
                ArgumentCaptor.forClass(MesRouteDccProjectBindingDO.class);
        verify(bindingMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getRouteId());
        assertEquals(200L, captor.getValue().getDccProjectCodeId());
        assertEquals(1L, captor.getValue().getVersion());
        assertFalse(Boolean.TRUE.equals(captor.getValue().getDeleted()));
        assertEquals(1L, result.getVersion());
        assertEquals(200L, result.getDccProjectCodeId());
        assertTrue(result.getBound());
    }

    @Test
    void rebind_rejectsStaleExpectedVersionAndKeepsCurrentBinding() {
        MesRouteDccProjectBindingDO current = binding(10L, 100L, 200L, 3L, false);
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L)).thenReturn(current);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveBinding(
                new MesRouteDccProjectBindingSaveReqVO()
                        .setRouteId(100L)
                        .setDccProjectCodeId(201L)
                        .setExpectedVersion(2L)));

        assertEquals(PRO_ROUTE_DCC_BINDING_VERSION_CONFLICT.getCode(), ex.getCode());
        verify(bindingMapper, never()).markDeletedById(any());
        verify(bindingMapper, never()).insert(any(MesRouteDccProjectBindingDO.class));
    }

    @Test
    void rebind_closesOldCurrentAndCreatesNextVersion() {
        MesRouteDccProjectBindingDO current = binding(10L, 100L, 200L, 3L, false);
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L)).thenReturn(current);
        when(dccProjectCodeMapper.selectById(201L)).thenReturn(dccProjectCode(201L, DccProjectCodeStatusConstants.ENABLE));

        MesRouteDccProjectBindingRespVO result = service.saveBinding(new MesRouteDccProjectBindingSaveReqVO()
                .setRouteId(100L)
                .setDccProjectCodeId(201L)
                .setExpectedVersion(3L));

        verify(bindingMapper).markDeletedById(10L);
        ArgumentCaptor<MesRouteDccProjectBindingDO> captor =
                ArgumentCaptor.forClass(MesRouteDccProjectBindingDO.class);
        verify(bindingMapper).insert(captor.capture());
        assertEquals(201L, captor.getValue().getDccProjectCodeId());
        assertEquals(4L, captor.getValue().getVersion());
        assertEquals(4L, result.getVersion());
        assertEquals(201L, result.getDccProjectCodeId());
        assertTrue(result.getBound());
    }

    @Test
    void bind_rejectsDisabledDccProjectCode() {
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L)).thenReturn(null);
        when(bindingMapper.selectMaxVersionByRouteIdIncludeDeleted(100L)).thenReturn(null);
        when(dccProjectCodeMapper.selectById(200L)).thenReturn(dccProjectCode(200L, DccProjectCodeStatusConstants.DISABLE));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.saveBinding(
                new MesRouteDccProjectBindingSaveReqVO()
                        .setRouteId(100L)
                        .setDccProjectCodeId(200L)
                        .setExpectedVersion(0L)));

        assertEquals(PRO_ROUTE_DCC_PROJECT_INVALID.getCode(), ex.getCode());
        verify(bindingMapper, never()).insert(any(MesRouteDccProjectBindingDO.class));
    }

    @Test
    void unbind_writesTombstoneVersionAndReturnsUnboundState() {
        MesRouteDccProjectBindingDO current = binding(10L, 100L, 200L, 3L, false);
        when(routeMapper.selectByIdForUpdate(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteIdForUpdate(100L)).thenReturn(current);

        MesRouteDccProjectBindingRespVO result = service.deleteBinding(100L, 3L);

        verify(bindingMapper).markDeletedById(10L);
        ArgumentCaptor<MesRouteDccProjectBindingDO> captor =
                ArgumentCaptor.forClass(MesRouteDccProjectBindingDO.class);
        verify(bindingMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getRouteId());
        assertEquals(200L, captor.getValue().getDccProjectCodeId());
        assertEquals(4L, captor.getValue().getVersion());
        assertTrue(Boolean.TRUE.equals(captor.getValue().getDeleted()));
        assertEquals(4L, result.getVersion());
        assertNull(result.getDccProjectCodeId());
        assertFalse(result.getBound());
    }

    @Test
    void getBinding_returnsLatestHistoryVersionWhenUnbound() {
        when(routeMapper.selectById(100L)).thenReturn(route(100L));
        when(bindingMapper.selectCurrentByRouteId(100L)).thenReturn(null);
        when(bindingMapper.selectMaxVersionByRouteIdIncludeDeleted(100L)).thenReturn(4L);

        MesRouteDccProjectBindingRespVO result = service.getBinding(100L);

        assertEquals(4L, result.getVersion());
        assertNull(result.getDccProjectCodeId());
        assertFalse(result.getBound());
    }

    @Test
    void getBinding_rejectsMissingRoute() {
        when(routeMapper.selectById(100L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.getBinding(100L));

        assertEquals(PRO_ROUTE_NOT_EXISTS.getCode(), ex.getCode());
    }

    private MesProRouteDO route(Long routeId) {
        return MesProRouteDO.builder().id(routeId).code("RT-" + routeId).name("route").build();
    }

    private MesRouteDccProjectBindingDO binding(Long id, Long routeId, Long dccProjectCodeId,
                                                Long version, Boolean deleted) {
        MesRouteDccProjectBindingDO binding = MesRouteDccProjectBindingDO.builder()
                .id(id)
                .routeId(routeId)
                .dccProjectCodeId(dccProjectCodeId)
                .version(version)
                .build();
        binding.setDeleted(deleted);
        return binding;
    }

    private DccProjectCodeDO dccProjectCode(Long id, String status) {
        return DccProjectCodeDO.builder()
                .id(id)
                .projectCode("DCC-" + id)
                .projectName("project-" + id)
                .status(status)
                .build();
    }
}
