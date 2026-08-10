# Verification Report

## Automated Tests

- RED：旧服务缺少 `DccProjectCodeMapper` 正式解析依赖，`MesTeamLeaderActiveOrderServiceTest` 在 testCompile 按预期失败。
- GREEN：活跃订单服务与 ERP 计划开始时间测试 28/28 通过。
- 相邻回归：Controller、活跃订单服务与 ERP 计划开始时间测试 45/45 通过。
- 既有非目标失败：`mes-pqc-task-generation-static.spec.cjs` 要求 `SHIFT_AM="AM"`，而 `HEAD` 基线服务为 `SHIFT_FIRST="FIRST"`；本任务未修改班次逻辑或该静态合同。

## Real Data Read-only Verification

租户 1 的正式数据按“工单物料 -> 路线 -> ACTIVE 版本 -> 路线产品代码 -> DCC 项目 -> 路线版本 QA”只读解析：

| 工单 | 状态 | ACTIVE 路线版本 | DCC 上下文 | 已发布 MES QA | 已在活跃订单 |
| --- | ---: | --- | --- | ---: | ---: |
| 881MO090889 | 1 | 922119/627 | ID#11 | 1 | 1 |
| 881MO090935 | 1 | 922119/627 | ID#11 | 1 | 0 |
| 881MO090972 | 1 | 922119/627 | ID#11 | 1 | 0 |
| 881MO090973 | 1 | 922119/627 | ID#11 | 1 | 0 |
| 881MO090974 | 1 | 922119/627 | ID#11 | 1 | 0 |
| 881MO100066 | 3 | 无 | 无 | 0 | 0 |
| 881MO100524 | 3 | 980091/622 | IDI#14 | 0 | 0 |

结论：四个已确认且尚未加入的球囊扩张压力泵工单具备唯一正式路线、DCC 项目和已发布 QA；`090889` 已加入；`100066` 与 `100524` 因状态 3（已取消）必须先行阻断。

## Runtime Gate

- `48081` 当前 health `UP`，PID `52880`；运行 Jar 为 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260809-140430.jar`，SHA256 为 `538EBF0502CE0CE76B179B8A576AFD1BB7EBE30C9CC6BF543D4CD854ED8C2ED6`。
- 运行 Jar 内嵌 MES 模块的 `MesTeamLeaderActiveOrderServiceImpl` 包含 `DccProjectCodeMapper`、`生产工单已取消`、`产品工艺路线绑定指向已删除路线`、`工艺路线DCC项目代码绑定不唯一`，同名前缀 class 共 12 个，确认已加载本次正式解析链路。
- `8081` 前端入口 HTTP 200。

## Playwright Real-flow Verification

- 真实页面：`http://127.0.0.1:8081/mes/pro/process-pool/team-leader`，打开“新增活跃订单”弹窗后按工单号逐一搜索。
- `881MO090935`、`881MO090972`、`881MO090973`、`881MO090974` 均显示“符合要求”。
- `881MO100066`、`881MO100524` 均显示“生产工单已取消”。
- `881MO090889` 的资格校验显示“符合要求”，但只读数据库确认它已存在活跃订单；新增服务保持幂等并返回既有活跃订单，不产生重复记录。
- 未点击“加入活跃订单”，本次验收没有业务写请求；干净浏览会话控制台 error 0。
- 临时截图 `active-order-candidate-881MO090935.png` 显示目标工单及“符合要求”，SHA256 为 `D1907BC9D2D374EB373C606AA87D160AD1AC2B821AD8E7C808D5E9DA1262166F`，证据摘要保留于本报告后删除临时图片。

## Current Result

正式路线/DCC/QA 解析、自动化测试、真实数据库只读核验和真实页面只读验收均通过。四个已确认且尚未加入的目标工单现在可加入；两个已取消工单继续阻断；没有引入名称匹配、默认项目或旧版本 QA fallback。

## Closeout

- `task-closeout-cleanup` preview：`status: ready`，blocked 0，warnings 0。
- `task-closeout-cleanup` apply：`status: applied`；任务目录仅保留三份规定收尾文档。
- 本任务生成的 7 个 `.playwright-cli` 临时快照/控制台文件已按精确文件名删除，剩余 0；均为可重新生成的临时证据文件。
- 未执行 Git stage、commit、merge 或 push。
