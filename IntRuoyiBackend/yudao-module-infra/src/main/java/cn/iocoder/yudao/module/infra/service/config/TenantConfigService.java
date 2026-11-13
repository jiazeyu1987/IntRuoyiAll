package cn.iocoder.yudao.module.infra.service.config;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.TenantConfigDO;
import cn.iocoder.yudao.module.infra.dal.mysql.config.TenantConfigMapper;
import cn.iocoder.yudao.module.infra.enums.config.ConfigTypeEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.config.vo.ConfigPageReqVO;
import java.util.List;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.CONFIG_KEY_DUPLICATE;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.CONFIG_NOT_EXISTS;

/** Explicit tenant predicates also protect callers executing inside a TenantIgnore context. */
@Service
public class TenantConfigService {
    @Resource
    private TenantConfigMapper mapper;

    @Resource
    private cn.iocoder.yudao.module.infra.dal.mysql.config.ConfigMapper platformConfigMapper;

    /** One-time snapshot during tenant creation, in the caller's tenant creation transaction. */
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void initializeFromPlatformTemplate() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (mapper.selectCount(scope()) != 0) {
            throw new IllegalStateException("TENANT_CONFIGURATION_ALREADY_INITIALIZED");
        }
        List<ConfigDO> template = platformConfigMapper.selectList().stream()
                .filter(config -> !PlatformConfigKeys.contains(config.getConfigKey())).toList();
        if (template.isEmpty()) throw new IllegalStateException("TENANT_CONFIGURATION_TEMPLATE_REQUIRED");
        for (ConfigDO config : template) {
            TenantConfigDO row = BeanUtils.toBean(config, TenantConfigDO.class);
            row.setId(null);
            row.setTenantId(tenantId);
            mapper.insert(row);
        }
    }

    public ConfigDO getConfigByKey(String key) {
        TenantConfigDO row = mapper.selectOne(scope().eq(TenantConfigDO::getConfigKey, key));
        return row == null ? null : BeanUtils.toBean(row, ConfigDO.class);
    }

    public Long createConfig(ConfigSaveReqVO request) {
        return createConfig(request, ConfigTypeEnum.CUSTOM.getType());
    }

    Long createConfig(ConfigSaveReqVO request, Integer type) {
        requireTenantKey(request.getKey());
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (getConfigByKey(request.getKey()) != null) {
            throw exception(CONFIG_KEY_DUPLICATE);
        }
        TenantConfigDO row = toRow(request);
        row.setId(null);
        row.setTenantId(tenantId);
        row.setType(type);
        mapper.insert(row);
        return row.getId();
    }

    public void updateConfig(ConfigSaveReqVO request) {
        updateConfig(request, null);
    }

    void updateConfig(ConfigSaveReqVO request, Integer type) {
        requireTenantKey(request.getKey());
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (request.getId() == null
                || mapper.selectOne(scope().eq(TenantConfigDO::getId, request.getId())) == null) {
            throw exception(CONFIG_NOT_EXISTS);
        }
        ConfigDO sameKey = getConfigByKey(request.getKey());
        if (sameKey != null && !sameKey.getId().equals(request.getId())) {
            throw exception(CONFIG_KEY_DUPLICATE);
        }
        TenantConfigDO row = toRow(request);
        row.setType(type);
        row.setTenantId(tenantId);
        if (mapper.update(row, scope().eq(TenantConfigDO::getId, request.getId())) != 1) {
            throw exception(CONFIG_NOT_EXISTS);
        }
    }

    public ConfigDO getConfig(Long id) {
        return BeanUtils.toBean(mapper.selectOne(scope().eq(TenantConfigDO::getId, id)), ConfigDO.class);
    }

    public PageResult<ConfigDO> getConfigPage(ConfigPageReqVO request) {
        PageResult<TenantConfigDO> page = mapper.selectPage(request, scope()
                .likeIfPresent(TenantConfigDO::getName, request.getName())
                .likeIfPresent(TenantConfigDO::getConfigKey, request.getKey())
                .eqIfPresent(TenantConfigDO::getType, request.getType())
                .betweenIfPresent(TenantConfigDO::getCreateTime, request.getCreateTime())
                .orderByDesc(TenantConfigDO::getId));
        return new PageResult<>(BeanUtils.toBean(page.getList(), ConfigDO.class), page.getTotal());
    }

    void deleteConfigs(List<Long> ids) {
        if (mapper.delete(scope().in(TenantConfigDO::getId, ids)) != ids.size()) {
            throw exception(CONFIG_NOT_EXISTS);
        }
    }

    static void requireTenantKey(String key) {
        if (PlatformConfigKeys.contains(key)) {
            throw exception(cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.CONFIG_PLATFORM_READ_ONLY);
        }
    }

    private LambdaQueryWrapperX<TenantConfigDO> scope() {
        return new LambdaQueryWrapperX<TenantConfigDO>()
                .eq(TenantConfigDO::getTenantId, TenantContextHolder.getRequiredTenantId());
    }

    private TenantConfigDO toRow(ConfigSaveReqVO request) {
        TenantConfigDO row = BeanUtils.toBean(request, TenantConfigDO.class);
        row.setConfigKey(request.getKey());
        return row;
    }
}
