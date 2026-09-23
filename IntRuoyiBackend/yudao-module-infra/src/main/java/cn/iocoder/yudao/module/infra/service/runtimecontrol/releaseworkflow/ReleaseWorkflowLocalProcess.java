package cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Owns the process tree and pipe lifetime; Windows children never inherit a diagnostic file handle. */
final class ReleaseWorkflowLocalProcess {
    private ReleaseWorkflowLocalProcess() {}

    static Result run(List<String> command, Path directory, Map<String, String> environment,
                      Duration timeout, String errorPrefix) {
        return run(command, directory, environment, timeout, errorPrefix, true);
    }

    static Result run(List<String> command, Path directory, Map<String, String> environment,
                      Duration timeout, String errorPrefix, boolean mergeError) {
        Process process = null;
        Thread reader = null;
        Thread errorReader = null;
        RuntimeException primary = null;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        AtomicReference<IOException> readFailure = new AtomicReference<>();
        try {
            ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(mergeError);
            if (directory != null) builder.directory(directory.toFile());
            builder.environment().putAll(environment);
            process = builder.start();
            Process child = process;
            reader = new Thread(() -> {
                try (var input = child.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (output.size() + count > 8 * 1024 * 1024) throw new IOException("OUTPUT_LIMIT_EXCEEDED");
                        output.write(buffer, 0, count);
                    }
                } catch (IOException error) { readFailure.set(error); }
            }, "release-process-output-" + process.pid());
            reader.setDaemon(true);
            reader.start();
            if (!mergeError) {
                errorReader = new Thread(() -> {
                    try (var input = child.getErrorStream()) {
                        byte[] buffer = new byte[8192];
                        int count;
                        while ((count = input.read(buffer)) != -1) {
                            if (diagnostics.size() + count > 8 * 1024 * 1024) throw new IOException("OUTPUT_LIMIT_EXCEEDED");
                            diagnostics.write(buffer, 0, count);
                        }
                    } catch (IOException error) { readFailure.compareAndSet(null, error); }
                }, "release-process-diagnostics-" + process.pid());
                errorReader.setDaemon(true);
                errorReader.start();
            }
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException(errorPrefix + "_TIMEOUT");
            }
            reader.join(10000);
            if (errorReader != null) errorReader.join(10000);
            if (errorReader != null && errorReader.isAlive()) throw new IllegalStateException(errorPrefix + "_OUTPUT_UNCONFIRMED");
            if (reader.isAlive()) throw new IllegalStateException(errorPrefix + "_OUTPUT_UNCONFIRMED");
            if (readFailure.get() != null) throw readFailure.get();
            String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(output.toByteArray())).toString();
            String diagnosticText = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(diagnostics.toByteArray())).toString();
            return new Result(process.exitValue(), text, diagnosticText);
        } catch (IOException error) {
            primary = new IllegalStateException(errorPrefix + "_IO_FAILED", error);
            throw primary;
        } catch (InterruptedException error) {
            primary = new IllegalStateException(errorPrefix + "_INTERRUPTED", error);
            Thread.currentThread().interrupt();
            throw primary;
        } catch (RuntimeException error) {
            primary = error;
            throw error;
        } finally {
            boolean interrupted = Thread.interrupted();
            try {
                if (process != null) {
                    if (process.isAlive() || reader != null && reader.isAlive()
                            || errorReader != null && errorReader.isAlive()) terminate(process, errorPrefix);
                    process.getOutputStream().close();
                    process.getInputStream().close();
                    process.getErrorStream().close();
                }
                if (reader != null) {
                    reader.join(10000);
                    if (reader.isAlive()) throw new IllegalStateException(errorPrefix + "_READER_TERMINATION_UNCONFIRMED");
                }
                if (errorReader != null) {
                    errorReader.join(10000);
                    if (errorReader.isAlive()) throw new IllegalStateException(errorPrefix + "_READER_TERMINATION_UNCONFIRMED");
                }
            } catch (IOException | InterruptedException | RuntimeException cleanupFailure) {
                if (cleanupFailure instanceof InterruptedException) interrupted = true;
                if (primary != null) primary.addSuppressed(cleanupFailure);
                else throw new IllegalStateException(errorPrefix + "_RESOURCE_CLOSE_FAILED", cleanupFailure);
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
            }
        }
    }

    private static void terminate(Process process, String errorPrefix) throws InterruptedException {
        List<ProcessHandle> owned = new ArrayList<>(process.descendants().toList());
        owned.add(process.toHandle());
        process.destroyForcibly();
        owned.forEach(handle -> { if (handle.isAlive()) handle.destroyForcibly(); });
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (owned.stream().anyMatch(ProcessHandle::isAlive)) {
            if (System.nanoTime() >= deadline) throw new IllegalStateException(errorPrefix + "_TREE_TERMINATION_UNCONFIRMED");
            Thread.sleep(25);
        }
        if (!process.waitFor(10, TimeUnit.SECONDS)) throw new IllegalStateException(errorPrefix + "_REAPER_UNCONFIRMED");
    }

    record Result(int exitCode, String output, String diagnostics) {
        Result(int exitCode, String output) { this(exitCode, output, ""); }
    }
}
