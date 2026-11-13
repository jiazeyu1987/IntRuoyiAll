package cn.iocoder.yudao.module.infra.service.file;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.config.FileConfigPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.config.FileConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.convert.file.FileConfigConvert;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileConfigDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileConfigMapper;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClientConfig;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClientFactory;
import cn.iocoder.yudao.module.infra.framework.file.core.enums.FileStorageEnum;
import jakarta.annotation.Resource;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_CONFIG_DELETE_FAIL_MASTER;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_CONFIG_NOT_EXISTS;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_CONFIG_PROTECTED;

/**
 * 文件配置 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
@Slf4j
public class FileConfigServiceImpl implements FileConfigService {

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private List<ProtectedFileClientProvider> protectedFileClientProviders = List.of();

    @Resource
    private cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper fileMapper;

    @Resource
    private FileClientFactory fileClientFactory;

    @Resource
    private FileConfigMapper fileConfigMapper;

    @Resource
    private Validator validator;



    @Override
    public Long createFileConfig(FileConfigSaveReqVO createReqVO) {
        validateNotProtectedFileConfig(createReqVO.getId());
        FileConfigDO fileConfig = FileConfigConvert.INSTANCE.convert(createReqVO)
                .setConfig(parseClientConfig(createReqVO.getStorage(), createReqVO.getConfig()))
                .setMaster(false); // 默认非 master
        fileConfigMapper.insert(fileConfig);
        return fileConfig.getId();
    }

    @Override
    public void updateFileConfig(FileConfigSaveReqVO updateReqVO) {
        // 校验存在
        FileConfigDO config = validateFileConfigExists(updateReqVO.getId());
        validateNotProtectedFileConfig(config.getId());
        // 更新
        FileConfigDO updateObj = FileConfigConvert.INSTANCE.convert(updateReqVO)
                .setConfig(parseClientConfig(config.getStorage(), updateReqVO.getConfig()));
        fileConfigMapper.updateById(updateObj);


    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFileConfigMaster(Long id) {
        // 校验存在
        validateFileConfigExists(id);
        validateProtectedFileConfigNotTouchedByMasterSwitch();
        Long tenantId = cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        if (fileConfigMapper.lockTenantConfiguration(tenantId) == null) {
            throw new IllegalStateException("TENANT_FILE_CONFIGURATION_NOT_INITIALIZED");
        }
        // 更新其它为非 master
        fileConfigMapper.updateBatch(new FileConfigDO().setMaster(false));
        // 更新
        fileConfigMapper.updateById(new FileConfigDO().setId(id).setMaster(true));


    }

    private FileClientConfig parseClientConfig(Integer storage, Map<String, Object> config) {
        // 获取配置类
        Class<? extends FileClientConfig> configClass = FileStorageEnum.getByStorage(storage)
                .getConfigClass();
        FileClientConfig clientConfig = JsonUtils.parseObject2(JsonUtils.toJsonString(config), configClass);
        // 参数校验
        ValidationUtils.validate(validator, clientConfig);
        // 设置参数
        return clientConfig;
    }

    @Override
    public void deleteFileConfig(Long id) {
        // 校验存在
        FileConfigDO config = validateFileConfigExists(id);
        validateNotProtectedFileConfig(config.getId());
        if (Boolean.TRUE.equals(config.getMaster())) {
            throw exception(FILE_CONFIG_DELETE_FAIL_MASTER);
        }
        // 删除
        fileConfigMapper.deleteById(id);


    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileConfigList(List<Long> ids) {
        // 校验是否有主配置
        List<FileConfigDO> configs = ids.stream().map(this::validateFileConfigExists).toList();
        for (FileConfigDO config : configs) {
            validateNotProtectedFileConfig(config.getId());
            if (Boolean.TRUE.equals(config.getMaster())) {
                throw exception(FILE_CONFIG_DELETE_FAIL_MASTER);
            }
        }

        // 批量删除
        fileConfigMapper.deleteByIds(ids);


    }

    private FileConfigDO validateFileConfigExists(Long id) {
        FileConfigDO config = fileConfigMapper.selectById(id);
        if (config == null) {
            throw exception(FILE_CONFIG_NOT_EXISTS);
        }
        return config;
    }

    private void validateNotProtectedFileConfig(Long id) {
        if (ShowroomProtectedFileRules.isProtectedFileConfig(id)) {
            throw exception(FILE_CONFIG_PROTECTED);
        }
    }

    private void validateProtectedFileConfigNotTouchedByMasterSwitch() {
        if (fileConfigMapper.selectById(ShowroomProtectedFileRules.FILE_CONFIG_ID) != null) {
            throw exception(FILE_CONFIG_PROTECTED);
        }
    }

    @Override
    public FileConfigDO getFileConfig(Long id) {
        return fileConfigMapper.selectById(id);
    }

    @Override
    public PageResult<FileConfigDO> getFileConfigPage(FileConfigPageReqVO pageReqVO) {
        return fileConfigMapper.selectPage(pageReqVO);
    }

    @Override
    public String testFileConfig(Long id) throws Exception {
        // 校验存在
        validateFileConfigExists(id);
        // 上传文件
        byte[] content = ResourceUtil.readBytes("file/erweima.jpg");
        String path = TenantFilePath.qualify("storage-test/" + IdUtil.fastSimpleUUID() + ".jpg");
        String url = getFileClient(id).upload(content, path, "image/jpeg");
        fileMapper.insert(new cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO()
                .setConfigId(id).setName("storage-test.jpg").setPath(path).setUrl(url)
                .setType("image/jpeg").setSize((long) content.length));
        return url;
    }

    @Override
    public FileClient getFileClient(Long id) {
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        if (id != null && id < 0) {
            List<ProtectedFileClientProvider> providers = protectedFileClientProviders.stream()
                    .filter(provider -> id.equals(provider.getFileConfigId())).toList();
            if (providers.size() != 1) throw new IllegalStateException("受保护存储必须有唯一的正式配置来源");
            return providers.get(0).getFileClient();
        }
        return clientFor(validateFileConfigExists(id));
    }

    @Override
    public FileClient getMasterFileClient() {
        FileConfigDO config = fileConfigMapper.selectByMaster();
        if (config == null) throw exception(FILE_CONFIG_NOT_EXISTS);
        return clientFor(config);
    }

    @Override
    public void clearFileClientCache(Long id) {
        getFileClient(id);
    }

    private FileClient clientFor(FileConfigDO config) {
        fileClientFactory.createOrUpdateFileClient(config.getId(), config.getStorage(), config.getConfig());
        return fileClientFactory.getFileClient(config.getId());
    }

}
