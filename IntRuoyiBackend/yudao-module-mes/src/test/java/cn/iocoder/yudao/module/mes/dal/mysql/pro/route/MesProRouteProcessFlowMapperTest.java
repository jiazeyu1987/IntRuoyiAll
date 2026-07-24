package cn.iocoder.yudao.module.mes.dal.mysql.pro.route;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowEdgeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessFlowLayoutDO;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MesProRouteProcessFlowMapperTest extends BaseDbUnitTest {

    @Resource
    private MesProRouteProcessFlowEdgeMapper edgeMapper;
    @Resource
    private MesProRouteProcessFlowLayoutMapper layoutMapper;
    @Resource
    private DataSource dataSource;

    @Test
    void edgeMapper_shouldSelectAndDeleteByRouteId() {
        edgeMapper.insert(edge(1L, 100L, 11L, 12L, 1L));
        edgeMapper.insert(edge(2L, 100L, 12L, 13L, 1L));
        edgeMapper.insert(edge(3L, 101L, 21L, 22L, 1L));

        assertEquals(List.of(11L, 12L), edgeMapper.selectListByRouteId(100L).stream()
                .map(MesProRouteProcessFlowEdgeDO::getSourceRouteProcessId)
                .toList());

        assertEquals(2, edgeMapper.deleteByRouteIdAndTenantId(100L, 1L));

        assertEquals(0, edgeMapper.selectListByRouteId(100L).size());
        assertEquals(0, countRows("mes_pro_route_process_flow_edge", 100L));
        assertEquals(1, edgeMapper.selectListByRouteId(101L).size());
    }

    @Test
    void edgeMapper_shouldPhysicallyDeleteBeforeReusingSameRouteEdge() {
        edgeMapper.insert(edge(1L, 100L, 11L, 12L, 1L));

        assertEquals(1, edgeMapper.deleteByRouteIdAndTenantId(100L, 1L));
        edgeMapper.insert(edge(2L, 100L, 11L, 12L, 2L));

        assertEquals(1, edgeMapper.selectListByRouteId(100L).size());
        assertEquals(1, countRows("mes_pro_route_process_flow_edge", 100L));
        assertEquals(2L, edgeMapper.selectListByRouteId(100L).get(0).getGraphVersion());
    }

    @Test
    void layoutMapper_shouldSelectDeleteAndReturnMaxGraphVersion() {
        layoutMapper.insert(layout(1L, 100L, 11L, 10, 20, 1L));
        layoutMapper.insert(layout(2L, 100L, 12L, 30, 40, 3L));
        layoutMapper.insert(layout(3L, 101L, 21L, 50, 60, 2L));

        assertEquals(3L, layoutMapper.selectMaxGraphVersionByRouteId(100L));
        assertEquals(List.of(11L, 12L), layoutMapper.selectListByRouteId(100L).stream()
                .map(MesProRouteProcessFlowLayoutDO::getRouteProcessId)
                .toList());

        assertEquals(2, layoutMapper.deleteByRouteIdAndTenantId(100L, 1L));

        assertEquals(0L, layoutMapper.selectMaxGraphVersionByRouteId(100L));
        assertEquals(0, countRows("mes_pro_route_process_flow_layout", 100L));
        assertEquals(1, layoutMapper.selectListByRouteId(101L).size());
    }

    @Test
    void layoutMapper_shouldPhysicallyDeleteBeforeReusingSameRouteProcessLayout() {
        layoutMapper.insert(layout(1L, 100L, 11L, 10, 20, 1L));

        assertEquals(1, layoutMapper.deleteByRouteIdAndTenantId(100L, 1L));
        layoutMapper.insert(layout(2L, 100L, 11L, 30, 40, 2L));

        assertEquals(1, layoutMapper.selectListByRouteId(100L).size());
        assertEquals(1, countRows("mes_pro_route_process_flow_layout", 100L));
        assertEquals(2L, layoutMapper.selectListByRouteId(100L).get(0).getGraphVersion());
    }

    private int countRows(String tableName, Long routeId) {
        return new JdbcTemplate(dataSource).queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE route_id = ?",
                Integer.class,
                routeId
        );
    }

    private static MesProRouteProcessFlowEdgeDO edge(Long id, Long routeId, Long source, Long target, Long version) {
        MesProRouteProcessFlowEdgeDO edge = MesProRouteProcessFlowEdgeDO.builder()
                .id(id)
                .routeId(routeId)
                .sourceRouteProcessId(source)
                .targetRouteProcessId(target)
                .relationType("NORMAL")
                .graphVersion(version)
                .build();
        edge.setTenantId(1L);
        return edge;
    }

    private static MesProRouteProcessFlowLayoutDO layout(Long id, Long routeId, Long routeProcessId,
                                                         Integer x, Integer y, Long version) {
        MesProRouteProcessFlowLayoutDO layout = MesProRouteProcessFlowLayoutDO.builder()
                .id(id)
                .routeId(routeId)
                .routeProcessId(routeProcessId)
                .x(x)
                .y(y)
                .graphVersion(version)
                .build();
        layout.setTenantId(1L);
        return layout;
    }
}
