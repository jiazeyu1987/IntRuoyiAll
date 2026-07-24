package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasDirectoryTreeRespVO;
import jakarta.validation.constraints.NotBlank;

/**
 * NAS 目录读取 Service
 */
public interface NasDirectoryService {

    /**
     * 读取指定目录树
     *
     * @param path 服务器可访问目录路径
     * @return 目录树
     */
    FileNasDirectoryTreeRespVO getNasDirectoryTree(@NotBlank(message = "目录路径不能为空") String path);
}
