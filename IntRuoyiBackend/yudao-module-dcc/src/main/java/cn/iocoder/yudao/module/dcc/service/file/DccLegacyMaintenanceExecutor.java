package cn.iocoder.yudao.module.dcc.service.file;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Current development policy/schema reads and existing activation use the same physical transaction.
 */
@Service
public class DccLegacyMaintenanceExecutor {

    private final DccLegacyMaintenanceGate gate;

    private final DccLegacySourceNameRegistrationService registration;

    public DccLegacyMaintenanceExecutor(DccLegacyMaintenanceGate gate, DccLegacySourceNameRegistrationService registration) {
        this.gate = gate;
        this.registration = registration;
    }

    @Transactional(rollbackFor = Exception.class)
    public DccLegacySourceNameRegistrationService.Receipt execute(DccLegacyMaintenanceGate.Input input, DccLegacyNameVerifiedScope verifiedScope) {
        try {
            gate.currentDevelopmentPolicy(input);
            return registration.activateVerifiedScope(verifiedScope);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("DCC_MAINTENANCE_ACTUAL_POLICY_OR_SCHEMA_INVALID");
        }
    }
}
