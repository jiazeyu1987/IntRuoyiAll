# 一线 PQC 展示正式发布路线的全部工序

## Task Goal

修正一线 PQC 工序列表口径：选择生产订单后，列表展示该活跃订单正式发布工艺路线快照中的全部工序，不按 QA 规程配置、当前 `PENDING` 任务或“正在进行”工序缩减列表。

## Milestones

- [x] M1：接收用户口径并建立 BDD/验收契约。
- [x] M2：定位当前列表与任务状态耦合点，建立 RED 回归。
- [x] M3：实现正式发布路线全部工序列表，保留提交时的待检任务门禁。
- [x] M4：完成目标回归、接口/真实页面验证。
- [x] M5：完成证据校验、经验沉淀和任务清理。

## Expected Verification

- 正式发布路线快照中的工序即使没有 QA 规程或 `PENDING` PQC 任务，仍出现在工序列表。
- 有 `PENDING` 任务的工序继续返回任务选项；没有待检任务的工序不得绕过提交门禁。
- 粗洗等正式路线工序不得因没有 QA 检验配置或不在进行中而被隐藏。
- 草稿路线、当前配置表中未冻结到该订单的工序不得进入列表。
- `bug-regression-fix-loop` 与 `backend-api-delivery` 证据校验通过。

## 经验门禁

- 命中历史验收证据 `doc/tasks/20260808-frontline-pqc-product-line-process-picker/verification-report.md`：工序候选来自生产订单正式发布路线的冻结快照，`PENDING` 任务只附着任务上下文，不得缩减工序列表。
- 当前 `docs/backend-development.md#PQC 待检准入与工序选择必须分离` 的“仅 QA 工序”口径与本次用户明确要求冲突；本次以用户最新口径和正式路线快照为准，收尾时同步修订长期经验。
- 不得使用当前可变路线表、草稿路线、测试任务或前端补齐逻辑推断订单正式工序。
- 工序可见不等于可提交；提交仍必须携带与所选工序一致的正式 `PENDING` PQC 任务。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；分离“工序列表候选”与“待执行任务”。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed：正式路线快照候选与任务上下文已解耦；目标服务 38/38 通过，真实页面已验证球囊扩张压力泵展示 14 个正式路线工序（含粗洗）；长期经验已修正，任务临时产物已清理。

## Cleanup Candidates

- `doc/tasks/20260809-frontline-pqc-all-published-qa-processes/bug-regression-evidence.md`
- `doc/tasks/20260809-frontline-pqc-all-published-qa-processes/backend-api-evidence.md`
- `doc/tasks/20260809-frontline-pqc-all-published-qa-processes/frontline-pqc-all-published-processes.e2e.mjs`
- `doc/tasks/20260809-frontline-pqc-all-published-qa-processes/real-e2e-result.json`
- `doc/tasks/20260809-frontline-pqc-all-published-qa-processes/runtime-patch/`
- `output/playwright/frontline-pqc-all-published-processes-20260809.png`

## Final Verification

- 目标 Java 隔离回归：38/38 PASS。
- 真实 Playwright：订单 `881MO090889` 的接口与页面均返回 14 个正式路线工序，包含“粗洗工序”。
- 运行态：前端 `8081` HTTP 200，后端 `48081` health UP；并发任务重启后的运行 Jar 中本任务服务类哈希仍与已验证产物一致。
- 证据校验：`bug-regression-fix-loop` 与 `backend-api-delivery` 均通过。
- closeout preview/apply：无阻塞、无警告，仅清理本任务声明的临时证据、运行补丁目录和截图；保留三份核心任务文档。
