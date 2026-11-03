package cn.iocoder.yudao.module.mes.service.pro.batchrecordreport;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.service.pro.process.MesProProcessCreationService;
import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteService;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MesProBatchRecordProjectProcessTest {

    @Test
    void newProjectsWithSameProcessNameCreateIndependentDefinitions() {
        MesProBatchRecordRouteGenerationServiceImpl service = new MesProBatchRecordRouteGenerationServiceImpl();
        MesProProcessMapper mapper = mock(MesProProcessMapper.class);
        MesProProcessCreationService creation = mock(MesProProcessCreationService.class);
        ReflectionTestUtils.setField(service, "processMapper", mapper);
        ReflectionTestUtils.setField(service, "processCreationService", creation);
        AtomicLong nextId = new AtomicLong(100);
        when(creation.create(any())).thenAnswer(call -> {
            MesProProcessDO process = call.getArgument(0);
            process.setId(nextId.incrementAndGet());
            process.setCode("GX-" + process.getId());
            return process.getId();
        });

        MesProProcessDO first = ReflectionTestUtils.invokeMethod(service, "createProjectProcess", "粗洗工序", "项目甲");
        MesProProcessDO second = ReflectionTestUtils.invokeMethod(service, "createProjectProcess", "粗洗工序", "项目乙");

        assertNotEquals(first.getId(), second.getId());
        assertNotEquals(first.getCode(), second.getCode());
        assertEquals("项目甲", first.getProductName());
        assertEquals("项目乙", second.getProductName());
        assertEquals("粗洗工序", first.getName());
        assertEquals(0, first.getStatus());
        verify(creation).create(first);
        verify(creation).create(second);
        verifyNoInteractions(mapper);
    }

    @Test
    void candidateReusesOwnActiveProcessesInRouteOrder() throws Exception {
        Object first = activeProcess(11L, 101L, "粗洗工序", 1);
        Object second = activeProcess(22L, 102L, "粗洗工序", 3);
        Object other = activeProcess(33L, 103L, "精洗工序", 2);
        Map<Long, List<Object>> processes = new LinkedHashMap<>();
        processes.put(22L, List.of(second));
        processes.put(33L, List.of(other));
        processes.put(11L, List.of(first));
        Object source = activeSource(processes);

        assertSame(first, ReflectionTestUtils.invokeMethod(source, "findByName", "粗洗工序", 1));
        assertSame(second, ReflectionTestUtils.invokeMethod(source, "findByName", "粗洗工序", 2));
        assertSame(other, ReflectionTestUtils.invokeMethod(source, "findByName", "精洗工序", 1));
    }

    @Test
    void candidateCannotReuseAnotherRoutesSameNamedProcess() throws Exception {
        Object own = activeProcess(11L, 101L, "粗洗工序", 1);
        Object another = activeProcess(22L, 201L, "粗洗工序", 1);
        Object ownSource = activeSource(Map.of(11L, List.of(own)));
        Object anotherSource = activeSource(Map.of(22L, List.of(another)));

        assertSame(own, ReflectionTestUtils.invokeMethod(ownSource, "findByName", "粗洗工序", 1));
        assertSame(another, ReflectionTestUtils.invokeMethod(anotherSource, "findByName", "粗洗工序", 1));
        assertNull(ReflectionTestUtils.invokeMethod(ownSource, "findByName", "粗洗工序", 2));
        assertNull(ReflectionTestUtils.invokeMethod(ownSource, "findByName", "新增工序", 1));
    }

    @Test
    void activeGraphResolvesNamesThroughItsOwnFrozenProcessIds() {
        MesProBatchRecordRouteGenerationServiceImpl service = new MesProBatchRecordRouteGenerationServiceImpl();
        MesProProcessMapper mapper = mock(MesProProcessMapper.class);
        MesProRouteService routeService = mock(MesProRouteService.class);
        ReflectionTestUtils.setField(service, "processMapper", mapper);
        ReflectionTestUtils.setField(service, "routeService", routeService);
        when(routeService.buildCurrentRouteSnapshotJson(10L, 20L)).thenReturn("""
                {"configSnapshots":{"flowGraph":{"nodes":[
                  {"routeProcessId":101,"processId":11,"sort":1}
                ]},"batchUseConfigs":[]}}
                """);
        when(mapper.selectBatchIds(List.of(11L))).thenReturn(List.of(
                MesProProcessDO.builder().id(11L).name("粗洗工序").build()));

        Object source = ReflectionTestUtils.invokeMethod(service, "loadActiveRouteCandidateSource",
                MesProRouteDO.builder().id(10L).build(), MesProRouteVersionDO.builder().id(20L).build());

        Object process = ReflectionTestUtils.invokeMethod(source, "findByName", "粗洗工序", 1);
        assertNotNull(process);
        assertEquals(11L, (Long) ReflectionTestUtils.invokeMethod(process, "processId"));
        verify(mapper).selectBatchIds(List.of(11L));
        verifyNoMoreInteractions(mapper);
    }

    private Object activeProcess(Long processId, Long routeProcessId, String name, int sort) throws Exception {
        Class<?> type = Class.forName(MesProBatchRecordRouteGenerationServiceImpl.class.getName()
                + "$ActiveRouteProcessSnapshot");
        Constructor<?> constructor = type.getDeclaredConstructor(Map.class, Long.class, Long.class, int.class);
        constructor.setAccessible(true);
        return constructor.newInstance(Map.of("processName", name, "sort", sort), processId, routeProcessId, 1);
    }

    private Object activeSource(Map<Long, List<Object>> processes) throws Exception {
        Class<?> type = Class.forName(MesProBatchRecordRouteGenerationServiceImpl.class.getName()
                + "$ActiveRouteCandidateSource");
        Constructor<?> constructor = type.getDeclaredConstructor(Map.class, Map.class, JSONObject.class);
        constructor.setAccessible(true);
        return constructor.newInstance(processes, Map.of(), new JSONObject());
    }
}
