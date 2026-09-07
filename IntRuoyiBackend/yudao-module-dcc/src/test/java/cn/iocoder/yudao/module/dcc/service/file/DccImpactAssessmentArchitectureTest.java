package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DccImpactAssessmentArchitectureTest {

    @Test
    void impactStateServiceDoesNotDependOnWorkflowOrFinalizationAndMutationsAreTransactional() throws Exception {
        assertFalse(Arrays.stream(DccRelatedFileImpactAssessmentServiceImpl.class.getDeclaredFields())
                .anyMatch(field -> field.getType().equals(DccControlledFileWorkflowService.class)
                        || field.getType().equals(DccControlledFileFinalizationService.class)));
        for (String methodName : List.of("startTask", "submitDecision", "reassignTask", "reopenTask",
                "linkExistingMajorRevision", "resolveLinkedRevisionAfterPublication")) {
            Method method = Arrays.stream(DccRelatedFileImpactAssessmentServiceImpl.class.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
            assertNotNull(method.getAnnotation(Transactional.class), methodName + " must be transactional");
        }
        assertNotNull(DccImpactRevisionCommandService.class.getDeclaredMethod(
                "createAndLinkMajorRevision", Long.class, Long.class, Integer.class, Long.class, String.class)
                .getAnnotation(Transactional.class));
    }
}
