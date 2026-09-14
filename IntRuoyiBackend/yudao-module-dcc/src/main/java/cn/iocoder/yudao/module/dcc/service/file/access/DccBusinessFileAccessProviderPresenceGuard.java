package cn.iocoder.yudao.module.dcc.service.file.access;

import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DccBusinessFileAccessProviderPresenceGuard implements SmartInitializingSingleton {

    private final List<BusinessFileAccessProvider> providers;

    public DccBusinessFileAccessProviderPresenceGuard(List<BusinessFileAccessProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    @Override
    public void afterSingletonsInstantiated() {
        long dccProviderCount = providers.stream()
                .filter(provider -> DccBusinessFileAccessProvider.PROVIDER_ID.equals(provider.providerId()))
                .count();
        if (dccProviderCount != 1) {
            throw new IllegalStateException("exactly one DCC business file access provider is required; found "
                    + dccProviderCount);
        }
    }
}
