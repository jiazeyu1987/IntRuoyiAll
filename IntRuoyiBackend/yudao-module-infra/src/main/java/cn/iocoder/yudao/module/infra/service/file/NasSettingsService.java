package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigSaveReqVO;

/**
 * NAS 配置 Service
 */
public interface NasSettingsService {

    /**
     * 获得当前 NAS 配置
     */
    FileNasConfigRespVO getNasConfig();

    /**
     * 保存 NAS 配置
     */
    void saveNasConfig(FileNasConfigSaveReqVO reqVO);

    /**
     * 将请求参数转换成连接配置
     */
    NasConnectionConfig toConnectionConfig(FileNasConfigSaveReqVO reqVO);

    /**
     * 获取当前已保存的 NAS 配置；缺失时显式失败
     */
    NasConnectionConfig getRequiredNasConfig();
}
