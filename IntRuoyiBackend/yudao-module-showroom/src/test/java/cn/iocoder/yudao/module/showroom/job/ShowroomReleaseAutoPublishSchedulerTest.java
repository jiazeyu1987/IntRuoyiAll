package cn.iocoder.yudao.module.showroom.job;

import cn.iocoder.yudao.framework.tenant.core.service.TenantFrameworkService;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomPublicSiteBindingDO;
import cn.iocoder.yudao.module.showroom.dal.mysql.release.ShowroomPublicSiteBindingMapper;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseApiException;
import cn.iocoder.yudao.module.showroom.release.ShowroomReleaseAutoPublishService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowroomReleaseAutoPublishSchedulerTest {

    @Mock
    private TenantFrameworkService tenantFrameworkService;
    @Mock
    private ShowroomReleaseAutoPublishService autoPublishService;
    @Mock
    private ShowroomPublicSiteBindingMapper siteBindingMapper;
    @Mock
    private ConfigService configService;

    @Test
    void scheduleShouldSkipCleanTenantWithoutCallingUnscopedEntrypoint() {
        ShowroomReleaseAutoPublishScheduler scheduler = newScheduler();
        when(autoPublishService.inspectState()).thenReturn(cleanState());

        assertDoesNotThrow(scheduler::schedule);

        verify(autoPublishService, never()).processDirtyReleaseIfDue();
    }

    @Test
    void scheduleShouldProcessDirtyTenantOnlyForConfiguredAutoPublishScope() {
        ShowroomReleaseAutoPublishScheduler scheduler = newScheduler();
        when(tenantFrameworkService.getTenantIds()).thenReturn(List.of(1001L));
        when(autoPublishService.inspectState()).thenReturn(dirtyState());
        lenient().when(configService.getConfigByKey("showroom.release.auto-publish.site-key"))
                .thenReturn(config("showroom.release.auto-publish.site-key", "yingtai-showroom"));
        lenient().when(configService.getConfigByKey("showroom.release.auto-publish.stage"))
                .thenReturn(config("showroom.release.auto-publish.stage", "TEST"));
        lenient().when(siteBindingMapper.selectEnabledBySiteStage("yingtai-showroom", "TEST"))
                .thenReturn(siteBinding("yingtai-showroom", "TEST", 1001L));
        when(autoPublishService.processDirtyReleaseIfDue("yingtai-showroom", "TEST"))
                .thenReturn(result(ShowroomReleaseAutoPublishService.ReleaseAutoPublishAction.PUBLISHED,
                        "release-test"));

        assertDoesNotThrow(scheduler::schedule);

        verify(autoPublishService, never()).processDirtyReleaseIfDue();
        verify(autoPublishService).processDirtyReleaseIfDue("yingtai-showroom", "TEST");
        verify(autoPublishService, never()).processDirtyReleaseIfDue("yingtai-showroom", "PROD");
        verify(siteBindingMapper, never()).selectEnabledByTenantId(anyLong());
    }

    @Test
    void scheduleShouldExecuteOnlyConfiguredBindingTenantWhenOtherTenantsExist() {
        ShowroomReleaseAutoPublishScheduler scheduler = newScheduler();
        when(tenantFrameworkService.getTenantIds()).thenReturn(List.of(1L, 121L, 122L));
        when(autoPublishService.inspectState()).thenReturn(dirtyState(), dirtyState());
        lenient().when(configService.getConfigByKey("showroom.release.auto-publish.site-key"))
                .thenReturn(config("showroom.release.auto-publish.site-key", "yingtai-showroom"));
        lenient().when(configService.getConfigByKey("showroom.release.auto-publish.stage"))
                .thenReturn(config("showroom.release.auto-publish.stage", "TEST"));
        lenient().when(siteBindingMapper.selectEnabledBySiteStage("yingtai-showroom", "TEST"))
                .thenReturn(siteBinding("yingtai-showroom", "TEST", 122L));
        when(autoPublishService.processDirtyReleaseIfDue("yingtai-showroom", "TEST"))
                .thenReturn(result(ShowroomReleaseAutoPublishService.ReleaseAutoPublishAction.PUBLISHED,
                        "release-test"));

        assertDoesNotThrow(scheduler::schedule);

        verify(autoPublishService, never()).processDirtyReleaseIfDue();
        verify(autoPublishService).processDirtyReleaseIfDue("yingtai-showroom", "TEST");
        verify(siteBindingMapper, never()).selectEnabledByTenantId(anyLong());
    }

    @Test
    void scheduleShouldFailFastWhenDirtyTenantHasNoConfiguredAutoPublishScope() {
        ShowroomReleaseAutoPublishScheduler scheduler = newScheduler();
        when(autoPublishService.inspectState()).thenReturn(dirtyState());

        ShowroomReleaseApiException exception = assertThrows(ShowroomReleaseApiException.class, scheduler::schedule);

        assertEquals("SHOWROOM_AUTO_PUBLISH_SCOPE_REQUIRED", exception.getCode());
        verify(siteBindingMapper, never()).selectEnabledByTenantId(anyLong());
        verify(siteBindingMapper, never()).selectEnabledBySiteStage(anyString(), anyString());
        verify(autoPublishService, never()).processDirtyReleaseIfDue();
        verify(autoPublishService, never()).processDirtyReleaseIfDue(anyString(), anyString());
    }

    @Test
    void scheduleShouldFailFastWhenConfiguredStageIsInvalid() {
        ShowroomReleaseAutoPublishScheduler scheduler = newScheduler();
        when(autoPublishService.inspectState()).thenReturn(dirtyState());
        lenient().when(configService.getConfigByKey("showroom.release.auto-publish.site-key"))
                .thenReturn(config("showroom.release.auto-publish.site-key", "yingtai-showroom"));
        lenient().when(configService.getConfigByKey("showroom.release.auto-publish.stage"))
                .thenReturn(config("showroom.release.auto-publish.stage", "STAGING"));

        ShowroomReleaseApiException exception = assertThrows(ShowroomReleaseApiException.class, scheduler::schedule);

        assertEquals("SHOWROOM_AUTO_PUBLISH_STAGE_INVALID", exception.getCode());
        verify(siteBindingMapper, never()).selectEnabledByTenantId(anyLong());
        verify(siteBindingMapper, never()).selectEnabledBySiteStage(anyString(), anyString());
        verify(autoPublishService, never()).processDirtyReleaseIfDue();
        verify(autoPublishService, never()).processDirtyReleaseIfDue(anyString(), anyString());
    }

    @Test
    void scheduleShouldFailFastWhenConfiguredScopeHasNoEnabledBinding() {
        ShowroomReleaseAutoPublishScheduler scheduler = newScheduler();
        when(autoPublishService.inspectState()).thenReturn(dirtyState());
        lenient().when(configService.getConfigByKey("showroom.release.auto-publish.site-key"))
                .thenReturn(config("showroom.release.auto-publish.site-key", "yingtai-showroom"));
        lenient().when(configService.getConfigByKey("showroom.release.auto-publish.stage"))
                .thenReturn(config("showroom.release.auto-publish.stage", "PROD"));
        when(siteBindingMapper.selectEnabledBySiteStage("yingtai-showroom", "PROD")).thenReturn(null);

        ShowroomReleaseApiException exception = assertThrows(ShowroomReleaseApiException.class, scheduler::schedule);

        assertEquals("SHOWROOM_AUTO_PUBLISH_SCOPE_BINDING_REQUIRED", exception.getCode());
        verify(siteBindingMapper, never()).selectEnabledByTenantId(anyLong());
        verify(autoPublishService, never()).processDirtyReleaseIfDue();
        verify(autoPublishService, never()).processDirtyReleaseIfDue(anyString(), anyString());
    }

    private ShowroomReleaseAutoPublishScheduler newScheduler() {
        ShowroomReleaseAutoPublishScheduler scheduler = new ShowroomReleaseAutoPublishScheduler();
        ReflectionTestUtils.setField(scheduler, "tenantFrameworkService", tenantFrameworkService);
        ReflectionTestUtils.setField(scheduler, "autoPublishService", autoPublishService);
        ReflectionTestUtils.setField(scheduler, "siteBindingMapper", siteBindingMapper);
        ReflectionTestUtils.setField(scheduler, "configService", configService);
        return scheduler;
    }

    private static ShowroomReleaseAutoPublishService.ReleaseAutoPublishState cleanState() {
        return new ShowroomReleaseAutoPublishService.ReleaseAutoPublishState(false, null, null, null,
                "", null, "", "", null);
    }

    private static ShowroomReleaseAutoPublishService.ReleaseAutoPublishState dirtyState() {
        return new ShowroomReleaseAutoPublishService.ReleaseAutoPublishState(true, 1L, 1L, null,
                "", 900L, "CONTENT_CHANGED", "", null);
    }

    private static ShowroomPublicSiteBindingDO siteBinding(String siteKey, String stage, Long tenantId) {
        return ShowroomPublicSiteBindingDO.builder()
                .siteKey(siteKey)
                .stage(stage)
                .tenantId(tenantId)
                .displayName(siteKey + " " + stage)
                .enabled(true)
                .build();
    }

    private static ConfigDO config(String key, String value) {
        ConfigDO config = new ConfigDO();
        config.setConfigKey(key);
        config.setValue(value);
        return config;
    }

    private static ShowroomReleaseAutoPublishService.ReleaseAutoPublishResult result(
            ShowroomReleaseAutoPublishService.ReleaseAutoPublishAction action, String releaseId) {
        return new ShowroomReleaseAutoPublishService.ReleaseAutoPublishResult(action, cleanState(), releaseId,
                action.name());
    }
}
