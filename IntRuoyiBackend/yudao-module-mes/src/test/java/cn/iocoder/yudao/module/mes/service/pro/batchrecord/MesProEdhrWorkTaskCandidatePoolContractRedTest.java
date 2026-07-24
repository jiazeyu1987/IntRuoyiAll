package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrWorkTaskCandidatePoolContractRedTest {

    @Test
    void assignmentRule_shouldSupportRoleAndDeptGroupCandidatePoolSnapshot() {
        Set<String> ruleFields = fieldNames(MesProEdhrWorkTaskAssignmentRuleDO.class);
        assertTrue(ruleFields.contains("candidateSourceType"),
                "T5 requires assignment rule sourceType USER/USER_GROUP/ROLE_GROUP/DEPT_GROUP; current rule only has assigneeUserId/reviewUserId");
        assertTrue(ruleFields.contains("candidateSourceId"),
                "T5 requires assignment rule sourceId to resolve role/dept/user groups before task creation");

        Set<String> taskFields = fieldNames(MesProEdhrWorkTaskDO.class);
        assertTrue(taskFields.contains("candidateSourceType"),
                "T5 requires work task to keep the resolved candidate source type snapshot");
        assertTrue(taskFields.contains("candidateSourceId"),
                "T5 requires work task to keep the resolved candidate source id snapshot");
        assertTrue(taskFields.contains("candidateUserSnapshot"),
                "T5 requires task creation to persist the candidate user snapshot; current model has only assigneeUserId");
    }

    @Test
    void emptyCandidatePool_shouldHaveExplicitFailFastErrorSemantic() {
        Set<String> errorConstants = fieldNames(MesProEdhrWorkTaskErrorCodeConstants.class);
        assertTrue(errorConstants.contains("PRO_EDHR_WORK_TASK_CANDIDATE_POOL_EMPTY"),
                "T5 requires fail-fast when USER_GROUP/ROLE_GROUP/DEPT_GROUP resolves to no authorized users");
        assertTrue(errorConstants.contains("PRO_EDHR_WORK_TASK_CANDIDATE_SOURCE_INVALID"),
                "T5 requires explicit invalid candidate source errors instead of falling back to a single assignee");
    }

    @Test
    void completingOneCandidateSignature_shouldCancelOrBlockPeerCandidateTodos() {
        Set<String> serviceMethods = methodNames(MesProEdhrWorkTaskService.class);
        assertTrue(serviceMethods.contains("completeCandidateSignatureTask"),
                "T5 requires one candidate to complete a signature slot by candidate snapshot, not only by single assignee task");
        assertTrue(serviceMethods.contains("cancelPeerCandidateSignatureTasks"),
                "T5 requires other candidates' same signature todos to be removed or made unprocessable after one candidate signs");
        assertTrue(serviceMethods.contains("getCandidateSignatureTodoPage"),
                "T5 requires candidate-aware todo query; current my-page query filters by assigneeUserId only");
    }

    private static Set<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    private static Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(Method::getName)
                .collect(Collectors.toSet());
    }
}
