package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_SNAPSHOT_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineActiveOrderSnapshotResolverTest {

    private static final long ACTIVE_ORDER_ID = 5001L;
    private static final long WORK_ORDER_ID = 1001L;
    private static final long ROUTE_ID = 2001L;
    private static final long ROUTE_VERSION_ID = 3001L;
    private static final long DCC_PROJECT_CODE_ID = 6001L;
    private static final long QA_REGULATION_ID = 7001L;
    private static final long QA_REGULATION_VERSION_ID = 8001L;

    @Mock
    private MesProcessPoolActiveOrderMapper activeOrderMapper;

    @Test
    void requireEffectiveReturnsServerLockedRouteAndQaSnapshot() {
        when(activeOrderMapper.selectMaps(any())).thenReturn(List.of(validSnapshotRow()));
        ActiveOrderSnapshotResolver resolver = new ActiveOrderSnapshotResolver(activeOrderMapper);

        ActiveOrderSnapshotResolver.ActiveOrderSnapshot snapshot = resolver.requireEffective(ACTIVE_ORDER_ID);

        assertEquals(ACTIVE_ORDER_ID, snapshot.activeOrderId());
        assertEquals(WORK_ORDER_ID, snapshot.workOrderId());
        assertEquals(ROUTE_ID, snapshot.routeId());
        assertEquals(ROUTE_VERSION_ID, snapshot.routeVersionId());
        assertEquals(DCC_PROJECT_CODE_ID, snapshot.dccProjectCodeId());
        assertEquals(QA_REGULATION_ID, snapshot.qaRegulationId());
        assertEquals(QA_REGULATION_VERSION_ID, snapshot.qaRegulationVersionId());

        ArgumentCaptor<Wrapper<MesProcessPoolActiveOrderDO>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(activeOrderMapper).selectMaps(wrapperCaptor.capture());
        QueryWrapper<MesProcessPoolActiveOrderDO> wrapper =
                (QueryWrapper<MesProcessPoolActiveOrderDO>) wrapperCaptor.getValue();
        assertEquals("id AS activeOrderId, work_order_id AS workOrderId, route_id AS routeId, "
                + "route_version_id AS routeVersionId, dcc_project_code_id AS dccProjectCodeId, "
                + "qa_regulation_id AS qaRegulationId, qa_regulation_version_id AS qaRegulationVersionId",
                wrapper.getSqlSelect());
        assertFalse(wrapper.getSqlSegment().contains("product_id"));
        verifyNoMoreInteractions(activeOrderMapper);
    }

    @Test
    void requireEffectiveDoesNotExposeWhetherCrossTenantOrRemovedOrderExists() {
        when(activeOrderMapper.selectMaps(any())).thenReturn(List.of());
        ActiveOrderSnapshotResolver resolver = new ActiveOrderSnapshotResolver(activeOrderMapper);

        ServiceException missing = assertThrows(ServiceException.class,
                () -> resolver.requireEffective(ACTIVE_ORDER_ID));

        assertEquals(PRO_PROCESS_POOL_ACTIVE_ORDER_NOT_EXISTS.getCode(), missing.getCode());
        verify(activeOrderMapper).selectMaps(any());
        verifyNoMoreInteractions(activeOrderMapper);
    }

    @Test
    void requireEffectiveFailsWhenRouteSnapshotIsIncomplete() {
        Map<String, Object> row = validSnapshotRow();
        row.put("routeVersionId", null);
        when(activeOrderMapper.selectMaps(any())).thenReturn(List.of(row));
        ActiveOrderSnapshotResolver resolver = new ActiveOrderSnapshotResolver(activeOrderMapper);

        ServiceException error = assertThrows(ServiceException.class,
                () -> resolver.requireEffective(ACTIVE_ORDER_ID));

        assertEquals(PRO_PROCESS_POOL_ACTIVE_ORDER_ROUTE_REQUIRED.getCode(), error.getCode());
    }

    @Test
    void requireEffectiveFailsWhenQaSnapshotIsIncomplete() {
        Map<String, Object> row = validSnapshotRow();
        row.put("qaRegulationVersionId", null);
        when(activeOrderMapper.selectMaps(any())).thenReturn(List.of(row));
        ActiveOrderSnapshotResolver resolver = new ActiveOrderSnapshotResolver(activeOrderMapper);

        ServiceException error = assertThrows(ServiceException.class,
                () -> resolver.requireEffective(ACTIVE_ORDER_ID));

        assertEquals(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID.getCode(), error.getCode());
    }

    @Test
    void requireEffectiveAcceptsOnlyActiveOrderId() throws NoSuchMethodException {
        Method method = ActiveOrderSnapshotResolver.class.getMethod("requireEffective", Long.class);

        assertEquals(1, method.getParameterCount());
        assertFalse(List.of(ActiveOrderSnapshotResolver.class.getMethods()).stream()
                .anyMatch(candidate -> "requireEffective".equals(candidate.getName())
                        && candidate.getParameterCount() > 1));
    }

    private static Map<String, Object> validSnapshotRow() {
        return new java.util.LinkedHashMap<>(Map.of(
                "activeOrderId", ACTIVE_ORDER_ID,
                "workOrderId", WORK_ORDER_ID,
                "routeId", ROUTE_ID,
                "routeVersionId", ROUTE_VERSION_ID,
                "dccProjectCodeId", DCC_PROJECT_CODE_ID,
                "qaRegulationId", QA_REGULATION_ID,
                "qaRegulationVersionId", QA_REGULATION_VERSION_ID
        ));
    }
}
