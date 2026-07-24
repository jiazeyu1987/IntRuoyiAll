package cn.iocoder.yudao.module.infra.framework.runtimecontrol.config;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "yudao.runtime-control")
@Data
public class RuntimeControlProperties implements InitializingBean {

    private static final String DEFAULT_REMOTE_RELEASE_ROOT = "/var/lib/docker/intruoyi-releases";
    private static final String DEFAULT_REMOTE_DATA_ROOT = "/var/lib/docker/intruoyi-data/runtime-data";
    private static final String DEFAULT_REMOTE_DATA_DISK_MOUNT = "/var/lib/docker";
    private static final String DEFAULT_REMOTE_DATA_DISK_DEVICE = "/dev/vdb";
    private static final String DEFAULT_REMOTE_MINIO_CONTAINER = "ragflow_compose-minio-1";
    public static final String TEST_SERVER_HOST = "172.30.30.58";
    public static final String PROD_SERVER_HOST = "172.30.30.57";
    public static final String BACKUP_SERVER_HOST = "172.30.30.59";

    private String repoRoot = "D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro";
    private String stateDir = "E:/Int/CacheData/IntRuoyi/runtime-control";
    private Duration statusCommandTimeout = Duration.ofSeconds(60);
    private Map<String, Environment> environments = defaultEnvironments();
    private List<String> components = List.of("intruoyi-frontend", "intruoyi-backend", "intruoyi-full", "website-frontend");
    private BackupOps backupOps = new BackupOps();
    private ReleasePackage releasePackage = new ReleasePackage();
    private StorageGuard storageGuard = new StorageGuard();

    public static RuntimeControlProperties createDefaultForTests(Path stateDir) {
        RuntimeControlProperties properties = new RuntimeControlProperties();
        properties.setStateDir(stateDir.toString());
        properties.setRepoRoot("D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro");
        properties.afterPropertiesSet();
        return properties;
    }

    public Target getTarget(String environment, String component) {
        Environment env = environments.get(environment);
        return env == null ? null : env.getTargets().get(component);
    }

    private static Map<String, Environment> defaultEnvironments() {
        Map<String, Environment> result = new LinkedHashMap<>();
        result.put("local", localEnvironment());
        result.put("test", remoteEnvironment("Test", TEST_SERVER_HOST));
        result.put("prod", disabledProductionEnvironment());
        result.put("backup", backupEnvironment());
        return result;
    }

    private static Environment localEnvironment() {
        Environment env = new Environment();
        env.setLabel("Local");
        env.setLocal(true);
        env.setAccessEnabled(true);
        env.setTargets(Map.of(
                "intruoyi-frontend", target("IntRuoyi 前端", 8081, "http://127.0.0.1:8081/", "frontend",
                        "script/deploy/show-int-ruoyi-local-status.ps1",
                        "script/deploy/restart-int-ruoyi-local.ps1"),
                "intruoyi-backend", target("IntRuoyi 后端", 48081, "http://127.0.0.1:48081/actuator/health", "backend",
                        "script/deploy/show-int-ruoyi-local-status.ps1",
                        "script/deploy/restart-int-ruoyi-local.ps1"),
                "intruoyi-full", target("IntRuoyi 整套", null, "http://127.0.0.1:8081/", "full",
                        "script/deploy/show-int-ruoyi-local-status.ps1",
                        "script/deploy/restart-int-ruoyi-local.ps1"),
                "website-frontend", target("Website 前端", 4173, "http://127.0.0.1:4173/", "website",
                        "script/deploy/show-int-ruoyi-local-status.ps1",
                        "script/deploy/restart-int-ruoyi-local.ps1")
        ));
        return env;
    }

    private static Environment remoteEnvironment(String label, String host) {
        Environment env = new Environment();
        env.setLabel(label);
        env.setAccessEnabled(true);
        env.setRemoteAppDir("/opt/intruoyi/runtime");
        setRemoteEnvironmentHost(env, host);
        return env;
    }

    private static Environment backupEnvironment() {
        Environment env = remoteEnvironment("Backup", BACKUP_SERVER_HOST);
        env.setRemoteReleaseRoot("/mnt/intruoyi-data/intruoyi-releases");
        env.setRemoteDataRoot("/mnt/intruoyi-data/runtime-data");
        env.setRemoteDataDiskMount("/mnt/intruoyi-data");
        env.setRemoteDataDiskDevice("/dev/mapper/cl-home");
        env.setRemoteMinioContainer("intruoyi-minio");
        return env;
    }

    private static Environment disabledProductionEnvironment() {
        Environment env = remoteEnvironment("Production", PROD_SERVER_HOST);
        env.setAccessEnabled(false);
        env.setAccessDisabledReason("正式环境写动作未授权，当前任务禁止写入、重启、发布、回滚或恢复正式服务器");
        return env;
    }

    private static void setRemoteEnvironmentHost(Environment env, String host) {
        env.setHost(host);
        env.setTargets(Map.of(
                "intruoyi-frontend", remoteTarget("IntRuoyi 前端", host, 8081, "frontend"),
                "intruoyi-backend", remoteTarget("IntRuoyi 后端", host, 48081, "backend"),
                "intruoyi-full", remoteTarget("IntRuoyi 整套", host, null, "full"),
                "website-frontend", remoteTarget("Website 前端", host, 8083, "website")
        ));
    }

    private static Target remoteTarget(String label, String host, Integer port, String actionComponent) {
        String url = remoteTargetUrl(host, actionComponent);
        return target(label, port, url, actionComponent,
                "script/deploy/show-int-ruoyi-remote-status.ps1",
                "script/deploy/restart-int-ruoyi-remote.ps1");
    }

    private static String remoteTargetUrl(String host, String actionComponent) {
        if (host == null || host.isBlank()) {
            return "";
        }
        return "website".equals(actionComponent)
                ? "http://" + host + ":8083/"
                : ("backend".equals(actionComponent)
                ? "http://" + host + ":48081/actuator/health"
                : "http://" + host + ":8081/");
    }

    private static Target target(String label, Integer port, String url, String actionComponent,
                                 String statusScript, String restartScript) {
        Target target = new Target();
        target.setLabel(label);
        target.setPort(port);
        target.setUrl(url);
        target.setActionComponent(actionComponent);
        target.setStatusScript(statusScript);
        target.setRestartScript(restartScript);
        target.setActionEnabled(true);
        return target;
    }

    public void afterPropertiesSet() {
        if (statusCommandTimeout == null || statusCommandTimeout.isZero() || statusCommandTimeout.isNegative()) {
            throw new IllegalArgumentException("yudao.runtime-control.status-command-timeout must be greater than 0");
        }
        Map<String, Environment> normalized = defaultEnvironments();
        environments.forEach((key, configured) -> normalized.put(key, mergeEnvironmentDefaults(key, configured, normalized.get(key))));
        environments = normalized;
    }

    private static Environment mergeEnvironmentDefaults(String environmentKey, Environment configured, Environment defaults) {
        if (configured == null) {
            return defaults;
        }
        if (defaults == null) {
            return configured;
        }
        if ("local".equals(environmentKey)) {
            if (configured.getTargets() == null || configured.getTargets().isEmpty()) {
                configured.setTargets(defaults.getTargets());
            }
            if (configured.getLabel() == null || configured.getLabel().isBlank()) {
                configured.setLabel(defaults.getLabel());
            }
            configured.setLocal(true);
            return configured;
        }

        if (configured.getLabel() == null || configured.getLabel().isBlank()) {
            configured.setLabel(defaults.getLabel());
        }
        if (configured.getHost() == null || configured.getHost().isBlank()) {
            configured.setHost(defaults.getHost());
        }
        if (configured.getServerUser() == null || configured.getServerUser().isBlank()) {
            configured.setServerUser(defaults.getServerUser());
        }
        if (configured.getRemoteAppDir() == null || configured.getRemoteAppDir().isBlank()) {
            configured.setRemoteAppDir(defaults.getRemoteAppDir());
        }
        if (isUnsetOrGenericDefault(configured.getRemoteReleaseRoot(), defaults.getRemoteReleaseRoot(),
                DEFAULT_REMOTE_RELEASE_ROOT)) {
            configured.setRemoteReleaseRoot(defaults.getRemoteReleaseRoot());
        }
        if (isUnsetOrGenericDefault(configured.getRemoteDataRoot(), defaults.getRemoteDataRoot(),
                DEFAULT_REMOTE_DATA_ROOT)) {
            configured.setRemoteDataRoot(defaults.getRemoteDataRoot());
        }
        if (isUnsetOrGenericDefault(configured.getRemoteDataDiskMount(), defaults.getRemoteDataDiskMount(),
                DEFAULT_REMOTE_DATA_DISK_MOUNT)) {
            configured.setRemoteDataDiskMount(defaults.getRemoteDataDiskMount());
        }
        if (isUnsetOrGenericDefault(configured.getRemoteDataDiskDevice(), defaults.getRemoteDataDiskDevice(),
                DEFAULT_REMOTE_DATA_DISK_DEVICE)) {
            configured.setRemoteDataDiskDevice(defaults.getRemoteDataDiskDevice());
        }
        if (isUnsetOrGenericDefault(configured.getRemoteMinioContainer(), defaults.getRemoteMinioContainer(),
                DEFAULT_REMOTE_MINIO_CONTAINER)) {
            configured.setRemoteMinioContainer(defaults.getRemoteMinioContainer());
        }
        if (configured.getAccessDisabledReason() == null || configured.getAccessDisabledReason().isBlank()) {
            configured.setAccessDisabledReason(defaults.getAccessDisabledReason());
        }
        if (configured.getAccessEnabled() == null) {
            configured.setAccessEnabled(defaults.getAccessEnabled());
        }
        setRemoteEnvironmentHost(configured, configured.getHost());
        return configured;
    }

    private static boolean isUnsetOrGenericDefault(String configuredValue, String environmentDefaultValue,
                                                   String genericDefaultValue) {
        if (configuredValue == null || configuredValue.isBlank()) {
            return true;
        }
        return genericDefaultValue.equals(configuredValue)
                && environmentDefaultValue != null
                && !genericDefaultValue.equals(environmentDefaultValue);
    }

    @Data
    public static class Environment {
        private String label;
        private boolean local;
        private String host;
        private String serverUser = "root";
        private String remoteAppDir;
        private String remoteReleaseRoot = DEFAULT_REMOTE_RELEASE_ROOT;
        private String remoteDataRoot = DEFAULT_REMOTE_DATA_ROOT;
        private String remoteDataDiskMount = DEFAULT_REMOTE_DATA_DISK_MOUNT;
        private String remoteDataDiskDevice = DEFAULT_REMOTE_DATA_DISK_DEVICE;
        private String remoteMinioContainer = DEFAULT_REMOTE_MINIO_CONTAINER;
        private Boolean accessEnabled;
        private String accessDisabledReason;
        private Map<String, Target> targets = new LinkedHashMap<>();

        public boolean isAccessEnabled() {
            return Boolean.TRUE.equals(accessEnabled);
        }

        public void setHost(String host) {
            this.host = host;
            if (!local && targets != null && !targets.isEmpty()) {
                targets.values().forEach(target -> target.setUrl(remoteTargetUrl(host, target.getActionComponent())));
            }
        }
    }

    @Data
    public static class Target {
        private String label;
        private Integer port;
        private String url;
        private String actionComponent;
        private String statusScript;
        private String restartScript;
        private boolean actionEnabled;
        private String blockedReason;

        public List<String> buildStatusArguments(Environment environment) {
            List<String> args = commonRemoteArguments(environment);
            args.add("-Component");
            args.add(actionComponent);
            args.add("-Json");
            return args;
        }

        public List<String> buildRestartArguments(Environment environment, String operationRecordPath) {
            List<String> args = commonRemoteArguments(environment);
            args.add("-Component");
            args.add(actionComponent);
            args.add("-OperationRecordPath");
            args.add(operationRecordPath);
            return args;
        }

        private List<String> commonRemoteArguments(Environment environment) {
            List<String> args = new ArrayList<>();
            if (!environment.isLocal()) {
                args.add("-ServerHost");
                args.add(environment.getHost());
                args.add("-ServerUser");
                args.add(environment.getServerUser());
                args.add("-RemoteAppDir");
                args.add(environment.getRemoteAppDir());
                args.add("-RemoteDataRoot");
                args.add(environment.getRemoteDataRoot());
                args.add("-RemoteDataDiskMount");
                args.add(environment.getRemoteDataDiskMount());
                args.add("-RemoteDataDiskDevice");
                args.add(environment.getRemoteDataDiskDevice());
                if (environment.getRemoteMinioContainer() != null && !environment.getRemoteMinioContainer().isBlank()) {
                    args.add("-RemoteMinioContainer");
                    args.add(environment.getRemoteMinioContainer());
                }
            }
            return args;
        }
    }

    @Data
    public static class BackupOps {
        private String executionMode = "powershell";
        private String configPath = "script/backup-ops/config/backup-ops.config.json";
        private String linuxScriptPath = "/opt/intruoyi/ops/backup-ops/linux-native/linux/backup-ops-linux.sh";
        private String linuxConfigPath = "/opt/intruoyi/ops/backup-ops/linux-native/backup-ops.linux-local.runtime.json";
        private String nasServer = "172.30.30.4";
        private String nasShare = "IT共享";
        private String nasBackupPointsRoot = "Backup/BackupPackage";
        private String linuxRuntimeEnvPath = "/opt/intruoyi/runtime/.env";
        private String linuxRunnerImageRepository = "intruoyi-backend";
        private List<String> linuxRunnerMounts = List.of(
                "/var/run/docker.sock:/var/run/docker.sock",
                "/opt/intruoyi/runtime:/opt/intruoyi/runtime",
                "/opt/intruoyi/ops:/opt/intruoyi/ops",
                "/backup:/backup",
                "/mnt/nas/Backup/BackupPackage:/mnt/nas/Backup/BackupPackage"
        );

        public boolean isLinuxLocal() {
            return "linux-local".equals(executionMode);
        }
    }

    @Data
    public static class ReleasePackage {
        private String nasServer = "172.30.30.4";
        private String nasShare = "IT共享";
        private String nasReleaseRoot = "Backup/ReleasePackage";
        private String backendRuntimeBaseMode = "";
        private String backendRuntimeBaseTarPath = "";
        private String backendRuntimeBaseTarSha256 = "";
        private String backendRuntimeBaseImage = "";
        private String backendRuntimeBaseDigest = "";
        private String backendRuntimeBaseVersion = "";
    }

    @Data
    public static class StorageGuard {

        private String monitorPath = ".";
        private String logDir = "E:/Int/CacheData/IntRuoyi/runtime";
        private Double diskUsageWarnPercent = 85.0;
        private Double diskUsageNoGoPercent = 95.0;
        private Long logDirWarnBytes = 1024L * 1024L * 1024L;
        private Long logGrowthWarnBytes = 128L * 1024L * 1024L;
    }
}
