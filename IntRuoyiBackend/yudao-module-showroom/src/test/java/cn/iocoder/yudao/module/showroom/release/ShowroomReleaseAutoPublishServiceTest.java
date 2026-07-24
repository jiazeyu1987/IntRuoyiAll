package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowroomReleaseAutoPublishServiceTest {

    private static final String SITE_KEY = "yingtai-showroom";
    private static final String STAGE = "TEST";

    @Mock
    private ConfigService configService;

    @Mock
    private ShowroomReleasePublisherService publisherService;

    @Test
    void processDirtyReleaseIfDueShouldSkipWhenNoDirtyStateExists() {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2026-05-24T07:00:00Z"));
        stubConfigService();
        ShowroomReleaseAutoPublishService service = new ShowroomReleaseAutoPublishService(
                configService, publisherService, clock);

        ShowroomReleaseAutoPublishService.ReleaseAutoPublishResult result =
                service.processDirtyReleaseIfDue(SITE_KEY, STAGE);

        assertEquals(ShowroomReleaseAutoPublishService.ReleaseAutoPublishAction.IDLE, result.action());
        assertFalse(result.state().dirty());
    }

    @Test
    void processDirtyReleaseIfDueShouldDebounceAndMergeMultipleDirtyMarksIntoOnePublish() {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2026-05-24T07:00:00Z"));
        stubConfigService();
        ShowroomReleaseAutoPublishService service = new ShowroomReleaseAutoPublishService(
                configService, publisherService, clock);
        when(publisherService.publishRelease(902L, Instant.parse("2026-05-24T07:02:31Z"), SITE_KEY, STAGE))
                .thenReturn(materializedRelease("release-merged", Instant.parse("2026-05-24T07:02:31Z")));

        service.markDirty("PRODUCT_REVISION_PUBLISHED", 901L);
        clock.setInstant(Instant.parse("2026-05-24T07:00:30Z"));
        service.markDirty("PRODUCT_REVISION_PUBLISHED", 902L);

        clock.setInstant(Instant.parse("2026-05-24T07:01:40Z"));
        ShowroomReleaseAutoPublishService.ReleaseAutoPublishResult waiting =
                service.processDirtyReleaseIfDue(SITE_KEY, STAGE);
        assertEquals(ShowroomReleaseAutoPublishService.ReleaseAutoPublishAction.WAITING_DEBOUNCE, waiting.action());
        assertTrue(waiting.state().dirty());

        clock.setInstant(Instant.parse("2026-05-24T07:02:31Z"));
        ShowroomReleaseAutoPublishService.ReleaseAutoPublishResult published =
                service.processDirtyReleaseIfDue(SITE_KEY, STAGE);

        assertEquals(ShowroomReleaseAutoPublishService.ReleaseAutoPublishAction.PUBLISHED, published.action());
        assertFalse(published.state().dirty());
        assertEquals("release-merged", published.state().lastPublishedReleaseId());
        verify(publisherService).publishRelease(902L, Instant.parse("2026-05-24T07:02:31Z"), SITE_KEY, STAGE);
    }

    @Test
    void publishNowShouldClearDirtyStateImmediately() {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2026-05-24T07:05:00Z"));
        stubConfigService();
        ShowroomReleaseAutoPublishService service = new ShowroomReleaseAutoPublishService(
                configService, publisherService, clock);
        when(publisherService.publishRelease(903L, Instant.parse("2026-05-24T07:06:00Z"), SITE_KEY, STAGE))
                .thenReturn(materializedRelease("release-manual", Instant.parse("2026-05-24T07:06:00Z")));

        service.markDirty("COMPANY_REVISION_PUBLISHED", 901L);
        clock.setInstant(Instant.parse("2026-05-24T07:06:00Z"));

        ShowroomMaterializedRelease release = service.publishNow(903L, Instant.parse("2026-05-24T07:06:00Z"),
                SITE_KEY, STAGE);

        assertEquals("release-manual", release.releaseId());
        assertFalse(service.inspectState().dirty());
        assertEquals("release-manual", service.inspectState().lastPublishedReleaseId());
    }

    @Test
    void unscopedAutoPublishEntrypointsShouldFailFast() {
        AdjustableClock clock = new AdjustableClock(Instant.parse("2026-05-24T07:05:00Z"));
        ShowroomReleaseAutoPublishService service = new ShowroomReleaseAutoPublishService(
                configService, publisherService, clock);

        assertThrows(ShowroomReleaseApiException.class, service::processDirtyReleaseIfDue);
        assertThrows(ShowroomReleaseApiException.class,
                () -> service.publishNow(903L, Instant.parse("2026-05-24T07:06:00Z")));
    }

    private final Map<String, ConfigDO> configStore = new HashMap<>();
    private final AtomicLong configIdSequence = new AtomicLong(1L);

    private void stubConfigService() {
        configStore.clear();
        configIdSequence.set(1L);
        when(configService.getConfigByKey(anyString())).thenAnswer(invocation -> configStore.get(invocation.getArgument(0)));
        lenient().when(configService.createConfig(any(ConfigSaveReqVO.class))).thenAnswer(invocation -> {
            ConfigSaveReqVO reqVO = invocation.getArgument(0);
            ConfigDO config = new ConfigDO();
            config.setId(configIdSequence.getAndIncrement());
            config.setCategory(reqVO.getCategory());
            config.setName(reqVO.getName());
            config.setConfigKey(reqVO.getKey());
            config.setValue(reqVO.getValue());
            config.setVisible(reqVO.getVisible());
            config.setRemark(reqVO.getRemark());
            configStore.put(reqVO.getKey(), config);
            return config.getId();
        });
        lenient().doAnswer(invocation -> {
            ConfigSaveReqVO reqVO = invocation.getArgument(0);
            ConfigDO config = configStore.get(reqVO.getKey());
            if (config == null) {
                config = new ConfigDO();
                config.setId(reqVO.getId() == null ? configIdSequence.getAndIncrement() : reqVO.getId());
            }
            config.setCategory(reqVO.getCategory());
            config.setName(reqVO.getName());
            config.setConfigKey(reqVO.getKey());
            config.setValue(reqVO.getValue());
            config.setVisible(reqVO.getVisible());
            config.setRemark(reqVO.getRemark());
            configStore.put(reqVO.getKey(), config);
            return null;
        }).when(configService).updateConfig(any(ConfigSaveReqVO.class));
    }

    private static ShowroomMaterializedRelease materializedRelease(String releaseId, Instant publishedAt) {
        return new ShowroomMaterializedRelease(
                releaseId,
                publishedAt,
                "manifest-hash-" + releaseId,
                "{}",
                "website-index",
                0,
                0,
                0L,
                null,
                List.of(),
                List.of(),
                List.of(),
                "{}",
                "legacy-hash-" + releaseId
        );
    }

    private static final class AdjustableClock extends Clock {
        private Instant instant;

        private AdjustableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }
    }
}
