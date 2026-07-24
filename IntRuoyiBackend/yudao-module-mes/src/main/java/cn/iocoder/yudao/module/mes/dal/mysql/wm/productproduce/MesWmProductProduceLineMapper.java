package cn.iocoder.yudao.module.mes.dal.mysql.wm.productproduce;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.wm.productproduce.vo.MesWmProductProduceLinePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productproduce.MesWmProductProduceLineDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * MES 生产入库单行 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesWmProductProduceLineMapper extends BaseMapperX<MesWmProductProduceLineDO> {

    default PageResult<MesWmProductProduceLineDO> selectPage(MesWmProductProduceLinePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmProductProduceLineDO>()
                .eqIfPresent(MesWmProductProduceLineDO::getFeedbackId, reqVO.getFeedbackId())
                .orderByDesc(MesWmProductProduceLineDO::getId));
    }

    default List<MesWmProductProduceLineDO> selectListByProduceId(Long produceId) {
        return selectList(MesWmProductProduceLineDO::getProduceId, produceId);
    }

    default List<MesWmProductProduceLineDO> selectListByFeedbackId(Long feedbackId) {
        return selectList(MesWmProductProduceLineDO::getFeedbackId, feedbackId);
    }

    @Select("""
            SELECT DISTINCT ppl.batch_code
            FROM mes_wm_product_produce_line ppl
            INNER JOIN mes_wm_product_produce pp ON pp.id = ppl.produce_id AND pp.deleted = 0
            WHERE pp.work_order_id = #{workOrderId}
              AND ppl.deleted = 0
              AND ppl.batch_code IS NOT NULL
              AND ppl.batch_code <> ''
            ORDER BY ppl.batch_code
            """)
    List<String> selectDistinctBatchCodesByWorkOrderId(@Param("workOrderId") Long workOrderId);

}
