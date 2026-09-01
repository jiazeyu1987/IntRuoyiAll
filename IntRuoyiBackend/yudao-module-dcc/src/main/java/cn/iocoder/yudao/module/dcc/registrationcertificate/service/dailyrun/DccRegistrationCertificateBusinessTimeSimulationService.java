package cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.registrationcertificate.job.DccRegistrationCertificateReminderDailyJob;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED;

@Service
public class DccRegistrationCertificateBusinessTimeSimulationService {

    private static final String REMINDER_JOB_HANDLER_NAME = "registrationCertificateReminderDailyJob";
    private static final int MORNING_RUN_HOUR = 9;

    private final JdbcTemplate jdbcTemplate;
    private final DccRegistrationCertificateReminderDailyJob reminderDailyJob;

    public DccRegistrationCertificateBusinessTimeSimulationService(
            JdbcTemplate jdbcTemplate,
            DccRegistrationCertificateReminderDailyJob reminderDailyJob) {
        this.jdbcTemplate = require(jdbcTemplate, "jdbcTemplate");
        this.reminderDailyJob = require(reminderDailyJob, "reminderDailyJob");
    }

    public DccRegistrationCertificateBusinessTimeSimulationResult simulateMorningRun(
            Long tenantId, LocalDate businessDate) {
        if (tenantId == null || tenantId <= 0 || businessDate == null) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_DAILY_RUN_CONFLICT);
        }
        String jobParam = resolveReminderJobParam();
        LocalDateTime simulatedAt = businessDate.atTime(MORNING_RUN_HOUR, 0);
        String jobResult = reminderDailyJob.executeTenantAtBusinessDate(tenantId, businessDate, jobParam);
        return new DccRegistrationCertificateBusinessTimeSimulationResult(
                tenantId, businessDate, simulatedAt, jobResult);
    }

    private String resolveReminderJobParam() {
        List<String> params = jdbcTemplate.queryForList("""
                SELECT handler_param
                  FROM infra_job
                 WHERE handler_name = ?
                   AND deleted = 0
                 ORDER BY id
                """, String.class, REMINDER_JOB_HANDLER_NAME);
        if (params.size() != 1 || isBlank(params.get(0))) {
            throw new ServiceException(REGISTRATION_CERTIFICATE_REMINDER_JOB_NOT_CONFIGURED);
        }
        return params.get(0).trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }
}
