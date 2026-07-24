package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MesProSchedulerWorkbenchSmokeProcessControllerImpl implements MesProSchedulerWorkbenchSmokeProcessController {

    private static final long STOP_WAIT_SECONDS = 10L;

    @Override
    public MesProSchedulerWorkbenchSmokeProcess start(List<String> command, File directory, Map<String, String> environment,
                                                      File logFile)
            throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(directory);
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
        if (environment != null && !environment.isEmpty()) {
            builder.environment().putAll(environment);
        }
        Process process = builder.start();
        return new JavaSmokeProcess(process);
    }

    @Override
    public void stop(MesProSchedulerWorkbenchSmokeProcess process, boolean windows) {
        process.stop();
    }

    private static class JavaSmokeProcess implements MesProSchedulerWorkbenchSmokeProcess {

        private final Process process;

        JavaSmokeProcess(Process process) {
            this.process = process;
        }

        @Override
        public long pid() {
            return process.pid();
        }

        @Override
        public boolean isAlive() {
            return process.isAlive();
        }

        @Override
        public Integer exitCode() {
            return process.isAlive() ? null : process.exitValue();
        }

        @Override
        public void stop() {
            ProcessHandle handle = process.toHandle();
            List<ProcessHandle> descendants = handle.descendants()
                    .sorted(Comparator.comparingLong(ProcessHandle::pid).reversed())
                    .toList();
            descendants.forEach(ProcessHandle::destroy);
            handle.destroy();
            waitForExit();
            if (process.isAlive()) {
                descendants.forEach(ProcessHandle::destroyForcibly);
                handle.destroyForcibly();
                waitForExit();
            }
            if (process.isAlive()) {
                log.warn("Smart scheduling smoke process still alive after stop request, pid={}", pid());
            }
        }

        private void waitForExit() {
            try {
                process.waitFor(STOP_WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
