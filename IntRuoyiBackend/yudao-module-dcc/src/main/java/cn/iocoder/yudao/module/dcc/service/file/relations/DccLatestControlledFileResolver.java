package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.FileVersion;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.ControlledEvent;

/** A/C adapter: exact version reads and Master.latestControlledFileId; no currentActive fallback. */
public interface DccLatestControlledFileResolver {
    FileVersion resolveLatest(Long masterId);
    /** Lock-current source pointer and exact selected row in the existing control transaction. */
    FileVersion resolveLatestForUpdate(Long masterId);
    FileVersion resolveSelected(Long controlledFileId);
    /** Current lock read in the caller's transaction, after locking its master; never a cached ordinary read. */
    FileVersion resolveSelectedForUpdate(Long controlledFileId);
    /** Verify the successful controlled fact, version, round, time and tenant inside A's transaction. */
    void assertControlledEvent(ControlledEvent event);
}
