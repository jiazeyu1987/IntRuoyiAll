package cn.iocoder.yudao.module.infra.service.file;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.framework.file.core.utils.FileTypeUtils;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigTestRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasDirectoryTreeRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msdtyp.ACL;
import com.hierynomus.msdtyp.SecurityDescriptor;
import com.hierynomus.msdtyp.SecurityInformation;
import com.hierynomus.msdtyp.ace.ACE;
import com.hierynomus.msdtyp.ace.AceFlags;
import com.hierynomus.msdtyp.ace.AceHeader;
import com.hierynomus.msdtyp.ace.AceType;
import com.google.common.annotations.VisibleForTesting;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.Share;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_ACL_READ_FAILED;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_AUTH_FAILED;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_CONNECT_FAILED;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_DEPENDENCY_MISSING;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_PATH_NOT_DIRECTORY;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_PATH_NOT_EXISTS;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_READ_FAILED;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NAS_SHARE_NOT_EXISTS;

@Service
public class NasBrowserServiceImpl implements NasBrowserService {

    private final NasSettingsService nasSettingsService;
    private final NasSessionFactory sessionFactory;

    @Autowired
    public NasBrowserServiceImpl(NasSettingsService nasSettingsService) {
        this(nasSettingsService, new SmbjNasSessionFactory());
    }

    @VisibleForTesting
    NasBrowserServiceImpl(NasSettingsService nasSettingsService, NasSessionFactory sessionFactory) {
        this.nasSettingsService = nasSettingsService;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public FileNasListRespVO listFiles(String path) {
        return listFiles(nasSettingsService.getRequiredNasConfig(), path);
    }

    @Override
    public FileNasListRespVO listFiles(NasConnectionConfig config, String path) {
        return listFiles(config, path, true);
    }

    @Override
    public <T> T executeInSession(NasConnectionConfig config, NasSessionCallback<T> callback) {
        try (NasSession session = sessionFactory.create(config)) {
            NasSessionScope scope = new SessionScopeImpl(config, session);
            return callback.execute(scope);
        } catch (ServiceException ex) {
            throw ex;
        } catch (NasBrowserException ex) {
            throw mapNasBrowserException(config, null, ex);
        } catch (Exception ex) {
            throw exception(FILE_NAS_READ_FAILED, ex.getMessage());
        }
    }

    @Override
    public FileNasConfigTestRespVO testConnection(NasConnectionConfig config) {
        FileNasListRespVO response = listFiles(config, "");
        return new FileNasConfigTestRespVO()
                .setRootPath(response.getRootPath())
                .setItemCount(response.getItems().size())
                .setMessage("NAS 连接成功");
    }

    @Override
    public FileNasDirectoryTreeRespVO getDirectoryTree() {
        NasConnectionConfig config = nasSettingsService.getRequiredNasConfig();
        try (NasSession session = sessionFactory.create(config)) {
            int[] count = {1};
            List<FileNasDirectoryTreeRespVO.SkippedNode> skipped = new ArrayList<>();
            List<FileNasDirectoryTreeRespVO.Node> children = readTreeChildren(session, "", count, skipped);
            return new FileNasDirectoryTreeRespVO()
                    .setRootName(config.share())
                    .setRootPath(config.rootUnc())
                    .setDirectoryCount(count[0])
                    .setChildren(children)
                    .setSkipped(skipped);
        } catch (NasBrowserException ex) {
            throw switch (ex.reason()) {
                case DEPENDENCY_MISSING -> exception(FILE_NAS_DEPENDENCY_MISSING, ex.getMessage());
                case AUTH_FAILED -> exception(FILE_NAS_AUTH_FAILED);
                case SHARE_NOT_EXISTS -> exception(FILE_NAS_SHARE_NOT_EXISTS, config.share());
                case PATH_NOT_EXISTS -> exception(FILE_NAS_PATH_NOT_EXISTS, "/");
                case PATH_NOT_DIRECTORY -> exception(FILE_NAS_PATH_NOT_DIRECTORY, "/");
                case ACCESS_DENIED -> exception(FILE_NAS_READ_FAILED, "access denied: " + ex.getMessage());
                case CONNECT_FAILED -> exception(FILE_NAS_CONNECT_FAILED, config.server());
                case READ_FAILED -> exception(FILE_NAS_READ_FAILED, ex.getMessage());
            };
        } catch (Exception ex) {
            throw exception(FILE_NAS_READ_FAILED, ex.getMessage());
        }
    }

    @Override
    public NasFileReadResult readFile(String path) {
        String normalized = normalizeRelativePath(path);
        try (NasSession session = sessionFactory.create(nasSettingsService.getRequiredNasConfig())) {
            return session.readFile(normalized);
        } catch (NasBrowserException ex) {
            throw switch (ex.reason()) {
                case DEPENDENCY_MISSING -> exception(FILE_NAS_DEPENDENCY_MISSING, ex.getMessage());
                case AUTH_FAILED -> exception(FILE_NAS_AUTH_FAILED);
                case SHARE_NOT_EXISTS -> exception(FILE_NAS_SHARE_NOT_EXISTS, nasSettingsService.getRequiredNasConfig().share());
                case PATH_NOT_EXISTS -> exception(FILE_NAS_PATH_NOT_EXISTS, normalizedOrRoot(normalized));
                case PATH_NOT_DIRECTORY -> exception(FILE_NAS_PATH_NOT_DIRECTORY, normalizedOrRoot(normalized));
                case ACCESS_DENIED -> exception(FILE_NAS_READ_FAILED, "access denied: " + ex.getMessage());
                case CONNECT_FAILED -> exception(FILE_NAS_CONNECT_FAILED, nasSettingsService.getRequiredNasConfig().server());
                case READ_FAILED -> exception(FILE_NAS_READ_FAILED, ex.getMessage());
            };
        } catch (Exception ex) {
            throw exception(FILE_NAS_READ_FAILED, ex.getMessage());
        }
    }

    @Override
    public void writeFileTo(String path, OutputStream outputStream) {
        writeFileTo(nasSettingsService.getRequiredNasConfig(), path, outputStream);
    }

    @Override
    public NasFileReadResult readFile(NasConnectionConfig config, String path) {
        String normalized = normalizeRelativePath(path);
        try (NasSession session = sessionFactory.create(config)) {
            return session.readFile(normalized);
        } catch (NasBrowserException ex) {
            throw switch (ex.reason()) {
                case DEPENDENCY_MISSING -> exception(FILE_NAS_DEPENDENCY_MISSING, ex.getMessage());
                case AUTH_FAILED -> exception(FILE_NAS_AUTH_FAILED);
                case SHARE_NOT_EXISTS -> exception(FILE_NAS_SHARE_NOT_EXISTS, config.share());
                case PATH_NOT_EXISTS -> exception(FILE_NAS_PATH_NOT_EXISTS, normalizedOrRoot(normalized));
                case PATH_NOT_DIRECTORY -> exception(FILE_NAS_PATH_NOT_DIRECTORY, normalizedOrRoot(normalized));
                case ACCESS_DENIED -> exception(FILE_NAS_READ_FAILED, "access denied: " + ex.getMessage());
                case CONNECT_FAILED -> exception(FILE_NAS_CONNECT_FAILED, config.server());
                case READ_FAILED -> exception(FILE_NAS_READ_FAILED, ex.getMessage());
            };
        } catch (Exception ex) {
            throw exception(FILE_NAS_READ_FAILED, ex.getMessage());
        }
    }

    @Override
    public void writeFileTo(NasConnectionConfig config, String path, OutputStream outputStream) {
        String normalized = normalizeRelativePath(path);
        try (NasSession session = sessionFactory.create(config)) {
            if (session instanceof SmbjNasSession smbjSession) {
                smbjSession.writeFile(normalized, outputStream);
                return;
            }
            NasFileReadResult result = session.readFile(normalized);
            IoUtil.write(outputStream, false, result.bytes());
        } catch (NasBrowserException ex) {
            throw switch (ex.reason()) {
                case DEPENDENCY_MISSING -> exception(FILE_NAS_DEPENDENCY_MISSING, ex.getMessage());
                case AUTH_FAILED -> exception(FILE_NAS_AUTH_FAILED);
                case SHARE_NOT_EXISTS -> exception(FILE_NAS_SHARE_NOT_EXISTS, config.share());
                case PATH_NOT_EXISTS -> exception(FILE_NAS_PATH_NOT_EXISTS, normalizedOrRoot(normalized));
                case PATH_NOT_DIRECTORY -> exception(FILE_NAS_PATH_NOT_DIRECTORY, normalizedOrRoot(normalized));
                case ACCESS_DENIED -> exception(FILE_NAS_READ_FAILED, "access denied: " + ex.getMessage());
                case CONNECT_FAILED -> exception(FILE_NAS_CONNECT_FAILED, config.server());
                case READ_FAILED -> exception(FILE_NAS_READ_FAILED, ex.getMessage());
            };
        } catch (Exception ex) {
            throw exception(FILE_NAS_READ_FAILED, ex.getMessage());
        }
    }

    @Override
    public NasAclReadResult readDirectoryAcl(String path) {
        NasConnectionConfig config = nasSettingsService.getRequiredNasConfig();
        String normalized = normalizeRelativePath(path);
        try (NasSession session = sessionFactory.create(config)) {
            return toAclReadResult(normalized, session.readDirectoryAcl(normalized));
        } catch (NasBrowserException ex) {
            throw switch (ex.reason()) {
                case DEPENDENCY_MISSING -> exception(FILE_NAS_DEPENDENCY_MISSING, ex.getMessage());
                case AUTH_FAILED -> exception(FILE_NAS_AUTH_FAILED);
                case SHARE_NOT_EXISTS -> exception(FILE_NAS_SHARE_NOT_EXISTS, config.share());
                case PATH_NOT_EXISTS -> exception(FILE_NAS_PATH_NOT_EXISTS, normalizedOrRoot(normalized));
                case PATH_NOT_DIRECTORY -> exception(FILE_NAS_PATH_NOT_DIRECTORY, normalizedOrRoot(normalized));
                case CONNECT_FAILED -> exception(FILE_NAS_CONNECT_FAILED, config.server());
                case ACCESS_DENIED -> exception(FILE_NAS_ACL_READ_FAILED, "access denied: " + ex.getMessage());
                case READ_FAILED -> exception(FILE_NAS_ACL_READ_FAILED, ex.getMessage());
            };
        } catch (Exception ex) {
            throw exception(FILE_NAS_ACL_READ_FAILED, StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()));
        }
    }

    private List<FileNasDirectoryTreeRespVO.Node> readTreeChildren(
            NasSession session,
            String normalizedRelativePath,
            int[] count,
            List<FileNasDirectoryTreeRespVO.SkippedNode> skipped
    ) {
        List<FileNasDirectoryTreeRespVO.Node> children = new ArrayList<>();
        for (FileNasListRespVO.Item item : session.list(normalizedRelativePath)) {
            if (!Boolean.TRUE.equals(item.getDir())) {
                continue;
            }
            try {
                List<FileNasDirectoryTreeRespVO.Node> nextChildren = readTreeChildren(session, item.getPath(), count, skipped);
                count[0] += 1;
                children.add(new FileNasDirectoryTreeRespVO.Node()
                        .setName(item.getName())
                        .setPath(item.getPath())
                        .setChildren(nextChildren));
            } catch (NasBrowserException ex) {
                if (ex.reason() != NasFailureReason.ACCESS_DENIED) {
                    throw ex;
                }
                skipped.add(new FileNasDirectoryTreeRespVO.SkippedNode()
                        .setPath(item.getPath())
                        .setReason("access_denied"));
            }
        }
        return children;
    }

    private FileNasListRespVO listFiles(NasConnectionConfig config, String path, boolean unused) {
        String normalized = normalizeRelativePath(path);
        try (NasSession session = sessionFactory.create(config)) {
            return buildListResp(config, normalized, session.list(normalized));
        } catch (NasBrowserException ex) {
            throw mapNasBrowserException(config, normalized, ex);
        } catch (Exception ex) {
            throw exception(FILE_NAS_READ_FAILED, ex.getMessage());
        }
    }

    private static FileNasListRespVO buildListResp(NasConnectionConfig config, String normalized, List<FileNasListRespVO.Item> items) {
        List<FileNasListRespVO.Item> sorted = new ArrayList<>(items);
        sorted.sort(Comparator
                .comparing((FileNasListRespVO.Item item) -> !Boolean.TRUE.equals(item.getDir()))
                .thenComparing(item -> String.valueOf(item.getName()), String.CASE_INSENSITIVE_ORDER));
        return new FileNasListRespVO()
                .setCurrentPath(normalized)
                .setParentPath(relativeParent(normalized))
                .setRootPath(config.rootUnc())
                .setItems(sorted);
    }

    private static RuntimeException mapNasBrowserException(NasConnectionConfig config, String normalizedPath, NasBrowserException ex) {
        return switch (ex.reason()) {
            case DEPENDENCY_MISSING -> exception(FILE_NAS_DEPENDENCY_MISSING, ex.getMessage());
            case AUTH_FAILED -> exception(FILE_NAS_AUTH_FAILED);
            case SHARE_NOT_EXISTS -> exception(FILE_NAS_SHARE_NOT_EXISTS, config.share());
            case PATH_NOT_EXISTS -> exception(FILE_NAS_PATH_NOT_EXISTS, normalizedOrRoot(normalizedPath));
            case PATH_NOT_DIRECTORY -> exception(FILE_NAS_PATH_NOT_DIRECTORY, normalizedOrRoot(normalizedPath));
            case ACCESS_DENIED -> exception(FILE_NAS_READ_FAILED, "access denied: " + ex.getMessage());
            case CONNECT_FAILED -> exception(FILE_NAS_CONNECT_FAILED, config.server());
            case READ_FAILED -> exception(FILE_NAS_READ_FAILED, ex.getMessage());
        };
    }

    private final class SessionScopeImpl implements NasSessionScope {

        private final NasConnectionConfig config;
        private final NasSession session;

        private SessionScopeImpl(NasConnectionConfig config, NasSession session) {
            this.config = config;
            this.session = session;
        }

        @Override
        public FileNasListRespVO listFiles(String path) {
            String normalized = normalizeRelativePath(path);
            try {
                return buildListResp(config, normalized, session.list(normalized));
            } catch (NasBrowserException ex) {
                throw mapNasBrowserException(config, normalized, ex);
            }
        }

        @Override
        public NasFileReadResult readFile(String path) {
            String normalized = normalizeRelativePath(path);
            try {
                return session.readFile(normalized);
            } catch (NasBrowserException ex) {
                throw mapNasBrowserException(config, normalized, ex);
            }
        }

        @Override
        public void writeFileTo(String path, OutputStream outputStream) {
            String normalized = normalizeRelativePath(path);
            try {
                if (session instanceof SmbjNasSession smbjSession) {
                    smbjSession.writeFile(normalized, outputStream);
                    return;
                }
                NasFileReadResult result = session.readFile(normalized);
                IoUtil.write(outputStream, false, result.bytes());
            } catch (NasBrowserException ex) {
                throw mapNasBrowserException(config, normalized, ex);
            }
        }

        @Override
        public NasAclReadResult readDirectoryAcl(String path) {
            String normalized = normalizeRelativePath(path);
            try {
                return toAclReadResult(normalized, session.readDirectoryAcl(normalized));
            } catch (NasBrowserException ex) {
                throw mapAclBrowserException(config, normalized, ex);
            }
        }
    }

    private static RuntimeException mapAclBrowserException(NasConnectionConfig config, String normalizedPath, NasBrowserException ex) {
        return switch (ex.reason()) {
            case DEPENDENCY_MISSING -> exception(FILE_NAS_DEPENDENCY_MISSING, ex.getMessage());
            case AUTH_FAILED -> exception(FILE_NAS_AUTH_FAILED);
            case SHARE_NOT_EXISTS -> exception(FILE_NAS_SHARE_NOT_EXISTS, config.share());
            case PATH_NOT_EXISTS -> exception(FILE_NAS_PATH_NOT_EXISTS, normalizedOrRoot(normalizedPath));
            case PATH_NOT_DIRECTORY -> exception(FILE_NAS_PATH_NOT_DIRECTORY, normalizedOrRoot(normalizedPath));
            case CONNECT_FAILED -> exception(FILE_NAS_CONNECT_FAILED, config.server());
            case ACCESS_DENIED -> exception(FILE_NAS_ACL_READ_FAILED, "access denied: " + ex.getMessage());
            case READ_FAILED -> exception(FILE_NAS_ACL_READ_FAILED, ex.getMessage());
        };
    }

    @VisibleForTesting
    static String normalizeRelativePath(String pathText) {
        String raw = pathText == null ? "" : pathText.replace("\\", "/");
        if (raw.isBlank()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (String part : raw.split("/")) {
            if (part.isEmpty() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!parts.isEmpty()) {
                    parts.remove(parts.size() - 1);
                }
                continue;
            }
            parts.add(part);
        }
        return String.join("/", parts);
    }

    @VisibleForTesting
    static String relativeParent(String normalizedPath) {
        String cleanPath = normalizeRelativePath(normalizedPath);
        if (StrUtil.isBlank(cleanPath)) {
            return null;
        }
        int index = cleanPath.lastIndexOf('/');
        if (index < 0) {
            return "";
        }
        return cleanPath.substring(0, index);
    }

    private static String normalizedOrRoot(String normalizedPath) {
        return StrUtil.isBlank(normalizedPath) ? "/" : normalizedPath;
    }

    static NasAclReadResult toAclReadResult(String normalizedPath, SecurityDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor must not be null");
        Set<SecurityDescriptor.Control> controls = descriptor.getControl() == null
                ? Set.of()
                : descriptor.getControl();
        List<String> controlFlags = controls.stream()
                .map(Enum::name)
                .sorted()
                .toList();
        ACL dacl = descriptor.getDacl();
        boolean daclPresent = controls.contains(SecurityDescriptor.Control.DP);
        boolean daclProtected = controls.contains(SecurityDescriptor.Control.PD);
        List<NasAclAce> aces = new ArrayList<>();
        if (dacl != null) {
            List<ACE> sourceAces = dacl.getAces();
            for (int i = 0; i < sourceAces.size(); i++) {
                ACE ace = sourceAces.get(i);
                AceHeader header = ace.getAceHeader();
                Set<AceFlags> flags = header == null || header.getAceFlags() == null
                        ? Set.of()
                        : header.getAceFlags();
                List<String> flagNames = flags.stream()
                        .map(Enum::name)
                        .sorted()
                        .toList();
                aces.add(new NasAclAce(
                        i,
                        shortAceType(header == null ? null : header.getAceType()),
                        flagNames,
                        ace.getAccessMask(),
                        ace.getSid() == null ? null : ace.getSid().toString(),
                        flags.contains(AceFlags.INHERITED_ACE)
                ));
            }
        }
        return new NasAclReadResult(
                normalizedPath,
                descriptor.getOwnerSid() == null ? null : descriptor.getOwnerSid().toString(),
                descriptor.getGroupSid() == null ? null : descriptor.getGroupSid().toString(),
                controlFlags,
                daclPresent,
                daclProtected,
                aces
        );
    }

    private static String shortAceType(AceType aceType) {
        if (aceType == null) {
            return null;
        }
        String name = aceType.name();
        if (name.startsWith("ACCESS_ALLOWED")) {
            return "ALLOW";
        }
        if (name.startsWith("ACCESS_DENIED")) {
            return "DENY";
        }
        return name;
    }

    interface NasSessionFactory {
        NasSession create(NasConnectionConfig config);
    }

    interface NasSession extends AutoCloseable {
        List<FileNasListRespVO.Item> list(String normalizedRelativePath);

        NasFileReadResult readFile(String normalizedRelativePath);

        SecurityDescriptor readDirectoryAcl(String normalizedRelativePath);

        @Override
        void close();
    }

    enum NasFailureReason {
        DEPENDENCY_MISSING,
        AUTH_FAILED,
        SHARE_NOT_EXISTS,
        PATH_NOT_EXISTS,
        PATH_NOT_DIRECTORY,
        ACCESS_DENIED,
        CONNECT_FAILED,
        READ_FAILED
    }

    static final class NasBrowserException extends RuntimeException {
        private final NasFailureReason reason;

        private NasBrowserException(NasFailureReason reason, String message, Throwable cause) {
            super(message, cause);
            this.reason = reason;
        }

        NasFailureReason reason() {
            return reason;
        }

        static NasBrowserException dependencyMissing(String message, Throwable cause) {
            return new NasBrowserException(NasFailureReason.DEPENDENCY_MISSING, message, cause);
        }

        static NasBrowserException authFailed(Throwable cause) {
            return new NasBrowserException(NasFailureReason.AUTH_FAILED, "nas_auth_failed", cause);
        }

        static NasBrowserException shareNotExists(String share, Throwable cause) {
            return new NasBrowserException(NasFailureReason.SHARE_NOT_EXISTS, share, cause);
        }

        static NasBrowserException pathNotExists(String path, Throwable cause) {
            return new NasBrowserException(NasFailureReason.PATH_NOT_EXISTS, path, cause);
        }

        static NasBrowserException pathNotDirectory(String path, Throwable cause) {
            return new NasBrowserException(NasFailureReason.PATH_NOT_DIRECTORY, path, cause);
        }

        static NasBrowserException accessDenied(String path, Throwable cause) {
            return new NasBrowserException(NasFailureReason.ACCESS_DENIED, path, cause);
        }

        static NasBrowserException connectFailed(String server, Throwable cause) {
            return new NasBrowserException(NasFailureReason.CONNECT_FAILED, server, cause);
        }

        static NasBrowserException readFailed(String message, Throwable cause) {
            return new NasBrowserException(NasFailureReason.READ_FAILED, message, cause);
        }
    }

    static final class SmbjNasSessionFactory implements NasSessionFactory {

        @Override
        public NasSession create(NasConnectionConfig config) {
            try {
                SMBClient client = new SMBClient();
                Connection connection = client.connect(config.server(), config.port());
                AuthenticationContext auth = new AuthenticationContext(
                        config.username(),
                        config.password().toCharArray(),
                        StrUtil.blankToDefault(config.domain(), "")
                );
                Session session = connection.authenticate(auth);
                Share share = session.connectShare(config.share());
                if (!(share instanceof DiskShare diskShare)) {
                    closeQuietly(share);
                    closeQuietly(session);
                    closeQuietly(connection);
                    closeQuietly(client);
                    throw NasBrowserException.shareNotExists(config.share(), null);
                }
                return new SmbjNasSession(client, connection, session, diskShare);
            } catch (NoClassDefFoundError | ExceptionInInitializerError ex) {
                throw NasBrowserException.dependencyMissing("smbj dependency missing", ex);
            } catch (Exception ex) {
                throw mapConnectionException(config, ex);
            }
        }

        private NasBrowserException mapConnectionException(NasConnectionConfig config, Exception ex) {
            String message = StrUtil.nullToEmpty(ex.getMessage()).toLowerCase();
            if (message.contains("logon failure") || message.contains("access denied") || message.contains("authentication")) {
                return NasBrowserException.authFailed(ex);
            }
            if (message.contains("bad network name") || message.contains("network name cannot be found")) {
                return NasBrowserException.shareNotExists(config.share(), ex);
            }
            if (message.contains("timeout") || message.contains("refused") || message.contains("host") || message.contains("unreachable")) {
                return NasBrowserException.connectFailed(config.server(), ex);
            }
            return NasBrowserException.readFailed(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), ex);
        }
    }

    static final class SmbjNasSession implements NasSession {

        private static final long ATTRIBUTE_DIRECTORY = FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue();
        private static final long ATTRIBUTE_HIDDEN = FileAttributes.FILE_ATTRIBUTE_HIDDEN.getValue();
        private static final long ATTRIBUTE_SYSTEM = FileAttributes.FILE_ATTRIBUTE_SYSTEM.getValue();

        private final SMBClient client;
        private final Connection connection;
        private final Session session;
        private final DiskShare share;

        SmbjNasSession(SMBClient client, Connection connection, Session session, DiskShare share) {
            this.client = client;
            this.connection = connection;
            this.session = session;
            this.share = share;
        }

        @Override
        public List<FileNasListRespVO.Item> list(String normalizedRelativePath) {
            String smbPath = normalizedRelativePath.replace("/", "\\");
            try {
                if (StrUtil.isNotBlank(smbPath)) {
                    if (share.fileExists(smbPath)) {
                        throw NasBrowserException.pathNotDirectory(normalizedRelativePath, null);
                    }
                    if (!share.folderExists(smbPath)) {
                        throw NasBrowserException.pathNotExists(normalizedRelativePath, null);
                    }
                }
                List<FileNasListRespVO.Item> items = new ArrayList<>();
                for (FileIdBothDirectoryInformation information : share.list(smbPath)) {
                    String name = information.getFileName();
                    if (".".equals(name) || "..".equals(name)) {
                        continue;
                    }
                    long fileAttributes = information.getFileAttributes();
                    boolean isDir = hasAttribute(fileAttributes, ATTRIBUTE_DIRECTORY);
                    String itemPath = StrUtil.isBlank(normalizedRelativePath) ? name : normalizedRelativePath + "/" + name;
                    items.add(new FileNasListRespVO.Item()
                            .setName(name)
                            .setPath(itemPath)
                            .setDir(isDir)
                            .setSystem(hasAttribute(fileAttributes, ATTRIBUTE_SYSTEM))
                            .setHidden(hasAttribute(fileAttributes, ATTRIBUTE_HIDDEN))
                            .setSize(isDir ? 0L : Math.max(information.getEndOfFile(), 0L))
                            .setModifiedAt(information.getLastWriteTime() == null
                                    ? null
                                    : information.getLastWriteTime().toEpochMillis()));
                }
                return items;
            } catch (NasBrowserException ex) {
                throw ex;
            } catch (Exception ex) {
                String message = StrUtil.nullToEmpty(ex.getMessage()).toLowerCase();
                if (message.contains("object path not found") || message.contains("name not found")) {
                    throw NasBrowserException.pathNotExists(normalizedRelativePath, ex);
                }
                if (message.contains("access denied") || message.contains("status_access_denied")) {
                    throw NasBrowserException.accessDenied(normalizedRelativePath, ex);
                }
                if (message.contains("not a directory")) {
                    throw NasBrowserException.pathNotDirectory(normalizedRelativePath, ex);
                }
                throw NasBrowserException.readFailed(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), ex);
            }
        }

        private static boolean hasAttribute(long fileAttributes, long attributeMask) {
            return (fileAttributes & attributeMask) != 0;
        }

        @Override
        public NasFileReadResult readFile(String normalizedRelativePath) {
            String smbPath = normalizedRelativePath.replace("/", "\\");
            try {
                if (StrUtil.isBlank(smbPath)) {
                    throw NasBrowserException.pathNotDirectory(normalizedRelativePath, null);
                }
                try (com.hierynomus.smbj.share.File file = share.openFile(
                        smbPath,
                        Set.of(AccessMask.GENERIC_READ),
                        null,
                        Set.of(
                                SMB2ShareAccess.FILE_SHARE_READ,
                                SMB2ShareAccess.FILE_SHARE_WRITE,
                                SMB2ShareAccess.FILE_SHARE_DELETE
                        ),
                        SMB2CreateDisposition.FILE_OPEN,
                        null);
                     InputStream inputStream = file.getInputStream()) {
                    byte[] bytes = cn.hutool.core.io.IoUtil.readBytes(inputStream);
                    String name = normalizedRelativePath.contains("/")
                            ? normalizedRelativePath.substring(normalizedRelativePath.lastIndexOf('/') + 1)
                            : normalizedRelativePath;
                    return new NasFileReadResult(
                            name,
                            normalizedRelativePath,
                            FileTypeUtils.getMineType(bytes, name),
                            bytes
                    );
                }
            } catch (NasBrowserException ex) {
                throw ex;
            } catch (Exception ex) {
                String message = StrUtil.nullToEmpty(ex.getMessage()).toLowerCase();
                if (message.contains("object path not found") || message.contains("name not found")) {
                    throw NasBrowserException.pathNotExists(normalizedRelativePath, ex);
                }
                if (message.contains("access denied") || message.contains("status_access_denied")) {
                    throw NasBrowserException.accessDenied(normalizedRelativePath, ex);
                }
                if (message.contains("is a directory") || message.contains("not a regular file")) {
                    throw NasBrowserException.pathNotDirectory(normalizedRelativePath, ex);
                }
                throw NasBrowserException.readFailed(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), ex);
            }
        }

        void writeFile(String normalizedRelativePath, OutputStream outputStream) {
            String smbPath = normalizedRelativePath.replace("/", "\\");
            try {
                if (StrUtil.isBlank(smbPath)) {
                    throw NasBrowserException.pathNotDirectory(normalizedRelativePath, null);
                }
                try (com.hierynomus.smbj.share.File file = share.openFile(
                        smbPath,
                        Set.of(AccessMask.GENERIC_READ),
                        null,
                        Set.of(
                                SMB2ShareAccess.FILE_SHARE_READ,
                                SMB2ShareAccess.FILE_SHARE_WRITE,
                                SMB2ShareAccess.FILE_SHARE_DELETE
                        ),
                        SMB2CreateDisposition.FILE_OPEN,
                        null);
                     InputStream inputStream = file.getInputStream()) {
                    IoUtil.copy(inputStream, outputStream);
                }
            } catch (NasBrowserException ex) {
                throw ex;
            } catch (Exception ex) {
                String message = StrUtil.nullToEmpty(ex.getMessage()).toLowerCase();
                if (message.contains("object path not found") || message.contains("name not found")) {
                    throw NasBrowserException.pathNotExists(normalizedRelativePath, ex);
                }
                if (message.contains("access denied") || message.contains("status_access_denied")) {
                    throw NasBrowserException.accessDenied(normalizedRelativePath, ex);
                }
                if (message.contains("is a directory") || message.contains("not a regular file")) {
                    throw NasBrowserException.pathNotDirectory(normalizedRelativePath, ex);
                }
                throw NasBrowserException.readFailed(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), ex);
            }
        }

        @Override
        public SecurityDescriptor readDirectoryAcl(String normalizedRelativePath) {
            String smbPath = normalizedRelativePath.replace("/", "\\");
            try {
                if (StrUtil.isNotBlank(smbPath)) {
                    if (share.fileExists(smbPath)) {
                        throw NasBrowserException.pathNotDirectory(normalizedRelativePath, null);
                    }
                    if (!share.folderExists(smbPath)) {
                        throw NasBrowserException.pathNotExists(normalizedRelativePath, null);
                    }
                }
                return share.getSecurityInfo(
                        smbPath,
                        Set.of(
                                SecurityInformation.OWNER_SECURITY_INFORMATION,
                                SecurityInformation.GROUP_SECURITY_INFORMATION,
                                SecurityInformation.DACL_SECURITY_INFORMATION
                        )
                );
            } catch (NasBrowserException ex) {
                throw ex;
            } catch (Exception ex) {
                String message = StrUtil.nullToEmpty(ex.getMessage()).toLowerCase();
                if (message.contains("object path not found") || message.contains("name not found")) {
                    throw NasBrowserException.pathNotExists(normalizedRelativePath, ex);
                }
                if (message.contains("access denied") || message.contains("status_access_denied")) {
                    throw NasBrowserException.accessDenied(normalizedRelativePath, ex);
                }
                if (message.contains("not a directory")) {
                    throw NasBrowserException.pathNotDirectory(normalizedRelativePath, ex);
                }
                throw NasBrowserException.readFailed(StrUtil.blankToDefault(ex.getMessage(), ex.getClass().getSimpleName()), ex);
            }
        }

        @Override
        public void close() {
            closeQuietly(share);
            closeQuietly(session);
            closeQuietly(connection);
            closeQuietly(client);
        }
    }

    private static void closeQuietly(Object closeable) {
        if (closeable == null) {
            return;
        }
        try {
            if (closeable instanceof Closeable item) {
                item.close();
                return;
            }
            if (closeable instanceof AutoCloseable item) {
                item.close();
            }
        } catch (Exception ignored) {
            // fail-fast applies to the main flow; close failures stay best-effort
        }
    }
}
