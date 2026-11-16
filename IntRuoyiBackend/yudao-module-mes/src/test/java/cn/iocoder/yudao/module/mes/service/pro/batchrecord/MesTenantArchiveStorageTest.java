package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClientFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesTenantArchiveStorageTest {
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void cachedPlatformRetentionSettingsStillRegisterEachTenantSeparately() {
        var storage = new MesEdhrArchiveProtectedStorage();
        var environment = new MockEnvironment()
                .withProperty("EDHR_S3_ENDPOINT","https://storage.invalid")
                .withProperty("EDHR_S3_BUCKET","archive")
                .withProperty("EDHR_S3_REGION","us-east-1")
                .withProperty("EDHR_S3_ACCESS_KEY","unit-test-key")
                .withProperty("EDHR_S3_SECRET_KEY","unit-test-secret")
                .withProperty("EDHR_S3_RETENTION_MODE","COMPLIANCE")
                .withProperty("EDHR_S3_RETAIN_UNTIL_DAYS","365")
                .withProperty("EDHR_S3_REQUIRE_LEGAL_HOLD","true");
        var factory = mock(FileClientFactory.class);
        List<Long> registeredTenants = new ArrayList<>();
        doAnswer(call -> { registeredTenants.add(TenantContextHolder.getRequiredTenantId()); return null; })
                .when(factory).createOrUpdateFileClient(anyLong(),anyInt(),any());
        ReflectionTestUtils.setField(storage,"environment",environment);
        ReflectionTestUtils.setField(storage,"fileClientFactory",factory);
        TenantContextHolder.setTenantId(11L);
        var config = storage.requireClientRegistered();
        TenantContextHolder.setTenantId(22L);
        assertSame(config,storage.requireClientRegistered());
        assertEquals(List.of(11L,22L),registeredTenants);
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class,storage::requireClientRegistered);
        assertEquals(2,registeredTenants.size());
    }
}
