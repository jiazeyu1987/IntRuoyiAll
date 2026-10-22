package cn.iocoder.yudao.module.dcc.dal.mysql.category;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileTypeTaxonomyDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * DCC controlled-file type taxonomy mapper.
 */
@Mapper
public interface DccFileTypeTaxonomyMapper extends BaseMapperX<DccFileTypeTaxonomyDO> {
    @Select("SELECT * FROM dcc_file_type_taxonomy WHERE tenant_id=#{tenantId} AND id=#{id} AND deleted=0 FOR UPDATE")
    DccFileTypeTaxonomyDO selectMappingTaxonomyForUpdate(@Param("tenantId") Long tenantId, @Param("id") Long id);
}
