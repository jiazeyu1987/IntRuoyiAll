package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Getter
public enum RuntimeControlOperationAction {

    BUILD_RELEASE("build-release", "构建发布包", "release", false,
            "script/deploy/publish-int-ruoyi.ps1",
            RuntimeControlOperationAction::buildReleaseArguments),

    PUBLISH_TEST("publish-test", "部署发布包到测试服", "test", false,
            "script/deploy/publish-int-ruoyi.ps1",
            reqVO -> deployReleaseArguments("test", reqVO, false)),

    APPLY_TEST_DB_SQL("apply-test-db-sql", "测试服数据库快应用", "test", false,
            "script/deploy/apply-test-db-sql.ps1",
            RuntimeControlOperationAction::applyTestDbSqlArguments),

    MARK_RELEASE_TESTED("mark-release-tested", "标记测试通过", "test", false,
            "script/deploy/publish-int-ruoyi.ps1",
            RuntimeControlOperationAction::markReleaseTestedArguments),

    PROMOTE_PROD("promote-prod", "上线已验证发布包", "prod", true,
            "script/deploy/publish-int-ruoyi.ps1",
            reqVO -> deployReleaseArguments("prod", reqVO, true)),

    PROMOTE_BACKUP("promote-backup", "上线备份服务器", "backup", true,
            "script/deploy/publish-int-ruoyi.ps1",
            reqVO -> deployReleaseArguments("backup", reqVO, true)),

    BACKUP_NOW("backup-now", "立即备份", "prod", true,
            "script/backup-ops/scripts/backup-ops.ps1",
            reqVO -> backupArguments("backup-now", reqVO)),

    ROLLBACK_APP("rollback-app", "回滚版本", "prod", true,
            "script/backup-ops/scripts/backup-ops.ps1",
            reqVO -> backupArguments("rollback-app", reqVO)),

    REHEARSAL("rehearsal", "恢复演练", "test", false,
            "script/backup-ops/scripts/backup-ops.ps1",
            reqVO -> backupArguments("rehearsal", reqVO)),

    RESTORE_DATA("restore-data", "恢复数据", "prod", true,
            "script/backup-ops/scripts/backup-ops.ps1",
            reqVO -> backupArguments("restore-data", reqVO));

    private final String action;
    private final String label;
    private final String environment;
    private final boolean prodConfirmRequired;
    private final String scriptPath;
    private final Function<RuntimeControlActionReqVO, List<String>> argumentsBuilder;
    public static final String PUBLISH_SCOPE_CODE_ONLY = "code-only";
    public static final String PUBLISH_SCOPE_WITH_DATA = "with-data";

    RuntimeControlOperationAction(String action, String label, String environment, boolean prodConfirmRequired,
                                  String scriptPath,
                                  Function<RuntimeControlActionReqVO, List<String>> argumentsBuilder) {
        this.action = action;
        this.label = label;
        this.environment = environment;
        this.prodConfirmRequired = prodConfirmRequired;
        this.scriptPath = scriptPath;
        this.argumentsBuilder = argumentsBuilder;
    }

    public static RuntimeControlOperationAction fromAction(String action) {
        for (RuntimeControlOperationAction value : values()) {
            if (value.action.equals(action)) {
                return value;
            }
        }
        return null;
    }

    public String resolveScriptPath(RuntimeControlProperties properties) {
        if (isBackupAction() && properties.getBackupOps().isLinuxLocal()) {
            return properties.getBackupOps().getLinuxScriptPath();
        }
        return scriptPath;
    }

    public List<String> buildArguments(RuntimeControlActionReqVO reqVO, String operatorName,
                                       RuntimeControlProperties properties) {
        boolean linuxBackup = isBackupAction() && properties.getBackupOps().isLinuxLocal();
        List<String> args = linuxBackup ? linuxBackupArguments(action, reqVO, properties)
                : new ArrayList<>(argumentsBuilder.apply(reqVO));
        if (deploysReleasePackage()) {
            appendRemoteDeployTargetArguments(args, resolveEnvironment(reqVO), properties);
        }
        if (this == APPLY_TEST_DB_SQL) {
            appendApplyTestDbSqlTargetArguments(args, properties);
        }
        if (this == BUILD_RELEASE || deploysReleasePackage()) {
            appendReleaseTargetHostArguments(args, properties, "prod".equals(resolveEnvironment(reqVO)));
        }
        if (Boolean.TRUE.equals(reqVO.getEnableSmartReleaseReport())) {
            if (!supportsSmartReleaseReport()) {
                throw new IllegalArgumentException("enableSmartReleaseReport is not supported for action: " + action);
            }
            args.add("-EnableSmartReleaseReport");
        }
        if (isBackupAction() && !linuxBackup) {
            args.add("-OperatorName");
            args.add(StrUtil.trim(operatorName));
        }
        return args;
    }

    public String resolveEnvironment(RuntimeControlActionReqVO reqVO) {
        if (this != BACKUP_NOW && this != ROLLBACK_APP && this != RESTORE_DATA) {
            return environment;
        }
        String targetEnvironment = StrUtil.trimToEmpty(reqVO.getTargetEnvironment());
        return StrUtil.isBlank(targetEnvironment) ? environment : targetEnvironment;
    }

    public boolean isProdConfirmRequired(RuntimeControlActionReqVO reqVO) {
        if (this == BACKUP_NOW) {
            return "prod".equals(resolveEnvironment(reqVO));
        }
        if (this == ROLLBACK_APP || this == RESTORE_DATA) {
            return "backup".equals(resolveEnvironment(reqVO));
        }
        return prodConfirmRequired;
    }

    public Map<String, String> safeParameters(RuntimeControlActionReqVO reqVO) {
        Map<String, String> parameters = switch (this) {
            case BUILD_RELEASE -> Map.of(
                    "publishScope", StrUtil.blankToDefault(reqVO.getPublishScope(), ""),
                    "includeOnlyOffice", String.valueOf(Boolean.TRUE.equals(reqVO.getIncludeOnlyOffice())),
                    "includeShowroomBuildPackage",
                    String.valueOf(Boolean.TRUE.equals(reqVO.getIncludeShowroomBuildPackage())),
                    "releaseTag", StrUtil.blankToDefault(reqVO.getReleaseTag(), ""));
            case PUBLISH_TEST, PROMOTE_PROD, PROMOTE_BACKUP ->
                    Map.of("releaseTag", StrUtil.blankToDefault(reqVO.getReleaseTag(), ""));
            case APPLY_TEST_DB_SQL -> Map.of("sqlPath", StrUtil.blankToDefault(reqVO.getSqlPath(), ""));
            case MARK_RELEASE_TESTED -> Map.of(
                    "releaseTag", StrUtil.blankToDefault(reqVO.getReleaseTag(), ""),
                    "testConclusion", StrUtil.blankToDefault(reqVO.getTestConclusion(), ""),
                    "selectedRecoverySetCandidateId",
                    StrUtil.blankToDefault(reqVO.getSelectedRecoverySetCandidateId(), ""),
                    "recoverySetId", StrUtil.blankToDefault(reqVO.getRecoverySetId(), ""));
            case BACKUP_NOW -> Map.of("targetEnvironment",
                    StrUtil.blankToDefault(reqVO.getTargetEnvironment(), ""));
            case ROLLBACK_APP -> Map.of("selectedImageCandidateId", StrUtil.blankToDefault(reqVO.getSelectedImageCandidateId(), ""),
                    "selectedImageTag", StrUtil.blankToDefault(reqVO.getSelectedImageTag(), ""),
                    "targetEnvironment", StrUtil.blankToDefault(reqVO.getTargetEnvironment(), ""));
            case REHEARSAL -> Map.of("selectedRecoverySetCandidateId",
                    StrUtil.blankToDefault(reqVO.getSelectedRecoverySetCandidateId(), ""),
                    "selectedBackupId", StrUtil.blankToDefault(reqVO.getSelectedBackupId(), ""));
            case RESTORE_DATA -> Map.of("selectedRecoverySetCandidateId",
                    StrUtil.blankToDefault(reqVO.getSelectedRecoverySetCandidateId(), ""),
                    "selectedBackupId", StrUtil.blankToDefault(reqVO.getSelectedBackupId(), ""),
                    "targetEnvironment", StrUtil.blankToDefault(reqVO.getTargetEnvironment(), ""));
            default -> Map.of();
        };
        return withSmartReleaseReportParameter(reqVO, parameters);
    }

    public boolean requiresPublishScope() {
        return this == BUILD_RELEASE;
    }

    public boolean supportsSmartReleaseReport() {
        return this == BUILD_RELEASE || deploysReleasePackage();
    }

    public boolean supportsPublishScope(String publishScope) {
        return PUBLISH_SCOPE_CODE_ONLY.equals(publishScope) || PUBLISH_SCOPE_WITH_DATA.equals(publishScope);
    }

    private boolean isBackupAction() {
        return this == BACKUP_NOW || this == ROLLBACK_APP || this == REHEARSAL || this == RESTORE_DATA;
    }

    public boolean requiresNasReleaseRepository() {
        return this == BUILD_RELEASE || this == PUBLISH_TEST || this == MARK_RELEASE_TESTED
                || this == PROMOTE_PROD || this == PROMOTE_BACKUP;
    }

    private boolean deploysReleasePackage() {
        return this == PUBLISH_TEST || this == PROMOTE_PROD || this == PROMOTE_BACKUP;
    }

    public boolean requiresReleaseTag() {
        return this == PUBLISH_TEST || this == MARK_RELEASE_TESTED || this == PROMOTE_PROD || this == PROMOTE_BACKUP;
    }

    public boolean requiresSqlPath() {
        return this == APPLY_TEST_DB_SQL;
    }

    public boolean requiresSelectedImageTag() {
        return this == ROLLBACK_APP;
    }

    public boolean requiresSelectedImageCandidateId() {
        return this == ROLLBACK_APP;
    }

    public boolean requiresSelectedBackupId() {
        return this == REHEARSAL || this == RESTORE_DATA;
    }

    public boolean requiresSelectedRecoverySetCandidateId() {
        return this == REHEARSAL || this == RESTORE_DATA;
    }

    public boolean requiresResponsibilityGate() {
        return this == PROMOTE_PROD || this == PROMOTE_BACKUP || this == ROLLBACK_APP || this == REHEARSAL
                || this == RESTORE_DATA;
    }

    public boolean requiresDetachedLinuxLocalRunner(RuntimeControlProperties properties) {
        return properties.getBackupOps().isLinuxLocal() && (this == RESTORE_DATA || this == ROLLBACK_APP);
    }

    private static List<String> buildReleaseArguments(RuntimeControlActionReqVO reqVO) {
        List<String> args = new ArrayList<>();
        args.add("-Mode");
        args.add("build-release");
        if (StrUtil.isNotBlank(reqVO.getReleaseTag())) {
            args.add("-ReleaseTag");
            args.add(StrUtil.trim(reqVO.getReleaseTag()));
        }
        args.add("-Component");
        args.add(Boolean.TRUE.equals(reqVO.getIncludeShowroomBuildPackage()) ? "full" : "intruoyi");
        if (Boolean.TRUE.equals(reqVO.getIncludeOnlyOffice())) {
            args.add("-IncludeOnlyOffice");
        }
        if (PUBLISH_SCOPE_CODE_ONLY.equals(reqVO.getPublishScope())) {
            args.add("-SkipDatabaseSync");
            args.add("-SkipMinioSync");
            return args;
        }
        if (PUBLISH_SCOPE_WITH_DATA.equals(reqVO.getPublishScope())) {
            return args;
        }
        throw new IllegalArgumentException("Invalid publishScope: " + reqVO.getPublishScope());
    }

    private static List<String> applyTestDbSqlArguments(RuntimeControlActionReqVO reqVO) {
        List<String> args = new ArrayList<>();
        args.add("-SqlPath");
        args.add(StrUtil.trim(reqVO.getSqlPath()));
        args.add("-Reason");
        args.add(StrUtil.trim(reqVO.getReason()));
        return args;
    }

    private static List<String> deployReleaseArguments(String environment, RuntimeControlActionReqVO reqVO,
                                                       boolean requireTested) {
        List<String> args = new ArrayList<>();
        args.add("-Mode");
        args.add("deploy-release");
        args.add("-Environment");
        args.add(environment);
        args.add("-ReleaseTag");
        args.add(StrUtil.trim(reqVO.getReleaseTag()));
        if ("prod".equals(environment) || "backup".equals(environment)) {
            args.add("-ConfirmText");
            args.add("PROD");
        }
        if (requireTested) {
            args.add("-RequireTested");
        }
        return args;
    }

    private static void appendRemoteDeployTargetArguments(List<String> args, String targetEnvironment,
                                                           RuntimeControlProperties properties) {
        RuntimeControlProperties.Environment environment = properties.getEnvironments().get(targetEnvironment);
        if (environment == null || environment.isLocal()) {
            throw new IllegalArgumentException("Missing remote deploy environment: " + targetEnvironment);
        }
        addRequiredArgument(args, "-ServerHost", environment.getHost());
        addRequiredArgument(args, "-ServerUser", environment.getServerUser());
        addRequiredArgument(args, "-RemoteAppDir", environment.getRemoteAppDir());
        addRequiredArgument(args, "-RemoteReleaseRoot", environment.getRemoteReleaseRoot());
        addRequiredArgument(args, "-RemoteDataRoot", environment.getRemoteDataRoot());
        addRequiredArgument(args, "-RemoteDataDiskMount", environment.getRemoteDataDiskMount());
        addRequiredArgument(args, "-RemoteDataDiskDevice", environment.getRemoteDataDiskDevice());
        addOptionalArgument(args, "-RemoteMinioContainer", environment.getRemoteMinioContainer());
    }

    private static void appendApplyTestDbSqlTargetArguments(List<String> args, RuntimeControlProperties properties) {
        RuntimeControlProperties.Environment environment = properties.getEnvironments().get("test");
        if (environment == null || environment.isLocal()) {
            throw new IllegalArgumentException("Missing remote deploy environment: test");
        }
        addRequiredArgument(args, "-ServerHost", environment.getHost());
        addRequiredArgument(args, "-ServerUser", environment.getServerUser());
        addRequiredArgument(args, "-RemoteAppDir", environment.getRemoteAppDir());
        args.add("-ExpectedServerHost");
        args.add(RuntimeControlProperties.TEST_SERVER_HOST);
    }

    private static void appendReleaseTargetHostArguments(List<String> args, RuntimeControlProperties properties,
                                                         boolean includeProd) {
        addRequiredArgument(args, "-TestServerHost", releaseTargetHost(properties, "test"));
        addRequiredArgument(args, "-BackupServerHost", releaseTargetHost(properties, "backup"));
        if (includeProd) {
            addRequiredArgument(args, "-ProdServerHost", releaseTargetHost(properties, "prod"));
        }
    }

    private static String releaseTargetHost(RuntimeControlProperties properties, String environmentKey) {
        RuntimeControlProperties.Environment environment = properties.getEnvironments().get(environmentKey);
        return environment == null ? null : environment.getHost();
    }

    private static void addRequiredArgument(List<String> args, String name, String value) {
        if (StrUtil.isBlank(value)) {
            throw new IllegalArgumentException("Missing " + name);
        }
        args.add(name);
        args.add(StrUtil.trim(value));
    }

    private static void addOptionalArgument(List<String> args, String name, String value) {
        if (StrUtil.isBlank(value)) {
            return;
        }
        args.add(name);
        args.add(StrUtil.trim(value));
    }

    private static Map<String, String> withSmartReleaseReportParameter(RuntimeControlActionReqVO reqVO,
                                                                       Map<String, String> parameters) {
        Map<String, String> result = new LinkedHashMap<>(parameters);
        if (Boolean.TRUE.equals(reqVO.getEnableSmartReleaseReport())) {
            result.put("enableSmartReleaseReport", "true");
        }
        return result;
    }

    private static List<String> markReleaseTestedArguments(RuntimeControlActionReqVO reqVO) {
        List<String> args = new ArrayList<>();
        args.add("-Mode");
        args.add("mark-tested");
        args.add("-ReleaseTag");
        args.add(StrUtil.trim(reqVO.getReleaseTag()));
        args.add("-TestConclusion");
        args.add(StrUtil.trim(reqVO.getTestConclusion()));
        args.add("-SelectedRecoverySetCandidateId");
        args.add(StrUtil.trim(reqVO.getSelectedRecoverySetCandidateId()));
        args.add("-RecoverySetId");
        args.add(StrUtil.trim(reqVO.getRecoverySetId()));
        args.add("-RecoverySetManifestHash");
        args.add(StrUtil.trim(reqVO.getRecoverySetManifestHash()));
        args.add("-RecoverySetProgramVersion");
        args.add(StrUtil.trim(reqVO.getRecoverySetProgramVersion()));
        args.add("-RecoverySetRedisPolicy");
        args.add(StrUtil.trim(reqVO.getRecoverySetRedisPolicy()));
        return args;
    }

    private static List<String> backupArguments(String mode, RuntimeControlActionReqVO reqVO) {
        List<String> args = new ArrayList<>();
        args.add("-Mode");
        args.add(mode);
        args.add("-NonInteractive");
        if ("backup-now".equals(mode)) {
            args.add("-TargetEnvironment");
            args.add(StrUtil.trim(reqVO.getTargetEnvironment()));
        }
        if ("rollback-app".equals(mode)) {
            args.add("-TargetEnvironment");
            args.add(StrUtil.trim(reqVO.getTargetEnvironment()));
            args.add("-SelectedImageTag");
            args.add(StrUtil.blankToDefault(reqVO.getSelectedImageTag(), ""));
        }
        if ("rehearsal".equals(mode)) {
            args.add("-SelectedBackupId");
            args.add(StrUtil.blankToDefault(reqVO.getSelectedBackupId(), ""));
        }
        if ("restore-data".equals(mode)) {
            args.add("-TargetEnvironment");
            args.add(StrUtil.trim(reqVO.getTargetEnvironment()));
            args.add("-SelectedBackupId");
            args.add(StrUtil.blankToDefault(reqVO.getSelectedBackupId(), ""));
        }
        return args;
    }

    private static List<String> linuxBackupArguments(String mode, RuntimeControlActionReqVO reqVO,
                                                     RuntimeControlProperties properties) {
        List<String> args = new ArrayList<>();
        args.add("--mode");
        args.add(mode);
        args.add("--config");
        args.add(properties.getBackupOps().getLinuxConfigPath());
        args.add("--non-interactive");
        if ("backup-now".equals(mode)) {
            args.add("--target-environment");
            args.add(StrUtil.trim(reqVO.getTargetEnvironment()));
        }
        if ("rollback-app".equals(mode)) {
            args.add("--target-environment");
            args.add(StrUtil.trim(reqVO.getTargetEnvironment()));
            args.add("--selected-image-tag");
            args.add(StrUtil.blankToDefault(reqVO.getSelectedImageTag(), ""));
        }
        if ("rehearsal".equals(mode)) {
            args.add("--selected-backup-id");
            args.add(StrUtil.blankToDefault(reqVO.getSelectedBackupId(), ""));
        }
        if ("restore-data".equals(mode)) {
            args.add("--target-environment");
            args.add(StrUtil.trim(reqVO.getTargetEnvironment()));
            args.add("--selected-backup-id");
            args.add(StrUtil.blankToDefault(reqVO.getSelectedBackupId(), ""));
        }
        return args;
    }
}
