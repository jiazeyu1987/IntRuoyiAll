package cn.iocoder.yudao.module.mdm.dal.mysql.product;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mdm.dal.dataobject.product.MdmProductImportRowDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MdmProductImportRowMapper extends BaseMapperX<MdmProductImportRowDO> {

    default List<MdmProductImportRowDO> selectListByBatchId(Long batchId) {
        return selectList(new LambdaQueryWrapperX<MdmProductImportRowDO>()
                .eq(MdmProductImportRowDO::getBatchId, batchId)
                .orderByAsc(MdmProductImportRowDO::getRowNo));
    }

}
