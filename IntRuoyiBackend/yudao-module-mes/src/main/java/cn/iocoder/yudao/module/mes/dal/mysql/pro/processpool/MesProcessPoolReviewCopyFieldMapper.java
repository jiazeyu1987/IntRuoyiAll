package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProcessPoolReviewCopyFieldDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProcessPoolReviewCopyFieldMapper extends BaseMapperX<MesProcessPoolReviewCopyFieldDO> {

    default List<MesProcessPoolReviewCopyFieldDO> selectListByReviewCopyId(Long reviewCopyId) {
        return selectList(new LambdaQueryWrapperX<MesProcessPoolReviewCopyFieldDO>()
                .eq(MesProcessPoolReviewCopyFieldDO::getReviewCopyId, reviewCopyId)
                .orderByAsc(MesProcessPoolReviewCopyFieldDO::getId));
    }
}
