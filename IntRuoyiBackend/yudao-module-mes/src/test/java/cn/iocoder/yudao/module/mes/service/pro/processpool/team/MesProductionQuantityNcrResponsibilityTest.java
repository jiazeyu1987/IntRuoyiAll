package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesProductionQuantityNcrResponsibilityTest {
    MesActiveOrderNcrResponsibilityReader reader = new MesActiveOrderNcrResponsibilityReader();
    MesProEdhrNonconformanceReviewMapper reviews = mock(MesProEdhrNonconformanceReviewMapper.class);
    AdminUserApi users = mock(AdminUserApi.class);
    @BeforeEach void setup() {
        ReflectionTestUtils.setField(reader, "reviews", reviews);
        ReflectionTestUtils.setField(reader, "users", users);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "ncr-responsibility"),
                MesProEdhrNonconformanceReviewDO.class);
    }
    MesTeamLeaderActiveOrderRow row(long id) {
        return new MesTeamLeaderActiveOrderRow().setId(id).setWorkOrderId(30L);
    }
    MesProEdhrNonconformanceReviewDO review(long id, long order, Long reviewer) {
        return new MesProEdhrNonconformanceReviewDO().setId(id).setTenantId(1L).setWorkOrderId(30L)
                .setActiveOrderId(order).setReviewCode("NCR-" + id).setReviewStatus("pending_review")
                .setQaReviewerUserId(reviewer);
    }
    @Test void usesExactOrderAndFormalAssigneeIncludingUnassignedAndMultipleReviews() {
        var first = row(20L); var other = row(21L); var clean = row(22L);
        var qa = new AdminUserRespDTO(); qa.setId(40L); qa.setTenantId(1L); qa.setNickname("QA Zhang");
        when(reviews.selectList(any(Wrapper.class))).thenAnswer(call -> {
            Wrapper<?> query = call.getArgument(0);
            String sql = query.getSqlSegment();
            assertTrue(sql.contains("active_order_id IN"));
            assertTrue(sql.contains("review_status ="));
            assertTrue(((com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?>) query)
                    .getParamNameValuePairs().containsValue("pending_review"));
            return List.of(review(1,20,40L), review(2,20,null), review(3,21,40L));
        });
        when(users.getUserMap(List.of(40L))).thenReturn(Map.of(40L, qa));
        reader.populate(List.of(first, other, clean));
        assertEquals(2, first.getPendingNonconformanceReviews().size());
        assertEquals("QA Zhang", first.getPendingNonconformanceReviews().get(0).getQaReviewerName());
        assertNull(first.getPendingNonconformanceReviews().get(1).getQaReviewerUserId());
        assertEquals("NCR-3", other.getPendingNonconformanceReviews().get(0).getReviewCode());
        assertTrue(clean.getPendingNonconformanceReviews().isEmpty());
    }
    @Test void emptyOrdersDoNotQueryAndNoPendingReviewDoesNotQueryUsers() {
        reader.populate(List.of()); verifyNoInteractions(reviews, users);
        when(reviews.selectList(any(Wrapper.class))).thenReturn(List.of());
        var row = row(20L); reader.populate(List.of(row));
        assertTrue(row.getPendingNonconformanceReviews().isEmpty()); verifyNoInteractions(users);
    }
    @Test void missingAssignedUserAndWrongTenantAndWrongWorkOrderFailClearly() {
        when(reviews.selectList(any(Wrapper.class))).thenReturn(List.of(review(1,20,40L)));
        when(users.getUserMap(anyCollection())).thenReturn(Map.of());
        assertThrows(IllegalStateException.class, () -> reader.populate(List.of(row(20))));
        var qa = new AdminUserRespDTO(); qa.setId(40L); qa.setTenantId(2L); qa.setNickname("Other tenant");
        when(users.getUserMap(anyCollection())).thenReturn(Map.of(40L,qa));
        assertThrows(IllegalStateException.class, () -> reader.populate(List.of(row(20))));
        when(reviews.selectList(any(Wrapper.class))).thenReturn(List.of(review(1,20,null).setWorkOrderId(999L)));
        assertThrows(IllegalStateException.class, () -> reader.populate(List.of(row(20))));
    }
    @Test void readFailurePropagates() {
        when(reviews.selectList(any(Wrapper.class))).thenThrow(new IllegalStateException("read failed"));
        assertThrows(IllegalStateException.class, () -> reader.populate(List.of(row(20))));
    }
}
