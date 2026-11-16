package cn.iocoder.yudao.module.mes.dal.mysql.qc.indicator;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qc.indicator.vo.MesQcIndicatorPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qc.indicator.MesQcIndicatorDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 质检指标 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesQcIndicatorMapper extends BaseMapperX<MesQcIndicatorDO> {

    java.util.Map<String, ListFilterQueryUtils.Field> LIST_FILTER_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("id", ListFilterQueryUtils.number("id")),
            java.util.Map.entry("code", ListFilterQueryUtils.text("code")),
            java.util.Map.entry("name", ListFilterQueryUtils.text("name")),
            java.util.Map.entry("type", ListFilterQueryUtils.number("type")),
            java.util.Map.entry("tool", ListFilterQueryUtils.text("tool")),
            java.util.Map.entry("resultType", ListFilterQueryUtils.number("result_type")),
            java.util.Map.entry("resultSpecification", ListFilterQueryUtils.text("result_specification")),
            java.util.Map.entry("remark", ListFilterQueryUtils.text("remark")),
            java.util.Map.entry("createTime", ListFilterQueryUtils.dateTime("create_time")),
            java.util.Map.entry("creator", ListFilterQueryUtils.text("creator"))
    );



    default PageResult<MesQcIndicatorDO> selectPage(MesQcIndicatorPageReqVO reqVO) {
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, new LambdaQueryWrapperX<MesQcIndicatorDO>()
                .likeIfPresent(MesQcIndicatorDO::getCode, reqVO.getCode())
                .likeIfPresent(MesQcIndicatorDO::getName, reqVO.getName())
                .eqIfPresent(MesQcIndicatorDO::getType, reqVO.getType())
                .eqIfPresent(MesQcIndicatorDO::getResultType, reqVO.getResultType())
                .orderByDesc(MesQcIndicatorDO::getId), LIST_FILTER_FIELDS));
    }

    default MesQcIndicatorDO selectByCode(String code) {
        return selectOne(MesQcIndicatorDO::getCode, code);
    }

    default MesQcIndicatorDO selectByName(String name) {
        return selectOne(MesQcIndicatorDO::getName, name);
    }

    default List<MesQcIndicatorDO> selectList() {
        return selectList(new LambdaQueryWrapperX<MesQcIndicatorDO>()
                .orderByDesc(MesQcIndicatorDO::getId));
    }

}
