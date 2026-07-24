package cn.iocoder.yudao.module.infra.service.job;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "yudao.local-job-control")
@Data
public class LocalJobControlProperties {

    /**
     * Enables local-only auto job containment after Quartz startup sync.
     */
    private boolean enabled = true;

    /**
     * Handlers that may keep automatic Quartz scheduling in local profile.
     */
    private List<String> quartzAutoRunHandlerWhitelist = new ArrayList<>();

    private boolean dccBatchRecognitionEnabled = true;

    private boolean dccNasTransferEnabled = true;

    private boolean dccNasPermissionRestoreEnabled = true;

    private boolean showroomReleaseAutoPublishEnabled = true;

    private boolean showroomProductCoverBatchResumeEnabled = true;

    private boolean showroomProductBatchNarrationAudioAutoCheckEnabled = true;

    private boolean showroomProductBatchNarrationScriptAutoCheckEnabled = true;

    public boolean allowsQuartzAutoRun(String handlerName) {
        return quartzAutoRunHandlerWhitelist.stream().anyMatch(handlerName::equals);
    }
}
