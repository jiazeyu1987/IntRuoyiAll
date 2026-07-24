package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.service.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ShowroomReleaseAutoPublishService {

    static final String AUTO_PUBLISH_CATEGORY = "showroom";
    static final String AUTO_PUBLISH_STATE_KEY = "showroom.release.auto-publish.state";
    static final String AUTO_PUBLISH_STATE_NAME = "展厅 release 自动发布状态";
    static final String AUTO_PUBLISH_STATE_REMARK = "展厅 release 自动发布最近状态";
    static final long SYSTEM_OPERATOR_ID = 0L;

    private static final Duration DEFAULT_DEBOUNCE_WINDOW = Duration.ofMinutes(2);

    private final ConfigService configService;
    private final ShowroomReleasePublisherService releasePublisherService;
    private final Clock clock;
    private final Duration debounceWindow;
    private final ReentrantLock publishLock = new ReentrantLock();

    @Autowired
    public ShowroomReleaseAutoPublishService(ConfigService configService,
                                             ShowroomReleasePublisherService releasePublisherService) {
        this(configService, releasePublisherService, Clock.systemUTC(), DEFAULT_DEBOUNCE_WINDOW);
    }

    ShowroomReleaseAutoPublishService(ConfigService configService,
                                      ShowroomReleasePublisherService releasePublisherService,
                                      Clock clock) {
        this(configService, releasePublisherService, clock, DEFAULT_DEBOUNCE_WINDOW);
    }

    ShowroomReleaseAutoPublishService(ConfigService configService,
                                      ShowroomReleasePublisherService releasePublisherService,
                                      Clock clock,
                                      Duration debounceWindow) {
        this.configService = Objects.requireNonNull(configService, "configService");
        this.releasePublisherService = Objects.requireNonNull(releasePublisherService, "releasePublisherService");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.debounceWindow = Objects.requireNonNull(debounceWindow, "debounceWindow");
    }

    public void markDirty(String reason, Long operatorUserId) {
        publishLock.lock();
        try {
            ReleaseAutoPublishState current = inspectState();
            long now = Instant.now(clock).toEpochMilli();
            ReleaseAutoPublishState next = new ReleaseAutoPublishState(
                    true,
                    now,
                    now + debounceWindow.toMillis(),
                    current.lastPublishedAt(),
                    current.lastPublishedReleaseId(),
                    operatorUserId,
                    normalizeReason(reason),
                    "",
                    null
            );
            saveState(next);
        } finally {
            publishLock.unlock();
        }
    }

    public ReleaseAutoPublishState inspectState() {
        return loadState();
    }

    public ReleaseAutoPublishResult processDirtyReleaseIfDue() {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    public ReleaseAutoPublishResult processDirtyReleaseIfDue(String siteKey, String stage) {
        if (!publishLock.tryLock()) {
            return new ReleaseAutoPublishResult(ReleaseAutoPublishAction.BUSY, inspectState(), "", "busy");
        }
        try {
            ReleaseAutoPublishState current = loadState();
            if (!current.dirty()) {
                return new ReleaseAutoPublishResult(ReleaseAutoPublishAction.IDLE, current, "", "clean");
            }
            long now = Instant.now(clock).toEpochMilli();
            if (current.nextEligiblePublishAt() != null && now < current.nextEligiblePublishAt()) {
                return new ReleaseAutoPublishResult(
                        ReleaseAutoPublishAction.WAITING_DEBOUNCE,
                        current,
                        "",
                        "debounce-waiting");
            }
            Long operatorUserId = current.lastDirtyOperatorUserId();
            if (operatorUserId == null) {
                operatorUserId = SYSTEM_OPERATOR_ID;
            }
            try {
                ShowroomMaterializedRelease release = releasePublisherService.publishRelease(operatorUserId,
                        Instant.ofEpochMilli(now), siteKey, stage);
                ReleaseAutoPublishState published = new ReleaseAutoPublishState(
                        false,
                        current.lastDirtyAt(),
                        null,
                        now,
                        release.releaseId(),
                        operatorUserId,
                        current.lastDirtyReason(),
                        "",
                        null
                );
                saveState(published);
                return new ReleaseAutoPublishResult(
                        ReleaseAutoPublishAction.PUBLISHED,
                        published,
                        release.releaseId(),
                        "published");
            } catch (RuntimeException exception) {
                ReleaseAutoPublishState failed = new ReleaseAutoPublishState(
                        true,
                        current.lastDirtyAt(),
                        current.nextEligiblePublishAt(),
                        current.lastPublishedAt(),
                        current.lastPublishedReleaseId(),
                        current.lastDirtyOperatorUserId(),
                        current.lastDirtyReason(),
                        truncateFailure(exception.getMessage()),
                        now
                );
                saveState(failed);
                return new ReleaseAutoPublishResult(
                        ReleaseAutoPublishAction.FAILED,
                        failed,
                        current.lastPublishedReleaseId(),
                        truncateFailure(exception.getMessage()));
            }
        } finally {
            publishLock.unlock();
        }
    }

    public ShowroomMaterializedRelease publishNow(Long operatorUserId, Instant publishedAt) {
        throw ShowroomPublicReleaseScopeResolver.siteSelectorRequired();
    }

    public ShowroomMaterializedRelease publishNow(Long operatorUserId, Instant publishedAt, String siteKey,
                                                  String stage) {
        publishLock.lock();
        try {
            Long resolvedOperatorUserId = operatorUserId == null ? SYSTEM_OPERATOR_ID : operatorUserId;
            ShowroomMaterializedRelease release = releasePublisherService.publishRelease(resolvedOperatorUserId,
                    publishedAt, siteKey, stage);
            ReleaseAutoPublishState current = loadState();
            ReleaseAutoPublishState next = new ReleaseAutoPublishState(
                    false,
                    current.lastDirtyAt(),
                    null,
                    publishedAt.toEpochMilli(),
                    release.releaseId(),
                    resolvedOperatorUserId,
                    current.lastDirtyReason(),
                    "",
                    null
            );
            saveState(next);
            return release;
        } finally {
            publishLock.unlock();
        }
    }

    private ReleaseAutoPublishState loadState() {
        ConfigDO config = configService.getConfigByKey(AUTO_PUBLISH_STATE_KEY);
        if (config == null || config.getValue() == null || config.getValue().isBlank()) {
            return ReleaseAutoPublishState.empty();
        }
        ReleaseAutoPublishState parsed = JsonUtils.parseObjectQuietly(config.getValue(), ReleaseAutoPublishState.class);
        if (parsed == null) {
            return ReleaseAutoPublishState.empty();
        }
        return new ReleaseAutoPublishState(
                parsed.dirty(),
                parsed.lastDirtyAt(),
                parsed.nextEligiblePublishAt(),
                parsed.lastPublishedAt(),
                nullToEmpty(parsed.lastPublishedReleaseId()),
                parsed.lastDirtyOperatorUserId(),
                nullToEmpty(parsed.lastDirtyReason()),
                nullToEmpty(parsed.lastFailureMessage()),
                parsed.lastFailureAt()
        );
    }

    private void saveState(ReleaseAutoPublishState state) {
        ConfigDO existing = configService.getConfigByKey(AUTO_PUBLISH_STATE_KEY);
        ConfigSaveReqVO reqVO = new ConfigSaveReqVO();
        if (existing != null) {
            reqVO.setId(existing.getId());
        }
        reqVO.setCategory(AUTO_PUBLISH_CATEGORY);
        reqVO.setName(AUTO_PUBLISH_STATE_NAME);
        reqVO.setKey(AUTO_PUBLISH_STATE_KEY);
        reqVO.setValue(JsonUtils.toJsonString(state));
        reqVO.setVisible(false);
        reqVO.setRemark(AUTO_PUBLISH_STATE_REMARK);
        if (existing == null) {
            configService.createConfig(reqVO);
            return;
        }
        configService.updateConfig(reqVO);
    }

    private static String normalizeReason(String reason) {
        return hasText(reason) ? reason.trim() : "CONTENT_CHANGED";
    }

    private static String truncateFailure(String message) {
        String text = nullToEmpty(message).trim();
        if (text.length() <= 200) {
            return text;
        }
        return text.substring(0, 200);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public enum ReleaseAutoPublishAction {
        IDLE,
        WAITING_DEBOUNCE,
        BUSY,
        PUBLISHED,
        FAILED
    }

    public record ReleaseAutoPublishState(boolean dirty,
                                          Long lastDirtyAt,
                                          Long nextEligiblePublishAt,
                                          Long lastPublishedAt,
                                          String lastPublishedReleaseId,
                                          Long lastDirtyOperatorUserId,
                                          String lastDirtyReason,
                                          String lastFailureMessage,
                                          Long lastFailureAt) {

        static ReleaseAutoPublishState empty() {
            return new ReleaseAutoPublishState(false, null, null, null, "", null, "", "", null);
        }
    }

    public record ReleaseAutoPublishResult(ReleaseAutoPublishAction action,
                                           ReleaseAutoPublishState state,
                                           String releaseId,
                                           String message) {
    }
}
