package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.craft.MesProCraftChangeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.craft.MesProCraftChangeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProCraftChangeRoutesTest {
    final MesProCraftService service = new MesProCraftService();
    final MesProCraftChangeMapper changes = mock(MesProCraftChangeMapper.class);
    final MesProRouteVersionMapper versions = mock(MesProRouteVersionMapper.class);
    final MesProCraftChangeDO change = new MesProCraftChangeDO();
    MesProCraftChangeRoutesTest() {
        ReflectionTestUtils.setField(service, "changeMapper", changes);
        ReflectionTestUtils.setField(service, "versionMapper", versions);
        change.setId(7L); change.setCandidateIdsJson("[30]");
        when(changes.selectById(7L)).thenReturn(change);
    }
    MesProRouteVersionDO candidate() {
        return MesProRouteVersionDO.builder().id(30L).routeId(10L).sourceRouteVersionId(20L)
                .versionNo("V3").lifecycleStatus("SUPERSEDED")
                .routeSnapshotJson("""
                    {"routeCode":"R-10","routeName":"当时的路线名称","configSnapshots":{"craftChangeId":7}}
                    """).build();
    }
    @Test void readsHistoricalBatchVersionsAndNamesWithoutCurrentMasterData() {
        when(versions.selectBatchIds(anyCollection())).thenAnswer(invocation ->
                ((java.util.Collection<?>)invocation.getArgument(0)).contains(30L) ? List.of(candidate()) :
                        List.of(MesProRouteVersionDO.builder().id(20L).routeId(10L).versionNo("V2").build()));
        var rows = service.changeRoutes(7L);
        assertEquals(1, rows.size());
        assertEquals("当时的路线名称", rows.get(0).get("routeName"));
        assertEquals("V2", rows.get(0).get("versionNo"));
        assertEquals("V3", rows.get(0).get("nextVersionNo"));
        assertEquals("SUPERSEDED", rows.get(0).get("status"));
        verify(versions, times(2)).selectBatchIds(anyCollection());
        verifyNoMoreInteractions(versions);
    }
    @Test void missingVersionsAreReportedRatherThanOmitted() {
        when(versions.selectBatchIds(anyCollection())).thenReturn(List.of());
        assertThrows(RuntimeException.class, () -> service.changeRoutes(7L));
    }
    @Test void unrelatedVersionCannotAppearInChangeHistory() {
        var candidate = candidate(); candidate.setRouteSnapshotJson("{\"configSnapshots\":{\"craftChangeId\":8}}");
        when(versions.selectBatchIds(anyCollection())).thenAnswer(invocation ->
                ((java.util.Collection<?>)invocation.getArgument(0)).contains(30L) ? List.of(candidate) :
                        List.of(MesProRouteVersionDO.builder().id(20L).routeId(10L).versionNo("V2").build()));
        assertThrows(RuntimeException.class, () -> service.changeRoutes(7L));
    }
    @Test void independentCreationHasNoRouteUpgrades() {
        change.setCandidateIdsJson("[]");
        assertTrue(service.changeRoutes(7L).isEmpty());
        verifyNoInteractions(versions);
    }
}
