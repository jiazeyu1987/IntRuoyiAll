package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrPrintEventDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrPrintEventMapper extends BaseMapperX<MesProEdhrPrintEventDO> {

    default PageResult<MesProEdhrPrintEventDO> selectPage(PageParam pageParam, Long printTaskId) {
        return selectPage(pageParam, new LambdaQueryWrapperX<MesProEdhrPrintEventDO>()
                .eqIfPresent(MesProEdhrPrintEventDO::getPrintTaskId, printTaskId)
                .orderByDesc(MesProEdhrPrintEventDO::getOccurredAt));
    }
}
