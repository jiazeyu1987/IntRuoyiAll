package cn.iocoder.yudao.module.system.api.workbench.index;

import java.util.List;
import java.util.function.Supplier;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;

/** The caller enters before any authoritative write; facts and the entire outbox batch share that transaction. */
public interface WorkbenchCommitProtocol {
    <T> T tenantWrite(long tenantId, boolean securityChanged, Supplier<T> businessWrite,
                      Supplier<List<Change>> committedFacts);
    <T> T globalWrite(boolean securityChanged, String registryRevision, Supplier<T> businessWrite);
    record Change(EventKind kind, Task task, Authorization authorization) {
        public Change {
            if((kind!=EventKind.TASK&&kind!=EventKind.AUTHORIZATION)
                || (kind==EventKind.TASK && (task==null||authorization!=null))
                || (kind==EventKind.AUTHORIZATION && (authorization==null||task!=null)))
                throw new IllegalArgumentException("Invalid neutral change");
        }
        public static Change task(Task task) { return new Change(EventKind.TASK,task,null); }
        public static Change authorization(Authorization authorization) { return new Change(EventKind.AUTHORIZATION,null,authorization); }
    }
}
