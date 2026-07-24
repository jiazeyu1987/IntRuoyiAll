package cn.iocoder.yudao.module.system.service.tablecolumn;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.tablecolumn.vo.UserTableColumnConfigRespVO;
import cn.iocoder.yudao.module.system.controller.admin.tablecolumn.vo.UserTableColumnConfigSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.tablecolumn.UserTableColumnConfigDO;
import cn.iocoder.yudao.module.system.dal.mysql.tablecolumn.UserTableColumnConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.USER_TABLE_COLUMN_CONFIG_INVALID;

@Service
@Validated
public class UserTableColumnConfigServiceImpl implements UserTableColumnConfigService {

    private static final int SCHEMA_VERSION = 1;
    private static final int MIN_WIDTH = 40;
    private static final int MAX_WIDTH = 1200;

    @Resource
    private UserTableColumnConfigMapper userTableColumnConfigMapper;

    @Override
    public UserTableColumnConfigRespVO getConfig(String tableKey) {
        validateTableKey(tableKey);
        UserTableColumnConfigDO config = userTableColumnConfigMapper.selectByUserAndTableKey(getLoginUserId(), tableKey);
        if (config == null) {
            return null;
        }
        return JsonUtils.parseObject(config.getConfigJson(), UserTableColumnConfigRespVO.class);
    }

    @Override
    public void saveConfig(UserTableColumnConfigSaveReqVO reqVO) {
        validateSaveReqVO(reqVO);
        Long loginUserId = getLoginUserId();
        UserTableColumnConfigRespVO config = buildConfig(reqVO);
        UserTableColumnConfigDO existing = userTableColumnConfigMapper.selectByUserAndTableKey(loginUserId,
                reqVO.getTableKey());
        if (existing == null) {
            UserTableColumnConfigDO createObj = new UserTableColumnConfigDO()
                    .setUserId(loginUserId)
                    .setTableKey(reqVO.getTableKey())
                    .setConfigJson(JsonUtils.toJsonString(config));
            createObj.setTenantId(TenantContextHolder.getRequiredTenantId());
            userTableColumnConfigMapper.insert(createObj);
            return;
        }
        existing.setConfigJson(JsonUtils.toJsonString(config));
        userTableColumnConfigMapper.updateById(existing);
    }

    @Override
    public void resetConfig(String tableKey) {
        validateTableKey(tableKey);
        userTableColumnConfigMapper.deleteByUserAndTableKey(getLoginUserId(), tableKey);
    }

    private UserTableColumnConfigRespVO buildConfig(UserTableColumnConfigSaveReqVO reqVO) {
        List<UserTableColumnConfigRespVO.Column> columns = reqVO.getColumns().stream()
                .map(column -> new UserTableColumnConfigRespVO.Column()
                        .setKey(StrUtil.trim(column.getKey()))
                        .setVisible(column.getVisible())
                        .setWidth(column.getWidth()))
                .toList();
        return new UserTableColumnConfigRespVO()
                .setSchemaVersion(SCHEMA_VERSION)
                .setTableKey(StrUtil.trim(reqVO.getTableKey()))
                .setColumns(columns)
                .setUpdatedAt(LocalDateTime.now());
    }

    private void validateSaveReqVO(UserTableColumnConfigSaveReqVO reqVO) {
        if (reqVO == null) {
            throw exception(USER_TABLE_COLUMN_CONFIG_INVALID);
        }
        validateTableKey(reqVO.getTableKey());
        if (CollUtil.isEmpty(reqVO.getColumns())) {
            throw exception(USER_TABLE_COLUMN_CONFIG_INVALID);
        }
        for (UserTableColumnConfigSaveReqVO.Column column : reqVO.getColumns()) {
            if (column == null || StrUtil.isBlank(column.getKey()) || column.getVisible() == null) {
                throw exception(USER_TABLE_COLUMN_CONFIG_INVALID);
            }
            Integer width = column.getWidth();
            if (width != null && (width < MIN_WIDTH || width > MAX_WIDTH)) {
                throw exception(USER_TABLE_COLUMN_CONFIG_INVALID);
            }
        }
    }

    private void validateTableKey(String tableKey) {
        if (StrUtil.isBlank(tableKey)) {
            throw exception(USER_TABLE_COLUMN_CONFIG_INVALID);
        }
    }

    private Long getLoginUserId() {
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null) {
            throw exception(USER_TABLE_COLUMN_CONFIG_INVALID);
        }
        return loginUserId;
    }

}
