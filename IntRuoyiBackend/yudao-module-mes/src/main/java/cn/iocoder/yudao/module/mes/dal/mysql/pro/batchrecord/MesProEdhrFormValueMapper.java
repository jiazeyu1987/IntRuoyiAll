package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrFormValueDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrFormValueMapper extends BaseMapperX<MesProEdhrFormValueDO> {

    default List<MesProEdhrFormValueDO> selectListByInstanceId(Long instanceId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrFormValueDO>()
                .eq(MesProEdhrFormValueDO::getInstanceId, instanceId)
                .orderByAsc(MesProEdhrFormValueDO::getId));
    }

    default void deleteByInstanceId(Long instanceId) {
        delete(new LambdaQueryWrapperX<MesProEdhrFormValueDO>()
                .eq(MesProEdhrFormValueDO::getInstanceId, instanceId));
    }
}
