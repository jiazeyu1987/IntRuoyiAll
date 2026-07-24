package cn.iocoder.yudao.module.mes.service.wm.materialstock;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesWmMaterialStockServiceDependencyContractTest {

    @Test
    void itemServiceDependencyIsLazyToAvoidRawProxyCircularReference() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/wm/materialstock/MesWmMaterialStockServiceImpl.java"));

        assertTrue(Pattern.compile("@Resource\\s+@Lazy\\s+private MesMdItemService itemService;")
                        .matcher(source).find(),
                "MesWmMaterialStockServiceImpl must inject MesMdItemService lazily to avoid raw proxy circular references");
    }
}
