package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrCatalogCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrCatalogPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateImpactPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateImpactReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateLifecycleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrDhrTemplateSignoffReqVO;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrDhrTemplateLifecycleContractTest {

    @Test
    void controllersExposeDhrTemplateRoutesAndPermissions() throws Exception {
        RequestMapping requestMapping = MesProEdhrDhrTemplateController.class.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{"/mes/pro/edhr-dhr-template"}, requestMapping.value());

        assertGetMapping("getCatalogPage", new Class<?>[]{MesProEdhrDhrCatalogPageReqVO.class},
                "/catalog/page", "mes:pro-edhr-dhr-template:query");
        assertPostMapping("createCatalog", new Class<?>[]{MesProEdhrDhrCatalogCreateReqVO.class},
                "/catalog/create", "mes:pro-edhr-dhr-template:create");
        assertGetMapping("getPage", new Class<?>[]{MesProEdhrDhrTemplatePageReqVO.class},
                "/page", "mes:pro-edhr-dhr-template:query");
        assertPostMapping("create", new Class<?>[]{MesProEdhrDhrTemplateCreateReqVO.class},
                "/create", "mes:pro-edhr-dhr-template:create");
        assertPostMapping("runIntegrityCheck", new Class<?>[]{MesProEdhrDhrTemplateLifecycleReqVO.class},
                "/integrity-check", "mes:pro-edhr-dhr-template:check");
        assertPostMapping("approve", new Class<?>[]{MesProEdhrDhrTemplateLifecycleReqVO.class},
                "/approve", "mes:pro-edhr-dhr-template:approve");
        assertPostMapping("signoff", new Class<?>[]{MesProEdhrDhrTemplateSignoffReqVO.class},
                "/signoff", "mes:pro-edhr-dhr-template:signoff");
        assertPostMapping("activate", new Class<?>[]{MesProEdhrDhrTemplateLifecycleReqVO.class},
                "/activate", "mes:pro-edhr-dhr-template:activate");
        assertPostMapping("retire", new Class<?>[]{MesProEdhrDhrTemplateImpactReqVO.class},
                "/retire", "mes:pro-edhr-dhr-template:retire");
        assertPostMapping("voidTemplate", new Class<?>[]{MesProEdhrDhrTemplateImpactReqVO.class},
                "/void", "mes:pro-edhr-dhr-template:void");
        assertGetMapping("getImpactPage", new Class<?>[]{MesProEdhrDhrTemplateImpactPageReqVO.class},
                "/impact/page", "mes:pro-edhr-dhr-template:query");
    }

    @Test
    void serviceRequiresBindingReviewSignoffAndImpactBeforeLifecycleChanges() throws Exception {
        String source = read("yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrDhrTemplateServiceImpl.java");

        assertTrue(source.contains("REQUIRED_BINDING_TYPES"));
        assertTrue(source.contains("requireNoIntegrityIssues(template);"));
        assertTrue(source.contains("requireApproved(template);"));
        assertTrue(source.contains("requireSignedOff(template);"));
        assertTrue(source.contains("requireImpactConfirmed(reqVO);"));
        assertTrue(source.contains("throw exception(PRO_EDHR_DHR_TEMPLATE_BINDING_REQUIRED"));
        assertTrue(source.contains("throw exception(PRO_EDHR_DHR_TEMPLATE_REVIEW_REQUIRED"));
        assertTrue(source.contains("throw exception(PRO_EDHR_DHR_TEMPLATE_SIGNOFF_REQUIRED"));
        assertTrue(source.contains("throw exception(PRO_EDHR_DHR_TEMPLATE_IMPACT_REQUIRED"));
        assertFalse(source.contains("DEFAULT_SUCCESS"));
        assertFalse(source.contains("MOCK_SIGNOFF"));
        assertFalse(source.contains("return true;"));
    }

    private static void assertGetMapping(String methodName,
                                         Class<?>[] parameterTypes,
                                         String path,
                                         String permission) throws Exception {
        Method method = MesProEdhrDhrTemplateController.class.getDeclaredMethod(methodName, parameterTypes);
        assertArrayEquals(new String[]{path}, method.getAnnotation(GetMapping.class).value());
        assertEquals("@ss.hasPermission('" + permission + "')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    private static void assertPostMapping(String methodName,
                                          Class<?>[] parameterTypes,
                                          String path,
                                          String permission) throws Exception {
        Method method = MesProEdhrDhrTemplateController.class.getDeclaredMethod(methodName, parameterTypes);
        assertArrayEquals(new String[]{path}, method.getAnnotation(PostMapping.class).value());
        assertEquals("@ss.hasPermission('" + permission + "')",
                method.getAnnotation(PreAuthorize.class).value());
    }

    private static String read(String relativePath) throws Exception {
        Path path = findProjectDir().resolve(relativePath);
        assertTrue(Files.exists(path), relativePath + " must exist");
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Path findProjectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir"));
        return "yudao-module-mes".equals(currentDir.getFileName().toString()) ? currentDir.getParent() : currentDir;
    }
}
