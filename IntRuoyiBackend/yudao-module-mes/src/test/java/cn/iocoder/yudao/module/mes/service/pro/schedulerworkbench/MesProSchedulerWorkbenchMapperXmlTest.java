package cn.iocoder.yudao.module.mes.service.pro.schedulerworkbench;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MesProSchedulerWorkbenchMapperXmlTest {

    private static final Path MAPPER_XML = Path.of("src", "main", "resources",
            "mapper", "pro", "schedulerworkbench", "MesProSchedulerWorkbenchMapper.xml");

    @Test
    void selectTodayAvailableCapacity_shouldFilterBySelectedDateThroughScheduledTasks() throws IOException {
        String mapperXml = Files.readString(MAPPER_XML, StandardCharsets.UTF_8);
        String sql = selectSql(mapperXml, "selectTodayAvailableCapacity");

        assertTrue(sql.contains("EXISTS"), "今日可用产能必须通过当天已排任务限定工序快照");
        assertTrue(sql.contains("mes_pro_task_schedule_ext"), "今日可用产能必须关联任务排程扩展表");
        assertTrue(sql.contains("mes_pro_task"), "今日可用产能必须关联生产任务表");
        assertTrue(sql.contains("task.start_time &gt;= #{beginTime}"), "今日可用产能必须使用 beginTime");
        assertTrue(sql.contains("task.start_time &lt; #{endTime}"), "今日可用产能必须使用 endTime");
        assertTrue(sql.contains("task_ext.schedule_order_process_id = process_snapshot.id"),
                "今日可用产能必须按排产工序快照关联任务，避免全量统计");
    }

    @Test
    void selectBottlenecks_shouldPointToScheduleRoute() throws IOException {
        String mapperXml = Files.readString(MAPPER_XML, StandardCharsets.UTF_8);
        String sql = selectSql(mapperXml, "selectBottlenecks");

        assertTrue(sql.contains("CONCAT('/mes/pro/route/edit/', schedule_order.route_id,"),
                "工作台瓶颈入口必须指向工艺流程编辑页");
        assertTrue(sql.contains("'?tab=schedule-config&amp;routeProcessId=', process_snapshot.route_process_id) AS targetPath"),
                "工作台瓶颈入口必须带上排产配置 Tab 和目标路线工序");
        assertTrue(!sql.contains("'/mes/pro/route' AS targetPath"),
                "工作台瓶颈入口不得再指向旧工艺路线路由");
    }

    @Test
    void selectCurrentScheduleReportedQuantity_shouldUseActualReportedQuantityNotProgressFraction() throws IOException {
        String mapperXml = Files.readString(MAPPER_XML, StandardCharsets.UTF_8);
        String sql = selectSql(mapperXml, "selectCurrentScheduleReportedQuantity");

        assertTrue(sql.contains("MAX(sop.reported_quantity)"),
                "当前排产实际报工数量必须优先使用工序快照真实报工数量，不能使用进度折算值");
        assertTrue(sql.contains("MAX(feedback_by_order.feedback_quantity)"),
                "当前排产实际报工数量必须能回看真实报工单数量");
        assertTrue(sql.contains("GREATEST("),
                "同一排产工单多工序时必须取订单层实际完成量，避免把每道工序重复相加");
        assertTrue(sql.contains("GROUP BY so.id"),
                "当前排产实际报工数量必须先按排产工单归集，再汇总工作台总数");
        assertFalse(sql.contains("completed_quantity"),
                "completed_quantity 是进度折算值，不能作为用户直觉里的实际报工数量");
        assertFalse(sql.contains("progress_percent"),
                "进度百分比不能参与报工偏差数量计算");
    }

    @Test
    void selectRouteActiveOrders_shouldAlignWithProcessWipScope() throws IOException {
        String mapperXml = Files.readString(MAPPER_XML, StandardCharsets.UTF_8);
        String sql = selectSql(mapperXml, "selectRouteActiveOrders");

        assertTrue(sql.contains("MesProScheduleOrderStatusEnum@PREPARE.status"),
                "工艺路线在制订单必须覆盖待排产工单，和工序在制订单口径一致");
        assertTrue(sql.contains("MesProScheduleOrderStatusEnum@SCHEDULED.status"),
                "工艺路线在制订单必须覆盖已排产工单，避免工作台有工序在制但路线空白");
        assertTrue(sql.contains("MesProScheduleOrderStatusEnum@IN_PROGRESS.status"),
                "工艺路线在制订单必须覆盖生产中工单");
        assertTrue(sql.contains("mes_pro_schedule_order_process"),
                "工艺路线在制订单必须通过工序快照判断真实在制");
        assertTrue(sql.contains("sop.enabled = b'1'"),
                "工艺路线在制订单必须只统计启用工序");
        assertTrue(sql.contains("sop.progress_percent") && sql.contains("100"),
                "工艺路线在制订单必须只统计未完成工序，避免已完工路线继续显示在制");
        assertFalse(sql.contains("AND so.status = ${@cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum@IN_PROGRESS.status}"),
                "工艺路线在制订单不得只统计生产中状态，否则已排产但有未完成工序时会一直为空");
    }

    @Test
    void selectRouteActiveOrders_shouldExcludeFrozenScheduleOrders() throws IOException {
        String mapperXml = Files.readString(MAPPER_XML, StandardCharsets.UTF_8);
        String sql = selectSql(mapperXml, "selectRouteActiveOrders");

        assertTrue(countOccurrences(sql, "so.frozen = b'0'") >= 2,
                "工艺路线在制订单路线汇总和产品汇总都必须排除冻结工单，避免比排产工单未冻结列表多统计");
    }

    private static String selectSql(String mapperXml, String selectId) {
        Pattern pattern = Pattern.compile("<select id=\"" + selectId + "\"[\\s\\S]*?</select>");
        Matcher matcher = pattern.matcher(mapperXml);
        assertTrue(matcher.find(), "Mapper XML 缺少 select: " + selectId);
        return matcher.group();
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

}
