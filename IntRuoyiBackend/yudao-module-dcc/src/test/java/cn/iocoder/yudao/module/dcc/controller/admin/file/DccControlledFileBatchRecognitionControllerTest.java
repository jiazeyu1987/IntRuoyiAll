package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileBatchRecognitionCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileBatchRecognitionTaskRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileBatchRecognitionService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileBatchRecognitionControllerTest extends BaseMockitoUnitTest {

    private static final String CREATE_PATH = "/dcc/controlled-files/batch-recognition/tasks";
    private static final String LATEST_PATH = "/dcc/controlled-files/batch-recognition/tasks/latest";
    private static final String GET_PATH = "/dcc/controlled-files/batch-recognition/tasks/{taskId}";
    private static final String STOP_PATH = "/dcc/controlled-files/batch-recognition/tasks/{taskId}/stop";

    @Mock
    private DccControlledFileBatchRecognitionService batchRecognitionService;

    @InjectMocks
    private DccControlledFileController controller;

    @Test
    void batchRecognitionTaskEndpointsRequireDocControlAndDelegate() throws Exception {
        Method createMethod = findMappedMethod(PostMapping.class, CREATE_PATH);
        Method latestMethod = findMappedMethod(GetMapping.class, LATEST_PATH);
        Method getMethod = findMappedMethod(GetMapping.class, GET_PATH);
        Method stopMethod = findMappedMethod(PostMapping.class, STOP_PATH);
        assertCommonResultType(createMethod, DccControlledFileBatchRecognitionTaskRespVO.class);
        assertCommonResultType(latestMethod, DccControlledFileBatchRecognitionTaskRespVO.class);
        assertCommonResultType(getMethod, DccControlledFileBatchRecognitionTaskRespVO.class);
        assertCommonResultType(stopMethod, DccControlledFileBatchRecognitionTaskRespVO.class);

        assertDocControl(createMethod);
        assertDocControl(latestMethod);
        assertDocControl(getMethod);
        assertDocControl(stopMethod);

        DccControlledFileBatchRecognitionTaskRespVO taskRespVO = new DccControlledFileBatchRecognitionTaskRespVO();
        taskRespVO.setTaskId(100L);
        taskRespVO.setStatus("WAITING");
        taskRespVO.setScope("CURRENT");
        taskRespVO.setRecognitionType("FILE_CATEGORY");
        DccControlledFileBatchRecognitionCreateReqVO reqVO = new DccControlledFileBatchRecognitionCreateReqVO();
        reqVO.setRecognitionType("FILE_CATEGORY");
        reqVO.setScope("CURRENT");
        reqVO.setDirectoryId(10L);
        reqVO.setIncludeDescendantDirectories(true);
        reqVO.setOverwriteExisting(false);
        reqVO.setSyncFileNameTitle(true);
        reqVO.setWorkerCount(5);
        when(batchRecognitionService.createTask(99L, reqVO)).thenReturn(taskRespVO);
        when(batchRecognitionService.getLatestTask(99L, "FILE_CATEGORY")).thenReturn(taskRespVO);
        when(batchRecognitionService.getTask(99L, 100L)).thenReturn(taskRespVO);
        when(batchRecognitionService.stopTask(99L, 100L)).thenReturn(taskRespVO);

        CommonResult<?> createResult;
        CommonResult<?> latestResult;
        CommonResult<?> getResult;
        CommonResult<?> stopResult;
        try (MockedStatic<SecurityFrameworkUtils> securityFrameworkUtilsMock = mockStatic(SecurityFrameworkUtils.class)) {
            securityFrameworkUtilsMock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            createResult = (CommonResult<?>) createMethod.invoke(controller, reqVO);
            latestResult = (CommonResult<?>) latestMethod.invoke(controller, "FILE_CATEGORY");
            getResult = (CommonResult<?>) getMethod.invoke(controller, 100L);
            stopResult = (CommonResult<?>) stopMethod.invoke(controller, 100L);
        }

        assertEquals(taskRespVO, createResult.getData());
        assertEquals(taskRespVO, latestResult.getData());
        assertEquals(taskRespVO, getResult.getData());
        assertEquals(taskRespVO, stopResult.getData());
        verify(batchRecognitionService).createTask(99L, reqVO);
        verify(batchRecognitionService).getLatestTask(99L, "FILE_CATEGORY");
        verify(batchRecognitionService).getTask(99L, 100L);
        verify(batchRecognitionService).stopTask(99L, 100L);
    }

    @Test
    void createRequestSupportsWorkerCount() throws Exception {
        assertNotNull(DccControlledFileBatchRecognitionCreateReqVO.class.getDeclaredField("workerCount"));
        assertNotNull(DccControlledFileBatchRecognitionCreateReqVO.class.getDeclaredField("recognitionType"));
        assertNotNull(DccControlledFileBatchRecognitionCreateReqVO.class.getDeclaredField("existingRecordPolicy"));
        assertNotNull(DccControlledFileBatchRecognitionTaskRespVO.class.getDeclaredField("workerCount"));
        assertNotNull(DccControlledFileBatchRecognitionTaskRespVO.class.getDeclaredField("activeWorkerCount"));
        assertNotNull(DccControlledFileBatchRecognitionTaskRespVO.class.getDeclaredField("recordedFileCount"));
        assertNotNull(DccControlledFileBatchRecognitionTaskRespVO.class.getDeclaredField("existingRecordPolicy"));
        assertNotNull(DccControlledFileBatchRecognitionTaskRespVO.class.getDeclaredField("recognitionType"));
        assertNotNull(DccControlledFileBatchRecognitionTaskRespVO.class.getDeclaredField("unclassifiedCount"));
        assertNotNull(DccControlledFileBatchRecognitionTaskRespVO.class.getDeclaredField("ambiguousCount"));
        assertNotNull(DccControlledFileBatchRecognitionTaskRespVO.class.getDeclaredField("conflictCount"));
    }

    private void assertDocControl(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize");
        assertTrue(preAuthorize.value().contains("@ss.hasRole('doc_control')"));
    }

    private Method findMappedMethod(Class<? extends Annotation> mappingAnnotationType, String expectedFullPath) {
        return mappedMethods(mappingAnnotationType)
                .filter(method -> hasFullMappingPath(method.getAnnotation(mappingAnnotationType), expectedFullPath))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing endpoint mapping: " + expectedFullPath));
    }

    private Stream<Method> mappedMethods(Class<? extends Annotation> mappingAnnotationType) {
        return Arrays.stream(DccControlledFileController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(mappingAnnotationType));
    }

    private boolean hasFullMappingPath(Annotation methodMapping, String expectedFullPath) {
        return classPrefixes().flatMap(prefix -> annotationPaths(methodMapping)
                        .map(methodPath -> normalizePath(prefix + "/" + methodPath)))
                .anyMatch(expectedFullPath::equals);
    }

    private Stream<String> classPrefixes() {
        RequestMapping requestMapping = DccControlledFileController.class.getAnnotation(RequestMapping.class);
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
        ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
        assertEquals(CommonResult.class, parameterizedType.getRawType());
        assertEquals(expectedDataType, parameterizedType.getActualTypeArguments()[0]);
    }

    private String normalizePath(String path) {
        String normalized = path.replaceAll("/{2,}", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
