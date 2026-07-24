package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MesProRouteCandidateConfigServiceTest {

    @InjectMocks
    private MesProRouteCandidateConfigServiceImpl service;

    @Mock
    private MesProRouteVersionMapper routeVersionMapper;

    @Test
    void saveConfigSnapshot_shouldMergeIntoDraftCandidateWithoutActivatingIt() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(9001L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(2001L)
                .routeSnapshotJson("{\"routeId\":9001,\"routeCode\":\"R-001\"}")
                .build();
        Map<String, Object> flowGraphSnapshot = Map.of(
                "graphVersion", 3L,
                "edges", List.of(Map.of("sourceRouteProcessId", 11L, "targetRouteProcessId", 12L))
        );
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(9001L)).thenReturn(MesProRouteVersionDO.builder()
                .id(2001L)
                .routeId(9001L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build());

        service.saveConfigSnapshot(candidate.getId(), "flowGraph", flowGraphSnapshot);

        ArgumentCaptor<MesProRouteVersionDO> updateCaptor = ArgumentCaptor.forClass(MesProRouteVersionDO.class);
        verify(routeVersionMapper).updateById(updateCaptor.capture());
        MesProRouteVersionDO update = updateCaptor.getValue();
        assertEquals(candidate.getId(), update.getId());
        assertFalse(Boolean.TRUE.equals(update.getActive()));
        JSONObject snapshot = JSON.parseObject(update.getRouteSnapshotJson());
        assertEquals(9001L, snapshot.getLongValue("routeId"));
        assertEquals("R-001", snapshot.getString("routeCode"));
        JSONObject configSnapshots = snapshot.getJSONObject("configSnapshots");
        assertTrue(configSnapshots.containsKey("flowGraph"));
        assertEquals(3L, configSnapshots.getJSONObject("flowGraph").getLongValue("graphVersion"));
    }

    @Test
    void saveConfigSnapshot_shouldRejectActiveVersion() {
        MesProRouteVersionDO active = MesProRouteVersionDO.builder()
                .id(2001L)
                .routeId(9001L)
                .versionNo("V1")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .routeSnapshotJson("{\"routeId\":9001}")
                .build();
        when(routeVersionMapper.selectById(active.getId())).thenReturn(active);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.saveConfigSnapshot(active.getId(), "flowGraph", Map.of("graphVersion", 4L)));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE.getCode(), ex.getCode());
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
    }

    @Test
    void saveConfigSnapshot_shouldRejectWhenSourceActiveVersionDrifted() {
        MesProRouteVersionDO candidate = MesProRouteVersionDO.builder()
                .id(2002L)
                .routeId(9001L)
                .versionNo("V2")
                .active(Boolean.FALSE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_DRAFT)
                .sourceRouteVersionId(2001L)
                .routeSnapshotJson("{\"routeId\":9001,\"routeCode\":\"R-001\"}")
                .build();
        MesProRouteVersionDO currentActive = MesProRouteVersionDO.builder()
                .id(2003L)
                .routeId(9001L)
                .versionNo("V3")
                .active(Boolean.TRUE)
                .lifecycleStatus(MesProRouteVersionLifecycleServiceImpl.STATUS_ACTIVE)
                .build();
        when(routeVersionMapper.selectById(candidate.getId())).thenReturn(candidate);
        when(routeVersionMapper.selectActiveByRouteId(9001L)).thenReturn(currentActive);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.saveConfigSnapshot(candidate.getId(), "flowGraph", Map.of("graphVersion", 5L)));

        assertEquals(ErrorCodeConstants.PRO_ROUTE_VERSION_CONFLICT.getCode(), ex.getCode());
        verify(routeVersionMapper, never()).updateById(any(MesProRouteVersionDO.class));
    }
}
