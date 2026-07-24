package cn.iocoder.yudao.module.showroom.dal.mysql.workflow;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomChangeRequestSignatureDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomChangeRequestSignatureMapper extends BaseMapperX<ShowroomChangeRequestSignatureDO> {

    default List<ShowroomChangeRequestSignatureDO> selectListByChangeRequestId(Long changeRequestId) {
        return selectList(new LambdaQueryWrapperX<ShowroomChangeRequestSignatureDO>()
                .eq(ShowroomChangeRequestSignatureDO::getChangeRequestId, changeRequestId)
                .orderByAsc(ShowroomChangeRequestSignatureDO::getId));
    }

}
