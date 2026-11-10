package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.module.bpm.framework.flowable.core.util.BpmnModelUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.bpm.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.bpm.framework.flowable.core.enums.BpmnVariableConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BpmTaskServiceImplNextAssigneesTest {
    private final BpmTaskServiceImpl service = new BpmTaskServiceImpl();

    @Test
    void obsoleteSupervisorApprovalCopiesFrozenMapAndKeepsApprovalAndDepartmentObligations() {
        Map<String, List<Long>> frozenApprove = frozen(Map.of("MATRIX_APPROVAL", List.of(910521L)));
        Map<String, List<Long>> frozenStart = frozen(Map.of("MATRIX_REVIEW", List.of(910516L, 910516L)));
        Map<String, Object> original = Map.of(
                PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, frozenApprove,
                PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES, frozenStart);
        Map<String, Object> result = execute(model(34, "MATRIX_REVIEW"), original,
                new HashMap<>(original), Map.of("MATRIX_REVIEW", List.of(910516L, 910516L)));

        Map<String, List<Long>> updated = selected(result, 34);
        assertEquals(Map.of("MATRIX_APPROVAL", List.of(910521L),
                "MATRIX_REVIEW", List.of(910516L, 910516L)), updated);
        assertNotSame(frozenApprove, updated);
        assertEquals(Map.of("MATRIX_APPROVAL", List.of(910521L)), frozenApprove);
        assertSame(frozenStart, result.get(PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES));
    }

    @ParameterizedTest
    @ValueSource(ints = {35, 34})
    void frozenMapsAccumulateAllNextNodesWithoutMutatingExistingEvidence(int strategy) {
        String key = key(strategy);
        Map<String, List<Long>> frozen = frozen(Map.of("EXISTING", List.of(8L)));
        Map<String, Object> original = Map.of(key, frozen);
        Map<String, Object> result = execute(model(strategy, "A", "B"), original,
                new HashMap<>(original), Map.of("A", List.of(9L), "B", List.of(10L)));
        assertEquals(Map.of("EXISTING", List.of(8L), "A", List.of(9L), "B", List.of(10L)), selected(result, strategy));
        assertEquals(Map.of("EXISTING", List.of(8L)), frozen);
    }

    @Test
    void currentApprovalSelectionIsNotOverwrittenByThePreviousSelection() {
        Map<String, List<Long>> frozen = frozen(Map.of("A", List.of(8L), "EXISTING", List.of(7L)));
        Map<String, Object> original = Map.of(PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES, frozen);
        Map<String, Object> result = execute(model(34, "A"), original, new HashMap<>(original), Map.of("A", List.of(9L)));
        assertEquals(List.of(9L), selected(result, 34).get("A"));
        assertEquals(List.of(8L), frozen.get("A"));
        assertEquals(List.of(7L), selected(result, 34).get("EXISTING"));
    }

    @Test
    void preconfiguredStartSelectionCannotBeOverwritten() {
        Map<String, List<Long>> frozen = frozen(Map.of("A", List.of(8L)));
        Map<String, Object> original = Map.of(PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES, frozen);
        Map<String, Object> result = execute(model(35, "A"), original, new HashMap<>(original), Map.of("A", List.of(9L)));
        assertEquals(List.of(8L), selected(result, 35).get("A"));
    }

    @ParameterizedTest
    @ValueSource(ints = {35, 34})
    void missingRequiredSelectionStillFails(int strategy) {
        assertServiceException(() -> execute(model(strategy, "A"), Map.of(), new HashMap<>(), Map.of()),
                strategy == 35 ? PROCESS_INSTANCE_START_USER_SELECT_ASSIGNEES_NOT_CONFIG
                        : PROCESS_INSTANCE_APPROVE_USER_SELECT_ASSIGNEES_NOT_CONFIG, "A");
    }

    @ParameterizedTest
    @ValueSource(ints = {35, 34})
    void absentMapsAreCreatedAndKeepAllNextNodes(int strategy) {
        Map<String, Object> result = execute(model(strategy, "A", "B"), Map.of(), new HashMap<>(),
                Map.of("A", List.of(9L), "B", List.of(10L)));
        assertEquals(Map.of("A", List.of(9L), "B", List.of(10L)), selected(result, strategy));
    }

    private Map<String, Object> execute(BpmnModel model, Map<String, Object> original,
                                        Map<String, Object> variables, Map<String, List<Long>> selection) {
        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getProcessVariables()).thenReturn(original);
        return ReflectionTestUtils.invokeMethod(service, "validateAndSetNextAssignees",
                "CURRENT", variables, model, selection, instance);
    }

    private static Map<String, List<Long>> frozen(Map<String, List<Long>> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<Long>> selected(Map<String, Object> variables, int strategy) {
        return (Map<String, List<Long>>) variables.get(key(strategy));
    }

    private static String key(int strategy) {
        return strategy == 35 ? PROCESS_INSTANCE_VARIABLE_START_USER_SELECT_ASSIGNEES
                : PROCESS_INSTANCE_VARIABLE_APPROVE_USER_SELECT_ASSIGNEES;
    }

    private static BpmnModel model(int strategy, String... nextNodes) {
        StringBuilder xml = new StringBuilder("""
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                    xmlns:flowable="http://flowable.org/bpmn" targetNamespace="test">
                  <process id="dcc-controlled-file-obsolete" isExecutable="true">
                    <userTask id="CURRENT"/>
                """);
        for (String id : nextNodes) {
            xml.append("<userTask id=\"").append(id).append("\" name=\"").append(id)
                    .append("\"><extensionElements><flowable:candidateStrategy>").append(strategy)
                    .append("</flowable:candidateStrategy></extensionElements></userTask>")
                    .append("<sequenceFlow id=\"to_").append(id).append("\" sourceRef=\"CURRENT\" targetRef=\"")
                    .append(id).append("\"/>");
        }
        return BpmnModelUtils.getBpmnModel(xml.append("</process></definitions>").toString().getBytes(StandardCharsets.UTF_8));
    }
}
