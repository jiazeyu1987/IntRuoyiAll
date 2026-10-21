package cn.iocoder.yudao.module.system.api.workbench.index;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** System owns permission facts; modules declare their formal source policy in this neutral language. */
public interface WorkbenchAuthorityExport {
    enum Mode { ANY_PERMISSION, ANY_ROLE_OR_SUPER_ADMIN }
    record Requirement(Mode mode,Set<String> values) {
        public Requirement { if(mode==null||values==null||values.isEmpty()) throw new IllegalArgumentException("Missing source policy");
            values=Set.copyOf(values); values.forEach(value->ProjectionModel.text(value,128)); }
    }
    record Grant(long userId,String grantKey,Instant validFrom,Instant validUntil) {
        public Grant { if(userId<1||validFrom==null||(validUntil!=null&&!validFrom.isBefore(validUntil))) throw new IllegalArgumentException("Invalid authority grant");
            ProjectionModel.text(grantKey,64); }
    }
    List<Grant> exportGrants(long tenantId,Requirement requirement);
}
