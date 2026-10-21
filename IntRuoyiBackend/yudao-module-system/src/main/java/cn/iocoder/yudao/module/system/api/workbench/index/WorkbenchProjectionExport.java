package cn.iocoder.yudao.module.system.api.workbench.index;

import java.util.List;
import java.util.Set;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;

/** Module export is called only inside the master's frozen RR transaction and ordered fences. */
public interface WorkbenchProjectionExport {
    String ownerModule();
    Set<String> sourceIds();
    List<Task> exportTasks(long tenantId, Watermark watermark);
    List<Authorization> exportAuthorizations(long tenantId, Watermark watermark);
    /** Explicit producer identities checked against the enabled writer manifest before activation. */
    Set<String> writerIds();
}
