package cn.iocoder.yudao.module.mdm;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MdmDependencyDirectionContractTest {

    private static final Path BACKEND_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path COMPANY_SCOPE_ROOT = Path.of("src/main/java/cn/iocoder/yudao/module/mdm");

    @Test
    void moduleGraphRemainsDccToMdmToSystemWithoutReverseSystemDependency() throws IOException {
        String dccPom = Files.readString(BACKEND_ROOT.resolve("yudao-module-dcc/pom.xml"));
        String mdmPom = Files.readString(Path.of("pom.xml"));
        String systemPom = Files.readString(BACKEND_ROOT.resolve("yudao-module-system/pom.xml"));

        assertTrue(dccPom.contains("<artifactId>yudao-module-mdm</artifactId>"));
        assertTrue(mdmPom.contains("<artifactId>yudao-module-system</artifactId>"));
        assertFalse(systemPom.contains("<artifactId>yudao-module-mdm</artifactId>"));

        Path systemSources = BACKEND_ROOT.resolve("yudao-module-system/src/main/java");
        try (Stream<Path> paths = Files.walk(systemSources)) {
            assertFalse(paths.filter(path -> path.toString().endsWith(".java"))
                    .map(this::read)
                    .anyMatch(source -> source.contains("cn.iocoder.yudao.module.mdm")));
        }
    }

    @Test
    void companyScopeUsesFormalMappingsWithoutDepartmentTenantOrNameFallback() throws IOException {
        List<String> forbiddenTokens = List.of(
                "DeptApi", "getUserListByDeptIds", "getUserListByNickname", "TenantApi",
                "getDeptDataPermission", "getEnterpriseByName", "companyName");
        try (Stream<Path> paths = Files.walk(COMPANY_SCOPE_ROOT)) {
            String source = paths.filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("companyscope"))
                    .map(this::read)
                    .reduce("", (left, right) -> left + '\n' + right);
            forbiddenTokens.forEach(token -> assertFalse(source.contains(token), "forbidden fallback token: " + token));
        }
    }

    @Test
    void rawScopeQueriesRequireExplicitTenantAndFormalBusinessIds() throws IOException {
        String userMapper = Files.readString(COMPANY_SCOPE_ROOT.resolve(
                "dal/mysql/companyscope/MdmUserCompanyScopeMapper.java"));
        String roleMapper = Files.readString(COMPANY_SCOPE_ROOT.resolve(
                "dal/mysql/companyscope/MdmRoleCompanyScopeMapper.java"));

        assertTrue(userMapper.contains("tenant_id = #{tenantId}"));
        assertTrue(userMapper.contains("user_id = #{userId}"));
        assertTrue(userMapper.contains("company_id = #{companyId}"));
        assertTrue(roleMapper.contains("tenant_id = #{tenantId}"));
        assertTrue(roleMapper.contains("company_id = #{companyId}"));
        assertTrue(roleMapper.contains("role_id IN"));
    }

    @Test
    void companyScopeApiProvidesBatchUserCompanyReadContract() throws IOException {
        String api = Files.readString(COMPANY_SCOPE_ROOT.resolve(
                "api/companyscope/MdmCompanyScopeApi.java"));

        assertTrue(api.contains("Set<Long> getEnabledCompanyIdsForUser(Long userId)"),
                "DCC list queries require the formal enabled company ID set without row-by-row inference");
        assertTrue(api.contains(
                        "void validateUserCompanyAccessBatch(Long userId, Collection<Long> companyIds)"),
                "batch authorization must be a distinct fail-closed contract without overload ambiguity");
    }

    @Test
    void infrastructureFailuresHaveExecutablePropagationCoverage() throws IOException {
        Path companyScopeTestRoot = Path.of("src/test/java/cn/iocoder/yudao/module/mdm/service/companyscope");
        String serviceTest = Files.readString(companyScopeTestRoot.resolve("MdmCompanyScopeServiceImplTest.java"));
        String resolverTest = Files.readString(
                companyScopeTestRoot.resolve("MdmCompanyScopeRecipientResolverTest.java"));

        assertTrue(serviceTest.contains("authorizationReadsPropagateInfrastructureFailureUnchanged"));
        assertTrue(resolverTest.contains("resolvePropagatesInfrastructureFailureUnchanged"));
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot inspect source: " + path, exception);
        }
    }

}
