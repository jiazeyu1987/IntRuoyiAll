# PQC 清洗工序 QA 项目缺失提示修复

## Task Goal

修复按压式压力泵一线 PQC 选择“3. 清洗工序”时，页面错误显示“当前工序缺少发布态QA检验项目”的问题；清洗工序有正式发布态 QA 检验项目时，前后端必须按当前产品、路线、路线版本、路线工序和工序正确加载并展示 QA 项目。

## Milestones

- [x] M1 只读核对截图订单、清洗工序、PQC 任务与 QA 规程/项目的正式数据链路
- [x] M2 新增回归测试覆盖工序列表被待检任务裁剪、无设备 QA 项目可提交
- [x] M3 实现最小正式修复，不引入 fallback 或默认项目
- [x] M4 运行 GREEN 与相邻 PQC 回归验证
- [x] M5 更新验证报告、经验与收尾状态

## Expected Verification

- 后端回归证明：PQC 工序选择按当前产品正式路线返回全工序，仅有正式 `PENDING` PQC 任务的工序附着 `pqcTaskId` 与 `inspectionItems`。
- 数据核对证明：截图订单当前产品/路线/路线版本/清洗路线工序没有发布态 QA 规程和 `PENDING` PQC 任务；不能借用其它产品/路线的清洗 QA 项目。
- 相邻 PQC 回归证明：无待检工序仍不可提交、无设备 QA 项目仍不强制设备。
- 基础检查：目标 Maven 测试、`git diff --check`。

## Current Status

completed

实现、验证和 task-closeout-cleanup 已完成；保留 `task.md`、`execution-log.md`、`verification-report.md` 三份正式记录。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。必须读取发布态 QA 规程与正式项目映射，不用默认项目、空成功或前端隐藏提示。
- `是否从根因和长期维护角度解决`：是。后端按当前产品正式路线返回全工序，PQC 任务/QA 项目只从正式 `PENDING` 任务和发布规程附着；数据缺口明确为当前产品/路线/路线版本/路线工序未发布清洗 QA 规程。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- Bug Regression Fix Loop：先复现/RED，再最小修复和 GREEN。
- MES PQC 项目级检验快照门禁：QA 项目事实必须来自发布规程和结构化项目映射。
- PQC 待检准入与工序选择必须分离：工序展示、PQC 任务准入和 QA 项目加载不能互相替代或隐藏。
- Strict No-Fallback：不得用默认 QA 项目、前端文案或 API-only 说明掩盖正式映射缺失。

## Closeout Evidence

- `task-closeout-cleanup --mode preview` -> status `ready`, keep only `task.md` / `execution-log.md` / `verification-report.md`, delete 5 task-owned temporary artifacts, blocked `<none>`, warnings `<none>`.
- `task-closeout-cleanup --mode apply` -> status `applied`, deleted all 5 previewed temporary artifacts, linked worktree `False`, no Git commit requested or performed.
- Deleted temporary artifacts:
  - `doc/tasks/20260808-pqc-cleaning-qa-items-missing/javac-mes-frontline-pqc-context-test.args`
  - `doc/tasks/20260808-pqc-cleaning-qa-items-missing/mes-test-classpath.txt`
  - `IntRuoyiBackend/doc/tasks/20260808-pqc-cleaning-qa-items-missing/mes-test-classpath.txt`
  - `IntRuoyiBackend/doc/tasks/20260808-pqc-cleaning-qa-items-missing/deps`
  - `IntRuoyiBackend/yudao-framework/doc/tasks/20260808-pqc-cleaning-qa-items-missing/mes-test-classpath.txt`
- Project experience consolidation: existing `docs/backend-development.md#MES PQC 项目级检验快照门禁` and `docs/experience-index.md` already cover “PQC 工序选择显示产品路线全工序、仅待检任务附着上下文、禁止用 active-order 快照压掉路线工序、禁止借用其它产品/路线 QA 项目”，无需新增长期经验文档。
