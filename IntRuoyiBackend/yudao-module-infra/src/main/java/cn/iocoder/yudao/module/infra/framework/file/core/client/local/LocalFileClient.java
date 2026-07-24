package cn.iocoder.yudao.module.infra.framework.file.core.client.local;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.iocoder.yudao.module.infra.framework.file.core.client.AbstractFileClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 本地文件客户端
 *
 * @author 瑛泰源码
 */
public class LocalFileClient extends AbstractFileClient<LocalFileClientConfig> {

    public LocalFileClient(Long id, LocalFileClientConfig config) {
        super(id, config);
    }

    @Override
    protected void doInit() {
    }

    @Override
    public String upload(byte[] content, String path, String type) {
        // 执行写入
        String filePath = getFilePath(path);
        FileUtil.writeBytes(content, filePath);
        // 拼接返回路径
        return super.formatFileUrl(config.getDomain(), path);
    }

    @Override
    public String upload(InputStream content, long size, String path, String type) throws Exception {
        String filePath = getFilePath(path);
        FileUtil.mkParentDirs(filePath);
        try (OutputStream outputStream = Files.newOutputStream(Path.of(filePath))) {
            content.transferTo(outputStream);
        }
        return super.formatFileUrl(config.getDomain(), path);
    }

    @Override
    public String upload(Path content, long size, String path, String type) throws Exception {
        String filePath = getFilePath(path);
        FileUtil.mkParentDirs(filePath);
        Files.copy(content, Path.of(filePath), StandardCopyOption.REPLACE_EXISTING);
        return super.formatFileUrl(config.getDomain(), path);
    }

    @Override
    public String move(String sourcePath, String targetPath, String type) throws Exception {
        Path source = Path.of(getFilePath(sourcePath));
        if (!Files.exists(source)) {
            throw new FileNotFoundException("待移动文件不存在：" + sourcePath);
        }
        Path target = Path.of(getFilePath(targetPath));
        FileUtil.mkParentDirs(target.toFile());
        Files.move(source, target);
        return super.formatFileUrl(config.getDomain(), targetPath);
    }

    @Override
    public void delete(String path) {
        String filePath = getFilePath(path);
        FileUtil.del(filePath);
    }

    @Override
    public byte[] getContent(String path) {
        String filePath = getFilePath(path);
        try {
            return FileUtil.readBytes(filePath);
        } catch (IORuntimeException ex) {
            if (ex.getMessage().startsWith("File not exist:")) {
                return null;
            }
            throw ex;
        }
    }

    private String getFilePath(String path) {
        return config.getBasePath() + File.separator + path;
    }

}
