package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlLogRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOwnerMatrixSaveReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOverviewRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleasePackageRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRestartReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRestoreCandidateRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasFileReadResult;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RuntimeControlServiceImplTest extends BaseMockitoUnitTest {

    @TempDir
    private Path tempDir;

    @Mock
    private RuntimeControlCommandExecutor commandExecutor;

    @Mock
    private NasSettingsService nasSettingsService;
    @Mock
    private NasBrowserService nasBrowserService;

    private RuntimeControlProperties properties;
    private RuntimeControlServiceImpl runtimeControlService;
    private TestReleasePackageConfigService releasePackageConfigService;

    @BeforeEach
    void setUp() {
        properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getBackupOps().setNasBackupPointsRoot("nas-backup-points");
        releasePackageConfigService = new TestReleasePackageConfigService();
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties),
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir)),
                releasePackageConfigService,
                nasSettingsService, nasBrowserService);
    }

    @Test
    void defaultRuntimeControlPropertiesShouldSeparateReleaseAndBackupNasRoots() {
        RuntimeControlProperties defaults = new RuntimeControlProperties();
        assertEquals("E:/Int/CacheData/IntRuoyi/runtime-control", defaults.getStateDir());
        assertEquals("E:/Int/CacheData/IntRuoyi/runtime", defaults.getStorageGuard().getLogDir());
        assertEquals("172.30.30.58", defaults.getEnvironments().get("test").getHost());
        assertEquals("172.30.30.57", defaults.getEnvironments().get("prod").getHost());
        assertEquals("172.30.30.59", defaults.getEnvironments().get("backup").getHost());

        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);

        assertEquals("Backup/ReleasePackage", properties.getReleasePackage().getNasReleaseRoot());
        assertEquals("", properties.getReleasePackage().getBackendRuntimeBaseMode());
        assertEquals("", properties.getReleasePackage().getBackendRuntimeBaseTarPath());
        assertEquals("", properties.getReleasePackage().getBackendRuntimeBaseTarSha256());
        assertEquals("", properties.getReleasePackage().getBackendRuntimeBaseImage());
        assertEquals("", properties.getReleasePackage().getBackendRuntimeBaseDigest());
        assertEquals("", properties.getReleasePackage().getBackendRuntimeBaseVersion());
        assertEquals("Backup/BackupPackage", properties.getBackupOps().getNasBackupPointsRoot());
        assertEquals("ragflow_compose-minio-1", properties.getEnvironments().get("prod").getRemoteMinioContainer());
        assertEquals(false, properties.getEnvironments().get("prod").isAccessEnabled());
        assertEquals("intruoyi-minio", properties.getEnvironments().get("backup").getRemoteMinioContainer());
    }

    @Test
    void runtimeControlPropertiesShouldRehydrateDefaultTargetsAfterHostOnlyOverride() {
        RuntimeControlProperties properties = new RuntimeControlProperties();
        RuntimeControlProperties.Environment hostOnlyTest = new RuntimeControlProperties.Environment();
        hostOnlyTest.setHost("198.51.100.58");
        properties.getEnvironments().put("test", hostOnlyTest);

        properties.afterPropertiesSet();

        assertNotNull(properties.getTarget("test", "intruoyi-frontend"));
        assertNotNull(properties.getTarget("test", "intruoyi-backend"));
        assertEquals("http://198.51.100.58:8081/", properties.getTarget("test", "intruoyi-frontend").getUrl());
        assertEquals("http://198.51.100.58:48081/actuator/health",
                properties.getTarget("test", "intruoyi-backend").getUrl());
    }

    @Test
    void runtimeControlPropertiesShouldKeepProductionAccessDisabledAfterHostOnlyOverride() {
        RuntimeControlProperties properties = new RuntimeControlProperties();
        RuntimeControlProperties.Environment hostOnlyProd = new RuntimeControlProperties.Environment();
        hostOnlyProd.setHost("198.51.100.57");
        properties.getEnvironments().put("prod", hostOnlyProd);

        properties.afterPropertiesSet();

        assertFalse(properties.getEnvironments().get("prod").isAccessEnabled());
        assertEquals("正式环境写动作未授权，当前任务禁止写入、重启、发布、回滚或恢复正式服务器",
                properties.getEnvironments().get("prod").getAccessDisabledReason());
        assertEquals("http://198.51.100.57:8081/", properties.getTarget("prod", "intruoyi-frontend").getUrl());
    }

    @Test
    void runtimeControlPropertiesShouldKeepBackupRuntimePathsAfterHostOnlyOverride() {
        RuntimeControlProperties properties = new RuntimeControlProperties();
        RuntimeControlProperties.Environment hostOnlyBackup = new RuntimeControlProperties.Environment();
        hostOnlyBackup.setHost("198.51.100.59");
        properties.getEnvironments().put("backup", hostOnlyBackup);

        properties.afterPropertiesSet();

        RuntimeControlProperties.Environment backup = properties.getEnvironments().get("backup");
        assertEquals("/mnt/intruoyi-data/intruoyi-releases", backup.getRemoteReleaseRoot());
        assertEquals("/mnt/intruoyi-data/runtime-data", backup.getRemoteDataRoot());
        assertEquals("/mnt/intruoyi-data", backup.getRemoteDataDiskMount());
        assertEquals("/dev/mapper/cl-home", backup.getRemoteDataDiskDevice());
        assertEquals("intruoyi-minio", backup.getRemoteMinioContainer());
        assertEquals("http://198.51.100.59:8081/", properties.getTarget("backup", "intruoyi-frontend").getUrl());
    }

    @Test
    void getOverviewShouldExposeFixedEnvironmentsAndComponents() {
        doReturn(RuntimeControlStatusResult.error("unavailable")).when(commandExecutor).queryStatus(any());
        doReturn(RuntimeControlStatusResult.running("HTTP 200", "running"))
                .when(commandExecutor).queryStatus(argThat(command -> "local".equals(command.getEnvironment())
                && "intruoyi-backend".equals(command.getComponent())));

        RuntimeControlOverviewRespVO result = runtimeControlService.getOverview();

        assertEquals(List.of("local", "test", "prod", "backup"), result.getEnvironments());
        assertEquals(List.of("intruoyi-frontend", "intruoyi-backend", "intruoyi-full", "website-frontend"),
                result.getComponents());
        assertEquals("running", result.getStatuses().get("local").get("intruoyi-backend").getStatus());
        assertEquals("http://127.0.0.1:48081/actuator/health",
                result.getStatuses().get("local").get("intruoyi-backend").getUrl());
        assertEquals("http://172.30.30.59:8081/",
                result.getStatuses().get("backup").get("intruoyi-full").getUrl());
    }

    @Test
    void getOverviewShouldReadProductionStatusWhenWriteAccessIsDisabled() {
        doReturn(RuntimeControlStatusResult.error("unavailable")).when(commandExecutor).queryStatus(any());
        doReturn(RuntimeControlStatusResult.running("HTTP 200", "running"))
                .when(commandExecutor).queryStatus(argThat(command -> "prod".equals(command.getEnvironment())
                        && "intruoyi-backend".equals(command.getComponent())));

        RuntimeControlOverviewRespVO result = runtimeControlService.getOverview();

        RuntimeControlStatusRespVO prodBackend = result.getStatuses().get("prod").get("intruoyi-backend");
        assertEquals("running", prodBackend.getStatus());
        assertEquals("running", prodBackend.getRuntimeState());
        assertEquals("http://172.30.30.57:48081/actuator/health", prodBackend.getUrl());
        assertEquals(false, prodBackend.getActionEnabled());
        assertTrue(prodBackend.getBlockedReason().contains("正式环境写动作未授权"));
        verify(commandExecutor).queryStatus(argThat(command -> "prod".equals(command.getEnvironment())
                && "intruoyi-backend".equals(command.getComponent())));
    }

    @Test
    void backupStatusCommandShouldUseBackupRuntimeDataDiskArguments() {
        doReturn(RuntimeControlStatusResult.error("unavailable")).when(commandExecutor).queryStatus(any());

        runtimeControlService.getOverview();

        verify(commandExecutor).queryStatus(argThat(command -> "backup".equals(command.getEnvironment())
                && "intruoyi-full".equals(command.getComponent())
                && command.getArguments().contains("-RemoteDataRoot")
                && command.getArguments().contains("/mnt/intruoyi-data/runtime-data")
                && command.getArguments().contains("-RemoteDataDiskMount")
                && command.getArguments().contains("/mnt/intruoyi-data")
                && command.getArguments().contains("-RemoteDataDiskDevice")
                && command.getArguments().contains("/dev/mapper/cl-home")
                && command.getArguments().contains("-RemoteMinioContainer")
                && command.getArguments().contains("intruoyi-minio")
                && !command.getArguments().contains("/dev/vdb")));
    }

    @Test
    void getOverviewShouldQueryStatusesConcurrently() {
        AtomicInteger activeQueries = new AtomicInteger();
        AtomicInteger maxActiveQueries = new AtomicInteger();
        doAnswer(invocation -> {
            int active = activeQueries.incrementAndGet();
            maxActiveQueries.accumulateAndGet(active, Math::max);
            try {
                Thread.sleep(80);
                return RuntimeControlStatusResult.error("unavailable");
            } finally {
                activeQueries.decrementAndGet();
            }
        }).when(commandExecutor).queryStatus(any());

        runtimeControlService.getOverview();

        assertTrue(maxActiveQueries.get() > 1,
                "overview status probes should overlap instead of running one by one");
    }

    @Test
    void getOverviewShouldQuerySameEnvironmentComponentsSequentially() {
        Map<String, AtomicInteger> activeByEnvironment = new ConcurrentHashMap<>();
        AtomicBoolean sameEnvironmentOverlap = new AtomicBoolean(false);
        doAnswer(invocation -> {
            RuntimeControlCommand command = invocation.getArgument(0);
            AtomicInteger active = activeByEnvironment.computeIfAbsent(command.getEnvironment(), key -> new AtomicInteger());
            if (active.incrementAndGet() > 1) {
                sameEnvironmentOverlap.set(true);
            }
            try {
                Thread.sleep(40);
                RuntimeControlStatusResult result = RuntimeControlStatusResult.running("HTTP 200", "running");
                if ("test".equals(command.getEnvironment())) {
                    result.setCurrentReleaseTag("26-06-08_16-11-25");
                }
                return result;
            } finally {
                active.decrementAndGet();
            }
        }).when(commandExecutor).queryStatus(any());

        RuntimeControlOverviewRespVO result = runtimeControlService.getOverview();

        assertFalse(sameEnvironmentOverlap.get(), "same environment status probes must not overlap");
        assertEquals("26-06-08_16-11-25",
                result.getStatuses().get("test").get("intruoyi-full").getCurrentReleaseTag());
    }

    @Test
    void getOverviewShouldLimitConcurrentEnvironmentStatusProbes() {
        AtomicInteger activeQueries = new AtomicInteger();
        AtomicInteger maxActiveQueries = new AtomicInteger();
        doAnswer(invocation -> {
            int active = activeQueries.incrementAndGet();
            maxActiveQueries.accumulateAndGet(active, Math::max);
            try {
                Thread.sleep(40);
                return RuntimeControlStatusResult.running("HTTP 200", "running");
            } finally {
                activeQueries.decrementAndGet();
            }
        }).when(commandExecutor).queryStatus(any());

        runtimeControlService.getOverview();

        assertTrue(maxActiveQueries.get() > 1, "overview should keep bounded environment concurrency");
        assertTrue(maxActiveQueries.get() <= 2, "overview must not start more than two environment probes at once");
    }

    @Test
    void getOverviewShouldUseLocalWorktreePortsFromStatusScriptPayload() {
        doReturn(RuntimeControlStatusResult.error("unavailable")).when(commandExecutor).queryStatus(any());
        RuntimeControlStatusResult backendStatus = RuntimeControlStatusResult.running("HTTP 200", "listening");
        backendStatus.setWorktree("runtime-control-worktree");
        backendStatus.setFrontendPort(8087);
        backendStatus.setBackendPort(48087);
        doReturn(backendStatus).when(commandExecutor).queryStatus(argThat(command -> "local".equals(command.getEnvironment())
                && "intruoyi-backend".equals(command.getComponent())));

        RuntimeControlOverviewRespVO result = runtimeControlService.getOverview();

        RuntimeControlStatusRespVO backend = result.getStatuses().get("local").get("intruoyi-backend");
        assertEquals(48087, backend.getPort());
        assertEquals("http://127.0.0.1:48087/actuator/health", backend.getUrl());
    }

    @Test
    void getOverviewShouldExposeCurrentReleaseTagFromRemoteStatusPayload() {
        doReturn(RuntimeControlStatusResult.error("unavailable")).when(commandExecutor).queryStatus(any());
        RuntimeControlStatusResult fullStatus = RuntimeControlStatusResult.running("backend=HTTP 200; frontend=HTTP 200",
                "backend=running; frontend=running");
        fullStatus.setCurrentReleaseTag("26-05-29_21-05-42");
        doReturn(fullStatus).when(commandExecutor).queryStatus(argThat(command -> "test".equals(command.getEnvironment())
                && "intruoyi-full".equals(command.getComponent())));

        RuntimeControlOverviewRespVO result = runtimeControlService.getOverview();

        RuntimeControlStatusRespVO full = result.getStatuses().get("test").get("intruoyi-full");
        assertEquals("26-05-29_21-05-42", full.getCurrentReleaseTag());
    }

    @Test
    void getReleasePackagesShouldListDirectoriesFromReleaseRepositoryInDescendingOrder() {
        stubNasReleaseConfig();
        FileNasListRespVO response = new FileNasListRespVO()
                .setCurrentPath("Backup/ReleasePackage")
                .setItems(List.of(
                        new FileNasListRespVO.Item().setName("26-05-29_19-12-42").setPath("Backup/ReleasePackage/26-05-29_19-12-42").setDir(true).setSize(0L),
                        new FileNasListRespVO.Item().setName("26-05-29_20-00-00").setPath("Backup/ReleasePackage/26-05-29_20-00-00").setDir(true).setSize(0L),
                        new FileNasListRespVO.Item().setName("README.txt").setPath("Backup/ReleasePackage/README.txt").setDir(false).setSize(128L),
                        new FileNasListRespVO.Item().setName("26-05-29_21-05-42").setPath("Backup/ReleasePackage/26-05-29_21-05-42").setDir(true).setSize(0L)
                ));
        doReturn(response).when(nasBrowserService).listFiles(any(NasConnectionConfig.class), eq("Backup/ReleasePackage"));
        stubReleasePackageManifest("Backup/ReleasePackage/26-05-29_19-12-42", "26-05-29_19-12-42", true);
        stubReleasePackageManifest("Backup/ReleasePackage/26-05-29_20-00-00", "26-05-29_20-00-00", false);
        stubReleasePackageManifest("Backup/ReleasePackage/26-05-29_21-05-42", "26-05-29_21-05-42", true);

        List<RuntimeControlReleasePackageRespVO> result = runtimeControlService.getReleasePackages();

        assertEquals(List.of("26-05-29_21-05-42", "26-05-29_19-12-42"),
                result.stream().map(RuntimeControlReleasePackageRespVO::getReleaseTag).toList());
        assertTrue(result.stream().allMatch(item -> "AVAILABLE".equals(item.getStatus())));
        assertTrue(result.stream().allMatch(RuntimeControlReleasePackageRespVO::getChecksumPresent));
        assertTrue(result.stream().allMatch(RuntimeControlReleasePackageRespVO::getOnlyOfficeIncluded));
        assertTrue(result.stream().allMatch(item -> "intruoyi".equals(item.getComponent())));
        assertTrue(result.stream().allMatch(item -> Boolean.FALSE.equals(item.getIncludeShowroomBuildPackage())));
        verify(nasBrowserService).listFiles(argThat(config ->
                        "172.30.30.4".equals(config.server())
                                && "IT共享".equals(config.share())
                                && "nas-user".equals(config.username())
                                && "nas-secret".equals(config.password())),
                eq("Backup/ReleasePackage"));
    }

    @Test
    void getReleasePackagesShouldScanAllAvailableReleasePackageDirectories() {
        stubNasReleaseConfig();
        List<FileNasListRespVO.Item> items = new java.util.ArrayList<>();
        for (int index = 0; index < 10; index++) {
            String releaseTag = "26-06-08_12-00-%02d".formatted(index);
            String packagePath = "Backup/ReleasePackage/" + releaseTag;
            items.add(new FileNasListRespVO.Item().setName(releaseTag).setPath(packagePath).setDir(true).setSize(0L));
            stubReleasePackageManifest(packagePath, releaseTag, true);
        }
        FileNasListRespVO response = new FileNasListRespVO()
                .setCurrentPath("Backup/ReleasePackage")
                .setItems(items);
        doReturn(response).when(nasBrowserService).listFiles(any(NasConnectionConfig.class), eq("Backup/ReleasePackage"));

        List<RuntimeControlReleasePackageRespVO> result = runtimeControlService.getReleasePackages();

        assertEquals(10, result.size());
        assertEquals(List.of(
                        "26-06-08_12-00-09",
                        "26-06-08_12-00-08",
                        "26-06-08_12-00-07",
                        "26-06-08_12-00-06",
                        "26-06-08_12-00-05",
                        "26-06-08_12-00-04",
                        "26-06-08_12-00-03",
                        "26-06-08_12-00-02",
                        "26-06-08_12-00-01",
                        "26-06-08_12-00-00"),
                result.stream().map(RuntimeControlReleasePackageRespVO::getPackageDirectoryName).toList());
    }

    @Test
    void getReleasePackagesShouldIncludeNewestNightlyPackageEvenWhenDirectoryNamesSortBehindLegacyNames() {
        stubNasReleaseConfig();
        List<FileNasListRespVO.Item> items = new java.util.ArrayList<>();
        for (int index = 0; index < 6; index++) {
            String releaseTag = "26-06-10_10-31-%02d".formatted(index);
            items.add(new FileNasListRespVO.Item().setName(releaseTag)
                    .setPath("Backup/ReleasePackage/" + releaseTag)
                    .setDir(true).setSize(0L).setModifiedAt(1_718_000_000_000L - index));
        }
        items.add(new FileNasListRespVO.Item().setName("20260613_nightly_code_only_020000")
                .setPath("Backup/ReleasePackage/20260613_nightly_code_only_020000")
                .setDir(true).setSize(0L).setModifiedAt(1_718_000_001_000L));
        FileNasListRespVO response = new FileNasListRespVO()
                .setCurrentPath("Backup/ReleasePackage")
                .setItems(items);
        doReturn(response).when(nasBrowserService).listFiles(any(NasConnectionConfig.class), eq("Backup/ReleasePackage"));
        for (int index = 0; index < 6; index++) {
            String releaseTag = "26-06-10_10-31-%02d".formatted(index);
            stubReleasePackageManifest("Backup/ReleasePackage/" + releaseTag, releaseTag, true);
        }
        stubReleasePackageManifest("Backup/ReleasePackage/20260613_nightly_code_only_020000",
                "20260613_nightly_code_only_020000", true);

        List<RuntimeControlReleasePackageRespVO> result = runtimeControlService.getReleasePackages();

        assertTrue(result.stream()
                .map(RuntimeControlReleasePackageRespVO::getPackageDirectoryName)
                .toList()
                .contains("20260613_nightly_code_only_020000"));
    }

    @Test
    void getReleasePackagesShouldIncludeNewFormatPackageWhenNasModifiedAtIsMissing() {
        stubNasReleaseConfig();
        List<FileNasListRespVO.Item> items = new java.util.ArrayList<>();
        for (int index = 0; index < 6; index++) {
            String releaseTag = "26-06-10_10-31-%02d".formatted(index);
            String packagePath = "Backup/ReleasePackage/" + releaseTag;
            items.add(new FileNasListRespVO.Item().setName(releaseTag).setPath(packagePath).setDir(true).setSize(0L));
            stubReleasePackageManifest(packagePath, releaseTag, true);
        }
        String manualTag = "20260613_manual_code_only_114753";
        String manualPath = "Backup/ReleasePackage/" + manualTag;
        items.add(new FileNasListRespVO.Item().setName(manualTag).setPath(manualPath).setDir(true).setSize(0L));
        stubReleasePackageManifest(manualPath, manualTag, true);
        FileNasListRespVO response = new FileNasListRespVO()
                .setCurrentPath("Backup/ReleasePackage")
                .setItems(items);
        doReturn(response).when(nasBrowserService).listFiles(any(NasConnectionConfig.class), eq("Backup/ReleasePackage"));

        List<RuntimeControlReleasePackageRespVO> result = runtimeControlService.getReleasePackages();

        assertTrue(result.stream()
                .map(RuntimeControlReleasePackageRespVO::getPackageDirectoryName)
                .toList()
                .contains(manualTag));
    }

    @Test
    void restartProductionShouldRequireReasonAndExactConfirmText() {
        RuntimeControlRestartReqVO reqVO = new RuntimeControlRestartReqVO();
        reqVO.setEnvironment("prod");
        reqVO.setComponent("website-frontend");
        reqVO.setReason("");
        reqVO.setProdConfirmText("prod");

        assertServiceException(() -> runtimeControlService.restart(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_PROD_GUARD_REQUIRED);

        reqVO.setReason("发布后重启");
        assertServiceException(() -> runtimeControlService.restart(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_PROD_GUARD_REQUIRED);
    }

    @Test
    void restartShouldDispatchOnlyWhitelistedTargetAndPersistOperation() {
        RuntimeControlRestartReqVO reqVO = new RuntimeControlRestartReqVO();
        reqVO.setEnvironment("test");
        reqVO.setComponent("website-frontend");
        reqVO.setReason("测试服展厅前端重启");

        RuntimeControlOperationRespVO result = runtimeControlService.restart(reqVO, "1001");

        assertNotNull(result.getOperationId());
        assertEquals("test", result.getEnvironment());
        assertEquals("website-frontend", result.getComponent());
        assertEquals("running", result.getStatus());
        verify(commandExecutor).restart(argThat(command -> "test".equals(command.getEnvironment())
                && "website-frontend".equals(command.getComponent())
                && command.getArguments().contains("-Component")
                && command.getArguments().contains("website")));
        assertEquals(1, runtimeControlService.getOperations().size());
    }

    @Test
    void restartShouldFailFastWhenRequestedByIsBlank() {
        RuntimeControlRestartReqVO reqVO = new RuntimeControlRestartReqVO();
        reqVO.setEnvironment("test");
        reqVO.setComponent("website-frontend");
        reqVO.setReason("测试服展厅前端重启");

        assertServiceException(() -> runtimeControlService.restart(reqVO, " "),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "requestedBy");

        verify(commandExecutor, never()).restart(any());
    }

    @Test
    void executeBuildReleaseShouldUseNasConfigSnapshotAndPersistAudit() throws Exception {
        stubNasReleaseConfig();
        configureBackendRuntimeBase();
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("build-release");
        reqVO.setReason("构建发布包验证");
        reqVO.setPublishScope("with-data");
        reqVO.setReleaseTag("20260528_220000");
        reqVO.setIncludeOnlyOffice(true);
        reqVO.setIncludeShowroomBuildPackage(true);

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertNotNull(result.getOperationId());
        assertEquals("build-release", result.getAction());
        assertEquals("release", result.getEnvironment());
        assertEquals("ops", result.getComponent());
        assertEquals("running", result.getStatus());
        assertEquals("with-data", result.getParameters().get("publishScope"));
        assertEquals("true", result.getParameters().get("includeOnlyOffice"));
        assertEquals("true", result.getParameters().get("includeShowroomBuildPackage"));
        assertEquals("20260528_220000", result.getParameters().get("releaseTag"));
        verify(nasSettingsService).getRequiredNasConfig();
        verify(commandExecutor, timeout(1000)).executeOperation(argThat(command -> "release".equals(command.getEnvironment())
                && "ops".equals(command.getComponent())
                && "script/deploy/publish-int-ruoyi.ps1".equals(command.getScriptPath())
                && command.getArguments().contains("-Mode")
                && command.getArguments().contains("build-release")
                && command.getArguments().contains("-ReleaseTag")
                && command.getArguments().contains("20260528_220000")
                && containsArgumentPair(command.getArguments(), "-Component", "full")
                && command.getArguments().contains("-IncludeOnlyOffice")
                && command.getArguments().contains("-NasConfigPath")
                && command.getArguments().contains("-NasServer")
                && command.getArguments().contains("172.30.30.4")
                && command.getArguments().contains("-NasShare")
                && command.getArguments().contains("IT共享")
                && command.getArguments().contains("-NasReleaseRoot")
                && command.getArguments().contains("Backup/ReleasePackage")
                && containsArgumentPair(command.getArguments(), "-TestServerHost", "172.30.30.58")
                && containsArgumentPair(command.getArguments(), "-BackupServerHost", "172.30.30.59")
                && !command.getArguments().contains("-ProdServerHost")
                && !String.join(" ", command.getArguments()).contains("nas-secret")), any());
        waitOperationStatus(result.getOperationId(), "succeeded");
        assertTrue(java.nio.file.Files.notExists(
                tempDir.resolve("nas-release-config").resolve(result.getOperationId() + ".json")));
    }

    @Test
    void executeBuildReleaseShouldPassBackendRuntimeBaseConfigFromDatabase() throws Exception {
        stubNasReleaseConfig();
        configureBackendRuntimeBase();
        assertEquals("", properties.getReleasePackage().getBackendRuntimeBaseMode());
        assertEquals("", properties.getReleasePackage().getBackendRuntimeBaseTarPath());
        assertEquals("", properties.getReleasePackage().getBackendRuntimeBaseTarSha256());
        assertEquals("", properties.getReleasePackage().getBackendRuntimeBaseImage());
        assertEquals("", properties.getReleasePackage().getBackendRuntimeBaseDigest());
        assertEquals("", properties.getReleasePackage().getBackendRuntimeBaseVersion());
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("build-release");
        reqVO.setReason("构建发布包验证");
        reqVO.setPublishScope("code-only");
        reqVO.setReleaseTag("20260528_220002");
        reqVO.setIncludeOnlyOffice(false);
        reqVO.setIncludeShowroomBuildPackage(false);

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        verify(commandExecutor, timeout(1000)).executeOperation(argThat(command ->
                command.getArguments().contains("-BackendRuntimeBaseMode")
                        && command.getArguments().contains("offline-tar")
                        && command.getArguments().contains("-BackendRuntimeBaseTarPath")
                        && command.getArguments().contains("D:/ProjectPackage/Int/BaseImages/intruoyi-backend-runtime-base-20260604.tar")
                        && command.getArguments().contains("-BackendRuntimeBaseTarSha256")
                        && command.getArguments().contains("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                        && command.getArguments().contains("-BackendRuntimeBaseImage")
                        && command.getArguments().contains("intruoyi-backend-runtime-base:20260604")
                        && command.getArguments().contains("-BackendRuntimeBaseDigest")
                        && command.getArguments().contains("sha256:abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd")
                        && command.getArguments().contains("-BackendRuntimeBaseVersion")
                        && command.getArguments().contains("20260604")), any());
        waitOperationStatus(result.getOperationId(), "succeeded");
    }

    @Test
    void executeBuildReleaseShouldPassSmartReleaseReportOnlyFlagFromRequest() throws Exception {
        stubNasReleaseConfig();
        configureBackendRuntimeBase();
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("build-release");
        reqVO.setReason("Smart Release report-only 构建报告");
        reqVO.setPublishScope("code-only");
        reqVO.setReleaseTag("20260528_220005");
        reqVO.setIncludeOnlyOffice(false);
        reqVO.setIncludeShowroomBuildPackage(false);
        reqVO.setEnableSmartReleaseReport(true);

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("true", result.getParameters().get("enableSmartReleaseReport"));
        verify(commandExecutor, timeout(1000)).executeOperation(argThat(command ->
                containsArgumentPair(command.getArguments(), "-TestServerHost",
                        properties.getEnvironments().get("test").getHost())
                        && containsArgumentPair(command.getArguments(), "-BackupServerHost",
                        properties.getEnvironments().get("backup").getHost())
                        && !command.getArguments().contains("-ProdServerHost")
                        && command.getArguments().contains("-EnableSmartReleaseReport")
                        && command.getArguments().contains("-BackendRuntimeBaseMode")
                        && command.getArguments().contains("offline-tar")
                        && !String.join(" ", command.getArguments()).contains("203.0.113.")), any());
        waitOperationStatus(result.getOperationId(), "succeeded");
    }

    @Test
    void previewPublishTestShouldReturnSmartReleaseReportOnlyCommandWithoutExecuting() {
        stubNasReleaseConfig();
        stubReleasePackageList("20260528_220000", true, false);
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("publish-test");
        reqVO.setReason("Smart Release report-only 部署预检");
        reqVO.setReleaseTag("20260528_220000");
        reqVO.setEnableSmartReleaseReport(true);

        var result = runtimeControlService.previewAction(reqVO, "1001");

        assertEquals("publish-test", result.getAction());
        assertEquals("test", result.getEnvironment());
        assertEquals("script/deploy/publish-int-ruoyi.ps1", result.getScriptPath());
        assertEquals(true, result.getEnableSmartReleaseReport());
        assertTrue(result.getArguments().contains("-EnableSmartReleaseReport"));
        assertTrue(containsArgumentPair(result.getArguments(), "-ServerHost",
                properties.getEnvironments().get("test").getHost()));
        assertTrue(containsArgumentPair(result.getArguments(), "-TestServerHost",
                properties.getEnvironments().get("test").getHost()));
        assertTrue(containsArgumentPair(result.getArguments(), "-BackupServerHost",
                properties.getEnvironments().get("backup").getHost()));
        assertFalse(result.getArguments().contains("-ProdServerHost"));
        verify(commandExecutor, never()).executeOperation(any(), any());
        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
    }

    @Test
    void executeBuildReleaseShouldRequireConfiguredTargetHosts() {
        configureBackendRuntimeBase();
        properties.getEnvironments().get("test").setHost(" ");
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("build-release");
        reqVO.setReason("构建发布包验证");
        reqVO.setPublishScope("code-only");
        reqVO.setReleaseTag("20260528_220004");
        reqVO.setIncludeOnlyOffice(false);
        reqVO.setIncludeShowroomBuildPackage(false);

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "-TestServerHost");
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeBuildReleaseShouldRequireBackendRuntimeBaseConfig() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("build-release");
        reqVO.setReason("构建发布包验证");
        reqVO.setPublishScope("code-only");
        reqVO.setReleaseTag("20260528_220003");
        reqVO.setIncludeOnlyOffice(false);
        reqVO.setIncludeShowroomBuildPackage(false);

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED,
                "infra_config.runtime-control.release-package.backend-runtime-base-mode (-BackendRuntimeBaseMode)");
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeBuildReleaseShouldRequireOnlyOfficeDecision() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("build-release");
        reqVO.setReason("构建发布包验证");
        reqVO.setPublishScope("code-only");
        reqVO.setReleaseTag("20260528_220000");
        reqVO.setIncludeShowroomBuildPackage(false);

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "includeOnlyOffice");
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeBuildReleaseShouldRequireShowroomBuildPackageDecision() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("build-release");
        reqVO.setReason("构建发布包验证");
        reqVO.setPublishScope("code-only");
        reqVO.setReleaseTag("20260528_220000");
        reqVO.setIncludeOnlyOffice(false);

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "includeShowroomBuildPackage");
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeBuildReleaseShouldOmitOnlyOfficeArgumentWhenDecisionIsFalse() throws Exception {
        stubNasReleaseConfig();
        configureBackendRuntimeBase();
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("build-release");
        reqVO.setReason("构建发布包验证");
        reqVO.setPublishScope("code-only");
        reqVO.setReleaseTag("20260528_220001");
        reqVO.setIncludeOnlyOffice(false);
        reqVO.setIncludeShowroomBuildPackage(false);

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("false", result.getParameters().get("includeOnlyOffice"));
        assertEquals("false", result.getParameters().get("includeShowroomBuildPackage"));
        verify(commandExecutor, timeout(1000)).executeOperation(argThat(command ->
                command.getArguments().contains("-Mode")
                        && command.getArguments().contains("build-release")
                        && containsArgumentPair(command.getArguments(), "-Component", "intruoyi")
                        && command.getArguments().contains("-SkipDatabaseSync")
                        && command.getArguments().contains("-SkipMinioSync")
                        && !command.getArguments().contains("-IncludeOnlyOffice")), any());
        waitOperationStatus(result.getOperationId(), "succeeded");
    }

    @Test
    void executePublishTestShouldDeploySelectedReleasePackageToTest() throws Exception {
        stubNasReleaseConfig();
        stubReleasePackageList("20260528_220000", true, false);
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("publish-test");
        reqVO.setReason("发布测试服验证");
        reqVO.setReleaseTag("20260528_220000");

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertNotNull(result.getOperationId());
        assertEquals("publish-test", result.getAction());
        assertEquals("test", result.getEnvironment());
        assertEquals("ops", result.getComponent());
        assertEquals("running", result.getStatus());
        assertNotNull(result.getResultLogPath());
        assertEquals("20260528_220000", result.getParameters().get("releaseTag"));
        verify(commandExecutor, timeout(1000)).executeOperation(argThat(command -> "test".equals(command.getEnvironment())
                && "ops".equals(command.getComponent())
                && "script/deploy/publish-int-ruoyi.ps1".equals(command.getScriptPath())
                && command.getArguments().contains("-Mode")
                && command.getArguments().contains("deploy-release")
                && command.getArguments().contains("-Environment")
                && command.getArguments().contains("test")
                && command.getArguments().contains("-ReleaseTag")
                && command.getArguments().contains("20260528_220000")
                && command.getArguments().contains("-NasConfigPath")), any());
        waitOperationStatus(result.getOperationId(), "succeeded");
    }

    @Test
    void executeActionShouldFailFastWhenRequestedByIsBlank() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("publish-test");
        reqVO.setReason("发布测试服验证");
        reqVO.setReleaseTag("20260528_220000");

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, " "),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "requestedBy");

        verify(commandExecutor, never()).executeOperation(any(), any());
        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
    }

    @Test
    void executePublishTestShouldRequireReleaseTag() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("publish-test");
        reqVO.setReason("发布测试服验证");

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "releaseTag");
    }

    @Test
    void executeApplyTestDbSqlShouldDispatchOnlyTestServerScript() throws Exception {
        Path sqlPath = tempDir.resolve("quick-apply.sql");
        Files.writeString(sqlPath, "SELECT 1;\n", StandardCharsets.UTF_8);
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("apply-test-db-sql");
        reqVO.setReason("测试服数据库快应用验证");
        reqVO.setSqlPath(sqlPath.toString());

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("apply-test-db-sql", result.getAction());
        assertEquals("test", result.getEnvironment());
        assertEquals("ops", result.getComponent());
        assertEquals(sqlPath.toString(), result.getParameters().get("sqlPath"));
        verify(commandExecutor, timeout(1000)).executeOperation(argThat(command ->
                "test".equals(command.getEnvironment())
                        && "ops".equals(command.getComponent())
                        && "script/deploy/apply-test-db-sql.ps1".equals(command.getScriptPath())
                        && command.getArguments().contains("-SqlPath")
                        && command.getArguments().contains(sqlPath.toString())
                        && command.getArguments().contains("-ServerHost")
                        && command.getArguments().contains("172.30.30.58")
                        && command.getArguments().contains("-RemoteAppDir")
                        && command.getArguments().contains("/opt/intruoyi/runtime")
                        && !command.getArguments().contains("-NasConfigPath")), any());
        waitOperationStatus(result.getOperationId(), "succeeded");
    }

    @Test
    void executeApplyTestDbSqlShouldRequireSqlPath() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("apply-test-db-sql");
        reqVO.setReason("测试服数据库快应用验证");

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "sqlPath");
    }

    @Test
    void executeNonDbQuickApplyActionShouldRejectSqlPath() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("publish-test");
        reqVO.setReason("发布测试服验证");
        reqVO.setReleaseTag("20260528_220000");
        reqVO.setSqlPath("D:/tmp/quick.sql");

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "sqlPath");
    }

    @Test
    void executeMarkReleaseTestedShouldWriteTestedMarkerThroughScript() throws Exception {
        stubNasReleaseConfig();
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getBackupOps().setNasBackupPointsRoot("nas-backup-points");
        createRestoreCandidateFixture(properties, "20260525-215449", "20260525_200033");
        RuntimeOpsCandidateService candidateService =
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
        String recoverySetCandidateId = candidateService.listRestoreCandidates().get(0).getCandidateId();
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("mark-release-tested");
        reqVO.setReason("测试服验证通过");
        reqVO.setTestConclusion("回归通过，允许上线正式服");
        reqVO.setSelectedRecoverySetCandidateId(recoverySetCandidateId);
        RuntimeControlStatusResult testStatus = RuntimeControlStatusResult.running("HTTP 200", "running");
        testStatus.setCurrentReleaseTag("20260528_220000");
        doReturn(testStatus).when(commandExecutor).queryStatus(argThat(command -> "test".equals(command.getEnvironment())
                && "intruoyi-full".equals(command.getComponent())));

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("mark-release-tested", result.getAction());
        assertEquals("20260528_220000", result.getParameters().get("releaseTag"));
        assertEquals("回归通过，允许上线正式服", result.getParameters().get("testConclusion"));
        verify(commandExecutor, timeout(1000)).executeOperation(argThat(command ->
                "script/deploy/publish-int-ruoyi.ps1".equals(command.getScriptPath())
                        && command.getArguments().contains("-Mode")
                        && command.getArguments().contains("mark-tested")
                        && command.getArguments().contains("-ReleaseTag")
                        && command.getArguments().contains("20260528_220000")
                        && command.getArguments().contains("-TestConclusion")
                        && command.getArguments().contains("回归通过，允许上线正式服")
                        && command.getArguments().contains("-SelectedRecoverySetCandidateId")
                        && command.getArguments().contains(recoverySetCandidateId)
                        && command.getArguments().contains("-RecoverySetId")
                        && command.getArguments().contains("20260525-215449")
                        && command.getArguments().contains("-RecoverySetManifestHash")
                        && command.getArguments().contains("-NasConfigPath")), any());
        waitOperationStatus(result.getOperationId(), "succeeded");
    }

    @Test
    void executeMarkReleaseTestedShouldFailFastWhenCurrentTestReleaseTagMissing() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("mark-release-tested");
        reqVO.setReason("测试服验证通过");
        reqVO.setTestConclusion("回归通过，允许上线正式服");
        doReturn(RuntimeControlStatusResult.running("HTTP 200", "running"))
                .when(commandExecutor).queryStatus(any());

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "testCurrentReleaseTag");

        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeProductionActionsShouldRequireReasonAndProdConfirm() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(
                tempDir.resolve("enabled-prod-guard"));
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties),
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir)),
                nasSettingsService, nasBrowserService);
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("promote-prod");
        reqVO.setReason("");
        reqVO.setProdConfirmText("PROD");
        reqVO.setReleaseTag("20260528_220000");

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_PROD_GUARD_REQUIRED);

        reqVO.setReason("提升正式服");
        reqVO.setProdConfirmText("prod");
        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_PROD_GUARD_REQUIRED);
    }

    @Test
    void executeProductionActionsShouldFailFastWhenProductionAccessIsDisabled() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("promote-prod");
        reqVO.setReason("上线正式服");
        reqVO.setProdConfirmText("PROD");
        reqVO.setReleaseTag("20260528_220000");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));
        assertEquals(ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("prod 环境未启用"));
        assertTrue(exception.getMessage().contains("正式环境写动作未授权"));

        verify(commandExecutor, never()).executeOperation(any(), any());
        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
    }

    @Test
    void executePromoteProdShouldDeployOnlyVerifiedReleasePackageAndKeepProdGuard() throws Exception {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(
                tempDir.resolve("enabled-prod-promote"));
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties),
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir)),
                nasSettingsService, nasBrowserService);
        stubNasReleaseConfig();
        stubReleasePackageList("20260528_220000", true, true);
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("promote-prod");
        reqVO.setReason("上线正式服");
        reqVO.setProdConfirmText("PROD");
        reqVO.setReleaseTag("20260528_220000");

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("promote-prod", result.getAction());
        assertEquals("prod", result.getEnvironment());
        assertEquals("20260528_220000", result.getParameters().get("releaseTag"));
        verify(commandExecutor, timeout(1000)).executeOperation(argThat(command ->
                "script/deploy/publish-int-ruoyi.ps1".equals(command.getScriptPath())
                        && command.getArguments().contains("-Mode")
                        && command.getArguments().contains("deploy-release")
                        && command.getArguments().contains("-Environment")
                        && command.getArguments().contains("prod")
                        && command.getArguments().contains("-ConfirmText")
                        && command.getArguments().contains("PROD")
                        && command.getArguments().contains("-RequireTested")
                        && command.getArguments().contains("-ReleaseTag")
                        && command.getArguments().contains("20260528_220000")
                        && command.getArguments().contains("-RemoteMinioContainer")
                        && command.getArguments().contains("ragflow_compose-minio-1")), any());
        waitOperationStatus(result.getOperationId(), "succeeded");
    }

    @Test
    void executePromoteBackupShouldDeployOnlyVerifiedReleasePackageAndKeepProdGuard() throws Exception {
        stubNasReleaseConfig();
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.getBackupOps().setNasBackupPointsRoot("nas-backup-points");
        RuntimeControlRestoreCandidateRespVO recoverySet = createAvailableRecoverySetCandidate(properties);
        stubReleasePackageList("20260528_220000", true, true, recoverySet);
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("promote-backup");
        reqVO.setReason("上线备用服务器");
        reqVO.setProdConfirmText("PROD");
        reqVO.setReleaseTag("20260528_220000");

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("promote-backup", result.getAction());
        assertEquals("backup", result.getEnvironment());
        assertEquals("20260528_220000", result.getParameters().get("releaseTag"));
        verify(commandExecutor, timeout(1000)).executeOperation(argThat(command ->
                "script/deploy/publish-int-ruoyi.ps1".equals(command.getScriptPath())
                        && command.getArguments().contains("-Mode")
                        && command.getArguments().contains("deploy-release")
                        && command.getArguments().contains("-Environment")
                        && command.getArguments().contains("backup")
                        && command.getArguments().contains("-ConfirmText")
                        && command.getArguments().contains("PROD")
                        && command.getArguments().contains("-RequireTested")
                        && command.getArguments().contains("-ReleaseTag")
                        && command.getArguments().contains("20260528_220000")
                        && command.getArguments().contains("-RemoteReleaseRoot")
                        && command.getArguments().contains("/mnt/intruoyi-data/intruoyi-releases")
                        && command.getArguments().contains("-RemoteDataRoot")
                        && command.getArguments().contains("/mnt/intruoyi-data/runtime-data")
                        && command.getArguments().contains("-RemoteDataDiskMount")
                        && command.getArguments().contains("/mnt/intruoyi-data")
                        && command.getArguments().contains("-RemoteDataDiskDevice")
                        && command.getArguments().contains("/dev/mapper/cl-home")
                        && command.getArguments().contains("-RemoteMinioContainer")
                        && command.getArguments().contains("intruoyi-minio")
                        && !command.getArguments().contains("ragflow_compose-minio-1")), any());
        waitOperationStatus(result.getOperationId(), "succeeded");
    }

    @Test
    void executePromoteBackupWithDefaultReleaseOwnerShouldReachReleasePackageValidation() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(
                tempDir.resolve("default-backup-owner"));
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        RuntimeOpsResponsibilityServiceImpl responsibilityService =
                new RuntimeOpsResponsibilityServiceImpl(new RuntimeOpsOwnerMatrixStore(properties));
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                responsibilityService,
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir)),
                nasSettingsService, nasBrowserService);
        stubNasReleaseConfig();
        stubReleasePackageList("20260528_220000", true, false);

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("promote-backup");
        reqVO.setReason("上线备用服务器");
        reqVO.setProdConfirmText("PROD");
        reqVO.setReleaseTag("20260528_220000");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));
        assertEquals(ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("尚未测试通过"));
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executePromoteBackupShouldBlockReleasePackageWithoutTestedMarkerBeforeDispatch() {
        stubNasReleaseConfig();
        stubReleasePackageList("20260528_220000", true, false);
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("promote-backup");
        reqVO.setReason("上线备用服务器");
        reqVO.setProdConfirmText("PROD");
        reqVO.setReleaseTag("20260528_220000");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));
        assertEquals(ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("尚未测试通过"));
        RuntimeControlOperationRespVO blocked = runtimeControlService.getOperations().get(0);
        assertEquals("blocked", blocked.getStatus());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executePromoteProdShouldRequireReleaseTag() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(
                tempDir.resolve("enabled-prod-release-tag"));
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties),
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir)),
                nasSettingsService, nasBrowserService);
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("promote-prod");
        reqVO.setReason("上线正式服");
        reqVO.setProdConfirmText("PROD");

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "releaseTag");
    }

    @Test
    void executeNonPublishActionShouldRejectPublishScope() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("backup-now");
        reqVO.setReason("立即备份");
        reqVO.setTargetEnvironment("prod");
        reqVO.setProdConfirmText("PROD");
        reqVO.setPublishScope("code-only");

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "publishScope");
    }

    @Test
    void executeNonBuildReleaseActionShouldRejectOnlyOfficeDecision() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("publish-test");
        reqVO.setReason("发布测试服验证");
        reqVO.setReleaseTag("20260528_220000");
        reqVO.setIncludeOnlyOffice(false);
        reqVO.setIncludeShowroomBuildPackage(false);

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "includeOnlyOffice");
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeNonBuildReleaseActionShouldRejectShowroomBuildPackageDecision() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("publish-test");
        reqVO.setReason("发布测试服验证");
        reqVO.setReleaseTag("20260528_220000");
        reqVO.setIncludeShowroomBuildPackage(false);

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "includeShowroomBuildPackage");
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executePublishTestShouldBlockIncompleteReleasePackageBeforeDispatch() {
        stubNasReleaseConfig();
        stubReleasePackageList("20260528_220000", false, false);
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("publish-test");
        reqVO.setReason("发布测试服验证");
        reqVO.setReleaseTag("20260528_220000");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));
        assertEquals(ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("manifest/checksum"));
        RuntimeControlOperationRespVO blocked = runtimeControlService.getOperations().get(0);
        assertEquals("blocked", blocked.getStatus());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executePublishTestShouldBlockReleasePackageWithoutComponentBeforeDispatch() {
        stubNasReleaseConfig();
        String releaseTag = "20260528_220000";
        String packagePath = "Backup/ReleasePackage/" + releaseTag;
        doReturn(nasList("Backup/ReleasePackage",
                new FileNasListRespVO.Item().setName(releaseTag).setPath(packagePath).setDir(true).setSize(0L)))
                .when(nasBrowserService).listFiles(any(NasConnectionConfig.class), eq("Backup/ReleasePackage"));
        stubReleasePackageManifestWithoutComponent(packagePath, releaseTag);
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("publish-test");
        reqVO.setReason("发布测试服验证");
        reqVO.setReleaseTag(releaseTag);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));
        assertEquals(ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("manifest"));
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executePromoteProdShouldBlockReleasePackageWithoutTestedMarkerBeforeDispatch() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(
                tempDir.resolve("enabled-prod-untested"));
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties),
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir)),
                nasSettingsService, nasBrowserService);
        stubNasReleaseConfig();
        stubReleasePackageList("20260528_220000", true, false);
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("promote-prod");
        reqVO.setReason("上线正式服");
        reqVO.setProdConfirmText("PROD");
        reqVO.setReleaseTag("20260528_220000");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));
        assertEquals(ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("尚未测试通过"));
        RuntimeControlOperationRespVO blocked = runtimeControlService.getOperations().get(0);
        assertEquals("blocked", blocked.getStatus());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeBackupNowShouldAllowTestEnvironmentWithoutProdConfirmAndPersistTargetEnvironment() throws Exception {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("backup-now");
        reqVO.setTargetEnvironment("test");
        reqVO.setReason("测试服立即备份");

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("backup-now", result.getAction());
        assertEquals("test", result.getEnvironment());
        assertEquals("test", result.getParameters().get("targetEnvironment"));
        verify(commandExecutor, timeout(1000)).executeOperation(argThat(command ->
                "test".equals(command.getEnvironment())
                        && command.getArguments().contains("-TargetEnvironment")
                        && command.getArguments().contains("test")
                        && !command.getArguments().contains("-ConfirmText")), any());
        waitOperationStatus(result.getOperationId(), "succeeded");
    }

    @Test
    void executeBackupNowShouldPersistBlockedOperationWhenProdConfirmIsMissing() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("backup-now");
        reqVO.setTargetEnvironment("prod");
        reqVO.setReason("正式服立即备份");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));

        assertEquals(ErrorCodeConstants.RUNTIME_CONTROL_PROD_GUARD_REQUIRED.getCode(), exception.getCode());
        List<RuntimeControlOperationRespVO> operations = runtimeControlService.getOperations();
        assertEquals(1, operations.size());
        RuntimeControlOperationRespVO blocked = operations.get(0);
        assertEquals("backup-now", blocked.getAction());
        assertEquals("prod", blocked.getEnvironment());
        assertEquals("blocked", blocked.getStatus());
        assertTrue(blocked.getSummary().contains("PROD"));
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeBackupNowShouldAllowProdReadonlyBackupWhenProductionAccessIsDisabled() throws Exception {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("backup-now");
        reqVO.setTargetEnvironment("prod");
        reqVO.setReason("正式服只读立即备份");
        reqVO.setProdConfirmText("PROD");

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("backup-now", result.getAction());
        assertEquals("prod", result.getEnvironment());
        assertEquals("prod", result.getParameters().get("targetEnvironment"));
        verify(commandExecutor, timeout(1000)).executeOperation(argThat(command ->
                "prod".equals(command.getEnvironment())
                        && command.getArguments().contains("-TargetEnvironment")
                        && command.getArguments().contains("prod")
                        && command.getArguments().contains("backup-now")), any());
        waitOperationStatus(result.getOperationId(), "succeeded");
    }

    @Test
    void executeBackupNowShouldUseLinuxLocalBackupOpsWhenConfigured() throws Exception {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.setRepoRoot(tempDir.toString());
        properties.getBackupOps().setExecutionMode("linux-local");
        properties.getBackupOps().setLinuxScriptPath("/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh");
        properties.getBackupOps().setLinuxConfigPath("/opt/intruoyi/ops/backup-ops/linux-native/backup-ops.linux-local.runtime.json");
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties),
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir)));

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("backup-now");
        reqVO.setTargetEnvironment("test");
        reqVO.setReason("测试服立即备份");

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("backup-now", result.getAction());
        assertEquals("test", result.getEnvironment());
        verify(commandExecutor, timeout(1000)).executeOperation(argThat(command ->
                "/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh".equals(command.getScriptPath())
                        && command.getArguments().contains("--mode")
                        && command.getArguments().contains("backup-now")
                        && command.getArguments().contains("--target-environment")
                        && command.getArguments().contains("test")
                        && command.getArguments().contains("--config")
                        && command.getArguments().contains("/opt/intruoyi/ops/backup-ops/linux-native/backup-ops.linux-local.runtime.json")
                        && command.getArguments().contains("--non-interactive")
                        && !command.getArguments().contains("-Mode")), any());
        waitOperationStatus(result.getOperationId(), "succeeded");
    }

    @Test
    void executeRehearsalShouldUseLinuxLocalBackupOpsCandidateWhenConfigured() throws InterruptedException {
        RuntimeControlProperties properties = createLinuxLocalRuntimeControlProperties();
        createRestoreCandidateFixture(properties, "20260525-215449", "20260525_200033");
        RuntimeOpsCandidateService candidateService =
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties), candidateService);

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("rehearsal");
        reqVO.setReason("恢复演练");
        reqVO.setSelectedRecoverySetCandidateId(candidateService
                .listRestoreCandidates().get(0).getCandidateId());

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("rehearsal", result.getAction());
        assertEquals("test", result.getEnvironment());
        assertEquals("20260525-215449", result.getParameters().get("selectedBackupId"));
        verify(commandExecutor, timeout(1000)).executeOperation(argThat(command ->
                "/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh".equals(command.getScriptPath())
                        && command.getArguments().contains("--mode")
                        && command.getArguments().contains("rehearsal")
                        && command.getArguments().contains("--selected-backup-id")
                        && command.getArguments().contains("20260525-215449")
                        && !command.getArguments().contains("--target-environment")), any());
        waitOperationStatus(result.getOperationId(), "succeeded");
    }

    @Test
    void executeRehearsalShouldRequireSelectedRecoverySetCandidateId() {
        RuntimeControlProperties properties = createLinuxLocalRuntimeControlProperties();
        RuntimeOpsCandidateService candidateService =
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties), candidateService);

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("rehearsal");
        reqVO.setReason("恢复演练");

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "selectedRecoverySetCandidateId");
        verify(commandExecutor, never()).executeOperation(any(), any());
        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
    }

    @Test
    void executeRestoreDataShouldUseDetachedLinuxLocalRunnerForTestTargetWhenConfigured() {
        RuntimeControlProperties properties = createLinuxLocalRuntimeControlProperties();
        createRestoreCandidateFixture(properties, "20260525-215449", "20260525_200033");
        RuntimeOpsCandidateService candidateService =
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties), candidateService);

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("restore-data");
        reqVO.setReason("测试服恢复数据");
        reqVO.setTargetEnvironment("test");
        reqVO.setSelectedRecoverySetCandidateId(candidateService
                .listRestoreCandidates().get(0).getCandidateId());

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("restore-data", result.getAction());
        assertEquals("test", result.getEnvironment());
        assertEquals("test", result.getParameters().get("targetEnvironment"));
        assertEquals("running", result.getStatus());
        verify(commandExecutor, timeout(1000)).executeDetachedOperation(argThat(command ->
                "/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh".equals(command.getScriptPath())
                        && command.getArguments().contains("--mode")
                        && command.getArguments().contains("restore-data")
                        && command.getArguments().contains("--target-environment")
                        && command.getArguments().contains("test")
                        && command.getArguments().contains("--selected-backup-id")
                        && command.getArguments().contains("20260525-215449")), any(), eq(result.getOperationId()),
                eq("恢复数据 completed"));
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeRestoreDataShouldUseDetachedLinuxLocalRunnerForBackupTargetWhenConfigured() {
        RuntimeControlProperties properties = createLinuxLocalRuntimeControlProperties();
        createRestoreCandidateFixture(properties, "20260525-215449", "20260525_200033");
        RuntimeOpsCandidateService candidateService =
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties), candidateService);

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("restore-data");
        reqVO.setReason("备份服恢复数据");
        reqVO.setTargetEnvironment("backup");
        reqVO.setProdConfirmText("PROD");
        reqVO.setSelectedRecoverySetCandidateId(candidateService
                .listRestoreCandidates().get(0).getCandidateId());

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("restore-data", result.getAction());
        assertEquals("backup", result.getEnvironment());
        assertEquals("backup", result.getParameters().get("targetEnvironment"));
        assertEquals("running", result.getStatus());
        verify(commandExecutor, timeout(1000)).executeDetachedOperation(argThat(command ->
                "/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh".equals(command.getScriptPath())
                        && command.getArguments().contains("--mode")
                        && command.getArguments().contains("restore-data")
                        && command.getArguments().contains("--target-environment")
                        && command.getArguments().contains("backup")
                        && command.getArguments().contains("--selected-backup-id")
                        && command.getArguments().contains("20260525-215449")), any(), eq(result.getOperationId()),
                eq("恢复数据 completed"));
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeRestoreDataShouldRequireProdConfirmForBackupTarget() {
        RuntimeControlProperties properties = createLinuxLocalRuntimeControlProperties();
        createRestoreCandidateFixture(properties, "20260525-215449", "20260525_200033");
        RuntimeOpsCandidateService candidateService =
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties), candidateService);

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("restore-data");
        reqVO.setReason("备份服恢复数据");
        reqVO.setTargetEnvironment("backup");
        reqVO.setSelectedRecoverySetCandidateId(candidateService
                .listRestoreCandidates().get(0).getCandidateId());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));

        assertEquals(ErrorCodeConstants.RUNTIME_CONTROL_PROD_GUARD_REQUIRED.getCode(), exception.getCode());
        RuntimeControlOperationRespVO blocked = runtimeControlService.getOperations().get(0);
        assertEquals("restore-data", blocked.getAction());
        assertEquals("backup", blocked.getEnvironment());
        assertEquals("backup", blocked.getParameters().get("targetEnvironment"));
        assertEquals("blocked", blocked.getStatus());
        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeRestoreDataShouldPersistBlockedOperationWhenTargetEnvironmentIsProd() {
        RuntimeControlProperties properties = createLinuxLocalRuntimeControlProperties();
        createRestoreCandidateFixture(properties, "20260525-215449", "20260525_200033");
        RuntimeOpsCandidateService candidateService =
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties), candidateService);

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("restore-data");
        reqVO.setReason("禁止正式服恢复数据");
        reqVO.setTargetEnvironment("prod");
        reqVO.setProdConfirmText("PROD");
        reqVO.setSelectedRecoverySetCandidateId(candidateService
                .listRestoreCandidates().get(0).getCandidateId());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> runtimeControlService.executeAction(reqVO, "1001"));

        assertEquals(ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID.getCode(), exception.getCode());
        RuntimeControlOperationRespVO blocked = runtimeControlService.getOperations().get(0);
        assertEquals("restore-data", blocked.getAction());
        assertEquals("prod", blocked.getEnvironment());
        assertEquals("prod", blocked.getParameters().get("targetEnvironment"));
        assertEquals("blocked", blocked.getStatus());
        verify(commandExecutor, never()).executeDetachedOperation(any(), any(), any(), any());
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeRollbackAppShouldUseDetachedLinuxLocalRunnerForTestTargetWhenConfigured() {
        RuntimeControlProperties properties = createLinuxLocalRuntimeControlProperties();
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        createRollbackCandidateFixture(properties, "20260525_200033", "20260525_200033");
        RuntimeOpsCandidateService candidateService =
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties), candidateService);

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("rollback-app");
        reqVO.setReason("测试服回滚版本");
        reqVO.setTargetEnvironment("test");
        reqVO.setProdConfirmText("PROD");
        reqVO.setSelectedImageCandidateId(candidateService
                .listRollbackCandidates().get(0).getCandidateId());

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("rollback-app", result.getAction());
        assertEquals("test", result.getEnvironment());
        assertEquals("test", result.getParameters().get("targetEnvironment"));
        assertEquals("running", result.getStatus());
        verify(commandExecutor, timeout(1000)).executeDetachedOperation(argThat(command ->
                "/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh".equals(command.getScriptPath())
                        && command.getArguments().contains("--mode")
                        && command.getArguments().contains("rollback-app")
                        && command.getArguments().contains("--target-environment")
                        && command.getArguments().contains("test")
                        && command.getArguments().contains("--selected-image-tag")
                        && command.getArguments().contains("20260525_200033")), any(), eq(result.getOperationId()),
                eq("回滚版本 completed"));
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeRollbackAppShouldNotRequireProdConfirmForTestTarget() {
        RuntimeControlProperties properties = createLinuxLocalRuntimeControlProperties();
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        createRollbackCandidateFixture(properties, "20260525_200033", "20260525_200033");
        RuntimeOpsCandidateService candidateService =
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties), candidateService);

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("rollback-app");
        reqVO.setReason("测试服回滚版本");
        reqVO.setTargetEnvironment("test");
        reqVO.setSelectedImageCandidateId(candidateService
                .listRollbackCandidates().get(0).getCandidateId());

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("rollback-app", result.getAction());
        assertEquals("test", result.getEnvironment());
        assertEquals("test", result.getParameters().get("targetEnvironment"));
        assertEquals("running", result.getStatus());
        verify(commandExecutor, timeout(1000)).executeDetachedOperation(argThat(command ->
                "/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh".equals(command.getScriptPath())
                        && command.getArguments().contains("--mode")
                        && command.getArguments().contains("rollback-app")
                        && command.getArguments().contains("--target-environment")
                        && command.getArguments().contains("test")
                        && command.getArguments().contains("--selected-image-tag")
                        && command.getArguments().contains("20260525_200033")), any(), eq(result.getOperationId()),
                eq("回滚版本 completed"));
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeRollbackAppShouldUseDetachedLinuxLocalRunnerForBackupTargetWhenConfigured() {
        RuntimeControlProperties properties = createLinuxLocalRuntimeControlProperties();
        properties.getEnvironments().get("prod").setAccessEnabled(true);
        createRollbackCandidateFixture(properties, "20260525_200033", "20260525_200033");
        RuntimeOpsCandidateService candidateService =
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir));
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties), candidateService);

        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("rollback-app");
        reqVO.setReason("备份服务器回滚版本");
        reqVO.setTargetEnvironment("backup");
        reqVO.setProdConfirmText("PROD");
        reqVO.setSelectedImageCandidateId(candidateService
                .listRollbackCandidates().get(0).getCandidateId());

        RuntimeControlOperationRespVO result = runtimeControlService.executeAction(reqVO, "1001");

        assertEquals("rollback-app", result.getAction());
        assertEquals("backup", result.getEnvironment());
        assertEquals("backup", result.getParameters().get("targetEnvironment"));
        assertEquals("running", result.getStatus());
        verify(commandExecutor, timeout(1000)).executeDetachedOperation(argThat(command ->
                "/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh".equals(command.getScriptPath())
                        && command.getArguments().contains("--mode")
                        && command.getArguments().contains("rollback-app")
                        && command.getArguments().contains("--target-environment")
                        && command.getArguments().contains("backup")
                        && command.getArguments().contains("--selected-image-tag")
                        && command.getArguments().contains("20260525_200033")), any(), eq(result.getOperationId()),
                eq("回滚版本 completed"));
        verify(commandExecutor, never()).executeOperation(any(), any());
    }

    @Test
    void executeRollbackShouldRequireSelectedImageCandidateId() {
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("rollback-app");
        reqVO.setReason("回滚正式应用版本");
        reqVO.setTargetEnvironment("test");
        reqVO.setProdConfirmText("PROD");
        reqVO.setSelectedImageTag("20260525_200033");

        assertServiceException(() -> runtimeControlService.executeAction(reqVO, "1001"),
                ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "selectedImageCandidateId");
    }

    @Test
    void getOperationLogShouldReadOnlyRegisteredOperationLog() throws Exception {
        stubNasReleaseConfig();
        configureBackendRuntimeBase();
        RuntimeControlActionReqVO reqVO = new RuntimeControlActionReqVO();
        reqVO.setAction("build-release");
        reqVO.setReason("构建发布包验证");
        reqVO.setPublishScope("code-only");
        reqVO.setReleaseTag("20260528_220001");
        reqVO.setIncludeOnlyOffice(false);
        reqVO.setIncludeShowroomBuildPackage(false);

        RuntimeControlOperationRespVO operation = runtimeControlService.executeAction(reqVO, "1001");
        waitOperationStatus(operation.getOperationId(), "succeeded");
        java.nio.file.Files.writeString(Path.of(operation.getResultLogPath()), "line-1\nline-2\n");

        RuntimeControlLogRespVO log = runtimeControlService.getOperationLog(operation.getOperationId(), 1024);

        assertEquals(operation.getOperationId(), log.getOperationId());
        assertEquals("line-1\nline-2\n", log.getContent());
        assertNotNull(log.getStatus());
    }

    @Test
    void getOperationLogShouldReconcileRunningStatusFromTerminalSuccessLog() throws Exception {
        RuntimeControlOperationStore operationStore = new RuntimeControlOperationStore(properties);
        runtimeControlService = new RuntimeControlServiceImpl(properties, commandExecutor, operationStore,
                createSeededResponsibilityService(properties),
                new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir)),
                nasSettingsService, nasBrowserService);
        RuntimeControlOperationRespVO operation = new RuntimeControlOperationRespVO();
        operation.setOperationId("op-terminal-success");
        operation.setRequestedBy("1001");
        operation.setRequestedAt(LocalDateTime.now());
        operation.setEnvironment("test");
        operation.setComponent("ops");
        operation.setAction("backup-now");
        operation.setActionLabel("立即备份");
        operation.setParameters(Map.of("targetEnvironment", "test"));
        operation.setReason("测试服立即备份");
        operation.setStatus("running");
        operation.setSummary("立即备份 dispatched");
        Path logPath = operationStore.getOperationLogPath(operation.getOperationId());
        operation.setResultLogPath(logPath.toString());
        operationStore.initializeLog(logPath);
        Files.writeString(logPath, """
                [7/7] 写入结果并发送通知...
                操作完成：成功
                动作类型：立即备份
                结果代码：INTBK-0000
                结果说明：立即备份完成。通知未发送（disabled）。
                """, StandardCharsets.UTF_8);
        operationStore.save(operation);

        RuntimeControlLogRespVO log = runtimeControlService.getOperationLog(operation.getOperationId(), 1024);

        assertEquals("succeeded", log.getStatus());
        RuntimeControlOperationRespVO persisted = operationStore.findById(operation.getOperationId());
        assertEquals("succeeded", persisted.getStatus());
        assertEquals("立即备份 completed", persisted.getSummary());
    }

    private void waitOperationStatus(String operationId, String status) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            boolean matched = runtimeControlService.getOperations().stream()
                    .anyMatch(operation -> operationId.equals(operation.getOperationId())
                            && status.equals(operation.getStatus()));
            if (matched) {
                return;
            }
            Thread.sleep(50);
        }
        String currentStatuses = runtimeControlService.getOperations().stream()
                .filter(operation -> operationId.equals(operation.getOperationId()))
                .map(operation -> operation.getStatus() + ":" + operation.getSummary())
                .findFirst()
                .orElse("<missing>");
        assertTrue(false, "operation did not reach status " + status + ", current=" + currentStatuses);
    }

    private static boolean containsArgumentPair(List<String> args, String name, String value) {
        int index = args.indexOf(name);
        return index >= 0 && index + 1 < args.size() && value.equals(args.get(index + 1));
    }

    private void stubNasReleaseConfig() {
        doReturn(new NasConnectionConfig("172.30.30.4", 1445, "IT共享", "WORKGROUP", "nas-user", "nas-secret"))
                .when(nasSettingsService).getRequiredNasConfig();
    }

    private void stubReleasePackageManifest(String packagePath, String releaseTag, boolean checksumPresent) {
        stubReleasePackageManifest(packagePath, releaseTag, checksumPresent, false);
    }

    private void stubReleasePackageList(String releaseTag, boolean checksumPresent, boolean tested) {
        stubReleasePackageList(releaseTag, checksumPresent, tested, null);
    }

    private void stubReleasePackageList(String releaseTag, boolean checksumPresent, boolean tested,
                                        RuntimeControlRestoreCandidateRespVO recoverySet) {
        String packagePath = "Backup/ReleasePackage/" + releaseTag;
        doReturn(nasList("Backup/ReleasePackage",
                new FileNasListRespVO.Item().setName(releaseTag).setPath(packagePath).setDir(true).setSize(0L)))
                .when(nasBrowserService).listFiles(any(NasConnectionConfig.class), eq("Backup/ReleasePackage"));
        stubReleasePackageManifest(packagePath, releaseTag, checksumPresent, tested, recoverySet);
    }

    private void stubReleasePackageManifest(String packagePath, String releaseTag, boolean checksumPresent,
                                            boolean tested) {
        stubReleasePackageManifest(packagePath, releaseTag, checksumPresent, tested, null);
    }

    private void stubReleasePackageManifest(String packagePath, String releaseTag, boolean checksumPresent,
                                            boolean tested, RuntimeControlRestoreCandidateRespVO recoverySet) {
        String manifestPath = packagePath + "/release-manifest.json";
        String testedPath = packagePath + "/tested.json";
        FileNasListRespVO.Item[] files = tested
                ? new FileNasListRespVO.Item[] {
                    file("release-manifest.json", manifestPath),
                    file("tested.json", testedPath)
                }
                : new FileNasListRespVO.Item[] { file("release-manifest.json", manifestPath) };
        doReturn(nasList(packagePath, files))
                .when(nasBrowserService).listFiles(any(NasConnectionConfig.class), eq(packagePath));
        doReturn(new NasFileReadResult("release-manifest.json", manifestPath, "application/json",
                """
                        {"releaseTag":"%s","packageDirectoryName":"%s","builtAt":"2026-05-29T21:05:42Z","component":"intruoyi","includeShowroomBuildPackage":false,"onlyOfficeIncluded":true,"artifacts":[%s]}
                        """.formatted(releaseTag, releaseTag,
                        checksumPresent ? "{\"path\":\"intruoyi-images.tar\",\"sha256\":\"abc\"}" : "{\"path\":\"intruoyi-images.tar\"}")
                        .getBytes(StandardCharsets.UTF_8)))
                .when(nasBrowserService).readFile(any(NasConnectionConfig.class), eq(manifestPath));
        if (tested) {
            String recoverySetJson = recoverySet == null
                    ? "{}"
                    : """
                            {"selectedRecoverySetCandidateId":"%s","recoverySetId":"%s","recoverySetManifestHash":"%s","programVersion":"%s","redisPolicy":"%s"}
                            """.formatted(recoverySet.getCandidateId(), recoverySet.getRecoverySetId(),
                            recoverySet.getRecoverySetManifestHash(), recoverySet.getProgramVersion(),
                            recoverySet.getRedisPolicy());
            doReturn(new NasFileReadResult("tested.json", testedPath, "application/json",
                    """
                            {"releaseTag":"%s","packageDirectoryName":"%s","testedAt":"2026-05-29T22:00:00Z","operatorName":"tester","recoverySet":%s}
                            """.formatted(releaseTag, releaseTag, recoverySetJson).getBytes(StandardCharsets.UTF_8)))
                    .when(nasBrowserService).readFile(any(NasConnectionConfig.class), eq(testedPath));
        }
    }

    private void stubReleasePackageManifestWithoutComponent(String packagePath, String releaseTag) {
        String manifestPath = packagePath + "/release-manifest.json";
        doReturn(nasList(packagePath, file("release-manifest.json", manifestPath)))
                .when(nasBrowserService).listFiles(any(NasConnectionConfig.class), eq(packagePath));
        doReturn(new NasFileReadResult("release-manifest.json", manifestPath, "application/json",
                """
                        {"releaseTag":"%s","packageDirectoryName":"%s","builtAt":"2026-05-29T21:05:42Z","includeShowroomBuildPackage":false,"onlyOfficeIncluded":true,"artifacts":[{"path":"intruoyi-images.tar","sha256":"abc"}]}
                        """.formatted(releaseTag, releaseTag).getBytes(StandardCharsets.UTF_8)))
                .when(nasBrowserService).readFile(any(NasConnectionConfig.class), eq(manifestPath));
    }

    private FileNasListRespVO nasList(String path, FileNasListRespVO.Item... items) {
        return new FileNasListRespVO().setCurrentPath(path).setItems(List.of(items));
    }

    private FileNasListRespVO.Item file(String name, String path) {
        return new FileNasListRespVO.Item().setName(name).setPath(path).setDir(false).setSize(128L);
    }

    private RuntimeControlProperties createLinuxLocalRuntimeControlProperties() {
        RuntimeControlProperties properties = RuntimeControlProperties.createDefaultForTests(tempDir);
        properties.setRepoRoot(tempDir.toString());
        properties.getBackupOps().setExecutionMode("linux-local");
        properties.getBackupOps().setLinuxScriptPath("/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh");
        properties.getBackupOps().setLinuxConfigPath(writeBackupOpsConfig(tempDir.resolve("nas-backup-points")).toString());
        properties.getBackupOps().setNasBackupPointsRoot("nas-backup-points");
        properties.getReleasePackage().setNasReleaseRoot("nas-release-packages");
        return properties;
    }

    private void configureBackendRuntimeBase() {
        releasePackageConfigService.config = new RuntimeControlReleasePackageConfig(
                "offline-tar",
                "D:/ProjectPackage/Int/BaseImages/intruoyi-backend-runtime-base-20260604.tar",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "intruoyi-backend-runtime-base:20260604",
                "sha256:abcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcdefabcd",
                "20260604");
    }

    private static class TestReleasePackageConfigService implements RuntimeControlReleasePackageConfigService {

        private RuntimeControlReleasePackageConfig config;

        @Override
        public RuntimeControlReleasePackageConfig getRequiredBackendRuntimeBaseConfig() {
            if (config == null) {
                throw exception(ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED,
                        "infra_config.runtime-control.release-package.backend-runtime-base-mode (-BackendRuntimeBaseMode)");
            }
            return config;
        }
    }

    private void createRollbackCandidateFixture(RuntimeControlProperties properties, String releaseTag, String imageTag) {
        try {
            Path root = tempDir.resolve(properties.getReleasePackage().getNasReleaseRoot()).resolve(imageTag);
            java.nio.file.Files.createDirectories(root);
            java.nio.file.Files.writeString(root.resolve("release-manifest.json"),
                    "{\"releaseTag\":\"" + releaseTag + "\",\"packageDirectoryName\":\"" + imageTag + "\"}");
            java.nio.file.Files.writeString(root.resolve("prod-latest.json"),
                    "{\"releaseTag\":\"" + releaseTag + "\",\"packageDirectoryName\":\"" + imageTag
                            + "\",\"action\":\"deploy\",\"environment\":\"prod\"}");
            java.nio.file.Files.writeString(root.resolve("rollback-compatibility.json"),
                    "{\"schemaVersion\":\"v1\",\"packageDirectoryName\":\"" + imageTag
                            + "\",\"status\":\"COMPATIBLE\",\"checkedAt\":\"2026-05-30T00:12:00Z\",\"summary\":\"compatible\"}");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void createBackupPointFixture(String backupId, String imageTag) {
        try {
            Path root = tempDir.resolve("nas-backup-points").resolve(backupId);
            java.nio.file.Files.createDirectories(root.resolve("deploy"));
            java.nio.file.Files.createDirectories(root.resolve("manifest"));
            java.nio.file.Files.createDirectories(root.resolve("mysql"));
            java.nio.file.Files.createDirectories(root.resolve("objects"));
            java.nio.file.Files.writeString(root.resolve("deploy").resolve("image-tag.txt"), imageTag);
            java.nio.file.Files.writeString(root.resolve("deploy").resolve("runtime.env"),
                    "IMAGE_TAG=" + imageTag + "\nBACKEND_HOST_PORT=48081\nFRONTEND_HOST_PORT=8081\n");
            java.nio.file.Files.writeString(root.resolve("deploy").resolve("docker-compose.yml"), "services: {}\n");
            java.nio.file.Files.writeString(root.resolve("mysql").resolve("ruoyi-vue-pro.sql.gz"), "dump");
            java.nio.file.Files.writeString(root.resolve("objects").resolve("manifest-object-inventory.json"),
                    "{\"mode\":\"incremental-manifest\",\"objects\":[]}");
            java.nio.file.Files.writeString(root.resolve("manifest").resolve("manifest.json"),
                    "{\"schemaVersion\":\"v2\",\"backupId\":\"" + backupId
                            + "\",\"targetEnvironment\":\"test\",\"targetHost\":\"172.30.30.58\",\"status\":\"success\",\"deploy\":{\"imageTag\":\""
                            + imageTag + "\"},\"recoverySet\":{\"id\":\"" + backupId
                            + "\",\"status\":\"COMPLETE\",\"program\":{\"imageTag\":\"" + imageTag
                            + "\"},\"mysql\":{\"dumpPath\":\"mysql/ruoyi-vue-pro.sql.gz\"},\"minio\":{\"bucket\":\"yudao\",\"snapshotPath\":\"objects/manifest-object-inventory.json\"},\"businessFiles\":{\"snapshotPath\":\"objects/manifest-object-inventory.json\"},\"redis\":{\"policy\":\"CLEAR_AND_REBUILD\"},\"configuration\":{\"manifestPath\":\"deploy/runtime.env\",\"composePath\":\"deploy/docker-compose.yml\"},\"checksums\":{\"path\":\"manifest/checksums.txt\",\"sha256\":\"abc\"}}}");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void createRestoreCandidateFixture(RuntimeControlProperties properties, String backupId, String imageTag) {
        createBackupPointFixture(backupId, imageTag);
        try {
            Path manifest = tempDir.resolve("nas-backup-points").resolve(backupId).resolve("manifest");
            java.nio.file.Files.writeString(manifest.resolve("checksums.txt"), "sha256  deploy/runtime.env");
            java.nio.file.Files.writeString(manifest.resolve("dcc-backup-manifest.json"), dccBackupManifest(backupId));
            java.nio.file.Files.writeString(manifest.resolve("rehearsal-report.json"), "{\"status\":\"PASSED\"}");
            java.nio.file.Files.writeString(manifest.resolve("现场快照.md"), "snapshot");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String dccBackupManifest(String backupId) {
        return """
                {"schemaVersion":"dcc-backup-manifest-v1","backupId":"%s","targetEnvironment":"test","backupMode":"incremental","chainStatus":"COMPLETE","changeSummary":{"addedRecords":1,"changedRecords":0,"deletedRecords":0,"invalidatedRecords":0,"addedObjects":1,"changedObjects":0,"reusedObjects":2,"tombstoneObjects":0}}
                """.formatted(backupId);
    }

    private RuntimeControlRestoreCandidateRespVO createAvailableRecoverySetCandidate(RuntimeControlProperties properties) {
        createRestoreCandidateFixture(properties, "20260525-215449", "20260525_200033");
        return new RuntimeOpsCandidateServiceImpl(properties, new RuntimeControlNasBrowserServiceStub(tempDir))
                .listRestoreCandidates().get(0);
    }

    private Path writeBackupOpsConfig(Path backupPointsRoot) {
        try {
            java.nio.file.Files.createDirectories(tempDir.resolve("backup-ops-config"));
            Path configPath = tempDir.resolve("backup-ops-config").resolve("backup-ops.config.json");
            java.nio.file.Files.writeString(configPath, """
                    {
                      "servers": {
                        "test": {
                          "backupPointsRoot": "%s"
                        }
                      }
                    }
                    """.formatted(backupPointsRoot.toString().replace("\\", "\\\\")));
            return configPath;
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private RuntimeOpsResponsibilityService createSeededResponsibilityService(RuntimeControlProperties properties) {
        RuntimeOpsResponsibilityServiceImpl responsibilityService =
                new RuntimeOpsResponsibilityServiceImpl(new RuntimeOpsOwnerMatrixStore(properties));
        responsibilityService.createOwner(owner("prod", "promote-prod", "release-owner", 1001L));
        responsibilityService.createOwner(owner("backup", "promote-backup", "release-owner", 1001L));
        responsibilityService.createOwner(owner("prod", "rollback-app", "release-owner", 1001L));
        responsibilityService.createOwner(owner("test", "rollback-app", "release-owner", 1001L));
        responsibilityService.createOwner(owner("backup", "rollback-app", "release-owner", 1001L));
        responsibilityService.createOwner(owner("prod", "restore-data", "data-owner", 1002L));
        responsibilityService.createOwner(owner("test", "restore-data", "data-owner", 1002L));
        responsibilityService.createOwner(owner("backup", "restore-data", "data-owner", 1002L));
        return responsibilityService;
    }

    private RuntimeControlOwnerMatrixSaveReqVO owner(String environment, String action, String role, Long ownerUserId) {
        RuntimeControlOwnerMatrixSaveReqVO reqVO = new RuntimeControlOwnerMatrixSaveReqVO();
        reqVO.setEnvironment(environment);
        reqVO.setAction(action);
        reqVO.setRole(role);
        reqVO.setRequired(true);
        reqVO.setOwnerUserId(ownerUserId);
        reqVO.setOwnerName("owner-" + ownerUserId);
        return reqVO;
    }
}
