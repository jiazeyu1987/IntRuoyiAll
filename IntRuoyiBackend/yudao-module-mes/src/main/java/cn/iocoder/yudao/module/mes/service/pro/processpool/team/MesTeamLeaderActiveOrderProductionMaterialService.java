package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kingdee.MesKingdeeProductionMaterialListRespVO;
import cn.iocoder.yudao.module.mes.service.pro.workorder.kingdee.MesKingdeeProductionMaterialListQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class MesTeamLeaderActiveOrderProductionMaterialService {

    private final MesTeamLeaderActiveOrderDetailService detailService;
    private final MesKingdeeProductionMaterialListQueryService materialQueryService;

    public MesTeamLeaderActiveOrderProductionMaterialService(MesTeamLeaderActiveOrderDetailService detailService,
            MesKingdeeProductionMaterialListQueryService materialQueryService) {
        this.detailService = detailService;
        this.materialQueryService = materialQueryService;
    }

    @Transactional(readOnly = true)
    public List<MesKingdeeProductionMaterialListRespVO> getList(Long leaderUserId, Long activeOrderId) {
        // Reuse the formal detail's current owner, ACTIVE status and source validation.
        MesTeamLeaderActiveOrderDetail detail = detailService.getDetail(leaderUserId, activeOrderId);
        String productionOrderNo = detail.getWorkOrderCode();
        if (productionOrderNo == null || productionOrderNo.isBlank()) {
            throw new IllegalStateException("ACTIVE_ORDER_PRODUCTION_ORDER_NO_REQUIRED:" + activeOrderId);
        }
        List<MesKingdeeProductionMaterialListRespVO> rows = new ArrayList<>();
        Set<Long> rowIds = new HashSet<>();
        Long total = null;
        int pageNo = 1;
        do {
            MesKingdeeProductionMaterialListPageReqVO request = new MesKingdeeProductionMaterialListPageReqVO();
            request.setProductionOrderNo(productionOrderNo);
            request.setPageNo(pageNo++);
            request.setPageSize(100);
            var page = materialQueryService.getPage(request);
            if (page == null || page.getList() == null || page.getTotal() == null || page.getTotal() < 0
                    || (total != null && !Objects.equals(total, page.getTotal()))) {
                throw new IllegalStateException("ACTIVE_ORDER_PRODUCTION_MATERIAL_PAGE_INVALID:" + activeOrderId);
            }
            total = page.getTotal();
            if ((page.getList().isEmpty() && rows.size() < total) || rows.size() + page.getList().size() > total) {
                throw new IllegalStateException("ACTIVE_ORDER_PRODUCTION_MATERIAL_PAGE_INCOMPLETE:" + activeOrderId);
            }
            for (var row : page.getList()) {
                if (row == null || !Objects.equals(productionOrderNo, row.getProductionOrderNo())
                        || (row.getWorkOrderId() != null && !Objects.equals(detail.getWorkOrderId(), row.getWorkOrderId()))
                        || row.getId() == null || !rowIds.add(row.getId())) {
                    throw new IllegalStateException("ACTIVE_ORDER_PRODUCTION_MATERIAL_SOURCE_MISMATCH:" + activeOrderId);
                }
                rows.add(row);
            }
        } while (rows.size() < total);
        return rows;
    }
}
