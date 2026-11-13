package cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListGroupRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionMaterialListDO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * ERP 生产用料清单同步明细 Mapper
 *
 * @author Codex
 */
@Mapper
public interface MesKingdeeProductionMaterialListMapper extends BaseMapperX<MesKingdeeProductionMaterialListDO> {

    IPage<MesKingdeeProductionMaterialListGroupRespVO> selectGroupPage(
            IPage<MesKingdeeProductionMaterialListGroupRespVO> page,
            @Param("reqVO") MesKingdeeProductionMaterialListPageReqVO reqVO,
            @Param("ew") com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Object> filters);

    List<MesKingdeeProductionMaterialListGroupRespVO> selectGroupList(
            @Param("reqVO") MesKingdeeProductionMaterialListPageReqVO reqVO,
            @Param("ew") com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Object> filters);

    default PageResult<MesKingdeeProductionMaterialListDO> selectPage(
            MesKingdeeProductionMaterialListPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO>()
                .eqIfPresent(MesKingdeeProductionMaterialListDO::getSourceBillNo, reqVO.getSyncCode())
                .likeIfPresent(MesKingdeeProductionMaterialListDO::getSourceBillNo, reqVO.getSourceBillNo())
                .likeIfPresent(MesKingdeeProductionMaterialListDO::getProductCode, reqVO.getProductCode())
                .eqIfPresent(MesKingdeeProductionMaterialListDO::getProductionOrderNo, reqVO.getProductionOrderNo())
                .likeIfPresent(MesKingdeeProductionMaterialListDO::getChildMaterialCode, reqVO.getChildMaterialCode())
                .likeIfPresent(MesKingdeeProductionMaterialListDO::getChildMaterialName, reqVO.getChildMaterialName())
                .betweenIfPresent(MesKingdeeProductionMaterialListDO::getSourceModifyTime, reqVO.getSourceModifyTime())
                .betweenIfPresent(MesKingdeeProductionMaterialListDO::getLastSyncTime, reqVO.getLastSyncTime())
                .orderByDesc(MesKingdeeProductionMaterialListDO::getSourceModifyTime)
                .orderByDesc(MesKingdeeProductionMaterialListDO::getId));
    }

    default List<MesKingdeeProductionMaterialListDO> selectBySourceLines(
            Collection<MesKingdeeProductionMaterialListDO> keys) {
        List<MesKingdeeProductionMaterialListDO> keyList = new java.util.ArrayList<>(keys);
        List<MesKingdeeProductionMaterialListDO> result = new java.util.ArrayList<>();
        // Bound expression depth: the configured JSQLParser serialization cache overflows on 1000 ORs.
        for (int offset = 0; offset < keyList.size(); offset += 100) {
            List<MesKingdeeProductionMaterialListDO> batch = keyList.subList(offset, Math.min(offset + 100, keyList.size()));
            LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO> query = new LambdaQueryWrapperX<>();
            // Existing payloads can be large; deduplication/backfill needs only these columns.
            query.select(MesKingdeeProductionMaterialListDO::getId,
                    MesKingdeeProductionMaterialListDO::getSourceBillNo,
                    MesKingdeeProductionMaterialListDO::getProductionOrderNo,
                    MesKingdeeProductionMaterialListDO::getProductionOrderLineNo,
                    MesKingdeeProductionMaterialListDO::getChildMaterialCode,
                    MesKingdeeProductionMaterialListDO::getDrawingNumber,
                    MesKingdeeProductionMaterialListDO::getDemandTime);
            query.and(group -> batch.forEach(key -> group.or(line -> line
                    .eq(MesKingdeeProductionMaterialListDO::getSourceBillNo, key.getSourceBillNo())
                    .eq(MesKingdeeProductionMaterialListDO::getProductionOrderNo, key.getProductionOrderNo())
                    .eq(MesKingdeeProductionMaterialListDO::getProductionOrderLineNo, key.getProductionOrderLineNo())
                    .eq(MesKingdeeProductionMaterialListDO::getChildMaterialCode, key.getChildMaterialCode()))));
            result.addAll(selectList(query));
        }
        return result;
    }

    default MesKingdeeProductionMaterialListDO selectBySourceLine(String sourceBillNo, String productionOrderNo,
                                                                  Integer productionOrderLineNo,
                                                                  String childMaterialCode) {
        return selectOne(new LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO>()
                .eq(MesKingdeeProductionMaterialListDO::getSourceBillNo, sourceBillNo)
                .eq(MesKingdeeProductionMaterialListDO::getProductionOrderNo, productionOrderNo)
                .eq(MesKingdeeProductionMaterialListDO::getProductionOrderLineNo, productionOrderLineNo)
                .eq(MesKingdeeProductionMaterialListDO::getChildMaterialCode, childMaterialCode));
    }

    default List<MesKingdeeProductionMaterialListDO> selectListByProductionOrderNo(String productionOrderNo) {
        return selectList(new LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO>()
                .eq(MesKingdeeProductionMaterialListDO::getProductionOrderNo, productionOrderNo)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getProductionOrderLineNo)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getId));
    }

    default List<MesKingdeeProductionMaterialListDO> selectListByProductCode(String productCode) {
        return selectList(new LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO>()
                .eq(MesKingdeeProductionMaterialListDO::getProductCode, productCode)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getProductionOrderNo)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getProductionOrderLineNo)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getId));
    }

    default List<MesKingdeeProductionMaterialListDO> selectListByWorkOrderId(Long workOrderId) {
        return selectList(new LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO>()
                .eq(MesKingdeeProductionMaterialListDO::getWorkOrderId, workOrderId)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getId));
    }

    default List<MesKingdeeProductionMaterialListDO> selectListByWorkOrderIds(Collection<Long> workOrderIds) {
        return selectList(new LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO>()
                .inIfPresent(MesKingdeeProductionMaterialListDO::getWorkOrderId, workOrderIds)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getWorkOrderId)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getId));
    }

    default List<MesKingdeeProductionMaterialListDO> selectListBySourceBillNo(String sourceBillNo) {
        return selectList(new LambdaQueryWrapperX<MesKingdeeProductionMaterialListDO>()
                .eq(MesKingdeeProductionMaterialListDO::getSourceBillNo, sourceBillNo)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getProductionOrderLineNo)
                .orderByAsc(MesKingdeeProductionMaterialListDO::getId));
    }


    static com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Object> groupFilters(
            MesKingdeeProductionMaterialListPageReqVO reqVO) {
        return cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.wrapper(reqVO, java.util.Map.ofEntries(
                java.util.Map.entry("productCode", cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.textCollection("COALESCE(JSON_UNQUOTE(CONCAT(CHAR(34), REPLACE(SUBSTRING(CAST((SELECT JSON_ARRAYAGG(COALESCE(i.product_code, '')) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_kingdee_production_material_list i WHERE i.source_bill_no=summary.source_bill_no AND i.tenant_id=summary.tenant_id AND i.deleted=0 LIMIT 1) AS CHAR), 3, CHAR_LENGTH(CAST((SELECT JSON_ARRAYAGG(COALESCE(i.product_code, '')) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_kingdee_production_material_list i WHERE i.source_bill_no=summary.source_bill_no AND i.tenant_id=summary.tenant_id AND i.deleted=0 LIMIT 1) AS CHAR))-4), CONCAT(CHAR(34), ', ', CHAR(34)), ' '), CHAR(34))), '')", "SELECT 1 FROM mes_kingdee_production_material_list i WHERE i.source_bill_no=summary.source_bill_no AND i.tenant_id=summary.tenant_id AND i.deleted=0", "i.product_code")),
                java.util.Map.entry("productionOrderNo", cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.textCollection("COALESCE(JSON_UNQUOTE(CONCAT(CHAR(34), REPLACE(SUBSTRING(CAST((SELECT JSON_ARRAYAGG(COALESCE(i.production_order_no, '')) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_kingdee_production_material_list i WHERE i.source_bill_no=summary.source_bill_no AND i.tenant_id=summary.tenant_id AND i.deleted=0 LIMIT 1) AS CHAR), 3, CHAR_LENGTH(CAST((SELECT JSON_ARRAYAGG(COALESCE(i.production_order_no, '')) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_kingdee_production_material_list i WHERE i.source_bill_no=summary.source_bill_no AND i.tenant_id=summary.tenant_id AND i.deleted=0 LIMIT 1) AS CHAR))-4), CONCAT(CHAR(34), ', ', CHAR(34)), ' '), CHAR(34))), '')", "SELECT 1 FROM mes_kingdee_production_material_list i WHERE i.source_bill_no=summary.source_bill_no AND i.tenant_id=summary.tenant_id AND i.deleted=0", "i.production_order_no")),
                java.util.Map.entry("childMaterialCode", cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.textCollection("COALESCE(JSON_UNQUOTE(CONCAT(CHAR(34), REPLACE(SUBSTRING(CAST((SELECT JSON_ARRAYAGG(COALESCE(i.child_material_code, '')) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_kingdee_production_material_list i WHERE i.source_bill_no=summary.source_bill_no AND i.tenant_id=summary.tenant_id AND i.deleted=0 LIMIT 1) AS CHAR), 3, CHAR_LENGTH(CAST((SELECT JSON_ARRAYAGG(COALESCE(i.child_material_code, '')) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_kingdee_production_material_list i WHERE i.source_bill_no=summary.source_bill_no AND i.tenant_id=summary.tenant_id AND i.deleted=0 LIMIT 1) AS CHAR))-4), CONCAT(CHAR(34), ', ', CHAR(34)), ' '), CHAR(34))), '')", "SELECT 1 FROM mes_kingdee_production_material_list i WHERE i.source_bill_no=summary.source_bill_no AND i.tenant_id=summary.tenant_id AND i.deleted=0", "i.child_material_code")),
                java.util.Map.entry("childMaterialName", cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.textCollection("COALESCE(JSON_UNQUOTE(CONCAT(CHAR(34), REPLACE(SUBSTRING(CAST((SELECT JSON_ARRAYAGG(COALESCE(i.child_material_name, '')) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_kingdee_production_material_list i WHERE i.source_bill_no=summary.source_bill_no AND i.tenant_id=summary.tenant_id AND i.deleted=0 LIMIT 1) AS CHAR), 3, CHAR_LENGTH(CAST((SELECT JSON_ARRAYAGG(COALESCE(i.child_material_name, '')) OVER (ORDER BY i.id ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) FROM mes_kingdee_production_material_list i WHERE i.source_bill_no=summary.source_bill_no AND i.tenant_id=summary.tenant_id AND i.deleted=0 LIMIT 1) AS CHAR))-4), CONCAT(CHAR(34), ', ', CHAR(34)), ' '), CHAR(34))), '')", "SELECT 1 FROM mes_kingdee_production_material_list i WHERE i.source_bill_no=summary.source_bill_no AND i.tenant_id=summary.tenant_id AND i.deleted=0", "i.child_material_name")),
                java.util.Map.entry("sourceBillNo", cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text("summary.source_bill_no")),
                java.util.Map.entry("lineCount", cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.number("summary.line_count")),
                java.util.Map.entry("sourceModifyTime", cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.dateTime("summary.source_modify_time")),
                java.util.Map.entry("lastSyncTime", cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.dateTime("summary.last_sync_time")),
                java.util.Map.entry("productionOrderSummary", cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.text("CONCAT(COALESCE(summary.production_order_summary, ''), ' ', COALESCE(summary.production_order_summary, '无'), ' ', summary.production_order_count)"))
        ));
    }
}
