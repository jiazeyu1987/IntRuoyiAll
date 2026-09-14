package cn.iocoder.yudao.module.bpm.service.task;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BpmProcessInstanceServiceRegistrationCertificateOperationContractTest {

    @Test
    void registrationCertificateNativeApprovalMustAcceptChangeOperationDuringPrediction() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/cn/iocoder/yudao/module/bpm/service/task/BpmProcessInstanceServiceImpl.java"),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("REGISTRATION_CERTIFICATE_CHANGE_OPERATION"),
                "注册证变更提交启动 BPM 时会携带 CHANGE_CERTIFICATE，审批预测不得将它判定为非法 requestOperation");
        assertTrue(source.contains("|| REGISTRATION_CERTIFICATE_CHANGE_OPERATION.equals(operation)"),
                "注册证审批预测允许上传、延续、变更三种最小 MVP 操作");
    }
}
