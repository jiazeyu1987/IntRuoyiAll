package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.util.AssertUtils;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigTestRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasDirectoryTreeRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.msdtyp.ACL;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msdtyp.FileTime;
import com.hierynomus.msdtyp.SID;
import com.hierynomus.msdtyp.SecurityDescriptor;
import com.hierynomus.msdtyp.ace.ACE;
import com.hierynomus.msdtyp.ace.AceFlags;
import com.hierynomus.msdtyp.ace.AceTypes;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_ACL_READ_FAILED;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_AUTH_FAILED;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_PATH_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NasBrowserServiceImplTest {

    private static final String OWNER_SID = "S-1-5-21-100-200-300-500";
    private static final String GROUP_SID = "S-1-5-21-100-200-300-513";
    private static final String ALLOW_SID = "S-1-5-21-100-200-300-1101";
    private static final String DENY_SID = "S-1-5-21-100-200-300-1102";

    @Test
    void testListFiles_rootSuccess() {
        NasSettingsService nasSettingsService = savedConfigService();
        NasBrowserServiceImpl service = new NasBrowserServiceImpl(
                nasSettingsService,
                config -> new FakeNasSession(List.of(
                        new FileNasListRespVO.Item().setName("b.txt").setPath("b.txt").setDir(false).setSize(8L).setModifiedAt(2L),
                        new FileNasListRespVO.Item().setName("A").setPath("A").setDir(true).setSize(0L).setModifiedAt(1L)
                ))
        );

        FileNasListRespVO result = service.listFiles("");

        assertEquals("", result.getCurrentPath());
        assertEquals(null, result.getParentPath());
        assertEquals("\\\\172.30.30.4\\it共享", result.getRootPath());
        assertEquals(List.of("A", "b.txt"), result.getItems().stream().map(FileNasListRespVO.Item::getName).toList());
        assertEquals(List.of(true, false), result.getItems().stream().map(FileNasListRespVO.Item::getDir).toList());
    }

    @Test
    void testListFiles_subDirectoryNormalizesPath() {
        final String[] captured = new String[1];
        NasSettingsService nasSettingsService = savedConfigService();
        NasBrowserServiceImpl service = new NasBrowserServiceImpl(
                nasSettingsService,
                config -> new NasBrowserServiceImpl.NasSession() {
                    @Override
                    public List<FileNasListRespVO.Item> list(String normalizedRelativePath) {
                        captured[0] = normalizedRelativePath;
                        return List.of();
                    }

                    @Override
                    public NasFileReadResult readFile(String normalizedRelativePath) {
                        throw new UnsupportedOperationException();
                    }

                    public SecurityDescriptor readDirectoryAcl(String normalizedRelativePath) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void close() {
                    }
                }
        );

        FileNasListRespVO result = service.listFiles("\\QMS\\.\\1.QMS documents\\..\\2.DHF\\");

        assertEquals("QMS/2.DHF", captured[0]);
        assertEquals("QMS/2.DHF", result.getCurrentPath());
        assertEquals("QMS", result.getParentPath());
    }

    @Test
    void testListFiles_preservesTrailingNonBreakingSpaceWithinPathSegment() {
        final String[] captured = new String[1];
        String path = "3.DMR/01.图纸/02配件图纸/04 输注类_配件/按压式球囊扩充压力泵 IDI\u00A0";
        NasSettingsService nasSettingsService = savedConfigService();
        NasBrowserServiceImpl service = new NasBrowserServiceImpl(
                nasSettingsService,
                config -> new NasBrowserServiceImpl.NasSession() {
                    @Override
                    public List<FileNasListRespVO.Item> list(String normalizedRelativePath) {
                        captured[0] = normalizedRelativePath;
                        return List.of();
                    }

                    @Override
                    public NasFileReadResult readFile(String normalizedRelativePath) {
                        throw new UnsupportedOperationException();
                    }

                    public SecurityDescriptor readDirectoryAcl(String normalizedRelativePath) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void close() {
                    }
                }
        );

        FileNasListRespVO result = service.listFiles(path.replace("/", "\\"));

        assertEquals(path, captured[0]);
        assertEquals(path, result.getCurrentPath());
        assertEquals("3.DMR/01.图纸/02配件图纸/04 输注类_配件", result.getParentPath());
    }

    @Test
    void testListFiles_authFailed() {
        NasSettingsService nasSettingsService = savedConfigService();
        NasBrowserServiceImpl service = new NasBrowserServiceImpl(
                nasSettingsService,
                config -> {
                    throw NasBrowserServiceImpl.NasBrowserException.authFailed(new RuntimeException("auth"));
                }
        );

        AssertUtils.assertServiceException(() -> service.listFiles(""), FILE_NAS_AUTH_FAILED);
    }

    @Test
    void testListFiles_pathNotExists() {
        NasSettingsService nasSettingsService = savedConfigService();
        NasBrowserServiceImpl service = new NasBrowserServiceImpl(
                nasSettingsService,
                config -> {
                    throw NasBrowserServiceImpl.NasBrowserException.pathNotExists("missing", new RuntimeException("missing"));
                }
        );

        AssertUtils.assertServiceException(() -> service.listFiles("missing"), FILE_NAS_PATH_NOT_EXISTS, "missing");
    }

    @Test
    void testTestConnection() {
        NasSettingsService nasSettingsService = savedConfigService();
        NasBrowserServiceImpl service = new NasBrowserServiceImpl(
                nasSettingsService,
                config -> new FakeNasSession(List.of(
                        new FileNasListRespVO.Item().setName("1.QMS documents").setPath("1.QMS documents").setDir(true).setSize(0L).setModifiedAt(1L)
                ))
        );

        FileNasConfigTestRespVO result = service.testConnection(new NasConnectionConfig("172.30.30.4", 1445, "it共享", "WORKGROUP", "int", "Kdlyx123"));

        assertEquals("\\\\172.30.30.4\\it共享", result.getRootPath());
        assertEquals(1, result.getItemCount());
        assertEquals("NAS 连接成功", result.getMessage());
    }

    @Test
    void testGetDirectoryTree_filtersFilesAndBuildsHierarchy() {
        NasSettingsService nasSettingsService = savedConfigService();
        NasBrowserServiceImpl service = new NasBrowserServiceImpl(
                nasSettingsService,
                config -> new NasBrowserServiceImpl.NasSession() {
                    @Override
                    public List<FileNasListRespVO.Item> list(String normalizedRelativePath) {
                        return switch (normalizedRelativePath) {
                            case "" -> List.of(
                                    new FileNasListRespVO.Item().setName("1.QMS documents").setPath("1.QMS documents").setDir(true).setSize(0L).setModifiedAt(1L),
                                    new FileNasListRespVO.Item().setName("#recycle").setPath("#recycle").setDir(true).setSize(0L).setModifiedAt(2L),
                                    new FileNasListRespVO.Item().setName("readme.txt").setPath("readme.txt").setDir(false).setSize(8L).setModifiedAt(3L)
                            );
                            case "1.QMS documents" -> List.of(
                                    new FileNasListRespVO.Item().setName("2.DHF").setPath("1.QMS documents/2.DHF").setDir(true).setSize(0L).setModifiedAt(4L)
                            );
                            case "1.QMS documents/2.DHF" -> List.of();
                            case "#recycle" -> throw NasBrowserServiceImpl.NasBrowserException.accessDenied("#recycle", new RuntimeException("STATUS_ACCESS_DENIED"));
                            default -> List.of();
                        };
                    }

                    @Override
                    public NasFileReadResult readFile(String normalizedRelativePath) {
                        throw new UnsupportedOperationException();
                    }

                    public SecurityDescriptor readDirectoryAcl(String normalizedRelativePath) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void close() {
                    }
                }
        );

        FileNasDirectoryTreeRespVO result = service.getDirectoryTree();

        assertEquals("it共享", result.getRootName());
        assertEquals("\\\\172.30.30.4\\it共享", result.getRootPath());
        assertEquals(3, result.getDirectoryCount());
        assertEquals(List.of("1.QMS documents"),
                result.getChildren().stream().map(FileNasDirectoryTreeRespVO.Node::getName).toList());
        assertEquals(List.of("2.DHF"),
                result.getChildren().get(0).getChildren().stream().map(FileNasDirectoryTreeRespVO.Node::getName).toList());
        assertEquals(List.of("#recycle"),
                result.getSkipped().stream().map(FileNasDirectoryTreeRespVO.SkippedNode::getPath).toList());
        assertEquals(List.of("access_denied"),
                result.getSkipped().stream().map(FileNasDirectoryTreeRespVO.SkippedNode::getReason).toList());
    }

    @Test
    void testReadFile_readsBytesAndMetadata() {
        NasSettingsService nasSettingsService = savedConfigService();
        NasBrowserServiceImpl service = new NasBrowserServiceImpl(
                nasSettingsService,
                config -> new NasBrowserServiceImpl.NasSession() {
                    @Override
                    public List<FileNasListRespVO.Item> list(String normalizedRelativePath) {
                        return List.of();
                    }

                    @Override
                    public NasFileReadResult readFile(String normalizedRelativePath) {
                        assertEquals("3.DMR/设计转移方案和报告/spec.docx", normalizedRelativePath);
                        return new NasFileReadResult(
                                "spec.docx",
                                normalizedRelativePath,
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "docx-content".getBytes()
                        );
                    }

                    public SecurityDescriptor readDirectoryAcl(String normalizedRelativePath) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void close() {
                    }
                }
        );

        NasFileReadResult result = service.readFile("3.DMR/设计转移方案和报告/spec.docx");

        assertEquals("spec.docx", result.name());
        assertEquals("3.DMR/设计转移方案和报告/spec.docx", result.path());
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", result.contentType());
        assertEquals("docx-content", new String(result.bytes()));
    }

    @Test
    void testReadFile_openFileWhenFileExistsProbeReturnsFalse() {
        String path = "1. QMS documents/3-1 RE 可编辑/INT∕RE∕6.3-01-05  (E∕0)  检验设备（仪器）使用记录.docx";
        String smbPath = path.replace("/", "\\");
        DiskShare share = mock(DiskShare.class);
        File file = mock(File.class);
        when(share.folderExists(smbPath)).thenReturn(false);
        when(share.fileExists(smbPath)).thenReturn(false);
        when(share.openFile(eq(smbPath), anySet(), isNull(), anySet(), eq(SMB2CreateDisposition.FILE_OPEN), isNull()))
                .thenReturn(file);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("docx-content".getBytes(StandardCharsets.UTF_8)));
        NasBrowserServiceImpl.SmbjNasSession session = new NasBrowserServiceImpl.SmbjNasSession(null, null, null, share);

        NasFileReadResult result = session.readFile(path);

        assertEquals(path, result.path());
        assertEquals("INT∕RE∕6.3-01-05  (E∕0)  检验设备（仪器）使用记录.docx", result.name());
        assertEquals("docx-content", new String(result.bytes(), StandardCharsets.UTF_8));
        verify(share).openFile(eq(smbPath), anySet(), isNull(), anySet(), eq(SMB2CreateDisposition.FILE_OPEN), isNull());
    }

    @Test
    void testSmbjSessionList_populatesModifiedAtFromLastWriteTime() {
        DiskShare share = mock(DiskShare.class);
        FileIdBothDirectoryInformation information = mock(FileIdBothDirectoryInformation.class);
        when(share.list("")).thenReturn(List.of(information));
        when(information.getFileName()).thenReturn("规范.docx");
        when(information.getFileAttributes()).thenReturn(0L);
        when(information.getEndOfFile()).thenReturn(128L);
        when(information.getLastWriteTime()).thenReturn(FileTime.ofEpochMillis(1710000000000L));

        NasBrowserServiceImpl.SmbjNasSession session = new NasBrowserServiceImpl.SmbjNasSession(null, null, null, share);

        List<FileNasListRespVO.Item> result = session.list("");

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(1710000000000L), result.get(0).getModifiedAt());
    }

    @Test
    void testWriteFileTo_streamsBytesWithoutBufferingWholeResponseInServiceLayer() {
        DiskShare share = mock(DiskShare.class);
        File file = mock(File.class);
        String path = "3.DMR/设计转移方案和报告/spec.docx";
        String smbPath = path.replace("/", "\\");
        when(share.openFile(eq(smbPath), anySet(), isNull(), anySet(), eq(SMB2CreateDisposition.FILE_OPEN), isNull()))
                .thenReturn(file);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("stream-content".getBytes(StandardCharsets.UTF_8)));
        NasBrowserServiceImpl.SmbjNasSession session = new NasBrowserServiceImpl.SmbjNasSession(null, null, null, share);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        session.writeFile(path, outputStream);

        assertEquals("stream-content", outputStream.toString(StandardCharsets.UTF_8));
        verify(share).openFile(eq(smbPath), anySet(), isNull(), anySet(), eq(SMB2CreateDisposition.FILE_OPEN), isNull());
    }

    @Test
    void testExecuteInSession_reusesSingleSessionAcrossMultipleOperations() {
        AtomicInteger createdSessions = new AtomicInteger();
        NasSettingsService nasSettingsService = savedConfigService();
        NasBrowserServiceImpl service = new NasBrowserServiceImpl(
                nasSettingsService,
                config -> {
                    createdSessions.incrementAndGet();
                    return new NasBrowserServiceImpl.NasSession() {
                        @Override
                        public List<FileNasListRespVO.Item> list(String normalizedRelativePath) {
                            return List.of(new FileNasListRespVO.Item()
                                    .setName(normalizedRelativePath)
                                    .setPath(normalizedRelativePath)
                                    .setDir(true)
                                    .setSize(0L)
                                    .setModifiedAt(1L));
                        }

                        @Override
                        public NasFileReadResult readFile(String normalizedRelativePath) {
                            return new NasFileReadResult("file.txt", normalizedRelativePath, "text/plain",
                                    normalizedRelativePath.getBytes(StandardCharsets.UTF_8));
                        }

                        @Override
                        public SecurityDescriptor readDirectoryAcl(String normalizedRelativePath) {
                            return aclDescriptor(Set.of(SecurityDescriptor.Control.DP), List.of());
                        }

                        @Override
                        public void close() {
                        }
                    };
                }
        );

        String result = service.executeInSession(new NasConnectionConfig("172.30.30.4", 1445, "it共享", "WORKGROUP", "int", "Kdlyx123"), scope -> {
            scope.listFiles("A");
            scope.listFiles("B");
            return scope.readFile("C/file.txt").path();
        });

        assertEquals(1, createdSessions.get());
        assertEquals("C/file.txt", result);
    }

    @Test
    void testSmbjConfig_failsFastAndDoesNotUseDfsForDirectShareReads() {
        SmbConfig config = NasBrowserServiceImpl.SmbjNasSessionFactory.buildSmbConfig();

        assertFalse(config.isDfsEnabled());
        assertFalse(config.isUseMultiProtocolNegotiate());
        assertTrue(config.getReadTimeout() <= TimeUnit.SECONDS.toMillis(10));
        assertTrue(config.getTransactTimeout() <= TimeUnit.SECONDS.toMillis(10));
        assertTrue(config.getWriteTimeout() <= TimeUnit.SECONDS.toMillis(10));
        assertTrue(config.getSoTimeout() <= TimeUnit.SECONDS.toMillis(10));
    }

    @Test
    void testReadDirectoryAcl_normalizesPathAndReturnsOwnerGroupAndAces() {
        final String[] captured = new String[1];
        NasSettingsService nasSettingsService = savedConfigService();
        NasBrowserService service = new NasBrowserServiceImpl(
                nasSettingsService,
                config -> new NasBrowserServiceImpl.NasSession() {
                    @Override
                    public List<FileNasListRespVO.Item> list(String normalizedRelativePath) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public NasFileReadResult readFile(String normalizedRelativePath) {
                        throw new UnsupportedOperationException();
                    }

                    public SecurityDescriptor readDirectoryAcl(String normalizedRelativePath) {
                        captured[0] = normalizedRelativePath;
                        return aclDescriptor(
                                Set.of(SecurityDescriptor.Control.DP),
                                List.of(
                                        allowAce(Set.of(AceFlags.OBJECT_INHERIT_ACE), AccessMask.FILE_LIST_DIRECTORY, ALLOW_SID),
                                        denyAce(Set.of(AceFlags.INHERITED_ACE), AccessMask.DELETE, DENY_SID)
                                )
                        );
                    }

                    @Override
                    public void close() {
                    }
                }
        );

        NasAclReadResult result = service.readDirectoryAcl("\\QMS\\.\\1.QMS documents\\..\\2.DHF\\");

        assertEquals("QMS/2.DHF", captured[0]);
        assertEquals("QMS/2.DHF", result.path());
        assertEquals(OWNER_SID, result.ownerSid());
        assertEquals(GROUP_SID, result.groupSid());
        assertTrue(result.controlFlags().contains("DP"));
        assertTrue(result.daclPresent());
        assertFalse(result.daclProtected());
        assertEquals(2, result.aces().size());
        assertEquals("ALLOW", result.aces().get(0).aceType());
        assertEquals(ALLOW_SID, result.aces().get(0).trusteeSid());
        assertEquals("DENY", result.aces().get(1).aceType());
        assertEquals(DENY_SID, result.aces().get(1).trusteeSid());
    }

    @Test
    void testToAclReadResult_preservesDescriptorFieldsAndAceOrder() {
        SecurityDescriptor descriptor = aclDescriptor(
                Set.of(SecurityDescriptor.Control.DP, SecurityDescriptor.Control.PD),
                List.of(
                        denyAce(Set.of(AceFlags.INHERITED_ACE), AccessMask.DELETE, DENY_SID),
                        allowAce(Set.of(AceFlags.OBJECT_INHERIT_ACE), AccessMask.FILE_LIST_DIRECTORY, ALLOW_SID)
                )
        );

        NasAclReadResult result = NasBrowserServiceImpl.toAclReadResult("QMS/2.DHF", descriptor);

        assertEquals("QMS/2.DHF", result.path());
        assertEquals(OWNER_SID, result.ownerSid());
        assertEquals(GROUP_SID, result.groupSid());
        assertTrue(result.controlFlags().contains("DP"));
        assertTrue(result.controlFlags().contains("PD"));
        assertTrue(result.daclPresent());
        assertTrue(result.daclProtected());
        assertEquals(2, result.aces().size());

        NasAclAce denied = result.aces().get(0);
        assertEquals(0, denied.index());
        assertEquals("DENY", denied.aceType());
        assertEquals(DENY_SID, denied.trusteeSid());
        assertEquals(AccessMask.DELETE.getValue(), denied.accessMask());
        assertTrue(denied.aceFlags().contains("INHERITED_ACE"));
        assertTrue(denied.inherited());

        NasAclAce allowed = result.aces().get(1);
        assertEquals(1, allowed.index());
        assertEquals("ALLOW", allowed.aceType());
        assertEquals(ALLOW_SID, allowed.trusteeSid());
        assertEquals(AccessMask.FILE_LIST_DIRECTORY.getValue(), allowed.accessMask());
        assertTrue(allowed.aceFlags().contains("OBJECT_INHERIT_ACE"));
        assertFalse(allowed.inherited());
    }

    @Test
    void testReadDirectoryAcl_aclReadFailureMapsToAclReadFailedErrorCode() {
        NasSettingsService nasSettingsService = savedConfigService();
        NasBrowserService service = new NasBrowserServiceImpl(
                nasSettingsService,
                config -> new NasBrowserServiceImpl.NasSession() {
                    @Override
                    public List<FileNasListRespVO.Item> list(String normalizedRelativePath) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public NasFileReadResult readFile(String normalizedRelativePath) {
                        throw new UnsupportedOperationException();
                    }

                    public SecurityDescriptor readDirectoryAcl(String normalizedRelativePath) {
                        throw new IllegalStateException("STATUS_ACCESS_DENIED");
                    }

                    @Override
                    public void close() {
                    }
                }
        );

        ServiceException exception = assertThrows(ServiceException.class, () -> service.readDirectoryAcl("QMS/2.DHF"));

        assertEquals(FILE_NAS_ACL_READ_FAILED.getCode(), exception.getCode());
    }

    private static NasSettingsService savedConfigService() {
        return new NasSettingsService() {
            @Override
            public cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigRespVO getNasConfig() {
                return new cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigRespVO()
                        .setServer("172.30.30.4")
                        .setShare("it共享")
                        .setUsername("int")
                        .setPassword("Kdlyx123");
            }

            @Override
            public void saveNasConfig(cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigSaveReqVO reqVO) {
            }

            @Override
            public NasConnectionConfig toConnectionConfig(cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigSaveReqVO reqVO) {
                return new NasConnectionConfig(reqVO.getServer(), reqVO.getPort(), reqVO.getShare(), reqVO.getDomain(),
                        reqVO.getUsername(), reqVO.getPassword());
            }

            @Override
            public NasConnectionConfig getRequiredNasConfig() {
                return new NasConnectionConfig("172.30.30.4", 1445, "it共享", "WORKGROUP", "int", "Kdlyx123");
            }
        };
    }

    private record FakeNasSession(List<FileNasListRespVO.Item> items) implements NasBrowserServiceImpl.NasSession {
        @Override
        public List<FileNasListRespVO.Item> list(String normalizedRelativePath) {
            return items;
        }

        @Override
        public NasFileReadResult readFile(String normalizedRelativePath) {
            throw new UnsupportedOperationException();
        }

        public SecurityDescriptor readDirectoryAcl(String normalizedRelativePath) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
        }
    }

    private static SecurityDescriptor aclDescriptor(Set<SecurityDescriptor.Control> controlFlags, List<ACE> aces) {
        return new SecurityDescriptor(
                controlFlags,
                SID.fromString(OWNER_SID),
                SID.fromString(GROUP_SID),
                null,
                new ACL(ACL.ACL_REVISION, aces)
        );
    }

    private static ACE allowAce(Set<AceFlags> aceFlags, AccessMask accessMask, String trusteeSid) {
        return AceTypes.accessAllowedAce(aceFlags, Set.of(accessMask), SID.fromString(trusteeSid));
    }

    private static ACE denyAce(Set<AceFlags> aceFlags, AccessMask accessMask, String trusteeSid) {
        return AceTypes.accessDeniedAce(aceFlags, Set.of(accessMask), SID.fromString(trusteeSid));
    }
}
