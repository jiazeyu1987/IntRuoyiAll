package cn.iocoder.yudao.module.mes.service.md.importer;

import cn.iocoder.yudao.module.mes.controller.admin.dv.machinery.vo.MesDvMachineryImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.client.vo.MesMdClientImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.item.vo.MesMdItemImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.vendor.vo.MesMdVendorImportExcelVO;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesMasterDataExcelImportValidationContractTest {

    @Test
    void importVosDeclareRequiredAndFormatValidationForSharedExcelRead() throws Exception {
        assertFieldAnnotated(MesMdItemImportExcelVO.class, "code", "NotBlank");
        assertFieldAnnotated(MesMdItemImportExcelVO.class, "name", "NotBlank");
        assertFieldAnnotated(MesMdItemImportExcelVO.class, "unitMeasureCode", "NotBlank");
        assertFieldAnnotated(MesMdItemImportExcelVO.class, "itemTypeId", "NotNull");
        assertFieldAnnotated(MesMdItemImportExcelVO.class, "minStock", "DecimalMin");
        assertFieldAnnotated(MesMdItemImportExcelVO.class, "maxStock", "DecimalMin");

        assertFieldAnnotated(MesMdVendorImportExcelVO.class, "code", "NotBlank");
        assertFieldAnnotated(MesMdVendorImportExcelVO.class, "name", "NotBlank");
        assertFieldAnnotated(MesMdVendorImportExcelVO.class, "email", "Email");
        assertFieldAnnotated(MesMdVendorImportExcelVO.class, "contact1Email", "Email");
        assertFieldAnnotated(MesMdVendorImportExcelVO.class, "contact2Email", "Email");

        assertFieldAnnotated(MesMdClientImportExcelVO.class, "code", "NotBlank");
        assertFieldAnnotated(MesMdClientImportExcelVO.class, "name", "NotBlank");
        assertFieldAnnotated(MesMdClientImportExcelVO.class, "type", "NotNull");
        assertFieldAnnotated(MesMdClientImportExcelVO.class, "email", "Email");
        assertFieldAnnotated(MesMdClientImportExcelVO.class, "contact1Email", "Email");
        assertFieldAnnotated(MesMdClientImportExcelVO.class, "contact2Email", "Email");

        assertFieldAnnotated(MesDvMachineryImportExcelVO.class, "code", "NotBlank");
        assertFieldAnnotated(MesDvMachineryImportExcelVO.class, "name", "NotBlank");
        assertFieldAnnotated(MesDvMachineryImportExcelVO.class, "machineryTypeCode", "NotBlank");
        assertFieldAnnotated(MesDvMachineryImportExcelVO.class, "workshopCode", "NotBlank");
        assertFieldAnnotated(MesDvMachineryImportExcelVO.class, "standardHourlyCapacity", "DecimalMin");
    }

    @Test
    void importServicesRejectDuplicateCodesInsideSameWorkbook() throws Exception {
        assertSourceContains("src/main/java/cn/iocoder/yudao/module/mes/service/md/item/MesMdItemServiceImpl.java",
                "导入文件中物料编码重复");
        assertSourceContains("src/main/java/cn/iocoder/yudao/module/mes/service/md/vendor/MesMdVendorServiceImpl.java",
                "导入文件中供应商编码重复");
        assertSourceContains("src/main/java/cn/iocoder/yudao/module/mes/service/md/client/MesMdClientServiceImpl.java",
                "导入文件中客户编码重复");
        assertSourceContains("src/main/java/cn/iocoder/yudao/module/mes/service/dv/machinery/MesDvMachineryServiceImpl.java",
                "导入文件中设备编码重复");
    }

    private static void assertFieldAnnotated(Class<?> type, String fieldName, String annotationSimpleName)
            throws Exception {
        Field field = type.getDeclaredField(fieldName);
        for (Annotation annotation : field.getAnnotations()) {
            if (annotation.annotationType().getSimpleName().equals(annotationSimpleName)) {
                return;
            }
        }
        throw new AssertionError(type.getSimpleName() + "." + fieldName + " must declare @"
                + annotationSimpleName);
    }

    private static void assertSourceContains(String relativePath, String expected) throws Exception {
        String source = Files.readString(Path.of(relativePath));
        assertTrue(source.contains(expected), relativePath + " must contain: " + expected);
    }
}
