package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileNasTransferRespVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DccControlledFileNasTransferFailureReportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void write_generatesMarkdownWithFailureRows() throws Exception {
        DccControlledFileNasTransferFailureReportService service =
                new DccControlledFileNasTransferFailureReportService() {
                    @Override
                    protected Path resolveReportDirectory() {
                        return tempDir;
                    }
                };
        DccControlledFileNasTransferReqVO reqVO = new DccControlledFileNasTransferReqVO();
        reqVO.setSelectedNasPaths(List.of("1. QMS documents/PD可编辑"));
        reqVO.setTemplateCategoryId(900298L);
        reqVO.setEffectiveDate(LocalDate.of(2026, 5, 23));
        DccControlledFileNasTransferRespVO respVO = new DccControlledFileNasTransferRespVO();
        respVO.setCreatedFileCount(2);
        respVO.setFailedFileCount(1);
        DccControlledFileNasTransferRespVO.FailureItem failure = new DccControlledFileNasTransferRespVO.FailureItem();
        failure.setNasPath("1. QMS documents/PD可编辑/A.docx");
        failure.setStage("submit");
        failure.setReason("file number conflict");
        respVO.getFailures().add(failure);

        DccControlledFileNasTransferFailureReportService.FailureReport report = service.write(reqVO, respVO);

        Path reportPath = Path.of(report.path());
        assertTrue(Files.exists(reportPath));
        String content = Files.readString(reportPath);
        assertTrue(content.contains("# NAS 转移失败报告"));
        assertTrue(content.contains("1. QMS documents/PD可编辑/A.docx"));
        assertTrue(content.contains("file number conflict"));
    }
}
