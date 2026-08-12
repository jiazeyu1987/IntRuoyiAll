package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccProjectCodeStatusConstants;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesRouteDccProjectBindingDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesRouteDccProjectBindingMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesFrontlineDccProjectResolverTest {

    private static final long TENANT_ID = 1L;
    private static final long ROUTE_ID = 2001L;
    private static final long DCC_PROJECT_CODE_ID = 6001L;

    @Mock
    private MesRouteDccProjectBindingMapper bindingMapper;
    @Mock
    private DccProjectCodeMapper projectCodeMapper;

    private DccProjectResolver resolver;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        resolver = new DccProjectResolver(bindingMapper, projectCodeMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void requireEnabledByRouteReturnsUniqueEnabledSameTenantProject() {
        when(bindingMapper.selectList(any())).thenReturn(List.of(binding(11L, DCC_PROJECT_CODE_ID)));
        when(projectCodeMapper.selectById(DCC_PROJECT_CODE_ID)).thenReturn(project(
                DCC_PROJECT_CODE_ID, TENANT_ID, DccProjectCodeStatusConstants.ENABLE, false));

        DccProjectResolver.ResolvedProject result = resolver.requireEnabledByRoute(ROUTE_ID);

        assertEquals(DCC_PROJECT_CODE_ID, result.dccProjectCodeId());
        assertEquals("DCC-6001", result.projectCode());
        assertEquals("project-6001", result.projectName());
        verify(bindingMapper).selectList(any());
        verify(projectCodeMapper).selectById(DCC_PROJECT_CODE_ID);
        verifyNoMoreInteractions(bindingMapper, projectCodeMapper);
    }

    @Test
    void requireEnabledByRouteRejectsMissingFormalBinding() {
        when(bindingMapper.selectList(any())).thenReturn(List.of());

        ServiceException error = assertThrows(ServiceException.class,
                () -> resolver.requireEnabledByRoute(ROUTE_ID));

        assertEquals(DccProjectResolver.ROUTE_DCC_BINDING_REQUIRED.getCode(), error.getCode());
        verify(projectCodeMapper, never()).selectById(any());
    }

    @Test
    void requireEnabledByRouteRejectsInvalidRouteIdWithoutQuerying() {
        ServiceException error = assertThrows(ServiceException.class,
                () -> resolver.requireEnabledByRoute(0L));

        assertEquals(DccProjectResolver.ROUTE_DCC_BINDING_REQUIRED.getCode(), error.getCode());
        verifyNoMoreInteractions(bindingMapper, projectCodeMapper);
    }

    @Test
    void requireEnabledByRouteRejectsAmbiguousFormalBindings() {
        when(bindingMapper.selectList(any())).thenReturn(List.of(
                binding(11L, DCC_PROJECT_CODE_ID), binding(12L, 6002L)));

        ServiceException error = assertThrows(ServiceException.class,
                () -> resolver.requireEnabledByRoute(ROUTE_ID));

        assertEquals(DccProjectResolver.ROUTE_DCC_BINDING_AMBIGUOUS.getCode(), error.getCode());
        verify(projectCodeMapper, never()).selectById(any());
    }

    @Test
    void requireEnabledByRouteRejectsCrossTenantFormalBinding() {
        MesRouteDccProjectBindingDO binding = binding(11L, DCC_PROJECT_CODE_ID);
        binding.setTenantId(2L);
        when(bindingMapper.selectList(any())).thenReturn(List.of(binding));

        ServiceException error = assertThrows(ServiceException.class,
                () -> resolver.requireEnabledByRoute(ROUTE_ID));

        assertEquals(DccProjectResolver.DCC_PROJECT_REFERENCE_INVALID.getCode(), error.getCode());
        verify(projectCodeMapper, never()).selectById(any());
    }

    @Test
    void requireEnabledByRouteRejectsDeletedFormalBinding() {
        MesRouteDccProjectBindingDO binding = binding(11L, DCC_PROJECT_CODE_ID);
        binding.setDeleted(true);
        when(bindingMapper.selectList(any())).thenReturn(List.of(binding));

        ServiceException error = assertThrows(ServiceException.class,
                () -> resolver.requireEnabledByRoute(ROUTE_ID));

        assertEquals(DccProjectResolver.DCC_PROJECT_REFERENCE_INVALID.getCode(), error.getCode());
        verify(projectCodeMapper, never()).selectById(any());
    }

    @Test
    void requireEnabledByRouteRejectsMissingReferencedProject() {
        when(bindingMapper.selectList(any())).thenReturn(List.of(binding(11L, DCC_PROJECT_CODE_ID)));
        when(projectCodeMapper.selectById(DCC_PROJECT_CODE_ID)).thenReturn(null);

        assertInvalidProjectReference();
    }

    @Test
    void requireEnabledByRouteRejectsDisabledProject() {
        when(bindingMapper.selectList(any())).thenReturn(List.of(binding(11L, DCC_PROJECT_CODE_ID)));
        when(projectCodeMapper.selectById(DCC_PROJECT_CODE_ID)).thenReturn(project(
                DCC_PROJECT_CODE_ID, TENANT_ID, DccProjectCodeStatusConstants.DISABLE, false));

        assertInvalidProjectReference();
    }

    @Test
    void requireEnabledByRouteRejectsDeletedProject() {
        when(bindingMapper.selectList(any())).thenReturn(List.of(binding(11L, DCC_PROJECT_CODE_ID)));
        when(projectCodeMapper.selectById(DCC_PROJECT_CODE_ID)).thenReturn(project(
                DCC_PROJECT_CODE_ID, TENANT_ID, DccProjectCodeStatusConstants.ENABLE, true));

        assertInvalidProjectReference();
    }

    @Test
    void requireEnabledByRouteDoesNotExposeCrossTenantProject() {
        when(bindingMapper.selectList(any())).thenReturn(List.of(binding(11L, DCC_PROJECT_CODE_ID)));
        when(projectCodeMapper.selectById(DCC_PROJECT_CODE_ID)).thenReturn(project(
                DCC_PROJECT_CODE_ID, 2L, DccProjectCodeStatusConstants.ENABLE, false));

        assertInvalidProjectReference();
    }

    private void assertInvalidProjectReference() {
        ServiceException error = assertThrows(ServiceException.class,
                () -> resolver.requireEnabledByRoute(ROUTE_ID));

        assertEquals(DccProjectResolver.DCC_PROJECT_REFERENCE_INVALID.getCode(), error.getCode());
    }

    private static MesRouteDccProjectBindingDO binding(Long id, Long dccProjectCodeId) {
        MesRouteDccProjectBindingDO binding = MesRouteDccProjectBindingDO.builder()
                .id(id)
                .routeId(ROUTE_ID)
                .dccProjectCodeId(dccProjectCodeId)
                .version(id)
                .build();
        binding.setTenantId(TENANT_ID);
        binding.setDeleted(false);
        return binding;
    }

    private static DccProjectCodeDO project(Long id, Long tenantId, String status, Boolean deleted) {
        DccProjectCodeDO project = DccProjectCodeDO.builder()
                .id(id)
                .projectCode("DCC-" + id)
                .projectName("project-" + id)
                .status(status)
                .build();
        project.setTenantId(tenantId);
        project.setDeleted(deleted);
        return project;
    }
}
