package cn.iocoder.yudao.module.mes.service.pro.craft;

import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.mes.controller.admin.pro.craft.MesSharedChangeController;
import cn.iocoder.yudao.module.mes.controller.admin.pro.craft.vo.MesSharedChangeReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesSharedChangeControllerPermissionTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean MesSharedChangeController controller() { return new MesSharedChangeController(); }
    }
    @Test void deviceEditPermissionRemainsMandatory() {
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().registerSingleton("ss", mock(SecurityFrameworkService.class));
            context.getBeanFactory().registerSingleton("service", mock(MesProSharedChangeService.class));
            context.register(Config.class); context.refresh();
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("editor", "", List.of()));
            var security = context.getBean(SecurityFrameworkService.class);
            var service = context.getBean(MesProSharedChangeService.class);
            var controller = context.getBean(MesSharedChangeController.class);
            var request = new MesSharedChangeReqVO(); request.setSubjectType("DEVICE"); request.setAction("UPDATE");
            assertThrows(AccessDeniedException.class, () -> controller.submit("DEVICE", request));
            verifyNoInteractions(service);
            when(security.hasPermission("mes:dv-machinery:update")).thenReturn(true);
            assertDoesNotThrow(() -> controller.submit("DEVICE", request));
            verify(service).submit(request);
        } finally { SecurityContextHolder.clearContext(); }
    }
}
