package cn.iocoder.yudao.module.dcc.service.file;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_REVIEW_DATE_INVALID;

/** No default date boundary or reminder lead time is implied by the application. */
@Component
@ConfigurationProperties(prefix = "dcc.workflow")
@Data
public class DccWorkflowDatePolicy {
    public static final String AFTER_STANDARDIZATION_SIGNATURE = DccApplicationEffectiveSelection.AFTER_STANDARDIZATION_SIGNATURE;
    public static final String PRESET_DATE = DccApplicationEffectiveSelection.PRESET_DATE;
    public static final String DELAYED = DccApplicationEffectiveSelection.DELAYED;
    private String zoneId;
    private Integer reminderLeadDays;

    public LocalDateTime now() {
        if (zoneId == null || zoneId.isBlank()) throw new IllegalStateException("文控日期时区未配置：dcc.workflow.zone-id");
        return ZonedDateTime.now(ZoneId.of(zoneId)).toLocalDateTime();
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
            throw exception(CONTROLLED_FILE_REVIEW_DATE_INVALID);
    }

    /** Strict new-request contract. Historical null modes are never inferred here. */
    public void requireEffectiveConfiguration(String effectiveMode, LocalDate effectiveDate) {
        if (DELAYED.equals(effectiveMode)) {
            if (effectiveDate != null) throw new IllegalArgumentException("延迟生效申请阶段不填写具体生效时间或日期");
            return;
        }
        if (AFTER_STANDARDIZATION_SIGNATURE.equals(effectiveMode)) {
            if (effectiveDate != null) throw new IllegalArgumentException("文控签名后生效方式不得填写预设日期");
            return;
        }
        if (PRESET_DATE.equals(effectiveMode)) {
            requireReviewDate(effectiveDate);
            return;
        }
        throw new IllegalArgumentException("请明确选择正式生效方式");
    }

    /** Shape of a selection already accepted by a submitted, verified workflow.
     * Callers must prove that binding first; new or changed applications use the strict method above. */
    public void requireSubmittedEffectiveConfiguration(String effectiveMode, LocalDate effectiveDate) {
        if (DELAYED.equals(effectiveMode)) {
            if (effectiveDate != null) throw new IllegalArgumentException("延迟生效申请阶段不填写预设日期");
            return;
        }
        if (AFTER_STANDARDIZATION_SIGNATURE.equals(effectiveMode)) {
            if (effectiveDate != null) throw new IllegalArgumentException("文控签名后生效方式不得填写预设日期");
            return;
        }
        if (PRESET_DATE.equals(effectiveMode)) {
            if (effectiveDate == null) throw exception(CONTROLLED_FILE_REVIEW_DATE_INVALID);
            return;
        }
        throw new IllegalArgumentException("请明确选择正式生效方式");
    }

    public void requireApplicationEffectiveConfiguration(String mode, LocalDate date, String executionContract) {
        if ("PUBLIC_MATRIX_NATIVE_V2".equals(executionContract)) {
            if (!AFTER_STANDARDIZATION_SIGNATURE.equals(mode) && !DELAYED.equals(mode))
                throw new IllegalArgumentException("本次新流程仅允许文控审核后立即生效或延迟生效");
        } else if (DELAYED.equals(mode)) {
            throw new IllegalArgumentException("延迟生效确认仅允许本次正式新执行合同，不能修改历史申请语义");
        }
        requireEffectiveConfiguration(mode, date);
    }

    public void requireDelayedEffectiveTime(LocalDateTime selected, LocalDateTime confirmationTime) {
        if (!"Asia/Shanghai".equals(zoneId)) throw new IllegalStateException("延迟生效确认必须配置Asia/Shanghai业务时区");
        if (selected == null || selected.getNano() != 0 || confirmationTime == null || selected.isBefore(confirmationTime))
            throw new IllegalArgumentException("生效时间不得早于本次确认时间，请选择有效秒精度时间");
    }
}
