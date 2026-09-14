package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;

class MesProEdhrBatchVoidApprovalDependencyContractTest {

    @Test
    void batchVoidExecutorShouldNotDependOnRecordChangeSubmitFacade() {
        for (Constructor<?> constructor : MesProEdhrBatchVoidFormEffectExecutor.class.getDeclaredConstructors()) {
            assertFalse(Arrays.asList(constructor.getParameterTypes()).contains(MesProEdhrRecordChangeService.class),
                    "EDHR_BATCH_VOID executor must use a domain effect service, not the approval submit facade");
        }
    }
}
