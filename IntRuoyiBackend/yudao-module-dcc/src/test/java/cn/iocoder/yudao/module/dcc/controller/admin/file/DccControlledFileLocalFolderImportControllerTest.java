package cn.iocoder.yudao.module.dcc.controller.admin.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportChunkRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportUploadStateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileLocalFolderImportReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferRespVO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileNasTransferService;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccControlledFileLocalFolderImportControllerTest extends BaseMockitoUnitTest {

    private static final String LOCAL_FOLDER_IMPORT_PATH = "/dcc/controlled-files/local-folder-import";
    private static final String LOCAL_FOLDER_IMPORT_SESSION_PATH =
            "/dcc/controlled-files/local-folder-import/sessions";
    private static final String LOCAL_FOLDER_IMPORT_BATCH_PATH =
            "/dcc/controlled-files/local-folder-import/sessions/{taskId}/batches";
    private static final String LOCAL_FOLDER_IMPORT_UPLOAD_STATE_PATH =
            "/dcc/controlled-files/local-folder-import/sessions/{taskId}/upload-state";
    private static final String LOCAL_FOLDER_IMPORT_CHUNK_PATH =
            "/dcc/controlled-files/local-folder-import/sessions/{taskId}/chunks";
    private static final String LOCAL_FOLDER_IMPORT_COMPLETE_PATH =
            "/dcc/controlled-files/local-folder-import/sessions/{taskId}/complete";

    @Mock
    private DccControlledFileNasTransferService nasTransferService;

    @InjectMocks
    private DccControlledFileController controller;

    @Test
    void localFolderImport_mapsMultipartEndpointAndUsesSameDccPermissionsAsNasTransfer() {
        Method method = findMappedMethod(PostMapping.class, LOCAL_FOLDER_IMPORT_PATH);
        assertCommonResultType(method, DccControlledFileNasTransferRespVO.class);

        assertTrue(Arrays.stream(method.getParameterTypes())
                        .anyMatch(type -> "DccControlledFileLocalFolderImportReqVO".equals(type.getSimpleName())),
                "local folder import endpoint must bind a typed multipart request VO");

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on local folder import endpoint");
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:submit"));
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:directory:manage"));
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:category:manage"));
    }

    @Test
    void largeLocalFolderImport_mapsSessionBatchAndCompleteEndpointsWithSameDccPermissions() {
        Method sessionMethod = findMappedMethod(PostMapping.class, LOCAL_FOLDER_IMPORT_SESSION_PATH);
        assertCommonResultType(sessionMethod, DccControlledFileNasTransferRespVO.class);
        assertTrue(Arrays.stream(sessionMethod.getParameterTypes())
                        .anyMatch(type -> "DccControlledFileLocalFolderImportSessionCreateReqVO"
                                .equals(type.getSimpleName())),
                "large local folder import session endpoint must bind a typed JSON request VO");
        assertDccTransferPermissions(sessionMethod);

        Method batchMethod = findMappedMethod(PostMapping.class, LOCAL_FOLDER_IMPORT_BATCH_PATH);
        assertCommonResultType(batchMethod, DccControlledFileNasTransferRespVO.class);
        assertTrue(Arrays.stream(batchMethod.getParameterTypes())
                        .anyMatch(type -> "DccControlledFileLocalFolderImportBatchReqVO"
                                .equals(type.getSimpleName())),
                "large local folder import batch endpoint must bind a typed multipart request VO");
        assertDccTransferPermissions(batchMethod);

        Method completeMethod = findMappedMethod(PostMapping.class, LOCAL_FOLDER_IMPORT_COMPLETE_PATH);
        assertCommonResultType(completeMethod, DccControlledFileNasTransferRespVO.class);
        assertDccTransferPermissions(completeMethod);
    }

    @Test
    void resumableLocalFolderImport_mapsUploadStateAndChunkEndpointsWithSameDccPermissions() {
        Method uploadStateMethod = findMappedMethod(GetMapping.class, LOCAL_FOLDER_IMPORT_UPLOAD_STATE_PATH);
        assertCommonResultType(uploadStateMethod, DccControlledFileLocalFolderImportUploadStateRespVO.class);
        assertDccTransferPermissions(uploadStateMethod);

        Method chunkMethod = findMappedMethod(PostMapping.class, LOCAL_FOLDER_IMPORT_CHUNK_PATH);
        assertCommonResultType(chunkMethod, DccControlledFileLocalFolderImportChunkRespVO.class);
        assertTrue(Arrays.stream(chunkMethod.getParameterTypes())
                        .anyMatch(type -> "DccControlledFileLocalFolderImportChunkReqVO"
                                .equals(type.getSimpleName())),
                "local folder import chunk endpoint must bind a typed multipart request VO");
        assertDccTransferPermissions(chunkMethod);
    }

    @Test
    void nasTransferAndLocalFolderImportProductBindingMustBeOptional() throws Exception {
        assertNull(
                DccControlledFileNasTransferReqVO.class
                        .getDeclaredField("productMasterId")
                        .getAnnotation(NotNull.class),
                "NAS transfer productMasterId must be optional for non-product DCC files");
        assertNull(
                DccControlledFileLocalFolderImportReqVO.class
                        .getDeclaredField("productMasterId")
                        .getAnnotation(NotNull.class),
                "Local folder import productMasterId must be optional for non-product DCC files");
    }

    private Method findMappedMethod(Class<? extends Annotation> mappingAnnotationType, String expectedFullPath) {
        return Arrays.stream(DccControlledFileController.class.getDeclaredMethods())
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
        assertTrue(genericReturnType instanceof ParameterizedType);
        ParameterizedType parameterizedType = (ParameterizedType) genericReturnType;
        assertEquals(CommonResult.class, parameterizedType.getRawType());
        assertEquals(expectedDataType, parameterizedType.getActualTypeArguments()[0]);
    }

    private void assertDccTransferPermissions(Method method) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Missing @PreAuthorize on local folder import endpoint");
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:submit"));
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:directory:manage"));
        assertTrue(preAuthorize.value().contains("dcc:controlled-file:category:manage"));
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
