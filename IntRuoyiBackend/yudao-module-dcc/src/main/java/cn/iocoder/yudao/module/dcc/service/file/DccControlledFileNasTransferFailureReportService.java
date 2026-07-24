package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferRespVO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DccControlledFileNasTransferFailureReportService {

    private static final DateTimeFormatter FILE_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FailureReport write(DccControlledFileNasTransferReqVO reqVO,
                               DccControlledFileNasTransferRespVO respVO) throws IOException {
        LocalDateTime now = LocalDateTime.now();
        Path directory = resolveReportDirectory();
        Files.createDirectories(directory);
        Path reportPath = directory.resolve("nas-transfer-failures-" + FILE_NAME_FORMATTER.format(now) + ".md");
        Files.writeString(reportPath, buildMarkdown(reqVO, respVO, now), StandardCharsets.UTF_8);
        return new FailureReport(reportPath.toAbsolutePath().toString(), DISPLAY_FORMATTER.format(now));
    }

    protected Path resolveReportDirectory() {
        return Path.of(System.getProperty("user.dir"), "output", "runtime", "doc", "nas-transfer-failure-reports");
    }

    private String buildMarkdown(DccControlledFileNasTransferReqVO reqVO,
                                 DccControlledFileNasTransferRespVO respVO,
                                 LocalDateTime generatedAt) {
        StringBuilder builder = new StringBuilder();
        builder.append("# NAS 转移失败报告").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("- 生成时间：").append(DISPLAY_FORMATTER.format(generatedAt)).append(System.lineSeparator());
        builder.append("- 已选目录：").append(String.join("、", safeList(reqVO.getSelectedNasPaths()))).append(System.lineSeparator());
        builder.append("- 模板类别ID：").append(reqVO.getTemplateCategoryId()).append(System.lineSeparator());
        builder.append("- 生效日期：").append(reqVO.getEffectiveDate()).append(System.lineSeparator());
        builder.append("- 成功文件数：").append(respVO.getCreatedFileCount()).append(System.lineSeparator());
        builder.append("- 失败文件数：").append(respVO.getFailedFileCount()).append(System.lineSeparator())
                .append(System.lineSeparator());
        builder.append("## 失败明细").append(System.lineSeparator()).append(System.lineSeparator());
        builder.append("| NAS路径 | 阶段 | 原因 |").append(System.lineSeparator());
        builder.append("| --- | --- | --- |").append(System.lineSeparator());
        for (DccControlledFileNasTransferRespVO.FailureItem failure : respVO.getFailures()) {
            builder.append("| ")
                    .append(escapeCell(failure.getNasPath()))
                    .append(" | ")
                    .append(escapeCell(failure.getStage()))
                    .append(" | ")
                    .append(escapeCell(failure.getReason()))
                    .append(" |")
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String escapeCell(String value) {
        return StrUtil.nullToDefault(value, "").replace("|", "\\|").replace(System.lineSeparator(), "<br/>");
    }

    public record FailureReport(String path, String generatedAt) {
    }
}
