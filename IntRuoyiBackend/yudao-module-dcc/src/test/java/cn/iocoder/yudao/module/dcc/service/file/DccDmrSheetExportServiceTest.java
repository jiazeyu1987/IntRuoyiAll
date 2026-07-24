package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import cn.iocoder.yudao.module.infra.service.file.NasAclReadResult;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasFileReadResult;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigSaveReqVO;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DMR_SHEET_ROOT_CONFIG_MISSING;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.DCC_DMR_SHEET_ROOT_UNAVAILABLE;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_READ_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DccDmrSheetExportServiceTest {

    private static final NasConnectionConfig NAS_CONFIG = new NasConnectionConfig(
            "172.30.30.4", 445, "质量体系文件", "WORKGROUP", "redacted", "redacted");

    @Test
    void exportWorkbook_createsOneSheetPerDmrCategoryWithFileFolder() throws Exception {
        FakeNasBrowserService nasBrowserService = new FakeNasBrowserService()
                .addDir("3.DMR", "01.图纸")
                .addDir("3.DMR", "02.说明书")
                .addFile("3.DMR/01.图纸", "根目录文件.xlsx")
                .addDir("3.DMR/01.图纸", "二级")
                .addDir("3.DMR/01.图纸/二级", "三级")
                .addFile("3.DMR/01.图纸/二级/三级", "A图纸.pdf")
                .addFile("3.DMR/02.说明书", "产品说明书.docx");

        DccDmrSheetExportProperties properties = new DccDmrSheetExportProperties();
        properties.setRootPath("//172.30.30.4/质量体系文件/3.DMR");
        DccDmrSheetExportService service = new DccDmrSheetExportServiceImpl(
                properties, new FakeNasSettingsService(), nasBrowserService);

        byte[] workbookBytes = service.exportWorkbook();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(workbookBytes))) {
            DataFormatter formatter = new DataFormatter();
            assertEquals(2, workbook.getNumberOfSheets());
            assertNotNull(workbook.getSheet("图纸"));
            assertNotNull(workbook.getSheet("说明书"));
            assertEquals("序号", formatter.formatCellValue(workbook.getSheet("图纸").getRow(0).getCell(0)));
            assertEquals("文件名称", formatter.formatCellValue(workbook.getSheet("图纸").getRow(0).getCell(1)));
            assertEquals("所在文件夹", formatter.formatCellValue(workbook.getSheet("图纸").getRow(0).getCell(2)));
            assertEquals("1", formatter.formatCellValue(workbook.getSheet("图纸").getRow(1).getCell(0)));
            assertEquals("根目录文件.xlsx", formatter.formatCellValue(workbook.getSheet("图纸").getRow(1).getCell(1)));
            assertEquals("01.图纸", formatter.formatCellValue(workbook.getSheet("图纸").getRow(1).getCell(2)));
            assertEquals("2", formatter.formatCellValue(workbook.getSheet("图纸").getRow(2).getCell(0)));
            assertEquals("A图纸.pdf", formatter.formatCellValue(workbook.getSheet("图纸").getRow(2).getCell(1)));
            assertEquals("01.图纸/二级/三级", formatter.formatCellValue(workbook.getSheet("图纸").getRow(2).getCell(2)));
            assertEquals("产品说明书.docx", formatter.formatCellValue(workbook.getSheet("说明书").getRow(1).getCell(1)));
            assertEquals("02.说明书", formatter.formatCellValue(workbook.getSheet("说明书").getRow(1).getCell(2)));
        }
    }

    @Test
    void exportWorkbook_skipsAccessDeniedDirectoryAndKeepsReadableFiles() throws Exception {
        FakeNasBrowserService nasBrowserService = new FakeNasBrowserService()
                .addDir("3.DMR", "01.图纸")
                .addFile("3.DMR/01.图纸", "公开图纸.pdf")
                .addDir("3.DMR/01.图纸", "无权限目录")
                .addFile("3.DMR/01.图纸/无权限目录", "不可见图纸.pdf")
                .denyPath("3.DMR/01.图纸/无权限目录");

        DccDmrSheetExportProperties properties = new DccDmrSheetExportProperties();
        properties.setRootPath("3.DMR");
        DccDmrSheetExportService service = new DccDmrSheetExportServiceImpl(
                properties, new FakeNasSettingsService(), nasBrowserService);

        byte[] workbookBytes = service.exportWorkbook();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(workbookBytes))) {
            DataFormatter formatter = new DataFormatter();
            assertEquals(1, workbook.getNumberOfSheets());
            assertNotNull(workbook.getSheet("图纸"));
            assertEquals("公开图纸.pdf", formatter.formatCellValue(workbook.getSheet("图纸").getRow(1).getCell(1)));
            assertEquals("01.图纸", formatter.formatCellValue(workbook.getSheet("图纸").getRow(1).getCell(2)));
            assertEquals(2, workbook.getSheet("图纸").getPhysicalNumberOfRows());
        }
    }

    @Test
    void exportWorkbook_skipsLocalizedAccessDeniedDirectoryAndKeepsReadableFiles() throws Exception {
        FakeNasBrowserService nasBrowserService = new FakeNasBrowserService()
                .addDir("3.DMR", "10.产品技术要求")
                .addFile("3.DMR/10.产品技术要求", "公开技术要求.pdf")
                .addDir("3.DMR/10.产品技术要求", "注册版技术要求")
                .denyPath("3.DMR/10.产品技术要求/注册版技术要求", "拒绝访问: 3.DMR/10.产品技术要求/注册版技术要求");

        DccDmrSheetExportProperties properties = new DccDmrSheetExportProperties();
        properties.setRootPath("3.DMR");
        DccDmrSheetExportService service = new DccDmrSheetExportServiceImpl(
                properties, new FakeNasSettingsService(), nasBrowserService);

        byte[] workbookBytes = service.exportWorkbook();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(workbookBytes))) {
            DataFormatter formatter = new DataFormatter();
            assertEquals(1, workbook.getNumberOfSheets());
            assertNotNull(workbook.getSheet("产品技术要求"));
            assertEquals("公开技术要求.pdf", formatter.formatCellValue(workbook.getSheet("产品技术要求").getRow(1).getCell(1)));
            assertEquals("10.产品技术要求", formatter.formatCellValue(workbook.getSheet("产品技术要求").getRow(1).getCell(2)));
            assertEquals(2, workbook.getSheet("产品技术要求").getPhysicalNumberOfRows());
        }
    }

    @Test
    void exportWorkbook_failsFastWhenRootPathMissing() {
        DccDmrSheetExportProperties properties = new DccDmrSheetExportProperties();
        DccDmrSheetExportService service = new DccDmrSheetExportServiceImpl(
                properties, new FakeNasSettingsService(), new FakeNasBrowserService());

        assertServiceException(service::exportWorkbook, DCC_DMR_SHEET_ROOT_CONFIG_MISSING,
                "yudao.dcc.dmr-sheet.root-path");
    }

    @Test
    void exportWorkbook_failsFastWhenRootDirectoryUnavailable() {
        DccDmrSheetExportProperties properties = new DccDmrSheetExportProperties();
        properties.setRootPath("missing-dmr");
        DccDmrSheetExportService service = new DccDmrSheetExportServiceImpl(
                properties, new FakeNasSettingsService(), new FakeNasBrowserService());

        assertServiceException(service::exportWorkbook, DCC_DMR_SHEET_ROOT_UNAVAILABLE, "missing-dmr");
    }

    private static final class FakeNasSettingsService implements NasSettingsService {

        @Override
        public FileNasConfigRespVO getNasConfig() {
            return new FileNasConfigRespVO();
        }

        @Override
        public void saveNasConfig(FileNasConfigSaveReqVO reqVO) {
        }

        @Override
        public NasConnectionConfig toConnectionConfig(FileNasConfigSaveReqVO reqVO) {
            return NAS_CONFIG;
        }

        @Override
        public NasConnectionConfig getRequiredNasConfig() {
            return NAS_CONFIG;
        }
    }

    private static final class FakeNasBrowserService implements NasBrowserService {

        private final Map<String, List<FileNasListRespVO.Item>> filesByPath = new HashMap<>();
        private final Set<String> deniedPaths = new HashSet<>();
        private final Map<String, String> deniedMessages = new HashMap<>();

        private FakeNasBrowserService addDir(String parentPath, String name) {
            return add(parentPath, name, true);
        }

        private FakeNasBrowserService addFile(String parentPath, String name) {
            return add(parentPath, name, false);
        }

        private FakeNasBrowserService add(String parentPath, String name, boolean directory) {
            String normalizedParentPath = normalize(parentPath);
            String path = normalizedParentPath.isBlank() ? name : normalizedParentPath + "/" + name;
            filesByPath.computeIfAbsent(normalizedParentPath, ignored -> new ArrayList<>())
                    .add(new FileNasListRespVO.Item()
                            .setName(name)
                            .setPath(path)
                            .setDir(directory)
                            .setSize(directory ? 0L : 1L));
            filesByPath.computeIfAbsent(path, ignored -> new ArrayList<>());
            return this;
        }

        private FakeNasBrowserService denyPath(String path) {
            deniedPaths.add(normalize(path));
            return this;
        }

        private FakeNasBrowserService denyPath(String path, String message) {
            String normalizedPath = normalize(path);
            deniedPaths.add(normalizedPath);
            deniedMessages.put(normalizedPath, message);
            return this;
        }

        @Override
        public FileNasListRespVO listFiles(String path) {
            return listFiles(NAS_CONFIG, path);
        }

        @Override
        public FileNasListRespVO listFiles(NasConnectionConfig config, String path) {
            return list(path);
        }

        @Override
        public <T> T executeInSession(NasConnectionConfig config, NasSessionCallback<T> callback) {
            return callback.execute(new FakeNasSessionScope());
        }

        @Override
        public cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigTestRespVO testConnection(
                NasConnectionConfig config) {
            throw new UnsupportedOperationException();
        }

        @Override
        public cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasDirectoryTreeRespVO getDirectoryTree() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NasFileReadResult readFile(String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public NasFileReadResult readFile(NasConnectionConfig config, String path) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeFileTo(String path, OutputStream outputStream) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void writeFileTo(NasConnectionConfig config, String path, OutputStream outputStream) {
            throw new UnsupportedOperationException();
        }

        @Override
        public NasAclReadResult readDirectoryAcl(String path) {
            throw new UnsupportedOperationException();
        }

        private FileNasListRespVO list(String path) {
            String normalizedPath = normalize(path);
            if (deniedPaths.contains(normalizedPath)) {
                throw exception(FILE_NAS_READ_FAILED,
                        deniedMessages.getOrDefault(normalizedPath, "access denied: " + normalizedPath));
            }
            List<FileNasListRespVO.Item> items = filesByPath.get(normalizedPath);
            if (items == null) {
                throw exception(DCC_DMR_SHEET_ROOT_UNAVAILABLE, normalizedPath);
            }
            return new FileNasListRespVO()
                    .setCurrentPath(normalizedPath)
                    .setRootPath(NAS_CONFIG.rootUnc())
                    .setItems(items);
        }

        private static String normalize(String path) {
            return path == null ? "" : path.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
        }

        private final class FakeNasSessionScope implements NasSessionScope {

            @Override
            public FileNasListRespVO listFiles(String path) {
                return list(path);
            }

            @Override
            public NasFileReadResult readFile(String path) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void writeFileTo(String path, OutputStream outputStream) {
                throw new UnsupportedOperationException();
            }

            @Override
            public NasAclReadResult readDirectoryAcl(String path) {
                throw new UnsupportedOperationException();
            }
        }
    }
}
