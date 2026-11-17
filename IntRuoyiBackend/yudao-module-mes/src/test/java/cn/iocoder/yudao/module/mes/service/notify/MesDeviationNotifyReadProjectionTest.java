package cn.iocoder.yudao.module.mes.service.notify;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.system.controller.admin.notify.vo.message.*;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.iocoder.yudao.module.system.service.notify.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MesDeviationNotifyReadProjectionTest {
    @Test void allUserVisibleReadsProjectCopiesAndNeverRewriteStoredSnapshot() {
        var mapper=mock(NotifyMessageMapper.class); var service=new NotifyMessageServiceImpl();
        ReflectionTestUtils.setField(service,"notifyMessageMapper",mapper);
        NotifyMessageReadEnricher enricher=message -> message.getTemplateParams().put("taskTitle","完成偏差调查结果验证");
        ReflectionTestUtils.setField(service,"readEnrichers",List.of(enricher));
        var snapshot=new NotifyMessageDO().setId(13L).setUserId(42L).setTemplateParams(Map.of("workTaskId",91L));
        var admin=new NotifyMessagePageReqVO(); var mine=new NotifyMessageMyPageReqVO();
        when(mapper.selectById(13L)).thenReturn(snapshot);
        when(mapper.selectPage(admin)).thenReturn(new PageResult<>(List.of(snapshot),1L));
        when(mapper.selectPage(mine,42L,2)).thenReturn(new PageResult<>(List.of(snapshot),1L));
        when(mapper.selectUnreadListByUserIdAndUserType(42L,2,10)).thenReturn(List.of(snapshot));
        for(var projected:List.of(service.getNotifyMessage(13L),service.getNotifyMessagePage(admin).getList().get(0),
                service.getMyMyNotifyMessagePage(mine,42L,2).getList().get(0),service.getUnreadNotifyMessageList(42L,2,10).get(0))) {
            assertEquals("完成偏差调查结果验证",projected.getTemplateParams().get("taskTitle"));
            assertNotSame(snapshot,projected); assertNotSame(snapshot.getTemplateParams(),projected.getTemplateParams());
        }
        assertEquals(Map.of("workTaskId",91L),snapshot.getTemplateParams());
        assertNull(service.getNotifyMessage(999L));
        verify(mapper).selectById(13L); verify(mapper).selectById(999L); verify(mapper).selectPage(admin);
        verify(mapper).selectPage(mine,42L,2); verify(mapper).selectUnreadListByUserIdAndUserType(42L,2,10);
        verifyNoMoreInteractions(mapper);
    }
}
