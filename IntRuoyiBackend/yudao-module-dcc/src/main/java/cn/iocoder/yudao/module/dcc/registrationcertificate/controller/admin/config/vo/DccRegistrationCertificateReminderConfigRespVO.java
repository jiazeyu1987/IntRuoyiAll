package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config.vo;

import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateReminderConfig;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateConfigService;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DccRegistrationCertificateReminderConfigRespVO {

    private Long id;
    private Boolean enabled;
    private String dailyRunTime;
    private String timezone;
    private String thresholdDaysJson;
    private Map<String, List<Long>> thresholdRecipientUserIds;
    private Integer rowVersion;

    public static DccRegistrationCertificateReminderConfigRespVO of(
            DccRegistrationCertificateReminderConfig config) {
        Map<String, List<Long>> recipients = DccRegistrationCertificateConfigService.parseThresholdRecipientUserIds(
                config.thresholdRecipientUserIdsJson());
        // The persisted default empty object means recipients have not been configured yet.
        if (recipients.isEmpty() && config.thresholdRecipientUserIdsJson() != null
                && config.thresholdRecipientUserIdsJson().strip().matches("\\{\\s*}")) {
            recipients = Map.of("T_30", List.of(), "T_8", List.of(), "T_2", List.of(), "T_1", List.of());
        }
        return DccRegistrationCertificateReminderConfigRespVO.builder()
                .id(config.id())
                .enabled(config.enabled())
                .dailyRunTime(config.dailyRunTime())
                .timezone(config.timezone())
                .thresholdDaysJson(config.thresholdDaysJson())
                .thresholdRecipientUserIds(recipients)
                .rowVersion(config.rowVersion())
                .build();
    }
}
