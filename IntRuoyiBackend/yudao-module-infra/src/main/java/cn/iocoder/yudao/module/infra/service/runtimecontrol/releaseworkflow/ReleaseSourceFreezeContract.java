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

    public static SourceFreezeEvidence verifyAgain(SourceFreezeEvidence expected,
                                                   VerificationPoint verificationPoint,
                                                   List<GitRootEvidence> gitRoots,
                                                   List<SourceRoleEvidence> sourceRoles) {
        if (expected == null) {
            throw new IllegalStateException("SOURCE_FREEZE_EVIDENCE_MISSING");
        }
        Map<String, SourceRoleEvidence> expectedRoles = rolesByName(expected.sourceRoles());
        Map<String, SourceRoleEvidence> actualRoles = rolesByName(sourceRoles);
        for (String role : List.of("maintenance", "backend", "frontend")) {
            SourceRoleEvidence before = expectedRoles.get(role);
            SourceRoleEvidence current = actualRoles.get(role);
            if (!before.rootRole().equals(current.rootRole())) {
                throw new IllegalStateException("SOURCE_ROLE_ROOT_DRIFT: " + role);
            }
            if (!before.relativePath().equals(current.relativePath())) {
                throw new IllegalStateException("SOURCE_ROLE_PATH_DRIFT: " + role);
            }
            if (!normalizeCommit(before.commit()).equals(normalizeCommit(current.commit()))) {
                throw new IllegalStateException("SOURCE_ROLE_COMMIT_DRIFT: " + role);
            }
        }

        SourceFreezeEvidence current = verify(verificationPoint, gitRoots, sourceRoles);
        Map<String, GitRootEvidence> expectedRoots = rootsByRole(expected.gitRoots());
        Map<String, GitRootEvidence> actualRoots = rootsByRole(current.gitRoots());
        for (String rootRole : List.of("maintenance", "application")) {
            GitRootEvidence before = expectedRoots.get(rootRole);
            GitRootEvidence after = actualRoots.get(rootRole);
            if (!before.normalizedRoot().equals(after.normalizedRoot())) {
                throw new IllegalStateException("SOURCE_ROOT_PATH_DRIFT: " + rootRole);
            }
            if (!before.approvedCommit().equals(after.approvedCommit())) {
                throw new IllegalStateException("SOURCE_ROOT_COMMIT_DRIFT: " + rootRole);
            }
        }
        return current;
    }

    private static Map<String, SourceRoleEvidence> rolesByName(List<SourceRoleEvidence> roles) {
        if (roles == null || roles.size() != 3) {
            throw new IllegalStateException("SOURCE_ROLE_SET_DRIFT");
        }
        Map<String, SourceRoleEvidence> result = new LinkedHashMap<>();
        for (SourceRoleEvidence role : roles) {
            if (role == null || result.put(role.sourceRole(), role) != null) {
                throw new IllegalStateException("SOURCE_ROLE_SET_DRIFT");
            }
        }
        if (!result.keySet().equals(REQUIRED_ROLE_ROOTS.keySet())) {
            throw new IllegalStateException("SOURCE_ROLE_SET_DRIFT");
        }
        return result;
    }

    private static Map<String, GitRootEvidence> rootsByRole(List<GitRootEvidence> roots) {
        if (roots == null || roots.size() != 2) {
            throw new IllegalStateException("SOURCE_ROOT_SET_DRIFT");
        }
        Map<String, GitRootEvidence> result = new LinkedHashMap<>();
        for (GitRootEvidence root : roots) {
            if (root == null || result.put(root.rootRole(), root) != null) {
                throw new IllegalStateException("SOURCE_ROOT_SET_DRIFT");
            }
        }
        if (!result.keySet().equals(REQUIRED_ROOTS)) {
            throw new IllegalStateException("SOURCE_ROOT_SET_DRIFT");
        }
        return result;
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
