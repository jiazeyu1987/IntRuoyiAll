package cn.iocoder.yudao.module.dcc.dal.mysql.file;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDistributionRecipientDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccControlledFileDistributionRecipientMapper extends BaseMapperX<DccControlledFileDistributionRecipientDO> {

    default List<DccControlledFileDistributionRecipientDO> selectListByDistributionId(Long distributionId) {
        return selectList(DccControlledFileDistributionRecipientDO::getDistributionId, distributionId);
    }

    default List<DccControlledFileDistributionRecipientDO> selectListByUserId(Long userId) {
        return selectList(DccControlledFileDistributionRecipientDO::getUserId, userId);
    }
}
