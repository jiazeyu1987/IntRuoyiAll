package cn.iocoder.yudao.module.showroom.job;

import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomPublicSiteBindingDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomPublicSiteBindingMapper;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseApiException;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseAutoPublishService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "yudao.local-job-control", name = "showroom-release-auto-publish-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class ShowroomReleaseAutoPublishScheduler {

    static final String AUTO_PUBLISH_SITE_KEY_CONFIG_KEY = "showroom.release.auto-publish.site-key";
    static final String AUTO_PUBLISH_STAGE_CONFIG_KEY = "showroom.release.auto-publish.stage";

    private static final Set<String> AUTO_PUBLISH_STAGES = Set.of("TEST", "PROD");

    @Resource
    private TenantFrameworkService tenantFrameworkService;

    @Resource
    private ShowroomReleaseAutoPublishService autoPublishService;

    @Resource
    private ShowroomPublicSiteBindingMapper siteBindingMapper;

    @Resource
    private ConfigService configService;

    @Scheduled(cron = "0 * * * * ?")
    public void schedule() {
        ShowroomReleaseAutoPublishService.ReleaseAutoPublishState state = autoPublishService.inspectState();
        if (!state.dirty()) {
            log.debug("[schedule][showroom release clean，跳过自动发布]");
            return;
        }

        AutoPublishScope scope = resolveAutoPublishScope();
        ShowroomPublicSiteBindingDO binding = resolveEnabledBinding(scope);
        Long bindingTenantId = binding.getTenantId();
        if (!tenantFrameworkService.getTenantIds().contains(bindingTenantId)) {
            log.error("[schedule][tenantId({}) siteKey({}) stage({}) showroom release dirty 但 binding tenant 不在启用租户列表]",
                    bindingTenantId, scope.siteKey(), scope.stage());
            throw new ShowroomReleaseApiException(HttpStatus.FAILED_DEPENDENCY,
                    "SHOWROOM_AUTO_PUBLISH_SCOPE_BINDING_REQUIRED",
                    "Configured showroom auto-publish scope must have an enabled public site binding.", false,
                    Map.of("tenantId", bindingTenantId, "siteKey", scope.siteKey(), "stage", scope.stage()));
        }

        TenantUtils.execute(bindingTenantId, () -> scheduleBoundTenant(bindingTenantId, scope));
    }

    private void scheduleBoundTenant(Long tenantId, AutoPublishScope scope) {
        ShowroomReleaseAutoPublishService.ReleaseAutoPublishResult result =
                autoPublishService.processDirtyReleaseIfDue(scope.siteKey(), scope.stage());
        logResult(tenantId, scope, result);
    }

    private AutoPublishScope resolveAutoPublishScope() {
        String siteKey = configValue(AUTO_PUBLISH_SITE_KEY_CONFIG_KEY);
        String stage = configValue(AUTO_PUBLISH_STAGE_CONFIG_KEY);
        if (!hasText(siteKey) || !hasText(stage)) {
            log.error("[resolveAutoPublishScope][showroom release dirty 但缺少自动发布 scope 配置]");
            throw new ShowroomReleaseApiException(HttpStatus.FAILED_DEPENDENCY,
                    "SHOWROOM_AUTO_PUBLISH_SCOPE_REQUIRED",
                    "Dirty showroom release requires configured auto-publish site key and stage.", false,
                    Map.of("siteKeyConfigKey", AUTO_PUBLISH_SITE_KEY_CONFIG_KEY,
                            "stageConfigKey", AUTO_PUBLISH_STAGE_CONFIG_KEY));
        }
        String normalizedStage = stage.trim().toUpperCase();
        if (!AUTO_PUBLISH_STAGES.contains(normalizedStage)) {
            throw new ShowroomReleaseApiException(HttpStatus.BAD_REQUEST,
                    "SHOWROOM_AUTO_PUBLISH_STAGE_INVALID",
                    "Configured showroom auto-publish stage must be TEST or PROD.", false,
                    Map.of("stage", stage.trim()));
        }
        return new AutoPublishScope(siteKey.trim(), normalizedStage);
    }

    private String configValue(String key) {
        ConfigDO config = configService.getConfigByKey(key);
        return config == null ? null : config.getValue();
    }

    private ShowroomPublicSiteBindingDO resolveEnabledBinding(AutoPublishScope scope) {
        ShowroomPublicSiteBindingDO binding = siteBindingMapper.selectEnabledBySiteStage(scope.siteKey(),
                scope.stage());
        if (binding == null || binding.getTenantId() == null || binding.getTenantId() <= 0) {
            log.error("[resolveEnabledBinding][siteKey({}) stage({}) showroom release dirty 但缺少 scope 对应 enabled binding]",
                    scope.siteKey(), scope.stage());
            throw new ShowroomReleaseApiException(HttpStatus.FAILED_DEPENDENCY,
                    "SHOWROOM_AUTO_PUBLISH_SCOPE_BINDING_REQUIRED",
                    "Configured showroom auto-publish scope must have an enabled public site binding.", false,
                    Map.of("siteKey", scope.siteKey(), "stage", scope.stage()));
        }
        return binding;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void logResult(Long tenantId, AutoPublishScope scope,
                           ShowroomReleaseAutoPublishService.ReleaseAutoPublishResult result) {
        if (result.action() == ShowroomReleaseAutoPublishService.ReleaseAutoPublishAction.PUBLISHED) {
            log.info("[logResult][tenantId({}) siteKey({}) stage({}) showroom release 自动发布成功][releaseId({})]",
                    tenantId, scope.siteKey(), scope.stage(), result.releaseId());
        } else if (result.action() == ShowroomReleaseAutoPublishService.ReleaseAutoPublishAction.FAILED) {
            log.error("[logResult][tenantId({}) siteKey({}) stage({}) showroom release 自动发布失败][message({})]",
                    tenantId, scope.siteKey(), scope.stage(), result.message());
        } else {
            log.debug("[logResult][tenantId({}) siteKey({}) stage({}) showroom release 自动发布跳过][action({})]",
                    tenantId, scope.siteKey(), scope.stage(), result.action());
        }
    }

    private record AutoPublishScope(String siteKey, String stage) {
    }
}
