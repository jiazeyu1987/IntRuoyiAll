package cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate;

import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class DccRegistrationCertificateCommandMutex {

    private static final int STRIPE_COUNT = 64;
    private final ReentrantLock[] stripes = new ReentrantLock[STRIPE_COUNT];

    public DccRegistrationCertificateCommandMutex() {
        for (int index = 0; index < stripes.length; index++) {
            stripes[index] = new ReentrantLock();
        }
    }

    public <T> T execute(String key, Supplier<T> action) {
        ReentrantLock lock = stripes[Math.floorMod(key.hashCode(), stripes.length)];
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
