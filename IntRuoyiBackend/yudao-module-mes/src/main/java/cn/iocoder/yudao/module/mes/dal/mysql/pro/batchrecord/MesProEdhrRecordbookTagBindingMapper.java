package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrRecordbookTagBindingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrRecordbookTagBindingMapper extends BaseMapperX<MesProEdhrRecordbookTagBindingDO> {

    default List<MesProEdhrRecordbookTagBindingDO> selectListByEntryId(Long entryId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrRecordbookTagBindingDO>()
                .eq(MesProEdhrRecordbookTagBindingDO::getEntryId, entryId)
                .orderByAsc(MesProEdhrRecordbookTagBindingDO::getId));
    }

    default void deleteByEntryId(Long entryId) {
        delete(new LambdaQueryWrapperX<MesProEdhrRecordbookTagBindingDO>()
                .eq(MesProEdhrRecordbookTagBindingDO::getEntryId, entryId));
    }
}
