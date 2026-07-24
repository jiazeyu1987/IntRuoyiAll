package cn.iocoder.yudao.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BpmModuleEnablementTest {

    @Test
    void serverClasspathShouldContainBpmController() {
        assertDoesNotThrow(() -> Class.forName(
                "cn.iocoder.yudao.module.bpm.controller.admin.task.BpmTaskController"));
    }

}
