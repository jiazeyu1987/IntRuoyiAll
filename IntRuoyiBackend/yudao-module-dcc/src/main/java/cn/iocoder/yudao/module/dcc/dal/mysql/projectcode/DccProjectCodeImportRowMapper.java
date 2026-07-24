package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeImportRowDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccProjectCodeImportRowMapper extends BaseMapperX<DccProjectCodeImportRowDO> {

    default List<DccProjectCodeImportRowDO> selectListByBatchId(Long batchId) {
        return selectList(new LambdaQueryWrapperX<DccProjectCodeImportRowDO>()
                .eq(DccProjectCodeImportRowDO::getBatchId, batchId)
                .orderByAsc(DccProjectCodeImportRowDO::getRowNo));
    }
}
