package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Supplier;

@Component
public class DccRegistrationCertificateBusinessClock {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final Clock clock;
    private final ThreadLocal<Clock> overrideClock = new ThreadLocal<>();

    public DccRegistrationCertificateBusinessClock() {
        this(Clock.system(BUSINESS_ZONE));
    }

    public DccRegistrationCertificateBusinessClock(Clock clock) {
        if (clock == null || !BUSINESS_ZONE.equals(clock.getZone())) {
            throw new IllegalArgumentException("注册证业务时钟必须使用亚洲上海时区");
        }
        this.clock = clock;
    }

    public LocalDate businessDate() {
        return LocalDate.now(currentClock());
    }

    public LocalDateTime now() {
        return LocalDateTime.now(currentClock());
    }

    public <T> T runAt(LocalDateTime businessTime, Supplier<T> action) {
        if (businessTime == null || action == null) {
            throw new IllegalArgumentException("registration certificate business time and action must not be null");
        }
        if (overrideClock.get() != null) {
            throw new IllegalStateException("registration certificate business time override is already active");
        }
        overrideClock.set(Clock.fixed(businessTime.atZone(BUSINESS_ZONE).toInstant(), BUSINESS_ZONE));
        try {
            return action.get();
        } finally {
            overrideClock.remove();
        }
    }

    private Clock currentClock() {
        Clock active = overrideClock.get();
        return active == null ? clock : active;
    }
}