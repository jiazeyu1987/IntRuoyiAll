package cn.iocoder.yudao.module.srm.dal.mysql.procurement;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmSourcingProjectLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmSourcingProjectLineMapper extends BaseMapperX<SrmSourcingProjectLineDO> {

    default List<SrmSourcingProjectLineDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<SrmSourcingProjectLineDO>()
                .eq(SrmSourcingProjectLineDO::getProjectId, projectId)
                .orderByAsc(SrmSourcingProjectLineDO::getId));
    }
}
