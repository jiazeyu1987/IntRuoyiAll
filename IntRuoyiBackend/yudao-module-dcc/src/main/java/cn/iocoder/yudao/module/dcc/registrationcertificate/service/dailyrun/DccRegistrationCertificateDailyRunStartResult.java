package cn.iocoder.yudao.module.dcc.registrationcertificate.service.dailyrun;

public record DccRegistrationCertificateDailyRunStartResult(
        DccRegistrationCertificateDailyRunRecord run,
        boolean started) {
}
