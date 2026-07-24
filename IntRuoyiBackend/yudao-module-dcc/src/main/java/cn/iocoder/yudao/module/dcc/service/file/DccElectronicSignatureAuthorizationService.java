package cn.iocoder.yudao.module.dcc.service.file;

import java.util.Collection;
import java.util.Map;

public interface DccElectronicSignatureAuthorizationService {

    boolean isElectronicSignatureEnabled(Long userId);

    void validateElectronicSignatureEnabled(Long userId);

    Map<Long, Boolean> getAuthorizationMap(Collection<Long> userIds);

    void updateAuthorization(Long userId, boolean enabled);

    void updateAuthorization(Long userId, boolean enabled, Long operatorId, String reason);
}
