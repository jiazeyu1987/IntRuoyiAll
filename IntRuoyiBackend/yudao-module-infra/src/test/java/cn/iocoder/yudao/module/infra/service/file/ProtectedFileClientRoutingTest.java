package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProtectedFileClientRoutingTest {
    private final FileConfigServiceImpl service = new FileConfigServiceImpl();

    @Test
    void reservedArchiveUsesItsRegisteredProviderWithoutTenantFileTables() {
        FileClient client = mock(FileClient.class);
        ProtectedFileClientProvider provider = mock(ProtectedFileClientProvider.class);
        when(provider.getFileConfigId()).thenReturn(-1040750314L);
        when(provider.getFileClient()).thenReturn(client);
        ReflectionTestUtils.setField(service, "protectedFileClientProviders", List.of(provider));
        assertSame(client, service.getFileClient(-1040750314L));
    }

    @Test
    void missingOrAmbiguousArchiveProviderCannotUseOrdinaryStorage() {
        assertThrows(IllegalStateException.class, () -> service.getFileClient(-1040750314L));
        ProtectedFileClientProvider provider = mock(ProtectedFileClientProvider.class);
        when(provider.getFileConfigId()).thenReturn(-1040750314L);
        ReflectionTestUtils.setField(service, "protectedFileClientProviders", List.of(provider, provider));
        assertThrows(IllegalStateException.class, () -> service.getFileClient(-1040750314L));
        verify(provider, never()).getFileClient();
    }
}
