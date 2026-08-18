package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config.vo;

import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateReminderConfig;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DccRegistrationCertificateReminderConfigRespVO {

    private Long id;
    private Boolean enabled;
    private String dailyRunTime;
    private String timezone;
    private String thresholdDaysJson;
    private Integer rowVersion;

    public static DccRegistrationCertificateReminderConfigRespVO of(
            DccRegistrationCertificateReminderConfig config) {
        return DccRegistrationCertificateReminderConfigRespVO.builder()
                .id(config.id())
                .enabled(config.enabled())
                .dailyRunTime(config.dailyRunTime())
                .timezone(config.timezone())
                .thresholdDaysJson(config.thresholdDaysJson())
                .rowVersion(config.rowVersion())
                .build();
    }
}
