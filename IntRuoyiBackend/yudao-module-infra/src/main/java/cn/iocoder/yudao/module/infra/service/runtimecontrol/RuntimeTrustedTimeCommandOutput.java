package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import lombok.Data;

@Data
class RuntimeTrustedTimeCommandOutput {

    private String targetEnvironment;
    private String serverHost;
    private String chronycTracking;
    private String chronycSources;
    private String timedatectlStatus;
    private String serverTimeUtc;
    private String databaseTimeUtc;
    private String checkedAtUtc;
}
