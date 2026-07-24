package cn.iocoder.yudao.module.infra.framework.file.core.client;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * 文件客户端
 *
 * @author 瑛泰源码
 */
public interface FileClient {

    /**
     * 获得客户端编号
     *
     * @return 客户端编号
     */
    Long getId();

    /**
     * 上传文件
     *
     * @param content 文件流
     * @param path    相对路径
     * @return 完整路径，即 HTTP 访问地址
     * @throws Exception 上传文件时，抛出 Exception 异常
     */
    String upload(byte[] content, String path, String type) throws Exception;

    /**
     * 流式上传文件。
     *
     * 非流式客户端必须显式失败，不能退回到读取完整 byte[]。
     *
     * @param content 文件流
     * @param size    文件大小
     * @param path    相对路径
     * @param type    文件 MIME 类型
     * @return 完整路径，即 HTTP 访问地址
     * @throws Exception 上传文件时，抛出 Exception 异常
     */
    default String upload(InputStream content, long size, String path, String type) throws Exception {
        throw new UnsupportedOperationException("当前文件客户端不支持流式上传");
    }

    /**
     * 从本地文件路径上传文件。
     *
     * 非路径上传客户端必须显式失败，不能退回到读取完整 byte[]。
     *
     * @param content 本地文件路径
     * @param size    文件大小
     * @param path    相对路径
     * @param type    文件 MIME 类型
     * @return 完整路径，即 HTTP 访问地址
     * @throws Exception 上传文件时，抛出 Exception 异常
     */
    default String upload(Path content, long size, String path, String type) throws Exception {
        throw new UnsupportedOperationException("当前文件客户端不支持本地文件路径上传");
    }

    /**
     * 移动文件。
     *
     * 不支持原生移动的客户端必须显式失败，不能静默退回到默认成功。
     *
     * @param sourcePath 原相对路径
     * @param targetPath 目标相对路径
     * @param type       文件 MIME 类型，允许空
     * @return 移动后的完整路径，即 HTTP 访问地址
     * @throws Exception 移动文件时抛出异常
     */
    default String move(String sourcePath, String targetPath, String type) throws Exception {
        throw new UnsupportedOperationException("当前文件客户端不支持文件移动");
    }

    /**
     * 上传文件，并要求存储侧返回 Retention/Object Lock/legal hold 证据。
     *
     * 非 retention-capable 客户端必须显式失败，不能返回默认成功。
     *
     * @param content 文件流
     * @param path    相对路径
     * @param type    文件 MIME 类型
     * @param policy  存储保留策略
     * @return 存储侧证据
     * @throws Exception 上传或验证存储侧证据失败时抛出异常
     */
    default StorageRetentionEvidence uploadWithStorageRetention(byte[] content, String path, String type,
                                                               StorageRetentionPolicy policy) throws Exception {
        throw new UnsupportedOperationException("当前文件客户端不支持存储保留证据");
    }

    /**
     * 删除文件
     *
     * @param path 相对路径
     * @throws Exception 删除文件时，抛出 Exception 异常
     */
    void delete(String path) throws Exception;

    /**
     * 获得文件的内容
     *
     * @param path 相对路径
     * @return 文件的内容
     */
    byte[] getContent(String path) throws Exception;

    /**
     * 获得同一受保护对象版本的文件内容。
     *
     * 非 retention-capable 客户端必须显式失败，不能 fallback 到普通 getContent。
     *
     * @param path   相对路径
     * @param policy 存储保留策略，必须包含 objectVersionId
     * @return 文件内容
     */
    default byte[] getContentWithStorageRetention(String path, StorageRetentionPolicy policy) throws Exception {
        throw new UnsupportedOperationException("当前文件客户端不支持按存储保留证据读取文件内容");
    }

    /**
     * 读取并校验对象级 Retention/Object Lock/legal hold 证据。
     *
     * 非 retention-capable 客户端必须显式失败，不能返回空证据或 checksum-only 成功。
     *
     * @param path   相对路径
     * @param policy 存储保留策略
     * @return 存储侧证据
     * @throws Exception 验证存储侧证据失败时抛出异常
     */
    default StorageRetentionEvidence requireStorageRetentionEvidence(String path,
                                                                    StorageRetentionPolicy policy) throws Exception {
        throw new UnsupportedOperationException("当前文件客户端不支持存储保留证据");
    }

    // ========== 文件签名，目前仅 S3 支持 ==========

    /**
     * 获得文件预签名地址，用于上传
     *
     * @param path 相对路径
     * @return 文件预签名地址
     */
    default String presignPutUrl(String path) {
        throw new UnsupportedOperationException("不支持的操作");
    }

    /**
     * 生成文件预签名地址，用于读取
     *
     * @param url 完整的文件访问地址
     * @param expirationSeconds 访问有效期，单位秒
     * @return 文件预签名地址
     */
    default String presignGetUrl(String url, Integer expirationSeconds) {
        throw new UnsupportedOperationException("不支持的操作");
    }

}
