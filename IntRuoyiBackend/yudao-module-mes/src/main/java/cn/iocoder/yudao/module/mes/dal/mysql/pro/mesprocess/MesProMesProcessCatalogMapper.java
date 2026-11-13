package cn.iocoder.yudao.module.mes.dal.mysql.pro.mesprocess;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mesprocess.vo.MesProMesProcessPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess.MesProMesProcessCatalogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProMesProcessCatalogMapper extends BaseMapperX<MesProMesProcessCatalogDO> {

    java.util.Map<String, ListFilterQueryUtils.Field> LIST_FILTER_FIELDS = java.util.Map.ofEntries(
            java.util.Map.entry("id", ListFilterQueryUtils.number("id")),
            java.util.Map.entry("sourceFileName", ListFilterQueryUtils.text("source_file_name")),
            java.util.Map.entry("sourceSheetName", ListFilterQueryUtils.text("source_sheet_name")),
            java.util.Map.entry("sourceRowNo", ListFilterQueryUtils.number("source_row_no")),
            java.util.Map.entry("sortNo", ListFilterQueryUtils.number("sort_no")),
            java.util.Map.entry("catalogCode", ListFilterQueryUtils.text("catalog_code")),
            java.util.Map.entry("productName", ListFilterQueryUtils.text("product_name")),
            java.util.Map.entry("sourceMachineryCodes", ListFilterQueryUtils.text("source_machinery_codes")),
            java.util.Map.entry("mesProcessName", ListFilterQueryUtils.text("mes_process_name")),
            java.util.Map.entry("sourceMachineryName", ListFilterQueryUtils.text("source_machinery_name")),
            java.util.Map.entry("sourceMachineryQuantity", ListFilterQueryUtils.text("source_machinery_quantity")),
            java.util.Map.entry("dailyCapacity10_5", ListFilterQueryUtils.text("daily_capacity_10_5")),
            java.util.Map.entry("dailyWorkerQuantity", ListFilterQueryUtils.text("daily_worker_quantity")),
            java.util.Map.entry("mesProcessCode", ListFilterQueryUtils.text("mes_process_code")),
            java.util.Map.entry("processPrice", ListFilterQueryUtils.text("process_price")),
            java.util.Map.entry("feedbackFlag", ListFilterQueryUtils.text("feedback_flag")),
            java.util.Map.entry("batchRecordFlag", ListFilterQueryUtils.text("batch_record_flag")),
            java.util.Map.entry("batchRecordProcessName", ListFilterQueryUtils.text("batch_record_process_name")),
            java.util.Map.entry("createTime", ListFilterQueryUtils.dateTime("create_time")),
            java.util.Map.entry("creator", ListFilterQueryUtils.text("creator"))
    );



    default PageResult<MesProMesProcessCatalogDO> selectPage(MesProMesProcessPageReqVO reqVO) {
        LambdaQueryWrapperX<MesProMesProcessCatalogDO> wrapper = new LambdaQueryWrapperX<MesProMesProcessCatalogDO>()
                .eq(MesProMesProcessCatalogDO::getDeleted, false);
        String keyword = StrUtil.trim(reqVO.getKeyword());
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(query -> query
                    .like(MesProMesProcessCatalogDO::getProductName, keyword)
                    .or().like(MesProMesProcessCatalogDO::getMesProcessName, keyword)
                    .or().like(MesProMesProcessCatalogDO::getMesProcessCode, keyword)
                    .or().like(MesProMesProcessCatalogDO::getSourceMachineryCodes, keyword)
                    .or().like(MesProMesProcessCatalogDO::getSourceMachineryName, keyword)
                    .or().like(MesProMesProcessCatalogDO::getBatchRecordProcessName, keyword));
        }
        wrapper.orderByAsc(MesProMesProcessCatalogDO::getSortNo)
                .orderByAsc(MesProMesProcessCatalogDO::getSourceRowNo);
        return selectPage(reqVO, ListFilterQueryUtils.apply(reqVO, wrapper, LIST_FILTER_FIELDS));
    }
}
