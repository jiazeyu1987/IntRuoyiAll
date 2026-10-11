package cn.iocoder.yudao.module.bpm.approval.content;

import cn.iocoder.yudao.module.bpm.approval.core.ApprovalModuleCode;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;

@Component
public final class ApprovalContentResolverRegistry {
    private final EnumMap<ApprovalModuleCode, ApprovalContentResolver> resolvers = new EnumMap<>(ApprovalModuleCode.class);
    public ApprovalContentResolverRegistry(List<ApprovalContentResolver> values) {
        for (var value : values) if (resolvers.put(value.moduleCode(), value) != null)
            throw new IllegalStateException("APPROVAL_CONTENT_OWNER_CONFLICT: " + value.moduleCode());
    }
    public ApprovalContentResolver require(ApprovalModuleCode module) {
        var result = resolvers.get(module);
        if (result == null) throw new IllegalStateException("APPROVAL_CONTENT_RESOLVER_MISSING: " + module);
        return result;
    }
}
