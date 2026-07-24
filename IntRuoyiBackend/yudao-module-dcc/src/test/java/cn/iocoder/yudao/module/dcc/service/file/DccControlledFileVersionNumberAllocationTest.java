package cn.iocoder.yudao.module.dcc.service.file;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DccControlledFileVersionNumberAllocationTest {

    @Test
    void submitAndResubmit_shouldRunInsideTransactionForNativeMasterVersionChainGuard() throws NoSuchMethodException {
        Method submit = DccControlledFileWorkflowServiceImpl.class.getMethod("submitControlledFile",
                Long.class, cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO.class);
        Method resubmit = DccControlledFileWorkflowServiceImpl.class.getMethod("resubmitWithdrawnControlledFile",
                Long.class, Long.class);

        assertRollbackForException(submit);
        assertRollbackForException(resubmit);
    }

    @Test
    void workflow_shouldLockDccMasterBeforeValidatingVersionChain() throws Exception {
        String source = Files.readString(sourcePath(
                "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/"
                        + "DccControlledFileWorkflowServiceImpl.java"), StandardCharsets.UTF_8);
        int loadMaster = source.indexOf("DccControlledFileMasterDO master = loadOrCreateMaster");
        int lockMaster = source.indexOf("lockNativeContentMaster(master);");
        int validateVersion = source.indexOf("validateVersionChain(master.getId()");

        assertTrue(loadMaster >= 0, "workflow must load or create native DCC master");
        assertTrue(lockMaster > loadMaster, "workflow must lock native DCC master after resolving it");
        assertTrue(validateVersion > lockMaster, "workflow must validate version chain after native master lock");
        assertTrue(source.contains("selectByIdForUpdate(master.getId())"),
                "DCC version chain guard must use a FOR UPDATE mapper method");
    }

    @Test
    void mapper_shouldExposeForUpdateLockForDccMaster() throws Exception {
        String mapper = Files.readString(sourcePath(
                "yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/"
                        + "DccControlledFileMasterMapper.java"), StandardCharsets.UTF_8);

        assertTrue(mapper.contains("selectByIdForUpdate"), "DCC master mapper must expose row lock method");
        assertTrue(mapper.contains("FOR UPDATE"), "DCC master lock query must use FOR UPDATE");
    }

    private void assertRollbackForException(Method method) {
        Transactional transactional = method.getAnnotation(Transactional.class);
        assertTrue(transactional != null, method.getName() + " must be transactional");
        assertEquals(Exception.class, transactional.rollbackFor()[0]);
    }

    private Path sourcePath(String moduleRelativePath) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path direct = current.resolve(moduleRelativePath);
            if (Files.exists(direct)) {
                return direct;
            }
            Path nestedBackend = current.resolve("ruoyi-vue-pro").resolve(moduleRelativePath);
            if (Files.exists(nestedBackend)) {
                return nestedBackend;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate source file: " + moduleRelativePath);
    }

}
