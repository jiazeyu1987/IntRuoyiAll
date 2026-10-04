package cn.iocoder.yudao.module.dcc.service.file;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** No default date boundary or reminder lead time is implied by the application. */
@Component
@ConfigurationProperties(prefix = "dcc.workflow")
@Data
public class DccWorkflowDatePolicy {
    private String zoneId;
    private Integer reminderLeadDays;

    public LocalDateTime now() {
        if (zoneId == null || zoneId.isBlank()) throw new IllegalStateException("文控日期时区未配置：dcc.workflow.zone-id");
        return ZonedDateTime.now(ZoneId.of(zoneId)).toLocalDateTime().withNano(0);
    }

    public LocalDate reminderThrough() {
        return reminderDates().reminderThrough();
    }

    public ReminderDates reminderDates() {
        if (reminderLeadDays == null || reminderLeadDays < 0)
            throw new IllegalStateException("文控提醒提前量未配置或无效：dcc.workflow.reminder-lead-days");
        LocalDate businessDate=now().toLocalDate();
        return new ReminderDates(businessDate,businessDate.plusDays(reminderLeadDays));
    }

    public String reminderStage(LocalDate effectiveDate) {
        return reminderDates().stage(effectiveDate);
    }

    public record ReminderDates(LocalDate businessDate,LocalDate reminderThrough) {
        public String stage(LocalDate effectiveDate) {
            if(effectiveDate==null) throw new IllegalStateException("已受控文件缺少预设生效日期");
            if(effectiveDate.isBefore(businessDate)) return "OVERDUE";
            if(effectiveDate.equals(businessDate)) return "DUE";
            return effectiveDate.isAfter(reminderThrough) ? "FUTURE" : "UPCOMING";
        }
    }

    public void requireReviewDate(LocalDate date) {
        if (date == null || date.isBefore(now().toLocalDate()))
            throw new IllegalArgumentException("生效日期为空或已过，请调整申请并重新审核");
    }
}
