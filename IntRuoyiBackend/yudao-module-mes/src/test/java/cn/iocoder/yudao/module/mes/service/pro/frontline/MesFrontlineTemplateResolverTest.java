package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineTemplateResolverTest {

    @Mock
    private ObjectProvider<MesFrontlineTemplateBindingSource> templateBindingSourceProvider;
    @Mock
    private MesFrontlineTemplateBindingSource templateBindingSource;

    private MesFrontlineTemplateResolverImpl templateResolver;

    @BeforeEach
    void setUp() {
        templateResolver = new MesFrontlineTemplateResolverImpl(templateBindingSourceProvider);
    }

    @Test
    void shouldResolveTemplateByActualEmployeeAndCurrentProcess() {
        when(templateBindingSourceProvider.getIfAvailable()).thenReturn(templateBindingSource);
        when(templateBindingSource.findTemplate(argThat(request -> request != null
                && request.loginUserId().equals(9001L)
                && request.actualEmployeeId().equals(10001L)
                && request.routeProcessId().equals(1001L)
                && request.processId().equals(201L)))).thenReturn(
                new MesFrontlineTemplateDescriptor("TPL-201-E1001", "BATCH_RECORD",
                        1001L, 201L, 10001L));

        MesFrontlineTemplateDescriptor template = templateResolver.resolve(
                new MesFrontlineTemplateRequest(9001L, 10001L, 101L, 1001L, 201L));

        assertEquals("TPL-201-E1001", template.templateNo());
    }

    @Test
    void shouldFailFastWhenFormalTemplateBindingIsMissing() {
        when(templateBindingSourceProvider.getIfAvailable()).thenReturn(templateBindingSource);
        when(templateBindingSource.findTemplate(argThat(request -> request != null
                && request.actualEmployeeId().equals(10002L)))).thenReturn(null);

        assertThrows(ServiceException.class, () -> templateResolver.resolve(
                new MesFrontlineTemplateRequest(9001L, 10002L, 101L, 1001L, 201L)));
    }

}
