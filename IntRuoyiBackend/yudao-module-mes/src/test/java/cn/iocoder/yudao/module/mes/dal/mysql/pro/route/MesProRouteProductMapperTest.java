package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProRouteProductMapperTest extends BaseDbUnitTest {

    @Resource
    private MesProRouteProductMapper routeProductMapper;

    @Test
    void selectListByItemId_shouldReturnAllRouteProductsForProductOnly() {
        routeProductMapper.insert(buildRouteProduct(1L, 101L, 9001L));
        routeProductMapper.insert(buildRouteProduct(2L, 102L, 9001L));
        routeProductMapper.insert(buildRouteProduct(3L, 103L, 9002L));

        List<MesProRouteProductDO> result = routeProductMapper.selectListByItemId(9001L);

        assertEquals(List.of(101L, 102L), result.stream().map(MesProRouteProductDO::getRouteId).toList());
    }

    private static MesProRouteProductDO buildRouteProduct(Long id, Long routeId, Long itemId) {
        return MesProRouteProductDO.builder()
                .id(id)
                .routeId(routeId)
                .itemId(itemId)
                .quantity(1)
                .productionTime(BigDecimal.ONE)
                .timeUnitType("HOUR")
                .build();
    }

}
