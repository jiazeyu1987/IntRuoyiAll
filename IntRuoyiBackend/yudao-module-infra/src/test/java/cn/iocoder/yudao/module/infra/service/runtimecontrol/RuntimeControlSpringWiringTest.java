package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class RuntimeControlSpringWiringTest {

    @TempDir
    private Path tempDir;

    @Test
    void runtimeBackupServicesShouldUseExplicitSpringConstructors() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getBackupOps().setNasBackupPointsRoot("nas-backup-points");

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(RuntimeControlProperties.class, () -> properties);
            context.registerBean(NasBrowserService.class, () -> new RuntimeControlNasBrowserServiceStub(tempDir));
            context.registerBean(NasSettingsService.class, () -> nasSettingsService());
            context.register(RuntimeBackupNasRepository.class,
                    RuntimeReleasePackageNasRepository.class,
                    RuntimeBackupDrillServiceImpl.class,
                    RuntimeOpsCandidateServiceImpl.class);

            context.refresh();

            assertNotNull(context.getBean(RuntimeBackupDrillService.class));
            assertNotNull(context.getBean(RuntimeOpsCandidateService.class));
        }
    }

    private NasSettingsService nasSettingsService() {
        return new NasSettingsService() {
            @Override
            public cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigRespVO getNasConfig() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void saveNasConfig(cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigSaveReqVO reqVO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public NasConnectionConfig toConnectionConfig(
                    cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigSaveReqVO reqVO) {
                throw new UnsupportedOperationException();
            }

            @Override
            public NasConnectionConfig getRequiredNasConfig() {
                return new NasConnectionConfig("172.30.30.4", 1445, "IT共享", "WORKGROUP", "nas-user", "nas-secret");
            }
        };
    }
}
