package cn.iocoder.yudao.module.bpm.service.task;

import org.flowable.task.api.Task;
import java.util.Map;
import java.util.Objects;

/** DCC owns verification; generic BPM endpoints cannot manufacture a signed domain approval. */
public final class DccSignedTaskActionGuard {
    private DccSignedTaskActionGuard() {}
    public static void require(Task task, Long actorId, String action) {
        String definition = task.getProcessDefinitionId();
        if (definition == null || !(definition.startsWith("dcc-controlled-file-upload:")
                || definition.startsWith("dcc-controlled-file-revision:")
                || definition.startsWith("dcc-controlled-file-obsolete:"))) return;
        Map<String,Object> local = task.getTaskLocalVariables();
        if (local == null || !(local.get("dccVerifiedSignatureId") instanceof Number id) || id.longValue() <= 0
                || !Objects.equals(String.valueOf(actorId),String.valueOf(local.get("dccVerifiedSignatureActorId")))
                || !Objects.equals(action,local.get("dccVerifiedSignatureAction")))
            throw new IllegalStateException("DCC 任务必须通过正式文控办理入口完成指派及电子签名");
    }
}
