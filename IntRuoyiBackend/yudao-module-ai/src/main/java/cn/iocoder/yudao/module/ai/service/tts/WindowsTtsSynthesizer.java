package cn.iocoder.yudao.module.ai.service.tts;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.ai.framework.ai.config.YudaoAiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class WindowsTtsSynthesizer {

    public byte[] synthesize(String text, YudaoAiProperties.Tts tts) {
        Path audioPath = null;
        try {
            audioPath = Files.createTempFile("intruoyi-tts-", ".wav");
            Path scriptPath = Files.createTempFile("intruoyi-tts-", ".ps1");
            Files.writeString(scriptPath, buildPowerShellScript(tts, audioPath), StandardCharsets.UTF_8);
            runPowerShell(scriptPath, text, tts != null ? tts.getTimeoutMs() : null);
            if (!Files.exists(audioPath)) {
                throw new IllegalStateException("tts_output_file_missing");
            }
            return Files.readAllBytes(audioPath);
        } catch (IOException ex) {
            throw new IllegalStateException("tts_file_io_failed", ex);
        } finally {
            deleteIfExists(audioPath);
        }
    }

    private static String buildPowerShellScript(YudaoAiProperties.Tts tts, Path audioPath) {
        String voice = escapePowerShellString(tts != null ? tts.getVoice() : "");
        int rate = tts != null && tts.getRate() != null ? tts.getRate() : 0;
        int volume = tts != null && tts.getVolume() != null ? tts.getVolume() : 100;
        return "param([string]$Text)\n"
                + "$ErrorActionPreference = 'Stop'\n"
                + "Add-Type -AssemblyName System.Speech\n"
                + "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer\n"
                + "$synth.Rate = " + Math.max(-10, Math.min(rate, 10)) + "\n"
                + "$synth.Volume = " + Math.max(0, Math.min(volume, 100)) + "\n"
                + (StrUtil.isNotBlank(voice) ? "$synth.SelectVoice('" + voice + "')\n" : "")
                + "$synth.SetOutputToWaveFile('" + escapePowerShellString(audioPath.toString()) + "')\n"
                + "$synth.Speak($Text)\n"
                + "$synth.Dispose()\n";
    }

    private static void runPowerShell(Path scriptPath, String text, Long timeoutMs) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("powershell");
        command.add("-NoProfile");
        command.add("-ExecutionPolicy");
        command.add("Bypass");
        command.add("-File");
        command.add(scriptPath.toString());
        command.add(text);
        ProcessBuilder builder = new ProcessBuilder(command);
        Process process = builder.start();
        long waitMs = timeoutMs != null ? Math.max(1000L, timeoutMs) : 30000L;
        try {
            boolean finished = process.waitFor(waitMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("tts_process_timeout");
            }
            if (process.exitValue() != 0) {
                String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException(StrUtil.blankToDefault(error, "tts_process_failed"));
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("tts_process_interrupted", ex);
        } finally {
            deleteIfExists(scriptPath);
        }
    }

    private static String escapePowerShellString(String input) {
        return String.valueOf(input).replace("'", "''");
    }

    private static void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("[deleteIfExists][Failed to delete temp file][path={}]", path, ex);
        }
    }

}
