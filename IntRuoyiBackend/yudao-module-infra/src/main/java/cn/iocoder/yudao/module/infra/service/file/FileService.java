package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileCreateReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FilePageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FilePresignedUrlRespVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.access.FileDirectLinkAccessContext;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionEvidence;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionPolicy;
import jakarta.validation.constraints.NotEmpty;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/**
 * 文件 Service 接口
 *
 * @author 瑛泰源码
 */
public interface FileService {

    /**
     * 获得文件分页
     *
     * @param pageReqVO 分页查询
     * @return 文件分页
     */
    PageResult<FileDO> getFilePage(FilePageReqVO pageReqVO);

    /**
     * 保存文件，并返回文件的访问路径
     *
     * @param content   文件内容
     * @param name      文件名称，允许空
     * @param directory 目录，允许空
     * @param type      文件的 MIME 类型，允许空
     * @return 文件路径
     */
    String createFile(@NotEmpty(message = "文件内容不能为空") byte[] content,
                      String name, String directory, String type);

    /**
     * 保存文件，并要求存储侧返回 Retention/Object Lock/legal hold 证据。
     *
     * @param content 文件内容
     * @param name 文件名称，允许空
     * @param directory 目录，允许空
     * @param type 文件的 MIME 类型，允许空
     * @param policy 存储保留策略
     * @return 存储侧证据
     */
    StorageRetentionEvidence createFileWithStorageRetention(@NotEmpty(message = "文件内容不能为空") byte[] content,
                                                            String name, String directory, String type,
                                                            StorageRetentionPolicy policy);

    /**
     * 使用指定文件配置保存文件，并要求存储侧返回 Retention/Object Lock/legal hold 证据。
     *
     * @param configId 文件配置编号
     * @param content 文件内容
     * @param name 文件名称，允许空
     * @param directory 目录，允许空
     * @param type 文件的 MIME 类型，允许空
     * @param policy 存储保留策略
     * @return 存储侧证据
     */
    StorageRetentionEvidence createFileWithStorageRetention(Long configId,
                                                            @NotEmpty(message = "文件内容不能为空") byte[] content,
                                                            String name, String directory, String type,
                                                            StorageRetentionPolicy policy);

    /**
     * 保存文件，并返回文件记录编号
     *
     * @param content   文件内容
     * @param name      文件名称，允许空
     * @param directory 目录，允许空
     * @param type      文件的 MIME 类型，允许空
     * @return 文件记录编号
     */
    Long createFileAndReturnId(@NotEmpty(message = "文件内容不能为空") byte[] content,
                               String name, String directory, String type);

    /**
     * 流式保存文件，并返回文件记录编号。
     *
     * @param content   文件内容流
     * @param size      文件大小
     * @param name      文件名称，允许空
     * @param directory 目录，允许空
     * @param type      文件的 MIME 类型，允许空
     * @return 文件记录编号
     */
    Long createFileAndReturnId(@NotEmpty(message = "文件内容不能为空") InputStream content,
                               long size, String name, String directory, String type);

    /**
     * 从本地文件路径保存文件，并返回文件记录编号。
     *
     * @param content   本地文件路径
     * @param size      文件大小
     * @param name      文件名称，允许空
     * @param directory 目录，允许空
     * @param type      文件的 MIME 类型，允许空
     * @return 文件记录编号
     */
    Long createFileAndReturnId(@NotEmpty(message = "文件路径不能为空") Path content,
                               long size, String name, String directory, String type);

    /**
     * 生成文件预签名地址信息，用于上传
     *
     * @param name      文件名
     * @param directory 目录
     * @return 预签名地址信息
     */
    FilePresignedUrlRespVO presignPutUrl(@NotEmpty(message = "文件名不能为空") String name,
                                         String directory);
    /**
     * 生成文件预签名地址信息，用于读取
     *
     * @param url 完整的文件访问地址
     * @param expirationSeconds 访问有效期，单位秒
     * @return 文件预签名地址
     */
    String presignGetUrl(String url, Integer expirationSeconds);

    /**
     * 创建文件
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createFile(FileCreateReqVO createReqVO);
    FileDO getFile(Long id);

    /**
     * 移动文件，并更新文件记录中的路径和访问地址。
     *
     * @param id         文件编号
     * @param targetPath 目标相对路径
     * @return 移动后的文件记录
     */
    FileDO moveFile(Long id, String targetPath);

    /**
     * 删除文件
     *
     * @param id 编号
     */
    void deleteFile(Long id) throws Exception;

    /**
     * 批量删除文件
     *
     * @param ids 编号列表
     */
    void deleteFileList(List<Long> ids) throws Exception;

    /**
     * 获得文件内容
     *
     * @param configId 配置编号
     * @param path     文件路径
     * @return 文件内容
     */
    byte[] getFileContent(Long configId, String path) throws Exception;

    /**
     * 校验通用直链访问是否允许。
     *
     * @param configId 配置编号
     * @param path     文件路径
     * @param context  访问上下文
     */
    void validateDirectLinkAllowed(Long configId, String path, FileDirectLinkAccessContext context);

    /**
     * 按文件记录编号读取同一受保护对象版本的文件内容。
     *
     * @param fileId 文件记录编号
     * @param policy 存储保留策略，必须包含 objectVersionId
     * @return 文件内容
     */
    byte[] getFileContentWithStorageRetention(Long fileId, StorageRetentionPolicy policy) throws Exception;

    /**
     * 按文件记录编号读取并校验存储侧 Retention/Object Lock/legal hold 证据。
     *
     * @param fileId 文件记录编号
     * @param policy 存储保留策略
     * @return 存储侧证据
     */
    StorageRetentionEvidence requireStorageRetentionEvidence(Long fileId, StorageRetentionPolicy policy) throws Exception;

    /**
     * 按文件配置编号和路径读取并校验存储侧 Retention/Object Lock/legal hold 证据。
     *
     * @param configId 配置编号
     * @param path 文件路径
     * @param policy 存储保留策略
     * @return 存储侧证据
     */
    StorageRetentionEvidence requireStorageRetentionEvidence(Long configId, String path,
                                                            StorageRetentionPolicy policy) throws Exception;

}
