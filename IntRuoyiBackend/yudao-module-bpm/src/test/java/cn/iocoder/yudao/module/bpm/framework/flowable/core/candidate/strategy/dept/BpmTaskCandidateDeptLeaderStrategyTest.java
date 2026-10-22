package cn.iocoder.yudao.module.bpm.framework.flowable.core.candidate.strategy.dept;

import cn.iocoder.yudao.framework.common.util.collection.SetUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class BpmTaskCandidateDeptLeaderStrategyTest extends BaseMockitoUnitTest {

    @InjectMocks
    private BpmTaskCandidateDeptLeaderStrategy strategy;

    @Mock
    private DeptApi deptApi;

    @Test
    public void testCalculateUsers() {
        // 准备参数
        String param = "10,20";
        // mock 方法
        when(deptApi.requireSupervisor(10L, null)).thenReturn(
                new cn.iocoder.yudao.module.system.api.dept.dto.DeptSupervisorRespDTO().setUserId(11L));
        when(deptApi.requireSupervisor(20L, null)).thenReturn(
                new cn.iocoder.yudao.module.system.api.dept.dto.DeptSupervisorRespDTO().setUserId(21L));

        // 调用
        Set<Long> userIds = strategy.calculateUsers(param);
        // 断言结果
        assertEquals(Sets.newLinkedHashSet(11L, 21L), userIds);
    }

}
