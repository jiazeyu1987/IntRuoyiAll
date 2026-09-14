package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config.vo;

import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateReminderConfigUpdateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DccRegistrationCertificateReminderConfigUpdateReqVO {

    @NotNull
    private Boolean enabled;
    @NotBlank
    @Size(min = 5, max = 5)
    private String dailyRunTime;
    @NotNull
    @Size(min = 4, max = 4)
    private Map<String, List<Long>> thresholdRecipientUserIds;
    @NotNull
    @Positive
    private Integer expectedRowVersion;

    public DccRegistrationCertificateReminderConfigUpdateCommand toCommand() {
        return new DccRegistrationCertificateReminderConfigUpdateCommand(
                enabled, dailyRunTime, thresholdRecipientUserIds, expectedRowVersion);
    }
}
