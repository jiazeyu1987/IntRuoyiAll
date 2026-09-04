package cn.iocoder.yudao.module.mes.service.pro.feedback.frontline;

import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFormalProductionPickListSourceResolver;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MesProFeedbackMaterialBatchQueryServiceImplTest {

    @Test
    void resolveEvidence_shouldExposeOnlyMatchedPickListNosForMaterial() {
        MesFormalProductionPickListSourceResolver sourceResolver =
                mock(MesFormalProductionPickListSourceResolver.class);
        MesProFeedbackMaterialBatchQueryServiceImpl service =
                new MesProFeedbackMaterialBatchQueryServiceImpl(sourceResolver);
        when(sourceResolver.resolve(9001L)).thenReturn(new MesFormalProductionPickListSourceResolver.Resolution(
                MesProWorkOrderDO.builder().id(9001L).code("SIM-WO-001").build(),
                "SIM-WO-001",
                List.of(
                        source(2087829649074102069L, "SIM-SOUT-001",
                                item(2201L, 2087829649074102069L, "MAT-B", "LOT-B")),
                        source(2087829649074102072L, "SIM-SOUT-004",
                                item(2202L, 2087829649074102072L, "MAT-A", "LOT-A"))),
                "resolution-hash"));

        MesProFeedbackMaterialBatchEvidence evidence = service.resolveEvidence(9001L, "MAT-A");

        assertEquals(List.of("LOT-A"), evidence.batchCodes());
        assertEquals(List.of(2087829649074102072L), evidence.pickListIds());
        assertEquals(List.of("SIM-SOUT-004"), evidence.pickListNos());
        assertEquals(List.of(2202L), evidence.pickListItemIds());
    }

    private static MesFormalProductionPickListSourceResolver.Source source(
            Long pickListId, String sourceBillNo, ErpKingdeeProductionPickListItemDO item) {
        return new MesFormalProductionPickListSourceResolver.Source(
                ErpKingdeeProductionPickListDO.builder()
                        .id(pickListId)
                        .sourceBillNo(sourceBillNo)
                        .documentStatus("C")
                        .build(),
                List.of(item),
                "hash-" + pickListId);
    }

    private static ErpKingdeeProductionPickListItemDO item(
            Long itemId, Long pickListId, String materialNumber, String lotNumber) {
        return ErpKingdeeProductionPickListItemDO.builder()
                .id(itemId)
                .productionPickListId(pickListId)
                .materialNumber(materialNumber)
                .lotNumber(lotNumber)
                .requestedQuantity(BigDecimal.ONE)
                .actualQuantity(BigDecimal.ONE)
                .baseActualQuantity(BigDecimal.ONE)
                .build();
    }
}
