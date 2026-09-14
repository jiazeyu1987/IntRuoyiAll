package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_READ_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class NasRecursiveScanServiceTest extends BaseMockitoUnitTest {

    @Mock
    private NasBrowserService nasBrowserService;
    @InjectMocks
    private NasRecursiveScanServiceImpl scanService;

    @Test
    void scan_skipsAccessDeniedChildDirectoryAndContinuesSiblingFiles() {
        NasConnectionConfig config = new NasConnectionConfig("nas.local", 445, "share", "", "user", "pwd");
        when(nasBrowserService.executeInSession(any(), any())).thenAnswer(invocation -> {
            NasBrowserService.NasSessionCallback<Void> callback = invocation.getArgument(1);
            return callback.execute(new NasBrowserService.NasSessionScope() {
                @Override
                public FileNasListRespVO listFiles(String path) {
                    if ("1. QMS documents/blocked".equals(path)) {
                        throw exception(FILE_NAS_READ_FAILED, "access denied: " + path);
                    }
                    if ("1. QMS documents".equals(path)) {
                        return list(path,
                                dir("blocked", "1. QMS documents/blocked"),
                                file("visible.pdf", "1. QMS documents/visible.pdf", false, true));
                    }
                    throw new AssertionError("unexpected scan path: " + path);
                }

                @Override
                public NasFileReadResult readFile(String path) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void writeFileTo(String path, java.io.OutputStream outputStream) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public NasAclReadResult readDirectoryAcl(String path) {
                    throw new UnsupportedOperationException();
                }
            });
        });
        List<String> files = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        scanService.scan(config, List.of("1. QMS documents"), new NasRecursiveScanHandler() {
            @Override
            public void onCurrentDirectory(String path) {
            }

            @Override
            public void onFile(NasRecursiveScannedFile file) {
                files.add(file.path());
            }

            @Override
            public void onSkippedDirectory(NasRecursiveSkippedDirectory directory) {
                skipped.add(directory.path() + ":" + directory.reason());
            }
        });

        assertEquals(List.of("1. QMS documents/visible.pdf"), files);
        assertEquals(List.of("1. QMS documents/blocked:ACCESS_DENIED"), skipped);
    }

    @Test
    void scan_rootAccessDeniedFailsFast() {
        NasConnectionConfig config = new NasConnectionConfig("nas.local", 445, "share", "", "user", "pwd");
        when(nasBrowserService.executeInSession(any(), any())).thenAnswer(invocation -> {
            NasBrowserService.NasSessionCallback<Void> callback = invocation.getArgument(1);
            return callback.execute(new NasBrowserService.NasSessionScope() {
                @Override
                public FileNasListRespVO listFiles(String path) {
                    throw exception(FILE_NAS_READ_FAILED, "access denied: " + path);
                }

                @Override
                public NasFileReadResult readFile(String path) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void writeFileTo(String path, java.io.OutputStream outputStream) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public NasAclReadResult readDirectoryAcl(String path) {
                    throw new UnsupportedOperationException();
                }
            });
        });

        ServiceException ex = assertThrows(ServiceException.class,
                () -> scanService.scan(config, List.of("1. QMS documents"), noopHandler()));
        assertEquals(FILE_NAS_READ_FAILED.getCode(), ex.getCode());
    }

    private NasRecursiveScanHandler noopHandler() {
        return new NasRecursiveScanHandler() {
            @Override
            public void onCurrentDirectory(String path) {
            }

            @Override
            public void onFile(NasRecursiveScannedFile file) {
            }

            @Override
            public void onSkippedDirectory(NasRecursiveSkippedDirectory directory) {
            }
        };
    }

    private static FileNasListRespVO list(String currentPath, FileNasListRespVO.Item... items) {
        return new FileNasListRespVO()
                .setCurrentPath(currentPath)
                .setItems(List.of(items));
    }

    private static FileNasListRespVO.Item dir(String name, String path) {
        return new FileNasListRespVO.Item()
                .setName(name)
                .setPath(path)
                .setDir(true);
    }

    private static FileNasListRespVO.Item file(String name, String path, boolean hidden, boolean system) {
        return new FileNasListRespVO.Item()
                .setName(name)
                .setPath(path)
                .setDir(false)
                .setHidden(hidden)
                .setSystem(system)
                .setSize(128L)
                .setModifiedAt(123456789L);
    }
}
