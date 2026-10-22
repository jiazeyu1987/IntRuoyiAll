package cn.iocoder.yudao.module.system.api.signature;

import cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner;
import cn.iocoder.yudao.module.system.api.signature.dto.SignatureAuthenticationOutcome;
import cn.iocoder.yudao.module.system.api.signature.dto.SignatureAuthenticationRequest;
import java.time.Instant;

/** Implemented separately by each identity domain, without a reverse module dependency. */
public interface SignatureAuthenticationApi {
    String identityDomain();
    SignatureAuthenticationOutcome authenticate(SignatureAuthenticationRequest request);
    AuthenticatedSigner lockAndRevalidate(AuthenticatedSigner signer, Instant signingInstant);
}
