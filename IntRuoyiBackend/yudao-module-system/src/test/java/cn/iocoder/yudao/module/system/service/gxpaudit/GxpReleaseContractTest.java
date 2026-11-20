package cn.iocoder.yudao.module.system.service.gxpaudit;

import cn.iocoder.yudao.framework.tracer.config.ReleaseRuntimeAutoConfiguration;
import cn.iocoder.yudao.framework.tracer.config.ReleaseRuntimeEndpoint;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.dal.mysql.config.ConfigMapper;
import cn.iocoder.yudao.module.infra.enums.config.GxpAuditMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GxpReleaseContractTest {
    @TempDir Path temp;
    @AfterEach void restore() {
        new GxpAuditDevelopmentMode(new MockEnvironment(), GxpAuditMode.FORMAL);
    }

    @Test void actualReleaseEndpointReadsRunningAndPersistedModesWithoutSwitchingRuntime() throws Exception {
        Path info = temp.resolve("release-info.json");
        Files.writeString(info, "{\"releaseTag\":\"2026-11-16-16-05-38\"}");
        var mapper = mock(ConfigMapper.class);
        var row = new ConfigDO();
        row.setConfigKey(GxpAuditMode.CONFIG_KEY);
        row.setType(1); row.setVisible(true); row.setValue("DEVELOPMENT_DISABLED");
        when(mapper.selectByKey(GxpAuditMode.CONFIG_KEY)).thenReturn(row);
        new ApplicationContextRunner()
                .withUserConfiguration(ReleaseRuntimeAutoConfiguration.class, GxpAuditDevelopmentMode.class)
                .withBean(ConfigMapper.class, () -> mapper)
                .withPropertyValues("spring.profiles.active=prod", "intruoyi.release.maintenance=true",
                        "intruoyi.release.info-path=" + info)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    var endpoint = context.getBean(ReleaseRuntimeEndpoint.class);
                    var state = endpoint.release();
                    assertEquals("available", state.get("status"), state.toString());
                    assertEquals("DEVELOPMENT_DISABLED", state.get("gxpAuditMode"));
                    assertEquals("DEVELOPMENT_DISABLED", state.get("configuredGxpAuditMode"));
                    row.setValue("FORMAL");
                    state = endpoint.release();
                    assertEquals("available", state.get("status"));
                    assertEquals("DEVELOPMENT_DISABLED", state.get("gxpAuditMode"));
                    assertEquals("FORMAL", state.get("configuredGxpAuditMode"));
                    assertTrue(GxpAuditDevelopmentMode.disabled());
                    row.setValue("invalid");
                    assertEquals("unavailable", endpoint.release().get("status"));
                    when(mapper.selectByKey(GxpAuditMode.CONFIG_KEY)).thenReturn(null);
                    assertEquals("unavailable", endpoint.release().get("status"));
                });
    }
}
