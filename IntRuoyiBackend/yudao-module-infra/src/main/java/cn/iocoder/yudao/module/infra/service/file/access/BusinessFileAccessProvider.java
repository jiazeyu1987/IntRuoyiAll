package cn.iocoder.yudao.module.infra.service.file.access;

import java.util.Optional;

public interface BusinessFileAccessProvider {

    String providerId();

    Optional<BusinessFileAccessReference> resolve(Long fileId);

    boolean supports(BusinessFileAccessOperation operation);

    void assertAllowed(BusinessFileAccessRequest request, BusinessFileAccessReference reference);
}
