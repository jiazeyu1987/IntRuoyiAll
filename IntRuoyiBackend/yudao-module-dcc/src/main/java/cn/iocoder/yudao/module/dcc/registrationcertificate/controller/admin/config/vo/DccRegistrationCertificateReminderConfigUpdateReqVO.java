package cn.iocoder.yudao.module.dcc.registrationcertificate.controller.admin.config.vo;

import cn.iocoder.yudao.module.dcc.registrationcertificate.service.config.DccRegistrationCertificateReminderConfigUpdateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DccRegistrationCertificateReminderConfigUpdateReqVO {

    @NotNull
    private Boolean enabled;
    @NotBlank
    @Size(min = 5, max = 5)
    private String dailyRunTime;
    @NotNull
    @Positive
    private Integer expectedRowVersion;

    public DccRegistrationCertificateReminderConfigUpdateCommand toCommand() {
        return new DccRegistrationCertificateReminderConfigUpdateCommand(
                enabled, dailyRunTime, expectedRowVersion);
    }
}
