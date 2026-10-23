package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccTargetedActivationJobTest {
    private static final String NAME = "dccControlledFileTargetedActivationJob";
    private AnnotationConfigApplicationContext context(DccControlledFileLifecycleService lifecycle) {
        var context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("dccControlledFileLifecycleService", lifecycle);
        var scanner = new ClassPathBeanDefinitionScanner(context, false);
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*\\.DccControlledFileTargetedActivationJob")));
        scanner.scan("cn.iocoder.yudao.module.dcc.service.file.listener");
        context.refresh();
        return context;
    }

    @Test void officialHandlerMustExistForTaskOwnedActivation() {
        try (var context = context(mock(DccControlledFileLifecycleService.class))) {
            assertTrue(context.containsBean(NAME), "A mandatory-target handler is required; the all-files job cannot isolate task assets");
        }
    }

    @Test void onlyExactTenantAndFileAreActivatedAndContextIsRestored() throws Exception {
        var lifecycle = mock(DccControlledFileLifecycleService.class);
        when(lifecycle.activateDue(4216L)).thenAnswer(invocation -> {
            assertEquals(1L, TenantContextHolder.getRequiredTenantId());
            return true;
        });
        TenantContextHolder.setTenantId(9L);
        try (var context = context(lifecycle)) {
            assertEquals("实际生效版本数：1", context.getBean(NAME, JobHandler.class).execute("1:4216"));
            assertEquals(9L, TenantContextHolder.getRequiredTenantId());
            verify(lifecycle).activateDue(4216L);
            verifyNoMoreInteractions(lifecycle);
        } finally { TenantContextHolder.clear(); }
    }

    @Test void missingOrInvalidTargetNeverScansOrActivates() {
        var lifecycle = mock(DccControlledFileLifecycleService.class);
        try (var context = context(lifecycle)) {
            var job = context.getBean(NAME, JobHandler.class);
            for (String parameter : new String[]{null,"","1","1:0","0:4216","1:4216:9","-1:4216","1:9223372036854775808"})
                assertThrows(IllegalArgumentException.class, () -> job.execute(parameter));
            verifyNoInteractions(lifecycle);
        }
    }

    @Test void notDueIsReportedAsZeroAndFailureIsNotSwallowed() throws Exception {
        var lifecycle = mock(DccControlledFileLifecycleService.class);
        when(lifecycle.activateDue(4216L)).thenReturn(false).thenThrow(new IllegalStateException("CONFIRMATION_INVALID"));
        TenantContextHolder.setTenantId(9L);
        try (var context = context(lifecycle)) {
            var job = context.getBean(NAME, JobHandler.class);
            assertEquals("实际生效版本数：0",job.execute("1:4216"));
            assertEquals("CONFIRMATION_INVALID",assertThrows(IllegalStateException.class,()->job.execute("1:4216")).getMessage());
            assertEquals(9L,TenantContextHolder.getRequiredTenantId());
            verify(lifecycle,times(2)).activateDue(4216L);
            verifyNoMoreInteractions(lifecycle);
        } finally { TenantContextHolder.clear(); }
    }
}
