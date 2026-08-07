package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

public final class MesTeamLeaderActiveOrderRouteLabelsFocusedHarness {

    private MesTeamLeaderActiveOrderRouteLabelsFocusedHarness() {
    }

    public static void main(String[] args) throws Exception {
        shouldReturnFormalRouteLabels();
        shouldFailWhenFormalRouteIsMissing();
        shouldFailWhenVersionDoesNotBelongToRoute();
        System.out.println("PASS: focused active-order route labels backend behavior");
    }

    private static void shouldReturnFormalRouteLabels() throws Exception {
        Fixture fixture = fixture();
        stubActiveOrder(fixture.activeOrderMapper());
        when(fixture.routeMapper().selectBatchIds(List.of(980091L))).thenReturn(List.of(
                MesProRouteDO.builder().id(980091L).name("按压式球囊扩充压力泵工艺路线").build()));
        when(fixture.routeVersionMapper().selectBatchIds(List.of(622L))).thenReturn(List.of(
                MesProRouteVersionDO.builder().id(622L).routeId(980091L).versionNo("V1").build()));

        List<MesTeamLeaderActiveOrderRow> rows = fixture.service().listActiveOrders(3001L);

        require(rows.size() == 1, "expected one active order");
        require("按压式球囊扩充压力泵工艺路线".equals(rows.get(0).getRouteName()),
                "expected formal route name");
        require("V1".equals(rows.get(0).getRouteVersionNo()), "expected formal version number");
    }

    private static void shouldFailWhenFormalRouteIsMissing() throws Exception {
        Fixture fixture = fixture();
        stubActiveOrder(fixture.activeOrderMapper());
        when(fixture.routeMapper().selectBatchIds(List.of(980091L))).thenReturn(List.of());

        ServiceException exception = expectServiceException(() -> fixture.service().listActiveOrders(3001L));

        require(exception.getCode().equals(ErrorCodeConstants.PRO_ROUTE_NOT_EXISTS.getCode()),
                "expected formal route missing error");
    }

    private static void shouldFailWhenVersionDoesNotBelongToRoute() throws Exception {
        Fixture fixture = fixture();
        stubActiveOrder(fixture.activeOrderMapper());
        when(fixture.routeMapper().selectBatchIds(List.of(980091L))).thenReturn(List.of(
                MesProRouteDO.builder().id(980091L).name("按压式球囊扩充压力泵工艺路线").build()));
        when(fixture.routeVersionMapper().selectBatchIds(List.of(622L))).thenReturn(List.of(
                MesProRouteVersionDO.builder().id(622L).routeId(980092L).versionNo("V1").build()));

        ServiceException exception = expectServiceException(() -> fixture.service().listActiveOrders(3001L));

        require(exception.getCode().equals(ErrorCodeConstants.PRO_ROUTE_VERSION_NOT_EXISTS.getCode()),
                "expected route-version identity error");
    }

    private static void stubActiveOrder(MesProcessPoolActiveOrderMapper activeOrderMapper) {
        when(activeOrderMapper.selectActiveListByLeader(3001L)).thenReturn(List.of(
                MesProcessPoolActiveOrderDO.builder()
                        .id(35L)
                        .leaderUserId(3001L)
                        .workOrderId(980022L)
                        .routeId(980091L)
                        .routeVersionId(622L)
                        .activeStatus("ACTIVE")
                        .businessStatus("ACTIVE")
                        .build()));
    }

    private static Fixture fixture() throws Exception {
        Constructor<?> constructor = MesTeamLeaderActiveOrderServiceImpl.class.getConstructors()[0];
        Map<Class<?>, Object> mocks = new LinkedHashMap<>();
        Object[] arguments = new Object[constructor.getParameterCount()];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        for (int index = 0; index < parameterTypes.length; index++) {
            Object mock = Mockito.mock(parameterTypes[index]);
            mocks.put(parameterTypes[index], mock);
            arguments[index] = mock;
        }
        return new Fixture(
                (MesTeamLeaderActiveOrderServiceImpl) constructor.newInstance(arguments),
                (MesProcessPoolActiveOrderMapper) mocks.get(MesProcessPoolActiveOrderMapper.class),
                (MesProRouteMapper) mocks.get(MesProRouteMapper.class),
                (MesProRouteVersionMapper) mocks.get(MesProRouteVersionMapper.class));
    }

    private static ServiceException expectServiceException(ThrowingRunnable action) throws Exception {
        try {
            action.run();
        } catch (ServiceException exception) {
            return exception;
        }
        throw new AssertionError("expected ServiceException");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record Fixture(
            MesTeamLeaderActiveOrderServiceImpl service,
            MesProcessPoolActiveOrderMapper activeOrderMapper,
            MesProRouteMapper routeMapper,
            MesProRouteVersionMapper routeVersionMapper) {
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
