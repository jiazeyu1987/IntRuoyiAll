package cn.iocoder.yudao.module.signature.service;

import cn.iocoder.yudao.module.system.api.signature.SignatureAuthenticationApi;
import cn.iocoder.yudao.module.system.api.signature.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/** Authentication completes outside the domain transaction; rejection events remain committed. */
@Service
public class ElectronicSignatureAuthenticationFacade {
    private final List<SignatureAuthenticationApi> providers;
    public ElectronicSignatureAuthenticationFacade(List<SignatureAuthenticationApi> providers) { this.providers = providers; }
    @Transactional(propagation = Propagation.NEVER)
    public AuthenticatedSigner authenticate(SignatureAuthenticationRequest request) {
        var candidates = providers.stream().filter(p -> p.identityDomain().equals(request.domain())).toList();
        if (candidates.size() != 1) throw new IllegalStateException("SIGNATURE_IDENTITY_PROVIDER_NOT_UNIQUE: " + request.domain());
        var outcome = candidates.get(0).authenticate(request);
        if (outcome == null) throw new IllegalStateException("SIGNATURE_AUTHENTICATION_OUTCOME_MISSING");
        if (!outcome.succeeded()) throw new IllegalArgumentException(outcome.rejectionCode());
        return outcome.signer();
    }
}
