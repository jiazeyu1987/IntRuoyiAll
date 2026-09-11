package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectFileTemplateItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccProjectFileTemplateItemMapper extends BaseMapperX<DccProjectFileTemplateItemDO> {

    default List<DccProjectFileTemplateItemDO> selectListByProjectCodeId(Long projectCodeId) {
        return selectList(new LambdaQueryWrapperX<DccProjectFileTemplateItemDO>()
                .eq(DccProjectFileTemplateItemDO::getProjectCodeId, projectCodeId)
                .orderByAsc(DccProjectFileTemplateItemDO::getSortOrder)
                .orderByAsc(DccProjectFileTemplateItemDO::getId));
    }

    default int deleteByProjectCodeId(Long projectCodeId) {
        return delete(new LambdaQueryWrapperX<DccProjectFileTemplateItemDO>()
                .eq(DccProjectFileTemplateItemDO::getProjectCodeId, projectCodeId));
    }
}
