package cn.iocoder.yudao.module.system.service.codextest;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.dal.dataobject.codextest.CodexTestRunnerSessionDO;
import cn.iocoder.yudao.module.system.dal.mysql.codextest.CodexTestRunnerSessionMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.CODEX_TEST_RUNNER_STARTER_MISSING;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Import(CodexTestRunnerBootstrapServiceImpl.class)
class CodexTestRunnerBootstrapServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CodexTestRunnerBootstrapService codexTestRunnerBootstrapService;
    @Resource
    private CodexTestRunnerSessionMapper codexTestRunnerSessionMapper;

    @Test
    void ensureRunnerAvailable_doesNotStartWrapperWhenHealthyRunnerAlreadyOnline() {
        insertOnlineRunner();
        ReflectionTestUtils.setField(codexTestRunnerBootstrapService, "runnerStarterScript", "");

        assertDoesNotThrow(() -> codexTestRunnerBootstrapService.ensureRunnerAvailable());
    }

    @Test
    void ensureRunnerAvailable_failsFastWhenStarterScriptMissing() {
        ReflectionTestUtils.setField(codexTestRunnerBootstrapService, "runnerOnDemandEnabled", true);
        ReflectionTestUtils.setField(codexTestRunnerBootstrapService, "runnerStarterScript", "");

        assertServiceException(() -> codexTestRunnerBootstrapService.ensureRunnerAvailable(),
                CODEX_TEST_RUNNER_STARTER_MISSING, "yudao.codex-test.runner.on-demand.starter-script");
    }

    @Test
    void ensureRunnerAvailable_startsConfiguredWrapperAndWaitsForRunnerRegistration() throws Exception {
        Path starterScript = Files.createTempFile("codex-runner-bootstrap-test", ".ps1");
        Files.writeString(starterScript,
                "param([Parameter(ValueFromRemainingArguments=$true)][string[]]$Rest)\nStart-Sleep -Milliseconds 500\n",
                StandardCharsets.UTF_8);
        ReflectionTestUtils.setField(codexTestRunnerBootstrapService, "runnerOnDemandEnabled", true);
        ReflectionTestUtils.setField(codexTestRunnerBootstrapService, "runnerStarterScript", starterScript.toString());
        ReflectionTestUtils.setField(codexTestRunnerBootstrapService, "runnerToken", "test-runner-token");
        ReflectionTestUtils.setField(codexTestRunnerBootstrapService, "startupTimeoutSeconds", 3);
        ReflectionTestUtils.setField(codexTestRunnerBootstrapService, "startupPollIntervalMillis", 50);

        Thread registrationThread = new Thread(() -> {
            try {
                Thread.sleep(200);
                insertOnlineRunner();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        registrationThread.start();

        assertDoesNotThrow(() -> codexTestRunnerBootstrapService.ensureRunnerAvailable());
        registrationThread.join();
    }

    private void insertOnlineRunner() {
        CodexTestRunnerSessionDO runner = new CodexTestRunnerSessionDO();
        runner.setRunnerName("local-runner");
        runner.setStatus("ONLINE");
        runner.setCapabilitiesJson("{\"playwright\":true,\"codex\":true}");
        runner.setMaxParallelism(1);
        runner.setLastHeartbeatTime(LocalDateTime.now());
        runner.setCurrentRunningCount(0);
        codexTestRunnerSessionMapper.insert(runner);
    }

}
