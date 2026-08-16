package cn.iocoder.yudao.module.infra.service.file.access;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class BusinessFileAccessService {

    private final List<BusinessFileAccessProvider> providers;

    public BusinessFileAccessService(List<BusinessFileAccessProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public Optional<BusinessFileAccessReference> assertAllowed(BusinessFileAccessRequest request) {
        validateBaseContext(request);
        List<ResolvedReference> resolved = new ArrayList<>();
        for (BusinessFileAccessProvider provider : providers) {
            String providerId = resolveProviderId(provider, request);
            Optional<BusinessFileAccessReference> reference;
            try {
                reference = provider.resolve(request.fileId());
            } catch (RuntimeException ex) {
                throw denied(request, providerId, "provider resolution failed", ex);
            }
            if (reference == null) {
                throw denied(request, providerId, "provider returned null resolution", null);
            }
            reference.ifPresent(value -> {
                validateReference(request, providerId, value);
                resolved.add(new ResolvedReference(provider, providerId, value));
            });
        }
        if (resolved.isEmpty()) {
            if (request.claim() != null) {
                throw denied(request, request.claim().providerId(), "claim has no provider", null);
            }
            return Optional.empty();
        }
        if (resolved.size() > 1) {
            throw denied(request, null, "multiple providers claimed the file", null);
        }

        ResolvedReference resolution = resolved.get(0);
        BusinessFileAccessReference reference = resolution.reference();
        if (request.tokenClaimRequired() && request.claim() == null) {
            throw denied(request, resolution.providerId(), "business token claim is required", null);
        }
        boolean supported;
        try {
            supported = resolution.provider().supports(request.operation());
        } catch (RuntimeException ex) {
            throw denied(request, resolution.providerId(), "provider operation check failed", ex);
        }
        if (!supported) {
            throw denied(request, resolution.providerId(), "operation is not supported", null);
        }
        if (request.operation() == BusinessFileAccessOperation.DIRECT_LINK) {
            invokeProviderAuthorization(request, resolution, reference);
            throw denied(request, resolution.providerId(), "business file direct link is forbidden", null);
        }
        validateClaimedContext(request, reference, resolution.providerId());
        if (request.claim() != null && !Objects.equals(request.claim(), reference)) {
            throw denied(request, resolution.providerId(), "claim mismatch", null);
        }
        invokeProviderAuthorization(request, resolution, reference);
        return Optional.of(reference);
    }

    private void invokeProviderAuthorization(BusinessFileAccessRequest request, ResolvedReference resolution,
                                             BusinessFileAccessReference reference) {
        try {
            resolution.provider().assertAllowed(request, reference);
        } catch (RuntimeException ex) {
            throw denied(request, resolution.providerId(), "provider authorization failed", ex);
        }
    }

    private void validateBaseContext(BusinessFileAccessRequest request) {
        if (request == null) {
            throw denied(null, null, "request is required", null);
        }
        if (request.operation() == null) {
            throw denied(request, null, "operation is required", null);
        }
        if (request.fileId() == null) {
            throw denied(request, null, "fileId is required", null);
        }
        if (isBlank(request.requestId())) {
            throw denied(request, null, "requestId is required", null);
        }
    }

    private String resolveProviderId(BusinessFileAccessProvider provider, BusinessFileAccessRequest request) {
        String providerId;
        try {
            providerId = provider.providerId();
        } catch (RuntimeException ex) {
            throw denied(request, null, "provider identity failed", ex);
        }
        if (isBlank(providerId)) {
            throw denied(request, null, "providerId is required", null);
        }
        return providerId.trim();
    }

    private void validateReference(BusinessFileAccessRequest request, String providerId,
                                   BusinessFileAccessReference reference) {
        if (!providerId.equals(reference.providerId())) {
            throw denied(request, providerId, "provider reference identity mismatch", null);
        }
        if (isBlank(reference.businessType()) || reference.businessId() == null
                || isBlank(reference.versionKey()) || reference.tenantId() == null) {
            throw denied(request, providerId, "provider returned incomplete formal reference", null);
        }
    }

    private void validateClaimedContext(BusinessFileAccessRequest request, BusinessFileAccessReference reference,
                                        String providerId) {
        if (request.tenantId() == null) {
            throw denied(request, providerId, "tenantId is required", null);
        }
        boolean hasUser = request.userId() != null;
        boolean hasService = !isBlank(request.serviceIdentity());
        if (!hasUser && !hasService) {
            throw denied(request, providerId, "userId or serviceIdentity is required", null);
        }
        if (hasUser && hasService) {
            throw denied(request, providerId, "exactly one subject is required", null);
        }
        if (!Objects.equals(request.tenantId(), reference.tenantId())) {
            throw denied(request, providerId, "tenant mismatch", null);
        }
    }

    private BusinessFileAccessDeniedException denied(BusinessFileAccessRequest request, String providerId,
                                                     String reason, Throwable cause) {
        BusinessFileAccessOperation operation = request != null ? request.operation() : null;
        Long fileId = request != null ? request.fileId() : null;
        Long tenantId = request != null ? request.tenantId() : null;
        String requestId = request != null ? request.requestId() : null;
        String message = reason + ": providerId=" + providerId + ", operation=" + operation + ", fileId=" + fileId;
        log.warn("[assertAllowed][business file access denied: operation({}) fileId({}) tenantId({}) "
                        + "requestId({}) providerId({}) reason({})]",
                operation, fileId, tenantId, requestId, providerId, reason);
        return cause == null
                ? new BusinessFileAccessDeniedException(message, operation, fileId, providerId)
                : new BusinessFileAccessDeniedException(message, operation, fileId, providerId, cause);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ResolvedReference(BusinessFileAccessProvider provider, String providerId,
                                     BusinessFileAccessReference reference) {
    }
}
