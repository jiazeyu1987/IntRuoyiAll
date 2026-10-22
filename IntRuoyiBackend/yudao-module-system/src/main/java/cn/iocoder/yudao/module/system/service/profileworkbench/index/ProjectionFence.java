package cn.iocoder.yudao.module.system.service.profileworkbench.index;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.WorkbenchFailure.*;

public final class ProjectionFence {
    private ProjectionFence() { }
    public record Version(String generation, String registryRevision, List<String> sources, long globalHead,
                          long tenantHead, long globalSecurityEpoch, long tenantSecurityEpoch, long visibilityVersion,
                          Instant authorizationAt, Instant authorizationExpiresAt, String queryHash) {
        public Version {
            text(generation,64); text(registryRevision,128); sources=List.copyOf(sources);
            if(sources.stream().anyMatch(Objects::isNull)||sources.stream().distinct().count()!=sources.size()
                ||globalHead<0||tenantHead<0||globalSecurityEpoch<0||tenantSecurityEpoch<0||visibilityVersion<1
                ||authorizationAt==null||queryHash==null
                ||(authorizationExpiresAt!=null&&!authorizationAt.isBefore(authorizationExpiresAt)))
                throw new IllegalArgumentException("Invalid fenced version");
        }
    }
    public static void caughtUp(Global global,Tenant tenant,Checkpoint checkpoint) {
        if(global==null||tenant==null||checkpoint==null||tenant.activeGeneration()==null)
            throw PROJECTION_CONTEXT_MISSING.exception();
        if(!tenant.activeGeneration().equals(checkpoint.generation())||tenant.tenantId()!=checkpoint.tenantId()
            ||!checkpoint.complete()||checkpoint.globalCheckpoint()!=global.head()
            ||checkpoint.tenantGlobalCheckpoint()!=global.head()||checkpoint.tenantCheckpoint()!=tenant.head())
            throw PROJECTION_NOT_CAUGHT_UP.exception();
    }
    public static void current(Version version,Global global,Tenant tenant,long visibility,String revision,
                               List<String> sources,String queryHash,Instant now) {
        alive(version,now);
        if(!version.generation().equals(tenant.activeGeneration())||!version.registryRevision().equals(revision)
            ||!revision.equals(global.registryRevision())||!version.sources().equals(sources)
            ||version.globalHead()!=global.head()||version.tenantHead()!=tenant.head()
            ||version.globalSecurityEpoch()!=global.securityEpoch()||version.tenantSecurityEpoch()!=tenant.securityEpoch()
            ||version.visibilityVersion()!=visibility||!version.queryHash().equals(queryHash)) throw SNAPSHOT_MISMATCH.exception();
    }
    public static void alive(Version version,Instant now) {
        if(now.isBefore(version.authorizationAt())
            ||(version.authorizationExpiresAt()!=null&&!now.isBefore(version.authorizationExpiresAt()))) throw VERSION_EXPIRED.exception();
    }
}
