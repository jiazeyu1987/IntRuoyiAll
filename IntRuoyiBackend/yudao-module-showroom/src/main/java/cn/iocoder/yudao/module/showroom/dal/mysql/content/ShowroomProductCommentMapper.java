package cn.iocoder.yudao.module.showroom.dal.mysql.content;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.content.ShowroomProductCommentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomProductCommentMapper extends BaseMapperX<ShowroomProductCommentDO> {

    default List<ShowroomProductCommentDO> selectListByProductId(Long productId) {
        return selectList(new LambdaQueryWrapperX<ShowroomProductCommentDO>()
                .eq(ShowroomProductCommentDO::getProductId, productId)
                .orderByAsc(ShowroomProductCommentDO::getCreatedAt)
                .orderByAsc(ShowroomProductCommentDO::getId));
    }

}
