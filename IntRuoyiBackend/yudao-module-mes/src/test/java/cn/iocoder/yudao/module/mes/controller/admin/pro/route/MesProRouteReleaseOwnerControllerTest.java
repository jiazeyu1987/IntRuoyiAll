package cn.iocoder.yudao.module.mes.controller.admin.pro.route;

import cn.iocoder.yudao.module.mes.service.pro.route.MesProRouteReleaseOwnerService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesProRouteReleaseOwnerControllerTest {
    @Test void routeReadersCanReadWithoutSystemPermissionsButUnrelatedUsersCannot() {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated("engineer", "", List.of()));
            var access = context.getBean(Access.class);
            var controller = context.getBean(MesProRouteReleaseOwnerController.class);
            var service = context.getBean(MesProRouteReleaseOwnerService.class);
            for (String permission : List.of("mes:pro-route:query", "mes:pro-route:schedule-config:query",
                    "mes:pro-route:batch-record-config:query")) {
                access.granted = Set.of(permission);
                assertEquals(0, controller.getOwner("MES_PQC_RELEASE_OWNER").getCode());
            }
            verify(service, times(3)).getOwner("MES_PQC_RELEASE_OWNER");
            access.granted = Set.of("system:user:query", "system:role:query");
            assertThrows(AccessDeniedException.class, () -> controller.getOwner("MES_PQC_RELEASE_OWNER"));
            access.granted = Set.of();
            assertThrows(AccessDeniedException.class, () -> controller.getOwner("MES_PQC_RELEASE_OWNER"));
            verifyNoMoreInteractions(service);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    public static class Access {
        Set<String> granted = Set.of();
        public boolean hasAnyPermissions(String... permissions) {
            return Arrays.stream(permissions).anyMatch(granted::contains);
        }
    }

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean(name = "ss") Access access() { return new Access(); }
        @Bean MesProRouteReleaseOwnerService service() { return mock(MesProRouteReleaseOwnerService.class); }
        @Bean MesProRouteReleaseOwnerController controller(MesProRouteReleaseOwnerService service) {
            return new MesProRouteReleaseOwnerController(service);
        }
    }
}
