package cn.iocoder.yudao.module.dcc.service.permission;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccNasPrincipalMappingController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPrincipalMappingRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasPrincipalMappingSaveReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccNasUnmappedPrincipalRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.permission.DccNasAclIdentityMappingDO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccNasPrincipalMappingControllerTest extends BaseMockitoUnitTest {

    private static final Long TASK_ID = 6201L;
    private static final Long OPERATOR_USER_ID = 99L;
    private static final String UNMAPPED_PATH = "/dcc/nas-permission/principals/unmapped";
    private static final String SAVE_MAPPING_PATH = "/dcc/nas-permission/principal-mappings";

    @Mock
    private DccNasPrincipalMappingService principalMappingService;

    @InjectMocks
    private DccNasPrincipalMappingController controller;

    @Test
    void listUnmappedPrincipals_mapsEndpointPermissionAndReturnsDesignedFields() throws Exception {
        when(principalMappingService.listUnmappedPrincipals(TASK_ID)).thenReturn(List.of(
                new DccNasPrincipalMappingService.UnmappedPrincipal(
                        "CORP",
                        "S-1-5-21-1000-2000-3000-1101",
                        "CORP\\qa-user",
                        "hash-unmapped",
                        42,
                        "Quality/SOP")));

        Method method = findMappedMethod(GetMapping.class, UNMAPPED_PATH);
        assertCommonResultType(method, DccNasUnmappedPrincipalRespVO.class);
        assertHasManagePermission(method);

        CommonResult<?> result = invoke(method, TASK_ID);

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertTrue(DccNasUnmappedPrincipalRespVO.class.isInstance(result.getData()));
        List<?> list = readListProperty(result.getData(), "list");
        assertEquals(1, list.size());
        assertEquals("CORP", readProperty(list.get(0), "sourceAuthority"));
        assertEquals("S-1-5-21-1000-2000-3000-1101", readProperty(list.get(0), "sourceSid"));
        assertEquals("CORP\\qa-user", readProperty(list.get(0), "sourceName"));
        assertEquals(42, readProperty(list.get(0), "aceCount"));
        assertEquals("Quality/SOP", readProperty(list.get(0), "firstNasPath"));
        verify(principalMappingService).listUnmappedPrincipals(TASK_ID);
    }

    @Test
    void savePrincipalMapping_buildsCommandWithLoginUserAndReturnsDesignedFields() throws Exception {
        DccNasPrincipalMappingSaveReqVO reqVO = new DccNasPrincipalMappingSaveReqVO();
        reqVO.setSourceAuthority("CORP");
        reqVO.setSourceSid("S-1-5-21-1000-2000-3000-1101");
        reqVO.setSourceName("CORP\\qa-user");
        reqVO.setAccountName("qa-user");
        reqVO.setAccountType("USER");
        reqVO.setTargetSubjectType("USER");
        reqVO.setTargetSubjectId(10023L);
        reqVO.setActive(true);
        reqVO.setChangeReason("Restore NAS ACL principal mapping");

        when(principalMappingService.saveMapping(org.mockito.ArgumentMatchers.any(
                DccNasPrincipalMappingService.SaveMappingCommand.class)))
                .thenReturn(DccNasAclIdentityMappingDO.builder()
                        .id(501L)
                        .sid("S-1-5-21-1000-2000-3000-1101")
                        .dccSubjectType("USER")
                        .dccSubjectId(10023L)
                        .mappingStatus("MAPPED")
                        .build());

        Method method = findMappedMethod(PutMapping.class, SAVE_MAPPING_PATH);
        assertCommonResultType(method, DccNasPrincipalMappingRespVO.class);
        assertHasManagePermission(method);

        CommonResult<?> result;
        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(OPERATOR_USER_ID);

            result = invoke(method, reqVO);
        }

        assertTrue(Boolean.TRUE.equals(result.isSuccess()));
        assertTrue(DccNasPrincipalMappingRespVO.class.isInstance(result.getData()));
        Object data = result.getData();
        assertEquals(501L, readProperty(data, "id"));
        assertEquals("S-1-5-21-1000-2000-3000-1101", readProperty(data, "sourceSid"));
        assertEquals("USER", readProperty(data, "targetSubjectType"));
        assertEquals(10023L, readProperty(data, "targetSubjectId"));
        assertEquals(Boolean.TRUE, readProperty(data, "active"));

        ArgumentCaptor<DccNasPrincipalMappingService.SaveMappingCommand> commandCaptor =
                ArgumentCaptor.forClass(DccNasPrincipalMappingService.SaveMappingCommand.class);
        verify(principalMappingService).saveMapping(commandCaptor.capture());
        DccNasPrincipalMappingService.SaveMappingCommand command = commandCaptor.getValue();
        assertEquals("CORP", command.sourceAuthority());
        assertEquals("S-1-5-21-1000-2000-3000-1101", command.sourceSid());
        assertEquals("CORP\\qa-user", command.sourceName());
        assertEquals("qa-user", command.accountName());
        assertEquals("USER", command.accountType());
        assertEquals("USER", command.targetSubjectType());
        assertEquals(10023L, command.targetSubjectId());
        assertEquals(Boolean.TRUE, command.active());
        assertEquals("Restore NAS ACL principal mapping", command.changeReason());
        assertEquals(OPERATOR_USER_ID, command.operatorUserId());
    }

    @Test
    void saveReqVO_requiresDesignedValidationAnnotations() throws Exception {
        assertFieldHasNotBlank("sourceSid");
        assertFieldHasNotBlank("accountType");
        assertFieldHasNotBlank("targetSubjectType");
        assertFieldHasNotNull("targetSubjectId");
        assertFieldHasNotNull("active");
    }

    @Test
    void principalMappingServiceContracts_exposeOnlyCanonicalRecordConstructors() {
        assertEquals(1, DccNasPrincipalMappingService.SaveMappingCommand.class.getDeclaredConstructors().length);
        assertEquals(1, DccNasPrincipalMappingService.UnmappedPrincipal.class.getDeclaredConstructors().length);
    }

    private CommonResult<?> invoke(Method method, Object... args) throws Exception {
        Object result = method.invoke(controller, args);
        assertTrue(result instanceof CommonResult);
        return (CommonResult<?>) result;
    }

    private Method findMappedMethod(Class<? extends Annotation> mappingAnnotationType, String expectedFullPath) {
        return Arrays.stream(DccNasPrincipalMappingController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(mappingAnnotationType))
                .filter(method -> hasFullMappingPath(method.getAnnotation(mappingAnnotationType), expectedFullPath))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing endpoint mapping: " + expectedFullPath));
    }

    private boolean hasFullMappingPath(Annotation methodMapping, String expectedFullPath) {
        return classPrefixes().flatMap(prefix -> annotationPaths(methodMapping)
                        .map(methodPath -> normalizePath(prefix + "/" + methodPath)))
                .anyMatch(expectedFullPath::equals);
    }

    private Stream<String> classPrefixes() {
        RequestMapping requestMapping = DccNasPrincipalMappingController.class.getAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            return Stream.of("");
        }
        return Stream.concat(Arrays.stream(requestMapping.value()), Arrays.stream(requestMapping.path()))
                .distinct();
    }

    private Stream<String> annotationPaths(Annotation annotation) {
        try {
            String[] value = (String[]) annotation.annotationType().getMethod("value").invoke(annotation);
            String[] path = (String[]) annotation.annotationType().getMethod("path").invoke(annotation);
            return Stream.concat(Arrays.stream(value), Arrays.stream(path)).distinct();
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Cannot inspect endpoint mapping annotation", ex);
        }
    }

    private void assertCommonResultType(Method method, Class<?> expectedDataType) {
        assertEquals(CommonResult.class, method.getReturnType());
        Type genericReturnType = method.getGenericReturnType();
        assertTrue(genericReturnType instanceof ParameterizedType);
        ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
        assertEquals(CommonResult.class, parameterizedType.getRawType());
        assertEquals(expectedDataType, parameterizedType.getActualTypeArguments()[0]);
    }

    private void assertHasManagePermission(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on backend API surface");
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:access-rule:manage"));
    }

    private void assertFieldHasNotBlank(String fieldName) throws NoSuchFieldException {
        assertNotNull(DccNasPrincipalMappingSaveReqVO.class.getDeclaredField(fieldName)
                .getAnnotation(NotBlank.class));
    }

    private void assertFieldHasNotNull(String fieldName) throws NoSuchFieldException {
        assertNotNull(DccNasPrincipalMappingSaveReqVO.class.getDeclaredField(fieldName)
                .getAnnotation(NotNull.class));
    }

    private List<?> readListProperty(Object target, String propertyName) throws Exception {
        Object value = readProperty(target, propertyName);
        assertTrue(value instanceof List);
        return (List<?>) value;
    }

    private Object readProperty(Object target, String propertyName) throws Exception {
        String suffix = propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        for (String getterName : List.of("get" + suffix, "is" + suffix)) {
            try {
                return target.getClass().getMethod(getterName).invoke(target);
            } catch (NoSuchMethodException ignored) {
                // Try the next JavaBeans getter style.
            }
        }
        throw new AssertionError("Missing getter for property: " + propertyName);
    }

    private String normalizePath(String path) {
        String normalized = path.replace('\\', '/').replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
