package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 可信时间巡检证据 Response VO")
@Data
public class RuntimeControlTrustedTimeRespVO {

    private String targetEnvironment;
    private String nodeName;
    private String serverHost;
    private String selectedSource;
    private Integer stratum;
    private Double lastOffsetMillis;
    private Double rmsOffsetMillis;
    private Double maxOffsetMillis;
    private String leapStatus;
    private Boolean systemClockSynchronized;
    private String ntpServiceState;
    private String serverTimeUtc;
    private String databaseTimeUtc;
    private String checkedAtUtc;
    private String chronycTracking;
    private String chronycSources;
    private String timedatectlStatus;
}
