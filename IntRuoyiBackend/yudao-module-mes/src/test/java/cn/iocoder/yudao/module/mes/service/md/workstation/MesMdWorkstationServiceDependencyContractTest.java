package cn.iocoder.yudao.module.mes.service.md.workstation;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MesMdWorkstationServiceDependencyContractTest {

    @Test
    void processServiceDependencyIsLazyToAvoidRawProxyCircularReference() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/mes/service/md/workstation/MesMdWorkstationServiceImpl.java"));

        assertTrue(Pattern.compile("@Resource\\s+@Lazy\\s+private MesProProcessService processService;")
                        .matcher(source).find(),
                "MesMdWorkstationServiceImpl must inject MesProProcessService lazily to avoid raw proxy circular references");
    }
}
