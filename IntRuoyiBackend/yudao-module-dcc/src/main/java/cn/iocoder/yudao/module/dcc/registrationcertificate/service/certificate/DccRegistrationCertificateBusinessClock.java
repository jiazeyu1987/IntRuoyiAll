package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class DccRegistrationCertificateBusinessClock {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final Clock clock;

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
        return LocalDate.now(clock);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
