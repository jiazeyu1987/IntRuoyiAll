package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesFrontlineSequenceGuardTest {
    // Start -> A -> (B, C) -> D -> End
    private JSONObject graph() {
        return JSON.parseObject("""
            {"nodes":[{"routeProcessId":1},{"routeProcessId":2},{"routeProcessId":3},{"routeProcessId":4}],
             "edges":[
              {"sourceRouteProcessId":1,"targetRouteProcessId":2,"relationType":"NORMAL"},
              {"sourceRouteProcessId":1,"targetRouteProcessId":3,"relationType":"NORMAL"},
              {"sourceRouteProcessId":2,"targetRouteProcessId":4,"relationType":"NORMAL"},
              {"sourceRouteProcessId":3,"targetRouteProcessId":4,"relationType":"NORMAL"}],
             "boundaryEdges":[{"boundaryType":"START","routeProcessId":1},{"boundaryType":"END","routeProcessId":4}]}
            """);
    }
    private MesFrontlineActiveOrderProcess progress(long id, String quantity) {
        return new MesFrontlineActiveOrderProcess(1L, 2L, 3L, "R", "路线", id, id, "P" + id,
                "工序" + id, (int) id, 1L, "W", "工位", BigDecimal.ONE,
                new BigDecimal("100"), new BigDecimal(quantity), false);
    }
    @Test void serialRequiresFullQuantityAndReportsBlockingProcess() {
        var failure = assertThrows(ServiceException.class, () -> MesFrontlineSequenceGuard.requirePredecessorsComplete(
                graph(), 2L, List.of(progress(1, "99.999"))));
        assertTrue(failure.getMessage().contains("工序1"));
        assertDoesNotThrow(() -> MesFrontlineSequenceGuard.requirePredecessorsComplete(graph(), 2L,
                List.of(progress(1, "100"))));
    }
    @Test void parallelBranchDoesNotWaitForSibling() {
        assertDoesNotThrow(() -> MesFrontlineSequenceGuard.requirePredecessorsComplete(graph(), 3L,
                List.of(progress(1, "100"), progress(2, "0"))));
    }
    @Test void joinRequiresEveryPredecessor() {
        assertThrows(ServiceException.class, () -> MesFrontlineSequenceGuard.requirePredecessorsComplete(graph(), 4L,
                List.of(progress(2, "100"), progress(3, "99"))));
        assertDoesNotThrow(() -> MesFrontlineSequenceGuard.requirePredecessorsComplete(graph(), 4L,
                List.of(progress(2, "100"), progress(3, "110"))));
    }
    @Test void firstProcessCanSubmit() {
        assertDoesNotThrow(() -> MesFrontlineSequenceGuard.requirePredecessorsComplete(graph(), 1L, List.of()));
    }
    @Test void missingGraphProgressOrBoundaryFailsClosed() {
        assertThrows(ServiceException.class, () -> MesFrontlineSequenceGuard.requirePredecessorsComplete(null, 1L, List.of()));
        assertThrows(ServiceException.class, () -> MesFrontlineSequenceGuard.requirePredecessorsComplete(graph(), 2L, List.of()));
        var incomplete = graph();
        incomplete.getJSONArray("boundaryEdges").clear();
        assertThrows(ServiceException.class, () -> MesFrontlineSequenceGuard.requirePredecessorsComplete(incomplete, 1L, List.of()));
    }
    @Test void disabledDoesNotReadRouteOrProgress() {
        var settings = mock(MesFrontlineSequenceSettingService.class);
        var versions = mock(MesProRouteVersionMapper.class);
        var processes = mock(MesFrontlineActiveOrderProcessService.class);
        new MesFrontlineSequenceGuard(settings, versions, processes).requireAllowed(null, 1L);
        verifyNoInteractions(versions, processes);
    }
    @Test void cycleCannotBeTreatedAsParallel() {
        var cyclic = graph();
        cyclic.getJSONArray("edges").add(JSON.parseObject("{\"sourceRouteProcessId\":4,\"targetRouteProcessId\":2,\"relationType\":\"NORMAL\"}"));
        assertThrows(ServiceException.class, () -> MesFrontlineSequenceGuard.requirePredecessorsComplete(cyclic, 1L, List.of()));
    }
}
