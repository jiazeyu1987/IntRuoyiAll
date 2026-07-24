package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomPublicSiteBindingDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomPublicSiteBindingMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Service
public class ShowroomPublicReleaseScopeResolver {

    private static final Set<String> STAGES = Set.of("TEST", "PROD");

    private final ShowroomPublicSiteBindingMapper siteBindingMapper;

    public ShowroomPublicReleaseScopeResolver(ShowroomPublicSiteBindingMapper siteBindingMapper) {
        this.siteBindingMapper = siteBindingMapper;
    }

    public ShowroomReleaseScope resolve(String siteKey, String stage) {
        ScopeSelector selector = normalizeSelector(siteKey, stage);
        ShowroomPublicSiteBindingDO binding = siteBindingMapper.selectEnabledBySiteStage(selector.siteKey(),
                selector.stage());
        if (binding == null) {
            throw new ShowroomReleaseApiException(HttpStatus.NOT_FOUND, "SHOWROOM_PUBLIC_SITE_NOT_BOUND",
                    "Public site is not bound.", false,
                    Map.of("siteKey", selector.siteKey(), "stage", selector.stage()));
        }
        if (binding.getTenantId() == null || binding.getTenantId() <= 0) {
            throw new ShowroomReleaseApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SHOWROOM_PUBLIC_SITE_BROKEN",
                    "Public site binding is missing tenant id.", false,
                    Map.of("siteKey", selector.siteKey(), "stage", selector.stage()));
        }
        return new ShowroomReleaseScope(binding.getTenantId(), selector.siteKey(), selector.stage());
    }

    public ShowroomReleaseScope resolvePublishScope(String siteKey, String stage) {
        ScopeSelector selector = normalizeSelector(siteKey, stage);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        upsertEnabledBinding(selector.siteKey(), selector.stage(), tenantId);
        return new ShowroomReleaseScope(tenantId, selector.siteKey(), selector.stage());
    }

    public <T> T executeInTenant(ShowroomReleaseScope scope, Supplier<T> supplier) {
        Long oldTenantId = TenantContextHolder.getTenantId();
        Boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setTenantId(scope.tenantId());
            TenantContextHolder.setIgnore(false);
            return supplier.get();
        } finally {
            TenantContextHolder.setTenantId(oldTenantId);
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }

    static ShowroomReleaseApiException siteSelectorRequired() {
        return new ShowroomReleaseApiException(HttpStatus.BAD_REQUEST, "SHOWROOM_SITE_SELECTOR_REQUIRED",
                "siteKey and stage are required for public release lookup.", false, Map.of());
    }

    private ScopeSelector normalizeSelector(String siteKey, String stage) {
        if (siteKey == null || siteKey.isBlank() || stage == null || stage.isBlank()) {
            throw siteSelectorRequired();
        }
        String normalizedStage = stage.trim().toUpperCase();
        if (!STAGES.contains(normalizedStage)) {
            throw new ShowroomReleaseApiException(HttpStatus.BAD_REQUEST, "SHOWROOM_STAGE_INVALID",
                    "Stage must be TEST or PROD.", false, Map.of("stage", stage));
        }
        return new ScopeSelector(siteKey.trim(), normalizedStage);
    }

    private void upsertEnabledBinding(String siteKey, String stage, Long tenantId) {
        ShowroomPublicSiteBindingDO binding = siteBindingMapper.selectAnyBySiteStage(siteKey, stage);
        if (binding == null) {
            siteBindingMapper.insert(ShowroomPublicSiteBindingDO.builder()
                    .siteKey(siteKey)
                    .stage(stage)
                    .tenantId(tenantId)
                    .displayName(siteKey + " " + stage)
                    .enabled(true)
                    .build());
            return;
        }
        binding.setTenantId(tenantId);
        binding.setEnabled(true);
        if (binding.getDisplayName() == null || binding.getDisplayName().isBlank()) {
            binding.setDisplayName(siteKey + " " + stage);
        }
        siteBindingMapper.updateById(binding);
    }

    private record ScopeSelector(String siteKey, String stage) {
    }
}
