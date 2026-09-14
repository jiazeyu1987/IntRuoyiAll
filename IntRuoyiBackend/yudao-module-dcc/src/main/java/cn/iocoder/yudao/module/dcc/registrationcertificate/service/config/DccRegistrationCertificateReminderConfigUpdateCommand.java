package cn.iocoder.yudao.module.dcc.registrationcertificate.service.config;

import java.util.List;
import java.util.Map;

public record DccRegistrationCertificateReminderConfigUpdateCommand(
        Boolean enabled,
        String dailyRunTime,
        Map<String, List<Long>> thresholdRecipientUserIds,
        Integer expectedRowVersion) {
}
