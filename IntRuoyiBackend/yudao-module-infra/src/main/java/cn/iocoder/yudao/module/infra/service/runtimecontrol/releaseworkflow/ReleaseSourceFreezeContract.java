package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ReleaseSourceFreezeContract {

    private static final Set<String> REQUIRED_ROOTS = Set.of("maintenance", "application");
    private static final Map<String, String> REQUIRED_ROLE_ROOTS = Map.of(
            "maintenance", "maintenance", "backend", "application", "frontend", "application");
    private static final Map<String, String> REQUIRED_ROLE_PATHS = Map.of(
            "maintenance", ".", "backend", "IntRuoyiBackend", "frontend", "IntRuoyiFronted");

    private ReleaseSourceFreezeContract() {
    }

    public static SourceFreezeEvidence verify(VerificationPoint verificationPoint,
                                              List<GitRootEvidence> gitRoots,
                                              List<SourceRoleEvidence> sourceRoles) {
        if (verificationPoint == null) {
            throw new IllegalStateException("SOURCE_VERIFICATION_POINT_MISSING");
        }
        Map<String, GitRootEvidence> roots = verifyRoots(gitRoots);
        List<SourceRoleEvidence> roles = verifyRoles(sourceRoles, roots);
        return new SourceFreezeEvidence(verificationPoint, List.copyOf(roots.values()), roles);
    }

    private static Map<String, GitRootEvidence> verifyRoots(List<GitRootEvidence> values) {
        if (values == null || values.size() != 2) {
            throw new IllegalStateException("SOURCE_ROOT_SET_INVALID");
        }
        Map<String, GitRootEvidence> roots = new LinkedHashMap<>();
        for (String requiredRole : List.of("maintenance", "application")) {
            GitRootEvidence value = values.stream()
                    .filter(item -> item != null && requiredRole.equals(item.rootRole()))
                    .findFirst().orElseThrow(() -> new IllegalStateException("SOURCE_ROOT_SET_INVALID"));
            String approvedCommit = normalizeCommit(value.approvedCommit());
            if (!approvedCommit.equals(normalizeCommit(value.actualHead()))) {
                throw new IllegalStateException("SOURCE_HEAD_DRIFT: " + requiredRole);
            }
            if (value.dirty()) {
                throw new IllegalStateException("SOURCE_DIRTY: " + requiredRole);
            }
            roots.put(requiredRole, new GitRootEvidence(requiredRole, normalizeRoot(value.normalizedRoot()),
                    approvedCommit, approvedCommit, false));
        }
        if (!roots.keySet().equals(REQUIRED_ROOTS)) {
            throw new IllegalStateException("SOURCE_ROOT_SET_INVALID");
        }
        if (roots.get("maintenance").normalizedRoot().equals(roots.get("application").normalizedRoot())) {
            throw new IllegalStateException("SOURCE_ROOT_PATH_DUPLICATE");
        }
        return roots;
    }

    private static List<SourceRoleEvidence> verifyRoles(List<SourceRoleEvidence> values,
                                                        Map<String, GitRootEvidence> roots) {
        if (values == null || values.size() != 3) {
            throw new IllegalStateException("SOURCE_ROLE_SET_INVALID");
        }
        Map<String, SourceRoleEvidence> roles = new LinkedHashMap<>();
        for (String sourceRole : List.of("maintenance", "backend", "frontend")) {
            SourceRoleEvidence value = values.stream()
                    .filter(item -> item != null && sourceRole.equals(item.sourceRole()))
                    .findFirst().orElseThrow(() -> new IllegalStateException("SOURCE_ROLE_SET_INVALID"));
            String expectedRoot = REQUIRED_ROLE_ROOTS.get(sourceRole);
            if (!expectedRoot.equals(value.rootRole())) {
                throw new IllegalStateException("SOURCE_ROLE_ROOT_MISMATCH: " + sourceRole);
            }
            if (!REQUIRED_ROLE_PATHS.get(sourceRole).equals(value.relativePath())) {
                throw new IllegalStateException("SOURCE_ROLE_PATH_MISMATCH: " + sourceRole);
            }
            if (!roots.get(expectedRoot).approvedCommit().equals(normalizeCommit(value.commit()))) {
                throw new IllegalStateException("SOURCE_ROLE_COMMIT_MISMATCH: " + sourceRole);
            }
            roles.put(sourceRole, new SourceRoleEvidence(sourceRole, expectedRoot, value.relativePath(),
                    normalizeCommit(value.commit())));
        }
        return List.copyOf(roles.values());
    }

    private static String normalizeRoot(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim())) {
            throw new IllegalStateException("SOURCE_ROOT_PATH_INVALID");
        }
        return Path.of(value).toAbsolutePath().normalize().toString().replace('\\', '/')
                .toLowerCase(Locale.ROOT);
    }

    private static String normalizeCommit(String value) {
        if (value == null || !value.matches("[0-9a-fA-F]{40}")) {
            throw new IllegalStateException("SOURCE_COMMIT_INVALID");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    public enum VerificationPoint { BEFORE_BUILD, BEFORE_PACKAGE }

    public record GitRootEvidence(String rootRole, String normalizedRoot, String approvedCommit,
                                  String actualHead, boolean dirty) { }

    public record SourceRoleEvidence(String sourceRole, String rootRole, String relativePath, String commit) { }

    public record SourceFreezeEvidence(VerificationPoint verificationPoint, List<GitRootEvidence> gitRoots,
                                       List<SourceRoleEvidence> sourceRoles) { }
}
