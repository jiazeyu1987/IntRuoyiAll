package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import org.flowable.engine.ProcessEngineConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Real private engine: exclusion must affect both count and pagination, not just visible rows. */
class BpmTodoExclusionPaginationTest {
    static final List<String> DCC = List.of("dcc-controlled-file-upload", "dcc-controlled-file-revision", "dcc-controlled-file-obsolete");

    @Test void nativeTodoExcludesDedicatedDccBeforeCountAndPagination() {
        var config = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl("jdbc:h2:mem:todo_exclusion_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE)
                .setAsyncExecutorActivate(false);
        var engine = config.buildProcessEngine();
        try {
            TenantContextHolder.setTenantId(1L);
            for (String key : List.of("native-todo-regression", DCC.get(0), DCC.get(1), DCC.get(2))) {
                String xml = """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                          xmlns:flowable="http://flowable.org/bpmn" targetNamespace="todo-test">
                          <process id="%s" isExecutable="true">
                            <startEvent id="start"/><sequenceFlow id="a" sourceRef="start" targetRef="work"/>
                            <userTask id="work" name="Waiting" flowable:assignee="100"/>
                            <sequenceFlow id="b" sourceRef="work" targetRef="end"/><endEvent id="end"/>
                          </process>
                        </definitions>
                        """.formatted(key);
                engine.getRepositoryService().createDeployment().tenantId("1").addString(key + ".bpmn20.xml", xml).deploy();
            }
            long time = System.currentTimeMillis();
            var keys = List.of("native-todo-regression", "native-todo-regression", DCC.get(0), DCC.get(1), DCC.get(2), DCC.get(0), DCC.get(1));
            for (int i = 0; i < keys.size(); i++) {
                config.getClock().setCurrentTime(new Date(time + i * 1000L));
                engine.getRuntimeService().startProcessInstanceByKeyAndTenantId(keys.get(i), "1");
            }
            var service = new BpmTaskServiceImpl();
            ReflectionTestUtils.setField(service, "taskService", engine.getTaskService());
            ReflectionTestUtils.setField(service, "repositoryService", engine.getRepositoryService());
            var ordinary = new BpmTaskPageReqVO(); ordinary.setPageNo(1); ordinary.setPageSize(1);
            assertEquals(7L, service.getTaskTodoPage(100L, ordinary).getTotal(), "Default query retains all seven tasks");
            var nativeOnly = new BpmTaskPageReqVO();
            nativeOnly.setExcludedProcessDefinitionKeys(DCC);
            nativeOnly.setPageNo(1); nativeOnly.setPageSize(1);
            var first = service.getTaskTodoPage(100L, nativeOnly);
            assertEquals(2L, first.getTotal(), "Count must exclude all DCC pages, not only the current page");
            assertEquals(1, first.getList().size());
            assertTrue(first.getList().get(0).getProcessDefinitionId().startsWith("native-todo-regression:"));
            nativeOnly.setPageNo(2);
            var second = service.getTaskTodoPage(100L, nativeOnly);
            assertEquals(2L, second.getTotal()); assertEquals(1, second.getList().size());
            assertNotEquals(first.getList().get(0).getId(), second.getList().get(0).getId());
        } finally {
            engine.close();
            config.getClock().reset();
            TenantContextHolder.clear();
        }
    }
}
