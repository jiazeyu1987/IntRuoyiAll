package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DccElectronicSignatureAuthorizationMapper extends BaseMapperX<DccElectronicSignatureAuthorizationDO> {

    default DccElectronicSignatureAuthorizationDO selectByUserId(Long userId) {
        return selectOne(DccElectronicSignatureAuthorizationDO::getUserId, userId);
    }

    default List<DccElectronicSignatureAuthorizationDO> selectListByUserIds(Collection<Long> userIds) {
        return selectList(DccElectronicSignatureAuthorizationDO::getUserId, userIds);
    }
}
