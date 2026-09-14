# Execution Log

## User Intent

- 用户指出：一个产品不可能只有一个 QA 工序；一线 PQC 工序列表应显示 QA 对应的所有不重复工序。
- 用户明确链路：活跃订单产品 → 对应工艺路线 → 工艺路线项目代码 → QA 检验项目 → 检验项目中的不重复工序。

## Evidence

- BDD: 一线 PQC 展示全部 QA 工序 -> Given 当前活跃订单产品绑定的工艺路线项目代码存在多个 QA 检验项目且覆盖多个工序 / When 设备账号打开一线 PQC 选工序 / Then 列表显示这些 QA 检验项目中所有不重复工序，而不是只显示第一个工序。

## Progress

- 2026-08-10：创建任务记录，准备定位源码与补回归用例。
- 2026-08-10：读取经验索引并命中 PQC / QA 工序来源门禁；确认本次修复不得从产品直接取单个 QA 工序，也不得用 `PENDING` 任务集合扩展或截断工序列表。
- 2026-08-10：源码定位：一线 PQC 工序接口在 `MesFrontlinePqcContextServiceImpl.listProcessesByActiveOrder` 中执行，链路为活跃订单、工单产品路线绑定、路线全部产品绑定代码、DCC 项目代码、QA 候选产品、`MES_QA/PUBLISHED` 规程，再按 `routeProcessId + processId` 去重生成工序。
- 2026-08-10：只读接口验证：本机默认租户登录后查询活跃订单，目标订单接口返回 1 个工序“清洗工序”，未发现前端数组渲染截断证据。
- 2026-08-10：只读数据库核对：目标路线版本下仅有 1 条 `MES_QA/PUBLISHED` QA 规程，产品代码为 `AW.107.02.01.2010`，工序为“清洗工序”；未对数据库做写入。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，39 tests, 0 failures, 0 errors。
- BLOCKED: 当前运行数据没有发布截图中全部 QA 工序对应的逐工序规程；若要页面显示全部，需要先正式保存/发布对应路线版本的多工序 QA 规程，或确认截图对应的产品/项目/路线版本。
