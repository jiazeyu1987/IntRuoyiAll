# 20260807-pressure-pump-loss-reasons

## Task Goal

通过本机真实页面，为“球囊扩张压力泵”和“按压式球囊扩充压力泵”两条工艺路线的每道对应工序新增 2～5 条损耗原因。新增内容必须是与工序匹配、人员可理解的中文原因，不使用编码或占位描述作为损耗原因显示值。

## Milestones

- [x] 建立任务目录并记录 BDD、范围与数据安全约束。
- [x] 核对经验门禁、本机运行态、登录身份、两条路线和工序现状。
- [x] 为每道工序建立候选原因池并生成可审计的 2～5 条新增计划，完成 RED。
- [x] 通过真实“工序配置/维护损耗”页面新增计划内原因。
- [x] 通过全新浏览器会话验证数量、中文语义、字段保持和非目标数据不变。
- [x] 完成经验沉淀检查和任务清理收尾。

## Expected Verification

- 真实登录身份和页面业务范围与本机默认环境一致，两条目标路线名称精确匹配且各自唯一。
- 每条目标路线的每道工序均新增 2～5 条启用的损耗原因；实际新增数量与冻结计划完全一致。
- 新增 `reasonName` 为与工序匹配的中文原因，不匹配内部编码或占位格式；`reasonCode` 仅作为系统内部编码存在。
- 不修改、不删除、不停用两条路线的既有原因，不影响其它路线、工序或业务数据。
- 所有新增请求均来自真实页面，HTTP 成功且业务码为 `0`；最终全新只读会话不产生 MES 写请求。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；路线、工序、现有原因、候选原因、请求或最终状态不符合冻结计划时立即停止。
- `是否从根因和长期维护角度解决`：是；在正式损耗原因配置中新增真实业务描述，不在展示层临时替换编码。
- `是否存在临时补丁或绕过`：否；仅使用真实页面维护入口写入，不使用直接 SQL、API-only 写入、mock 或默认原因。

## Data Safety

- 环境限定为本机 `int_main`：前端 `http://127.0.0.1:8081`，后端 `http://127.0.0.1:48081`。
- 写入范围限定为名称精确等于“球囊扩张压力泵”或“按压式球囊扩充压力泵”的路线及其当前页面工序。
- 每个工序必须先保存稳定工序 ID、既有原因快照和显式候选池，再从候选池中生成 2～5 条冻结计划。
- 发现同名路线不唯一、同工序候选不足、与既有原因重名、稳定 ID 漂移或并发写入时停止，不扩大范围。
- 不修改数据库 schema，不执行 SQL 写入，不修改、删除或停用既有损耗原因。

## Experience Gate

- `docs/login-access.md#本机登录来源`：写入前必须由真实登录页确认 `芋道源码/admin`、负责路线和当前列表范围，不切换到其它测试租户。
- `docs/backend-development.md#生产组长工序配置维护权限不得被工序开始快照误拦`：以正式 `process-config/list` 和页面“维护损耗”入口为准，不使用一线设备账号链路替代。
- `docs/e2e-rules.md#表格行定位`：按精确路线名、稳定 `routeProcessId` 和页面可见工序行定位，不使用接口数组下标操作表格。
- `docs/e2e-rules.md#写入型-e2e-响应不确定断点恢复门禁`：写请求响应不确定时，以全新只读会话按稳定 ID 分类，只继续原值记录，不盲目重放。
- `docs/e2e-rules.md#写入型-e2e-异常路径任务数据清理门禁`：每次业务码 `0` 后立即记录机器可读完成状态；本任务保留用户要求的正式新增数据，不把它作为测试临时数据清理。

## Cleanup Candidates

- `doc/tasks/20260807-pressure-pump-loss-reasons/pressure-pump-loss-reasons.e2e.mjs`
- `doc/tasks/20260807-pressure-pump-loss-reasons/inspection.json`
- `doc/tasks/20260807-pressure-pump-loss-reasons/change-manifest.json`
- `doc/tasks/20260807-pressure-pump-loss-reasons/apply-result.json`
- `doc/tasks/20260807-pressure-pump-loss-reasons/final-verification.json`
- `doc/tasks/20260807-pressure-pump-loss-reasons/final-pressure-pump.png`
- `doc/tasks/20260807-pressure-pump-loss-reasons/final-press-pressure-pump.png`

## Current Range Evidence

- 真实身份：`芋道源码/admin`；生产组长工序配置总行数 `106`。
- `球囊扩张压力泵(routeId=922119)`：14 道工序，初始原因 `0` 条。
- `按压式球囊扩充压力泵(routeId=980091)`：14 道工序，初始原因 `34` 条，全部为 `RLR0807M-*` 占位描述；其中 5 道工序只有 1 条原因。
- 冻结计划：28 道工序最终共 `106` 条原因；每工序数量分布为 `2:7、3:4、4:5、5:12`。
- 页面写入计划：原位修改既有占位原因 `34` 条，新增中文原因 `72` 条，不删除、不停用原因。

## Final Verification

- 新 Playwright 浏览器会话核验通过：两条路线各 14 道工序，最终 `106` 条原因；`球囊扩张压力泵=54`、`按压式球囊扩充压力泵=52`。
- 每道工序最终原因数均为 `2..5`，分布 `2:7、3:4、4:5、5:12`；目标路线 `RLR0807M-*` 占位描述为 `0`。
- 34 条既有记录的 ID、内部编码、路线工序归属和启用状态保持不变；页面只显示中文原因，不显示内部编码。
- 独立只读验证 MES 写请求 `0`、page error `0`、console error `0`、目标网络失败 `0`。
- 两张 1600x900 页面截图已人工检查；页面仍显示与本任务无关的既有 `team-device/list` 地址不存在提示，本任务未隐藏或扩大范围修复。
- `task-closeout-cleanup` preview/apply 均通过：删除 7 个任务临时产物，保留 3 份核心记录，无阻塞和警告。
