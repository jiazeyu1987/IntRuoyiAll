package cn.iocoder.yudao.module.bpm.businessapproval.controller;

import cn.iocoder.yudao.module.bpm.businessapproval.service.RoleApprovalConfigPackageService;
import cn.iocoder.yudao.module.bpm.controller.admin.businessapproval.RoleApprovalConfigPackageController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleApprovalConfigPackagePermissionTest {
    private static final Set<String> REQUIRED = Set.of("system:role:create", "system:role:update",
            "system:permission:assign-role-menu", "system:permission:assign-role-data-scope",
            "system:role-category:create", "bpm:business-approval-policy:create", "bpm:business-approval-policy:publish",
            "bpm:business-approval-policy:disable", "bpm:model:create", "bpm:model:deploy", "bpm:category:create");
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setup() {
        context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("transferService", mock(RoleApprovalConfigPackageService.class));
        context.register(Config.class);
        context.refresh();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("operator", "", List.of()));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void missingAnyRequiredPermissionMustRejectBeforeImport() {
        PermissionProbe permissions = context.getBean(PermissionProbe.class);
        var controller = context.getBean(RoleApprovalConfigPackageController.class);
        var service = context.getBean(RoleApprovalConfigPackageService.class);
        for (String missing : REQUIRED) {
            permissions.granted = new HashSet<>(REQUIRED);
            permissions.granted.remove(missing);
            assertThrows(AccessDeniedException.class, () -> controller.importConfigPackage(file()), missing);
        }
        verifyNoInteractions(service);
    }

    @Test
    void missingRoleOrPolicyOrModelReadPermissionMustRejectExport() {
        var required = Set.of("system:role:export", "bpm:business-approval-policy:query", "bpm:model:query");
        var permissions = context.getBean(PermissionProbe.class);
        var controller = context.getBean(RoleApprovalConfigPackageController.class);
        for (String missing : required) {
            permissions.granted = new HashSet<>(required);
            permissions.granted.remove(missing);
            assertThrows(AccessDeniedException.class,
                    () -> controller.exportConfigPackage(new org.springframework.mock.web.MockHttpServletResponse()), missing);
        }
        verifyNoInteractions(context.getBean(RoleApprovalConfigPackageService.class));
    }

    @Test
    void allRequiredPermissionsAllowOriginalFileToReachImporter() throws Exception {
        context.getBean(PermissionProbe.class).granted = REQUIRED;
        assertEquals(0, context.getBean(RoleApprovalConfigPackageController.class).importConfigPackage(file()).getData());
        verify(context.getBean(RoleApprovalConfigPackageService.class)).importPackage(new byte[]{1, 2});
    }

    private MockMultipartFile file() {
        return new MockMultipartFile("file", "roles.json", "application/json", new byte[]{1, 2});
    }

    public static class PermissionProbe {
        Set<String> granted = Set.of();
        public boolean hasPermission(String permission) {
            return granted.contains(permission);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class Config {
        @Bean RoleApprovalConfigPackageController roleController() { return new RoleApprovalConfigPackageController(); }
        @Bean(name = "ss") PermissionProbe permissions() { return new PermissionProbe(); }
    }
}
