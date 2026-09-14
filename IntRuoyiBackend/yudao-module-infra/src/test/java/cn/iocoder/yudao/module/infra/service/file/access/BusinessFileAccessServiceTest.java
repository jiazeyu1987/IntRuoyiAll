package cn.iocoder.yudao.module.infra.service.file.access;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessFileAccessServiceTest {

    @Test
    void exposesOperationAwareProviderContract() {
        assertAll(
                () -> assertPresent("BusinessFileAccessOperation"),
                () -> assertPresent("BusinessFileAccessRequest"),
                () -> assertPresent("BusinessFileAccessReference"),
                () -> assertPresent("BusinessFileAccessProvider"),
                () -> assertPresent("BusinessFileAccessService"),
                () -> assertPresent("BusinessFileAccessDeniedException")
        );
    }

    @Test
    void allowsOnlyFilesThatEveryProviderDeclaresOrdinary() {
        BusinessFileAccessProvider first = provider("dcc");
        BusinessFileAccessProvider second = provider("future-registration");
        when(first.resolve(77L)).thenReturn(Optional.empty());
        when(second.resolve(77L)).thenReturn(Optional.empty());
        BusinessFileAccessService service = new BusinessFileAccessService(List.of(first, second));

        Optional<BusinessFileAccessReference> result = assertDoesNotThrow(
                () -> service.assertAllowed(userRequest(77L, null)));

        assertTrue(result.isEmpty());
        verify(first, never()).assertAllowed(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(second, never()).assertAllowed(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resolvesAndAuthorizesOneFormalReference() {
        BusinessFileAccessProvider provider = provider("dcc");
        BusinessFileAccessReference reference = reference("dcc", 7L, 9L);
        when(provider.resolve(77L)).thenReturn(Optional.of(reference));
        when(provider.supports(BusinessFileAccessOperation.PREVIEW)).thenReturn(true);
        BusinessFileAccessService service = new BusinessFileAccessService(List.of(provider));
        BusinessFileAccessRequest request = userRequest(77L, reference);

        Optional<BusinessFileAccessReference> result = assertDoesNotThrow(() -> service.assertAllowed(request));

        assertSame(reference, result.orElseThrow());
        verify(provider).assertAllowed(request, reference);
    }

    @Test
    void deniesDuplicateFormalClaims() {
        BusinessFileAccessProvider first = provider("dcc");
        BusinessFileAccessProvider second = provider("other");
        when(first.resolve(77L)).thenReturn(Optional.of(reference("dcc", 7L, 9L)));
        when(second.resolve(77L)).thenReturn(Optional.of(reference("other", 7L, 9L)));

        assertDenied(new BusinessFileAccessService(List.of(first, second)), userRequest(77L, null),
                "multiple providers");
        verify(first, never()).assertAllowed(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(second, never()).assertAllowed(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deniesProviderResolutionFailureWithoutLeakingProviderMessage() {
        BusinessFileAccessProvider provider = provider("dcc");
        IllegalStateException providerFailure = new IllegalStateException("secret-storage-detail");
        when(provider.resolve(77L)).thenThrow(providerFailure);

        BusinessFileAccessDeniedException exception = assertThrows(BusinessFileAccessDeniedException.class,
                () -> new BusinessFileAccessService(List.of(provider)).assertAllowed(userRequest(77L, null)));

        assertEquals("provider resolution failed: providerId=dcc, operation=PREVIEW, fileId=77",
                exception.getMessage());
        assertSame(providerFailure, exception.getCause());
        assertFalse(exception.getMessage().contains("secret-storage-detail"));
    }

    @Test
    void deniesFormalReferenceWithoutVersionEvidenceBeforeAuthorization() {
        BusinessFileAccessProvider provider = provider("dcc");
        BusinessFileAccessReference missingVersion = new BusinessFileAccessReference(
                "dcc", "DCC_CONTROLLED_FILE", 7L, null, 9L, 31L);
        BusinessFileAccessReference blankVersion = new BusinessFileAccessReference(
                "dcc", "DCC_CONTROLLED_FILE", 7L, " ", 9L, 31L);
        when(provider.resolve(77L)).thenReturn(Optional.of(missingVersion), Optional.of(blankVersion));
        when(provider.supports(BusinessFileAccessOperation.PREVIEW)).thenReturn(true);
        BusinessFileAccessService service = new BusinessFileAccessService(List.of(provider));

        assertDenied(service, userRequest(77L, missingVersion), "incomplete formal reference");
        assertDenied(service, userRequest(77L, blankVersion), "incomplete formal reference");

        verify(provider, never()).assertAllowed(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deniesCallerClaimWhenNoProviderOwnsTheFile() {
        BusinessFileAccessProvider provider = provider("dcc");
        when(provider.resolve(77L)).thenReturn(Optional.empty());

        assertDenied(new BusinessFileAccessService(List.of(provider)),
                userRequest(77L, reference("dcc", 7L, 9L)), "claim has no provider");
    }

    @Test
    void tokenWithoutFormalClaimIsAllowedOnlyWhileTheFileRemainsOrdinary() {
        BusinessFileAccessProvider provider = provider("dcc");
        BusinessFileAccessReference registeredReference = reference("dcc", 7L, 9L);
        BusinessFileAccessService service = new BusinessFileAccessService(List.of(provider));
        BusinessFileAccessRequest callback = BusinessFileAccessRequest.tokenCallback(
                BusinessFileAccessOperation.ONLYOFFICE_PREVIEW, 77L, 9L, 22L, null,
                "TOKEN-1", null, null, null);

        when(provider.resolve(77L)).thenReturn(Optional.empty(), Optional.of(registeredReference));
        when(provider.supports(BusinessFileAccessOperation.ONLYOFFICE_PREVIEW)).thenReturn(true);

        assertTrue(service.assertAllowed(callback).isEmpty());
        assertDenied(service, callback, "business token claim is required");
        verify(provider, never()).assertAllowed(callback, registeredReference);
    }

    @Test
    void deniesIncompleteOrContradictoryClaimedContext() {
        BusinessFileAccessProvider provider = provider("dcc");
        BusinessFileAccessReference reference = reference("dcc", 7L, 9L);
        when(provider.resolve(77L)).thenReturn(Optional.of(reference));
        when(provider.supports(BusinessFileAccessOperation.PREVIEW)).thenReturn(true);
        BusinessFileAccessService service = new BusinessFileAccessService(List.of(provider));

        assertDenied(service, new BusinessFileAccessRequest(BusinessFileAccessOperation.PREVIEW, 77L,
                null, 22L, null, "REQ-1", reference), "tenantId is required");
        assertDenied(service, new BusinessFileAccessRequest(BusinessFileAccessOperation.PREVIEW, 77L,
                9L, null, null, "REQ-1", reference), "userId or serviceIdentity is required");
        assertDenied(service, new BusinessFileAccessRequest(BusinessFileAccessOperation.PREVIEW, 77L,
                9L, 22L, "converter", "REQ-1", reference), "exactly one subject is required");
        assertDenied(service, new BusinessFileAccessRequest(BusinessFileAccessOperation.PREVIEW, 77L,
                9L, 22L, null, " ", reference), "requestId is required");
        assertDenied(service, new BusinessFileAccessRequest(BusinessFileAccessOperation.PREVIEW, 77L,
                10L, 22L, null, "REQ-1", reference), "tenant mismatch");
        assertDenied(service, userRequest(77L, reference("dcc", 8L, 9L)), "claim mismatch");
    }

    @Test
    void deniesUnsupportedOperationAndPublicDirectLink() {
        BusinessFileAccessProvider provider = provider("dcc");
        BusinessFileAccessReference reference = reference("dcc", 7L, 9L);
        when(provider.resolve(77L)).thenReturn(Optional.of(reference));
        when(provider.supports(BusinessFileAccessOperation.PREVIEW)).thenReturn(false);
        when(provider.supports(BusinessFileAccessOperation.DIRECT_LINK)).thenReturn(true);
        BusinessFileAccessService service = new BusinessFileAccessService(List.of(provider));

        assertDenied(service, userRequest(77L, reference), "operation is not supported");
        assertDenied(service, BusinessFileAccessRequest.publicDirectLink(77L, "REQ-DIRECT"),
                "business file direct link is forbidden");
        verify(provider).assertAllowed(org.mockito.ArgumentMatchers.argThat(request ->
                        request.operation() == BusinessFileAccessOperation.DIRECT_LINK),
                org.mockito.ArgumentMatchers.eq(reference));
    }

    private BusinessFileAccessProvider provider(String providerId) {
        BusinessFileAccessProvider provider = mock(BusinessFileAccessProvider.class);
        when(provider.providerId()).thenReturn(providerId);
        return provider;
    }

    private BusinessFileAccessReference reference(String providerId, Long businessId, Long tenantId) {
        return new BusinessFileAccessReference(providerId, "DCC_CONTROLLED_FILE", businessId,
                "PUBLISHED", tenantId, 31L);
    }

    private BusinessFileAccessRequest userRequest(Long fileId, BusinessFileAccessReference claim) {
        return new BusinessFileAccessRequest(BusinessFileAccessOperation.PREVIEW, fileId,
                9L, 22L, null, "REQ-1", claim);
    }

    private void assertDenied(BusinessFileAccessService service, BusinessFileAccessRequest request,
                              String expectedMessagePart) {
        BusinessFileAccessDeniedException exception = assertThrows(BusinessFileAccessDeniedException.class,
                () -> service.assertAllowed(request));
        assertTrue(exception.getMessage().contains(expectedMessagePart), exception.getMessage());
    }

    private void assertPresent(String simpleName) {
        String className = getClass().getPackageName() + "." + simpleName;
        assertDoesNotThrow(() -> Class.forName(className), className + " must be part of the Infra contract");
    }
}
