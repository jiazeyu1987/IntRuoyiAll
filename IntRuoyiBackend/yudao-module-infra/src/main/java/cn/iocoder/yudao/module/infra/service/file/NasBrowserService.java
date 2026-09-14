package cn.iocoder.yudao.module.infra.service.file;

import cn.hutool.core.io.IoUtil;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigTestRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasDirectoryTreeRespVO;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * NAS 浏览 Service
 */
public interface NasBrowserService {

    @FunctionalInterface
    interface NasSessionCallback<T> {
        T execute(NasSessionScope scope);
    }

    interface NasSessionScope {
        FileNasListRespVO listFiles(String path);

        NasFileReadResult readFile(String path);

        default void writeFile(String path, InputStream inputStream) {
            throw new UnsupportedOperationException("NAS session write is not implemented");
        }

        void writeFileTo(String path, OutputStream outputStream);

        NasAclReadResult readDirectoryAcl(String path);
    }

    /**
     * 浏览 NAS 共享目录
     *
     * @param path 相对共享根目录的路径，允许为空
     * @return 目录列表
     */
    FileNasListRespVO listFiles(String path);

    /**
     * 使用指定 NAS 配置浏览共享目录
     *
     * @param config NAS 连接配置
     * @param path 相对共享根目录的路径，允许为空
     * @return 目录列表
     */
    FileNasListRespVO listFiles(NasConnectionConfig config, String path);

    /**
     * 在单个 NAS 会话内执行多个操作，适合批量遍历等场景。
     *
     * @param config NAS 连接配置
     * @param callback 会话内回调
     * @return 回调结果
     * @param <T> 返回值类型
     */
    <T> T executeInSession(NasConnectionConfig config, NasSessionCallback<T> callback);

    /**
     * 使用指定配置测试 NAS 连接
     *
     * @param config NAS 连接配置
     * @return 测试结果
     */
    FileNasConfigTestRespVO testConnection(NasConnectionConfig config);

    /**
     * 读取 NAS 目录树
     *
     * @return 目录树
     */
    FileNasDirectoryTreeRespVO getDirectoryTree();

    /**
     * 读取 NAS 文件字节和元信息
     *
     * @param path 相对共享根目录的文件路径
     * @return 文件内容与类型
     */
    NasFileReadResult readFile(String path);

    /**
     * 使用指定 NAS 配置读取 NAS 文件字节和元信息
     *
     * @param config NAS 连接配置
     * @param path 相对共享根目录的文件路径
     * @return 文件内容与类型
     */
    NasFileReadResult readFile(NasConnectionConfig config, String path);

    /**
     * 写入文件到默认 NAS 配置。
     *
     * @param path 相对共享根目录的文件路径
     * @param inputStream 待写入内容
     */
    default void writeFile(String path, InputStream inputStream) {
        throw new UnsupportedOperationException("NAS write is not implemented");
    }

    /**
     * 使用指定 NAS 配置写入文件。
     *
     * @param config NAS 连接配置
     * @param path 相对共享根目录的文件路径
     * @param inputStream 待写入内容
     */
    default void writeFile(NasConnectionConfig config, String path, InputStream inputStream) {
        throw new UnsupportedOperationException("NAS write is not implemented");
    }

    /**
     * 直接将 NAS 文件写入目标输出流。
     *
     * @param path 相对共享根目录的文件路径
     * @param outputStream 目标输出流
     */
    default void writeFileTo(String path, OutputStream outputStream) {
        NasFileReadResult result = readFile(path);
        IoUtil.write(outputStream, false, result.bytes());
    }

    /**
     * 使用指定 NAS 配置直接将文件写入目标输出流。
     *
     * @param config NAS 连接配置
     * @param path 相对共享根目录的文件路径
     * @param outputStream 目标输出流
     */
    default void writeFileTo(NasConnectionConfig config, String path, OutputStream outputStream) {
        NasFileReadResult result = readFile(config, path);
        IoUtil.write(outputStream, false, result.bytes());
    }

    /**
     * 读取 NAS 目录 ACL
     *
     * @param path 相对共享根目录的目录路径，允许为空
     * @return 目录 ACL 信息
     */
    NasAclReadResult readDirectoryAcl(String path);
}
