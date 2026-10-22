package cn.iocoder.yudao.module.dcc.service.file;

/** Authoritative relation read contract. IC32 reads the independently maintained User Management relation. */
public interface DccDirectSupervisorSource {
    record Context(Long tenantId,Long requesterId,Long controlledFileId,Long projectId,String applicationType) { }
    record Evidence(Long tenantId,Long requesterId,Long supervisorUserId,String sourceSystem,String sourceIdentity,String sourceVersion) { }
    /** Must reject missing/ambiguous source, cross-tenant identity, disabled or invalid account.
     * Caller additionally verifies the enabled account and freezes this exact source evidence.
     * Unknown provider is a not-ready dependency, never a department leader or client supplied user. */
    Evidence require(Context context);
}
