package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.EdhrBatchExecutionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProEdhrBatchExecutionRouteVersionFreezeTest {

    private static final Path SERVICE = Path.of(
            "src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/"
                    + "MesProEdhrBatchExecutionServiceImpl.java");

    @Test
    void batchExecutionFreezesRouteVersionAndSnapshotAtCreation() throws Exception {
        String source = Files.readString(SERVICE, StandardCharsets.UTF_8);

        assertHasFields(MesProEdhrBatchExecutionDO.class,
                "routeVersionId", "routeVersionNo", "routeSnapshotJson");
        assertHasFields(EdhrBatchExecutionRespVO.class,
                "routeVersionId", "routeVersionNo");
        assertTrue(source.contains("setRouteVersionId(activeRouteVersion.getId())"),
                "eDHR 批次创建必须冻结创建时 active routeVersionId。");
        assertTrue(source.contains("setRouteVersionNo(activeRouteVersion.getVersionNo())"),
                "eDHR 批次创建必须冻结创建时 routeVersionNo。");
        assertTrue(source.contains("setRouteSnapshotJson(activeRouteVersion.getRouteSnapshotJson())"),
                "eDHR 批次创建必须冻结创建时路线快照。");
        assertTrue(source.contains("setRouteVersionId(latest.getRouteVersionId())"),
                "eDHR 批次响应必须从批次持久化快照读取 routeVersionId，不能回查当前 active。");
    }

    private static void assertHasFields(Class<?> type, String... fieldNames) {
        for (String fieldName : fieldNames) {
            assertDoesNotThrow(() -> declaredField(type, fieldName),
                    () -> "Missing field " + type.getSimpleName() + "." + fieldName);
        }
    }

    private static Field declaredField(Class<?> type, String fieldName) throws NoSuchFieldException {
        return type.getDeclaredField(fieldName);
    }
}
