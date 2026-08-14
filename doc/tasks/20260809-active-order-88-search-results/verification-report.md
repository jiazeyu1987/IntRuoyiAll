# Verification Report

## Result

PASS。宽关键词 `88` 的候选上限已经移动到正式资格判定和排序之后，目标四个生产工单在真实页面中可见且均显示“符合要求”；取消工单仍被明确阻断。

## Verified Behavior

- `881MO090935`：符合要求。
- `881MO090972`：符合要求。
- `881MO090973`：符合要求。
- `881MO090974`：符合要求。
- `881MO090889`：同样符合要求并出现在候选列表。
- `881MO100646`：精确搜索显示“生产工单已取消”。

## Automated Verification

- RED：mapper 用例预期 24 条实际 20 条；service 用例预期最终 20 条实际 24 条，准确锁定资格前截断和 service 缺少最终上限。
- GREEN：2 tests，0 failures，0 errors。
- 相邻回归：46 tests，0 failures，0 errors。
- 合计：48 tests，0 failures，0 errors。
- 后端完整打包：30/30 Maven reactor modules `SUCCESS`。
- JAR 字节码：mapper 无资格前 `LIMIT`；service 在资格映射和排序后执行 `Stream.limit(20)`。

## Real Path E2E

- 页面：`http://127.0.0.1:8081/mes/pro/process-pool/team-leader`。
- 路径：登录 -> 工序池班组长工作台 -> 新增活跃订单 -> 输入 `88`。
- 结果：四个目标工单全部可见且显示“符合要求”。
- 取消契约：输入 `881MO100646`，显示“生产工单已取消”。
- 请求耗时：`88` 为 130 ms；`881MO100646` 为 88 ms。
- 浏览器控制台：0 errors，0 warnings。
- 截图：`playwright-active-order-88.png`，SHA-256 前缀 `4E15539154D55D59561D`。

## Data Safety

验证仅执行只读页面搜索。未选择候选，未点击“加入活跃订单”，未新增、修改或删除生产工单、活跃订单或其它业务数据。

## Residual Notes

共享运行态由其它任务管理，本任务没有停止或替换其进程。最终复验前已独立核对当前运行 JAR 的嵌套 MES 字节码，确认包含本次修复，后端健康状态为 `UP`。

任务收尾时已停止一个未监听端口但仍占用本任务旧日志的本任务遗留 Java 进程；当前共享 `48081` 运行进程未受影响，清理后 health 复核仍为 `UP`。未执行 Git 操作。
