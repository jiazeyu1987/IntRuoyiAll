package cn.iocoder.yudao.module.mes.service.pro;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrNonconformanceReviewServiceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;

/** Exercises the production freeze authority, with only persistence reads supplied by the fixture. */
public final class MesSa09Sa14FreezeFixture {
    public static MesProEdhrNonconformanceReviewServiceImpl authority(Long workOrderId, String state) {
        var service=new MesProEdhrNonconformanceReviewServiceImpl();
        var reviews=mock(MesProEdhrNonconformanceReviewMapper.class);
        var orders=mock(MesProWorkOrderMapper.class);
        ReflectionTestUtils.setField(service,"reviewMapper",reviews);
        ReflectionTestUtils.setField(service,"workOrderMapper",orders);
        if ("pending_review".equals(state)||"void".equals(state)) {
            when(reviews.selectFirstBlockingByWorkOrderId(workOrderId)).thenReturn(new MesProEdhrNonconformanceReviewDO()
                    .setId(909L).setWorkOrderId(workOrderId).setReviewStatus("pending_review".equals(state)?state:"closed")
                    .setDisposition("void".equals(state)?"void":null).setReviewCode("NCR-909"));
        } else {
            when(orders.selectByIdForUpdate(workOrderId)).thenReturn(MesProWorkOrderDO.builder().id(workOrderId)
                    .temporaryFrozen("external".equals(state)).build());
        }
        return service;
    }
}
