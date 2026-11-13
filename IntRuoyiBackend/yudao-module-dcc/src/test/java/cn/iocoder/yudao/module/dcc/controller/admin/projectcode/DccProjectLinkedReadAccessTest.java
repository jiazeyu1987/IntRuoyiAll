package cn.iocoder.yudao.module.dcc.controller.admin.projectcode;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.projectcode.DccProjectCodeService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccProjectLinkedReadAccessTest extends BaseMockitoUnitTest {

    @InjectMocks
    private DccProjectCodeController controller;
    @Mock
    private DccProjectCodeService projectCodeService;
    @Mock
    private DccFileTypeTaxonomyAdminService taxonomyAdminService;

    @Test
    void taxonomyReaderChecksProjectVisibilityBeforeReadingCategories() {
        var node = DccFileTypeTaxonomyDO.builder().id(9L).name("设计输入").levelNo(1).build();
        when(taxonomyAdminService.getTaxonomyList()).thenReturn(List.of(node));
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(17L);
            var result = controller.getProjectFileTaxonomies(42L);
            assertTrue(result.isSuccess());
            assertEquals(9L, result.getData().get(0).getId());
            assertEquals("设计输入", result.getData().get(0).getName());
            var order = inOrder(projectCodeService, taxonomyAdminService);
            order.verify(projectCodeService).getReadableProjectCode(17L, 42L);
            order.verify(taxonomyAdminService).getTaxonomyList();
        }
    }

    @Test
    void inaccessibleProjectDoesNotExposeTaxonomyData() {
        var denied = new IllegalStateException("project access denied");
        when(projectCodeService.getReadableProjectCode(17L, 42L)).thenThrow(denied);
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(17L);
            assertSame(denied, assertThrows(IllegalStateException.class,
                    () -> controller.getProjectFileTaxonomies(42L)));
            verifyNoInteractions(taxonomyAdminService);
        }
    }

    @Test
    void readOnlyEndpointRequiresBothQueryPermissionsWithoutCategoryMaintenance() throws Exception {
        var annotation = DccProjectCodeController.class.getMethod("getProjectFileTaxonomies", Long.class)
                .getAnnotation(PreAuthorize.class);
        var expression = new SpelExpressionParser().parseExpression(annotation.value());
        for (var permissions : List.of(Set.<String>of(), Set.of("dcc:project-code:query"),
                Set.of("dcc:controlled-file:query"), Set.of("dcc:controlled-file:category:manage"))) {
            assertFalse(expression.getValue(context(permissions), Boolean.class));
        }
        assertTrue(expression.getValue(context(Set.of("dcc:project-code:query", "dcc:controlled-file:query")), Boolean.class));
    }

    private StandardEvaluationContext context(Set<String> permissions) {
        var context = new StandardEvaluationContext();
        context.setBeanResolver((evaluationContext, beanName) -> {
            assertEquals("ss", beanName);
            return new PermissionProbe(permissions);
        });
        return context;
    }

    public record PermissionProbe(Set<String> permissions) {
        public boolean hasPermission(String permission) {
            return permissions.contains(permission);
        }
    }
}
