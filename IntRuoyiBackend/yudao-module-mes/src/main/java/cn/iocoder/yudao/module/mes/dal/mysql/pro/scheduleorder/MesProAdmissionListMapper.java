package cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder;

import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProScheduleOrderAdmissionDiffPageReqVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import java.util.List;
import java.util.Map;

@Mapper
public interface MesProAdmissionListMapper {
    @SelectProvider(type = MesProAdmissionListQuery.class, method = "products")
    List<Long> selectRouteCheckProductIds(@Param("tenantId") Long tenantId, @Param("base") QueryWrapper<Object> base);

    @SelectProvider(type = MesProAdmissionListQuery.class, method = "summary")
    Map<String, Object> selectSummary(@Param("tenantId") Long tenantId, @Param("base") QueryWrapper<Object> base,
            @Param("ew") QueryWrapper<Object> filter, @Param("baseOutcomes") List<MesProAdmissionListQuery.Outcome> baseOutcomes,
            @Param("routeOutcomes") List<MesProAdmissionListQuery.Outcome> routeOutcomes);

    @SelectProvider(type = MesProAdmissionListQuery.class, method = "page")
    List<Long> selectPageIds(@Param("tenantId") Long tenantId, @Param("base") QueryWrapper<Object> base,
            @Param("ew") QueryWrapper<Object> filter, @Param("baseOutcomes") List<MesProAdmissionListQuery.Outcome> baseOutcomes,
            @Param("routeOutcomes") List<MesProAdmissionListQuery.Outcome> routeOutcomes,
            @Param("req") MesProScheduleOrderAdmissionDiffPageReqVO request, @Param("offset") long offset);
}
