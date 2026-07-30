package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.vo.ProcessPoolReviewCopyGenerateSubmitReqVO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolReviewCopyService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProcessPoolReviewCopyControllerTest {

    @Test
    void shouldExposeGenerateSubmitWriteContractWithDedicatedPermission() throws Exception {
        RequestMapping classMapping = MesProcessPoolReviewCopyController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertArrayEquals(new String[]{"/mes/pro/process-pool/review-copy"}, classMapping.value());

        Method method = MesProcessPoolReviewCopyController.class.getMethod(
                "generateAndSubmit", ProcessPoolReviewCopyGenerateSubmitReqVO.class);
        assertEquals(CommonResult.class, method.getReturnType());

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping);
        assertArrayEquals(new String[]{"/generate-submit"}, postMapping.value());

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('mes:pro-process-pool-review-copy:generate-submit')",
                preAuthorize.value());
        assertTrue(!preAuthorize.value().contains(":query"));

        Parameter requestBody = method.getParameters()[0];
        assertNotNull(requestBody.getAnnotation(RequestBody.class));
        assertNotNull(requestBody.getAnnotation(Valid.class));

        Field serviceField = MesProcessPoolReviewCopyController.class.getDeclaredField("reviewCopyService");
        assertEquals(MesProcessPoolReviewCopyService.class, serviceField.getType());
        assertNotNull(serviceField.getAnnotation(Resource.class));

        String source = Files.readString(Paths.get("src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/"
                + "processpool/MesProcessPoolReviewCopyController.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("reviewCopyService.generateAndSubmitReviewCopy("));
        assertTrue(!source.contains("Mapper"));
    }

    @Test
    void generateSubmitRequestMustCarrySignatureAndLimitMappings() throws Exception {
        assertRequired(ProcessPoolReviewCopyGenerateSubmitReqVO.class, "eventId", Long.class, NotNull.class);
        assertRequired(ProcessPoolReviewCopyGenerateSubmitReqVO.class, "reviewerUserId", Long.class, NotNull.class);
        assertRequired(ProcessPoolReviewCopyGenerateSubmitReqVO.class, "reviewerSignatureId", Long.class, NotNull.class);
        assertRequired(ProcessPoolReviewCopyGenerateSubmitReqVO.class, "reviewerSignatureUserId", Long.class, NotNull.class);
        assertRequired(ProcessPoolReviewCopyGenerateSubmitReqVO.class, "reviewerSignatureSnapshot", String.class,
                NotBlank.class);

        Field mappings = ProcessPoolReviewCopyGenerateSubmitReqVO.class.getDeclaredField("fieldMappings");
        assertEquals(List.class, mappings.getType());
        assertNotNull(mappings.getAnnotation(NotEmpty.class));
        assertNotNull(mappings.getAnnotation(Valid.class));

        Class<?> mappingClass = ProcessPoolReviewCopyGenerateSubmitReqVO.FieldMapping.class;
        assertRequired(mappingClass, "fieldCode", String.class, NotBlank.class);
        assertRequired(mappingClass, "fieldName", String.class, NotBlank.class);
        assertRequired(mappingClass, "lowerLimit", BigDecimal.class, NotNull.class);
        assertRequired(mappingClass, "upperLimit", BigDecimal.class, NotNull.class);
    }

    private static void assertRequired(Class<?> clazz, String name, Class<?> type,
                                       Class<? extends java.lang.annotation.Annotation> annotation) throws Exception {
        Field field = clazz.getDeclaredField(name);
        assertEquals(type, field.getType(), clazz.getSimpleName() + "." + name);
        assertNotNull(field.getAnnotation(annotation), clazz.getSimpleName() + "." + name);
    }
}
