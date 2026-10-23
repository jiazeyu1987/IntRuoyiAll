package cn.iocoder.yudao.module.bpm.service.task;
import org.junit.jupiter.api.Test;
import org.flowable.task.api.Task;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccWorkflowSignedTaskActionGuardTest {
    @Test void unsignedAndWrongActorOrActionAreRejected() {
        Task task = mock(Task.class);
        when(task.getProcessDefinitionId()).thenReturn("dcc-controlled-file-obsolete:4:1");
        assertThrows(IllegalStateException.class,()->DccSignedTaskActionGuard.require(task,99L,"APPROVE"));
        when(task.getTaskLocalVariables()).thenReturn(Map.of("dccVerifiedSignatureId",1L,
                "dccVerifiedSignatureActorId",99L,"dccVerifiedSignatureAction","APPROVE"));
        assertDoesNotThrow(()->DccSignedTaskActionGuard.require(task,99L,"APPROVE"));
        assertThrows(IllegalStateException.class,()->DccSignedTaskActionGuard.require(task,100L,"APPROVE"));
        assertThrows(IllegalStateException.class,()->DccSignedTaskActionGuard.require(task,99L,"REJECT"));
    }
    @Test void otherModulesKeepTheirOwnSignatureBoundary() {
        Task task = mock(Task.class);
        when(task.getProcessDefinitionId()).thenReturn("mes-other:1:1");
        assertDoesNotThrow(()->DccSignedTaskActionGuard.require(task,99L,"APPROVE"));
    }
}
