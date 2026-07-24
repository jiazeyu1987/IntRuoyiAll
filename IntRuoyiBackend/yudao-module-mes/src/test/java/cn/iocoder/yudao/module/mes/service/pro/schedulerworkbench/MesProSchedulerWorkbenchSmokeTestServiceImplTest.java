package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchSmokeTestStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.schedulerworkbench.vo.MesProSchedulerWorkbenchSmokeTestStatusRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_CONFIG_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_RUNNING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_STOP_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProSchedulerWorkbenchSmokeTestServiceImplTest {

    @TempDir
    private Path tempDir;

    private FakeProcessController processController;
    private MesProSchedulerWorkbenchSmokeTestServiceImpl service;

    @BeforeEach
    void setUp() {
        processController = new FakeProcessController();
        service = new MesProSchedulerWorkbenchSmokeTestServiceImpl(
                processController,
                () -> "Windows 11",
                Clock.fixed(Instant.parse("2026-06-16T08:30:00Z"), ZoneId.of("UTC")));
        service.setFrontendDirectory(tempDir.toString());
        service.setScriptName("e2e:mes:smart-scheduling-smoke");
    }

    @Test
    void start_shouldUseNpmCmdOnWindowsAndReturnRunningStatus() throws IOException {
        Files.writeString(tempDir.resolve("package.json"), "{\"scripts\":{\"e2e:mes:smart-scheduling-smoke\":\"node test.js\"}}");
        processController.nextProcess = new FakeSmokeProcess(12345L, true);

        MesProSchedulerWorkbenchSmokeTestStatusRespVO status = service.start(new MesProSchedulerWorkbenchSmokeTestStartReqVO());

        assertEquals("RUNNING", status.getStatus());
        assertTrue(status.getRunning());
        assertFalse(status.getFeedbackApprovalEnabled());
        assertEquals(12345L, status.getPid());
        assertEquals("2026-06-16T08:30:00Z", status.getStartedAt().toString());
        assertEquals(List.of("npm.cmd", "run", "e2e:mes:smart-scheduling-smoke"), processController.startedCommand);
        assertEquals(tempDir.toFile(), processController.startedDirectory);
        assertEquals("ATTRIBUTE", processController.startedEnvironment.get("MES_SMOKE_STOP_AFTER_STAGE"));
    }

    @Test
    void start_shouldEnableFeedbackApprovalWhenRequested() throws IOException {
        Files.writeString(tempDir.resolve("package.json"), "{\"scripts\":{\"e2e:mes:smart-scheduling-smoke\":\"node test.js\"}}");
        processController.nextProcess = new FakeSmokeProcess(12347L, true);
        MesProSchedulerWorkbenchSmokeTestStartReqVO reqVO = new MesProSchedulerWorkbenchSmokeTestStartReqVO();
        reqVO.setFeedbackApprovalEnabled(true);

        MesProSchedulerWorkbenchSmokeTestStatusRespVO status = service.start(reqVO);

        assertTrue(status.getFeedbackApprovalEnabled());
        assertEquals("", processController.startedEnvironment.get("MES_SMOKE_STOP_AFTER_STAGE"));
    }

    @Test
    void start_shouldUseNpmOnLinux() throws IOException {
        service = new MesProSchedulerWorkbenchSmokeTestServiceImpl(
                processController,
                () -> "Linux",
                Clock.fixed(Instant.parse("2026-06-16T08:30:00Z"), ZoneId.of("UTC")));
        service.setFrontendDirectory(tempDir.toString());
        service.setScriptName("e2e:mes:smart-scheduling-smoke");
        Files.writeString(tempDir.resolve("package.json"), "{\"scripts\":{\"e2e:mes:smart-scheduling-smoke\":\"node test.js\"}}");
        processController.nextProcess = new FakeSmokeProcess(12346L, true);

        service.start(new MesProSchedulerWorkbenchSmokeTestStartReqVO());

        assertEquals(List.of("npm", "run", "e2e:mes:smart-scheduling-smoke"), processController.startedCommand);
    }

    @Test
    void start_shouldRejectDuplicateRunningProcess() throws IOException {
        Files.writeString(tempDir.resolve("package.json"), "{\"scripts\":{\"e2e:mes:smart-scheduling-smoke\":\"node test.js\"}}");
        processController.nextProcess = new FakeSmokeProcess(12345L, true);
        service.start(new MesProSchedulerWorkbenchSmokeTestStartReqVO());

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.start(new MesProSchedulerWorkbenchSmokeTestStartReqVO()));

        assertEquals(PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_RUNNING.getCode(), exception.getCode());
    }

    @Test
    void stop_shouldStopRunningProcessAndReturnStoppedStatus() throws IOException {
        Files.writeString(tempDir.resolve("package.json"), "{\"scripts\":{\"e2e:mes:smart-scheduling-smoke\":\"node test.js\"}}");
        FakeSmokeProcess process = new FakeSmokeProcess(12345L, true);
        processController.nextProcess = process;
        service.start(new MesProSchedulerWorkbenchSmokeTestStartReqVO());

        MesProSchedulerWorkbenchSmokeTestStatusRespVO status = service.stop();

        assertEquals("STOPPED", status.getStatus());
        assertFalse(status.getRunning());
        assertEquals(12345L, status.getPid());
        assertTrue(process.stopped);
        assertEquals(12345L, processController.stoppedPid);
    }

    @Test
    void stop_shouldFailFastWhenProcessIsStillAliveAfterStopRequest() throws IOException {
        Files.writeString(tempDir.resolve("package.json"), "{\"scripts\":{\"e2e:mes:smart-scheduling-smoke\":\"node test.js\"}}");
        FakeSmokeProcess process = new FakeSmokeProcess(12345L, true, false);
        processController.nextProcess = process;
        service.start(new MesProSchedulerWorkbenchSmokeTestStartReqVO());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.stop());

        assertEquals(PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_STOP_FAILED.getCode(), exception.getCode());
        assertTrue(service.getStatus().getRunning());
        assertEquals("RUNNING", service.getStatus().getStatus());
    }

    @Test
    void start_shouldFailFastWhenPackageScriptMissing() throws IOException {
        Files.writeString(tempDir.resolve("package.json"), "{\"scripts\":{\"other\":\"node test.js\"}}");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.start(new MesProSchedulerWorkbenchSmokeTestStartReqVO()));

        assertEquals(PRO_SCHEDULER_WORKBENCH_SMOKE_TEST_CONFIG_REQUIRED.getCode(), exception.getCode());
        assertTrue(exception.getMessage().contains("e2e:mes:smart-scheduling-smoke"));
        assertFalse(processController.started);
    }

    static class FakeProcessController implements MesProSchedulerWorkbenchSmokeProcessController {

        private FakeSmokeProcess nextProcess;
        private boolean started;
        private List<String> startedCommand;
        private File startedDirectory;
        private File startedLogFile;
        private Map<String, String> startedEnvironment;
        private Long stoppedPid;

        @Override
        public MesProSchedulerWorkbenchSmokeProcess start(List<String> command, File directory, Map<String, String> environment,
                                                          File logFile)
                throws IOException {
            started = true;
            startedCommand = new ArrayList<>(command);
            startedDirectory = directory;
            startedLogFile = logFile;
            startedEnvironment = Map.copyOf(environment);
            assertNotNull(environment);
            return nextProcess;
        }

        @Override
        public void stop(MesProSchedulerWorkbenchSmokeProcess process, boolean windows) {
            stoppedPid = process.pid();
            process.stop();
        }
    }

    static class FakeSmokeProcess implements MesProSchedulerWorkbenchSmokeProcess {

        private final long pid;
        private boolean alive;
        private boolean stopped;

        private final boolean stopChangesAlive;

        FakeSmokeProcess(long pid, boolean alive) {
            this(pid, alive, true);
        }

        FakeSmokeProcess(long pid, boolean alive, boolean stopChangesAlive) {
            this.pid = pid;
            this.alive = alive;
            this.stopChangesAlive = stopChangesAlive;
        }

        @Override
        public long pid() {
            return pid;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public Integer exitCode() {
            return alive ? null : 0;
        }

        @Override
        public void stop() {
            stopped = true;
            if (stopChangesAlive) {
                alive = false;
            }
        }
    }

}
