package cn.iocoder.yudao.module.signature.service;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.system.api.signature.SignatureAuthenticationApi;
import cn.iocoder.yudao.module.system.api.signature.dto.AuthenticatedSigner;
import cn.iocoder.yudao.module.system.api.signature.dto.SignatureAuthenticationOutcome;
import cn.iocoder.yudao.module.system.api.signature.dto.SignatureAuthenticationRequest;
import org.junit.jupiter.api.Test;
import java.util.List;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.ESIGN_IDENTITY_BAD_CREDENTIALS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ElectronicSignatureAuthenticationFacadeTest {
    private final SignatureAuthenticationApi provider = mock(SignatureAuthenticationApi.class);
    private final ElectronicSignatureAuthenticationFacade facade = new ElectronicSignatureAuthenticationFacade(List.of(provider));
    private final SignatureAuthenticationRequest request = new SignatureAuthenticationRequest(1L, "SYSTEM_USER", 2L, 2L, "test-request", "test-credential");

    @Test void rejectedCredentialRemainsARejectionWithActionableBusinessError() {
        when(provider.identityDomain()).thenReturn("SYSTEM_USER");
        when(provider.authenticate(request)).thenReturn(new SignatureAuthenticationOutcome(null, "BAD_CREDENTIALS"));
        var error = assertThrows(ServiceException.class, () -> facade.authenticate(request));
        assertEquals(ESIGN_IDENTITY_BAD_CREDENTIALS.getCode(), error.getCode());
        assertEquals(ESIGN_IDENTITY_BAD_CREDENTIALS.getMsg(), error.getMessage());
        verify(provider).authenticate(request);
    }

    @Test void validCredentialReturnsOnlyTheAuthenticatedSigner() {
        when(provider.identityDomain()).thenReturn("SYSTEM_USER");
        var signer = new AuthenticatedSigner(1L, "SYSTEM_USER", 2L, 2L, "64088047-a256-4ee8-b162-2fc79e437031", 1L, "test-request");
        when(provider.authenticate(request)).thenReturn(new SignatureAuthenticationOutcome(signer, null));
        assertSame(signer, facade.authenticate(request));
    }

    @Test void unknownRejectionStillFailsExplicitly() {
        when(provider.identityDomain()).thenReturn("SYSTEM_USER");
        when(provider.authenticate(request)).thenReturn(new SignatureAuthenticationOutcome(null, "UNRECOGNIZED_REJECTION"));
        assertEquals("UNRECOGNIZED_REJECTION", assertThrows(IllegalArgumentException.class, () -> facade.authenticate(request)).getMessage());
    }
}
