package cn.iocoder.yudao.module.infra.service.file;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FilePageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FilePresignedUrlRespVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionEvidence;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionPolicy;
import cn.iocoder.yudao.module.infra.framework.file.core.client.s3.S3FileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.utils.FileTypeUtils;
import cn.iocoder.yudao.module.infra.service.file.access.FileDirectLinkAccessContext;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessDeniedException;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessRequest;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.List;
import java.util.Objects;

import static cn.hutool.core.date.DatePattern.PURE_DATE_PATTERN;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_BUSINESS_DIRECT_LINK_BLOCKED;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_PROTECTED_SHOWROOM_MEDIA;

/**
 * 文件 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Slf4j
public class FileServiceImpl implements FileService {

    private static final int DELETE_FILE_PARALLELISM = 16;

    /**
     * 上传文件的前缀，是否包含日期（yyyyMMdd）
     *
     * 目的：按照日期，进行分目录
     */
    static boolean PATH_PREFIX_DATE_ENABLE = true;
    /**
     * 上传文件的后缀，是否启用
     *
     * 算法：当前时间戳（毫秒）+ 5 位随机数；目的是保证文件的唯一性，避免覆盖
     * 定制：可按需调整成 UUID、或者其他方式
     */
    static boolean PATH_SUFFIX_TIMESTAMP_ENABLE = false;
    /**
     * 后缀是否作为上级目录
     *
     * true：{@code yyyyMMdd/<后缀>/原文件名.ext}；保留原文件名
     * false：{@code yyyyMMdd/原文件名_<后缀>.ext}；后缀拼到文件名
     */
    static boolean PATH_SUFFIX_AS_DIRECTORY = true;

    @Resource
    private FileConfigService fileConfigService;

    @Resource
    private FileMapper fileMapper;
    @Resource
    private BusinessFileAccessService businessFileAccessService;

    @Override
    public PageResult<FileDO> getFilePage(FilePageReqVO pageReqVO) {
        return fileMapper.selectPage(pageReqVO);
    }

    @Override
    @SneakyThrows
    public String createFile(byte[] content, String name, String directory, String type) {
        FileDO file = createFileRecord(content, name, directory, type);
        return file.getUrl();
    }

    @Override
    @SneakyThrows
    public Long createFileAndReturnId(byte[] content, String name, String directory, String type) {
        return createFileRecord(content, name, directory, type).getId();
    }

    @Override
    @SneakyThrows
    public Long createFileAndReturnId(InputStream content, long size, String name, String directory, String type) {
        Assert.notNull(content, "文件内容流不能为空");
        Assert.isTrue(size >= 0, "文件大小不能小于 0");
        return createFileRecord(content, size, name, directory, type).getId();
    }

    @Override
    @SneakyThrows
    public Long createFileAndReturnId(Path content, long size, String name, String directory, String type) {
        Assert.notNull(content, "文件路径不能为空");
        Assert.isTrue(size >= 0, "文件大小不能小于 0");
        Assert.isTrue(Files.isRegularFile(content), "文件路径必须指向普通文件");
        Assert.isTrue(size == Files.size(content), "文件路径实际大小与入参大小不一致");
        return createFileRecord(content, size, name, directory, type).getId();
    }

    @SneakyThrows
    private FileDO createFileRecord(byte[] content, String name, String directory, String type) {
        FileCreateContext context = buildFileCreateContext(content, name, directory, type);
        // 2.2 上传到文件存储器
        FileClient client = fileConfigService.getMasterFileClient();
        Assert.notNull(client, "客户端(master) 不能为空");
        String url = client.upload(content, context.getPath(), context.getType());

        // 3. 保存到数据库
        FileDO file = buildFileDO(client.getId(), context, url, content.length);
        fileMapper.insert(file);
        return file;
    }

    private FileDO createFileRecord(InputStream content, long size, String name, String directory, String type)
            throws Exception {
        FileCreateContext context = buildFileCreateContext(name, directory, type);
        FileClient client = fileConfigService.getMasterFileClient();
        Assert.notNull(client, "客户端(master) 不能为空");
        String url = client.upload(content, size, context.getPath(), context.getType());

        FileDO file = buildFileDO(client.getId(), context, url, size);
        fileMapper.insert(file);
        return file;
    }

    private FileDO createFileRecord(Path content, long size, String name, String directory, String type)
            throws Exception {
        FileCreateContext context = buildFileCreateContext(name, directory, type);
        FileClient client = fileConfigService.getMasterFileClient();
        Assert.notNull(client, "客户端(master) 不能为空");
        String url = client.upload(content, size, context.getPath(), context.getType());

        FileDO file = buildFileDO(client.getId(), context, url, size);
        fileMapper.insert(file);
        return file;
    }

    @Override
    @SneakyThrows
    public StorageRetentionEvidence createFileWithStorageRetention(byte[] content, String name, String directory,
                                                                   String type, StorageRetentionPolicy policy) {
        FileClient client = fileConfigService.getMasterFileClient();
        Assert.notNull(client, "客户端(master) 不能为空");
        return createFileWithStorageRetention(client, content, name, directory, type, policy);
    }

    @Override
    @SneakyThrows
    public StorageRetentionEvidence createFileWithStorageRetention(Long configId, byte[] content, String name,
                                                                   String directory, String type,
                                                                   StorageRetentionPolicy policy) {
        FileClient client = fileConfigService.getFileClient(configId);
        Assert.notNull(client, "客户端({}) 不能为空", configId);
        return createFileWithStorageRetention(client, content, name, directory, type, policy);
    }

    private StorageRetentionEvidence createFileWithStorageRetention(FileClient client, byte[] content, String name,
                                                                    String directory, String type,
                                                                    StorageRetentionPolicy policy) throws Exception {
        FileCreateContext context = buildFileCreateContext(content, name, directory, type);
        StorageRetentionEvidence evidence = client.uploadWithStorageRetention(content, context.getPath(),
                context.getType(), policy);
        Assert.notNull(evidence, "存储保留证据不能为空");
        Assert.isTrue(StrUtil.isNotEmpty(evidence.getUrl()), "存储保留证据访问地址不能为空");

        FileDO file = buildFileDO(client.getId(), context, evidence.getUrl(), content.length);
        fileMapper.insert(file);
        evidence.setFileId(file.getId());
        return evidence;
    }

    private FileCreateContext buildFileCreateContext(byte[] content, String name, String directory, String type) {
        // 1.1 处理 type 为空的情况
        if (StrUtil.isEmpty(type)) {
            type = FileTypeUtils.getMineType(content, name);
        }
        // 1.2 处理 name 为空的情况
        if (StrUtil.isEmpty(name)) {
            name = DigestUtil.sha256Hex(content);
        }
        if (StrUtil.isEmpty(FileUtil.extName(name))) {
            // 如果 name 没有后缀 type，则补充后缀
            String extension = FileTypeUtils.getExtension(type);
            if (StrUtil.isNotEmpty(extension)) {
                name = name + extension;
            }
        }

        // 2.1 生成上传的 path，需要保证唯一
        String path = generateUploadPath(name, directory);
        return new FileCreateContext(name, path, type);
    }

    private FileCreateContext buildFileCreateContext(String name, String directory, String type) {
        Assert.notEmpty(name, "流式上传文件名不能为空");
        if (StrUtil.isEmpty(type)) {
            type = FileTypeUtils.getMineType(name);
        }
        if (StrUtil.isEmpty(FileUtil.extName(name))) {
            String extension = FileTypeUtils.getExtension(type);
            if (StrUtil.isNotEmpty(extension)) {
                name = name + extension;
            }
        }
        String path = generateUploadPath(name, directory);
        return new FileCreateContext(name, path, type);
    }

    private FileDO buildFileDO(Long configId, FileCreateContext context, String url, long contentLength) {
        return new FileDO().setConfigId(configId)
                .setName(context.getName()).setPath(context.getPath()).setUrl(url)
                .setType(context.getType()).setSize(contentLength);
    }

    @VisibleForTesting
    String generateUploadPath(String name, String directory) {
        // 1. 生成前缀、后缀
        String prefix = null;
        if (PATH_PREFIX_DATE_ENABLE) {
            prefix = LocalDateTimeUtil.format(LocalDateTimeUtil.now(), PURE_DATE_PATTERN);
        }
        String suffix = null;
        if (PATH_SUFFIX_TIMESTAMP_ENABLE) {
            // 5 位随机数，避免同一毫秒内的重复
            suffix = String.valueOf(System.currentTimeMillis()) + RandomUtil.randomInt(10000, 100000);
        }

        // 2.1 先拼接 suffix 后缀
        if (StrUtil.isNotEmpty(suffix)) {
            if (PATH_SUFFIX_AS_DIRECTORY) {
                name = suffix + StrUtil.SLASH + name;
            } else {
                String ext = FileUtil.extName(name);
                if (StrUtil.isNotEmpty(ext)) {
                    name = FileUtil.mainName(name) + StrUtil.C_UNDERLINE + suffix + StrUtil.DOT + ext;
                } else {
                    name = name + StrUtil.C_UNDERLINE + suffix;
                }
            }
        }
        // 2.2 再拼接 prefix 前缀
        if (StrUtil.isNotEmpty(prefix)) {
            name = prefix + StrUtil.SLASH + name;
        }
        // 2.3 最后拼接 directory 目录
        if (StrUtil.isNotEmpty(directory)) {
            name = directory + StrUtil.SLASH + name;
        }
        return name;
    }

    @Override
    @SneakyThrows
    public FilePresignedUrlRespVO presignPutUrl(String name, String directory) {
        // 1. 生成上传的 path，需要保证唯一
        String path = generateUploadPath(name, directory);

        // 2. 获取文件预签名地址
        FileClient fileClient = fileConfigService.getMasterFileClient();
        String uploadUrl = fileClient.presignPutUrl(path);
        String visitUrl = fileClient.presignGetUrl(path, null);
        return new FilePresignedUrlRespVO().setConfigId(fileClient.getId())
                .setPath(path).setUploadUrl(uploadUrl).setUrl(visitUrl);
    }

    @Override
    public String presignGetUrl(String url, Integer expirationSeconds) {
        FileClient fileClient = fileConfigService.getMasterFileClient();
        return fileClient.presignGetUrl(url, expirationSeconds);
    }

    @Override
    public Long createFile(FileCreateReqVO createReqVO) {
        createReqVO.setUrl(HttpUtils.removeUrlQuery(createReqVO.getUrl())); // 目的：移除私有桶情况下，URL 的签名参数
        FileDO file = BeanUtils.toBean(createReqVO, FileDO.class);
        fileMapper.insert(file);
        return file.getId();
    }

    @Override
    public FileDO getFile(Long id) {
        return validateFileExists(id);
    }

    @Override
    public FileDO moveFile(Long id, String targetPath) {
        FileDO file = validateFileExists(id);
        validateDeleteAllowed(file);
        String normalizedTargetPath = normalizeMoveTargetPath(targetPath);
        Assert.isFalse(StrUtil.equals(file.getPath(), normalizedTargetPath), "移动目标路径不能与原路径相同");

        FileClient client = fileConfigService.getFileClient(file.getConfigId());
        Assert.notNull(client, "客户端({}) 不能为空", file.getConfigId());
        String targetUrl;
        try {
            targetUrl = client.move(file.getPath(), normalizedTargetPath, file.getType());
        } catch (Exception ex) {
            targetUrl = completePreviouslyMovedFileIfStorageStateMatches(file, client, normalizedTargetPath, ex);
        }

        fileMapper.updateById(FileDO.builder()
                .id(file.getId())
                .path(normalizedTargetPath)
                .url(targetUrl)
                .build());
        file.setPath(normalizedTargetPath);
        file.setUrl(targetUrl);
        return file;
    }

    private String completePreviouslyMovedFileIfStorageStateMatches(FileDO file, FileClient client,
                                                                    String normalizedTargetPath,
                                                                    Exception moveFailure) {
        if (!isStoragePathReadable(client, normalizedTargetPath, moveFailure)) {
            throw moveFileFailed(file, normalizedTargetPath, moveFailure);
        }
        if (isStoragePathReadable(client, file.getPath(), moveFailure)) {
            throw moveFileFailed(file, normalizedTargetPath, moveFailure);
        }
        try {
            return client.presignGetUrl(normalizedTargetPath, null);
        } catch (RuntimeException ex) {
            throw moveFileFailed(file, normalizedTargetPath, ex);
        }
    }

    private boolean isStoragePathReadable(FileClient client, String path, Exception moveFailure) {
        try {
            return client.getContent(path) != null;
        } catch (Exception ex) {
            if (isStoragePathMissing(ex)) {
                return false;
            }
            moveFailure.addSuppressed(ex);
            throw moveFileFailed(path, moveFailure);
        }
    }

    private boolean isStoragePathMissing(Exception ex) {
        return ex instanceof NoSuchKeyException || ex instanceof NoSuchFileException;
    }

    private IllegalStateException moveFileFailed(FileDO file, String normalizedTargetPath, Exception cause) {
        return new IllegalStateException(StrUtil.format("文件移动失败，fileId={}, sourcePath={}, targetPath={}",
                file.getId(), file.getPath(), normalizedTargetPath), cause);
    }

    private IllegalStateException moveFileFailed(String path, Exception cause) {
        return new IllegalStateException(StrUtil.format("文件移动状态确认失败，path={}", path), cause);
    }

    @Override
    public void deleteFile(Long id) throws Exception {
        // 校验存在
        FileDO file = validateFileExists(id);
        validateDeleteAllowed(file);

        // 从文件存储器中删除
        FileClient client = fileConfigService.getFileClient(file.getConfigId());
        Assert.notNull(client, "客户端({}) 不能为空", file.getConfigId());
        client.delete(file.getPath());

        // 删除记录
        fileMapper.deleteById(id);
    }

    @Override
    @SneakyThrows
    public void deleteFileList(List<Long> ids) {
        // 删除文件
        List<FileDO> files = fileMapper.selectByIds(ids);
        validateDeleteAllowed(files);
        deleteStorageFilesConcurrently(files);

        // 删除记录
        fileMapper.deleteByIds(ids);
    }

    private void deleteStorageFilesConcurrently(List<FileDO> files) throws Exception {
        if (files.isEmpty()) {
            return;
        }
        int poolSize = Math.min(DELETE_FILE_PARALLELISM, files.size());
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);
        try {
            List<Callable<Void>> tasks = files.stream()
                    .<Callable<Void>>map(file -> () -> {
                        FileClient client = fileConfigService.getFileClient(file.getConfigId());
                        Assert.notNull(client, "客户端({}) 不能为空", file.getPath());
                        client.delete(file.getPath());
                        return null;
                    })
                    .toList();
            List<Future<Void>> futures = executorService.invokeAll(tasks);
            for (Future<Void> future : futures) {
                future.get();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw new IllegalStateException("文件删除失败", cause);
        } finally {
            executorService.shutdownNow();
        }
    }

    private FileDO validateFileExists(Long id) {
        FileDO fileDO = fileMapper.selectById(id);
        if (fileDO == null) {
            throw exception(FILE_NOT_EXISTS);
        }
        return fileDO;
    }

    private void validateDeleteAllowed(List<FileDO> files) {
        for (FileDO file : files) {
            validateDeleteAllowed(file);
        }
    }

    private void validateDeleteAllowed(FileDO file) {
        if (ShowroomProtectedFileRules.isProtectedShowroomFile(file)) {
            throw exception(FILE_PROTECTED_SHOWROOM_MEDIA, file.getId());
        }
    }

    private String normalizeMoveTargetPath(String targetPath) {
        Assert.isTrue(StrUtil.isNotBlank(targetPath), "移动目标路径不能为空");
        String normalized = StrUtil.trim(targetPath).replace('\\', '/');
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        normalized = StrUtil.removePrefix(normalized, "/");
        Assert.isTrue(StrUtil.isNotBlank(normalized), "移动目标路径不能为空");
        for (String segment : normalized.split("/")) {
            Assert.isFalse("..".equals(segment), "移动目标路径不能包含上级目录");
        }
        return normalized;
    }

    @Override
    public byte[] getFileContent(Long configId, String path) throws Exception {
        FileClient client = fileConfigService.getFileClient(configId);
        Assert.notNull(client, "客户端({}) 不能为空", configId);
        try {
            return client.getContent(path);
        } catch (NoSuchKeyException ex) {
            fileConfigService.clearFileClientCache(configId);
            FileClient refreshedClient = fileConfigService.getFileClient(configId);
            Assert.notNull(refreshedClient, "客户端({}) 不能为空", configId);
            try {
                return refreshedClient.getContent(path);
            } catch (NoSuchKeyException retryEx) {
                if (refreshedClient instanceof S3FileClient s3FileClient) {
                    return s3FileClient.getContentWithFreshClient(path);
                }
                throw retryEx;
            }
        }
    }

    @Override
    public void validateDirectLinkAllowed(Long configId, String path, FileDirectLinkAccessContext context) {
        List<FileDO> files = fileMapper.selectList(new LambdaQueryWrapperX<FileDO>()
                .eq(FileDO::getConfigId, configId)
                .eq(FileDO::getPath, path));
        for (FileDO file : files) {
            try {
                businessFileAccessService.assertAllowed(
                        BusinessFileAccessRequest.publicDirectLink(file.getId(), context.requestId(),
                                context.sourceIp(), context.userAgent()));
            } catch (BusinessFileAccessDeniedException ex) {
                throw exception(FILE_BUSINESS_DIRECT_LINK_BLOCKED, ex.getFileId());
            }
        }
    }

    @Override
    public byte[] getFileContentWithStorageRetention(Long fileId, StorageRetentionPolicy policy) throws Exception {
        FileDO file = validateFileExists(fileId);
        FileClient client = fileConfigService.getFileClient(file.getConfigId());
        Assert.notNull(client, "客户端({}) 不能为空", file.getConfigId());
        return client.getContentWithStorageRetention(file.getPath(), policy);
    }

    @Override
    public StorageRetentionEvidence requireStorageRetentionEvidence(Long fileId, StorageRetentionPolicy policy) throws Exception {
        FileDO file = validateFileExists(fileId);
        StorageRetentionEvidence evidence = requireStorageRetentionEvidence(file.getConfigId(), file.getPath(), policy);
        Assert.notNull(evidence, "存储保留证据不能为空");
        if (evidence.getFileId() != null && !Objects.equals(evidence.getFileId(), file.getId())) {
            throw new IllegalStateException("存储保留证据文件编号与数据库文件不一致");
        }
        evidence.setFileId(file.getId());
        return evidence;
    }

    @Override
    public StorageRetentionEvidence requireStorageRetentionEvidence(Long configId, String path,
                                                                   StorageRetentionPolicy policy) throws Exception {
        FileClient client = fileConfigService.getFileClient(configId);
        Assert.notNull(client, "客户端({}) 不能为空", configId);
        return client.requireStorageRetentionEvidence(path, policy);
    }

    @Data
    @AllArgsConstructor
    private static class FileCreateContext {

        private String name;
        private String path;
        private String type;
    }

}
