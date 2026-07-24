package cn.iocoder.yudao.module.showroom.dal.mysql.content;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductRevisionRelationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomProductRevisionRelationMapper extends BaseMapperX<ShowroomProductRevisionRelationDO> {

    default List<ShowroomProductRevisionRelationDO> selectListByProductRevisionId(Long productRevisionId) {
        return selectList(new LambdaQueryWrapperX<ShowroomProductRevisionRelationDO>()
                .eq(ShowroomProductRevisionRelationDO::getProductRevisionId, productRevisionId)
                .orderByAsc(ShowroomProductRevisionRelationDO::getId));
    }

}
