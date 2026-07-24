package cn.iocoder.yudao.module.mes.service.pro.schedule.component;

import org.springframework.stereotype.Component;

import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_CALENDAR_CONTEXT_CHANGED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_AUTO_SCHEDULE_CALENDAR_CONTEXT_REQUIRED;

/**
 * 排产应用前置校验。
 */
@Component
public class ScheduleApplyGuard {

    public void validateCalendarContextTokenProvided(String calendarContextToken) {
        if (calendarContextToken == null || calendarContextToken.isBlank()) {
            throw exception(PRO_AUTO_SCHEDULE_CALENDAR_CONTEXT_REQUIRED);
        }
    }

    public void validateCalendarContextToken(String providedToken, String expectedToken) {
        if (expectedToken == null || !Objects.equals(expectedToken, providedToken)) {
            throw exception(PRO_AUTO_SCHEDULE_CALENDAR_CONTEXT_CHANGED);
        }
    }

}
