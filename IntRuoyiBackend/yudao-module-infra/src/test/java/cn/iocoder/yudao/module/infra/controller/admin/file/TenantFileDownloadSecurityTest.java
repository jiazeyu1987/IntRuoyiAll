package cn.iocoder.yudao.module.infra.controller.admin.file;

import cn.iocoder.yudao.module.infra.service.file.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantFileDownloadSecurityTest {
    @Test void anonymousTenantHeaderCannotAuthorizeFileRead() throws Exception {
        try (var context = new AnnotationConfigApplicationContext(Config.class)) {
            var controller = context.getBean(FileController.class);
            var service = context.getBean(FileService.class);
            var request = new MockHttpServletRequest("GET","/admin-api/infra/file/10/get/tenants/11/test.txt");
            request.addHeader("tenant-id","11");
            request.addHeader("User-Agent","tenant-file-security-test");
            var response = new MockHttpServletResponse();
            SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("test","anonymous",
                    List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
            assertThrows(AccessDeniedException.class,()->controller.getFileContent(request,response,10L));
            verifyNoInteractions(service);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user","",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            controller.getFileContent(request,response,10L);
            verify(service).getFileContent(10L,"tenants/11/test.txt");
            assertEquals(404,response.getStatus());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Configuration(proxyBeanMethods=false)
    @EnableMethodSecurity
    static class Config {
        @Bean FileController controller() { return new FileController(); }
        @Bean FileService service() { return mock(FileService.class); }
        @Bean FileUploadSecurityPolicy policy() { return mock(FileUploadSecurityPolicy.class); }
        @Bean NasBrowserService browser() { return mock(NasBrowserService.class); }
        @Bean NasDirectoryService directory() { return mock(NasDirectoryService.class); }
        @Bean NasSettingsService settings() { return mock(NasSettingsService.class); }
    }
}
