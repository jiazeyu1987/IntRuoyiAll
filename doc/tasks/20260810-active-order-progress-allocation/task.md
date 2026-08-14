# 活跃订单生产进度按工序分配修复

## Task Goal

修复活跃订单池生产进度：FIFO 自动分配或手动分配满某个工序数量时，只按该产品正式工序总数累加进度；组长减少已分配工序数量后，进度同步回退。

## Milestones

- [x] 建立 BDD 场景和回归测试
- [x] 定位生产进度计算链路和错误根因
- [x] 实现最小正式修复，不引入 fallback
- [x] 运行后端隔离回归、前端静态验证和只读真实路径 E2E
- [x] 确认生产代码已进入 `origin/int_main`
- [x] 使用用户明确授权的 `芋道源码/admin` 和当前数据运行写入型分配 E2E，并恢复原始分配状态
- [x] 清理本任务临时脚本、隔离测试产物和截图，保留核心任务记录

## Expected Verification

- 后端回归覆盖分配满 1 个工序为 1/N、分配满多个工序按数量累计、减少后回退。
- 前端静态回归覆盖分配数量允许 0 或空且空按 0 处理。
- 真实前端只读路径核对活跃订单池生产进度列与正式接口一致。
- 写入型 E2E 使用用户明确指定的本机 `芋道源码/admin` 和当前数据；只修改一个可编辑报工事件，测试前保存原始快照，测试后通过真实页面恢复并复核。
- 检查无静默降级、无吞异常、无默认成功值。

## Experience Gate Summary

- `docs/backend-development.md#fifo-自动分配当前工序快照边界`：正式分配必须使用 `activeOrderId + routeProcessId + processId` 和正数目标数量快照，指定订单缺失时 fail-fast。
- `docs/backend-development.md#工序共享分配池与旧报工终结链路边界`：FIFO 和手动分配共享同一正式分配事实链路，不以来源工序或前端展示补丁替代。
- `docs/e2e-rules.md` 与 `docs/login-access.md`：写入型 E2E 默认使用任务自有数据；本轮用户随后明确要求使用 `芋道源码/admin` 和当前数据，因此将写入范围收敛到单个事件并强制恢复原始分配数量。
- `docs/e2e-rules.md#当前共享数据写入-e2e-派生状态恢复门禁`：当前数据写入必须同时恢复正式源事实并复核派生进度；相同源事实重新保存触发正式重算时，不得强制恢复陈旧派生值。
- 已执行 `project-experience-consolidation` 检查；上述已有长期经验文档已经覆盖本次经验，无需创建或修改长期经验文档。

## Current Status

completed - 真实写入 Playwright 已通过：0 和空值均使目标订单生产进度从 7.142857% 回退到 0%，FIFO 和手动满额均恢复到 7.142857%；最终分配数量、FIFO 模式和全部规范化进度均恢复。任务临时产物已按预览范围清理，核心记录保留。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，生产进度读取当前正式分配记录，并以产品正式路线工序总数作为分母；数量减少后按当前分配重新计算。
- 是否存在临时补丁或绕过：否。

## Cleanup Candidates

- doc/tasks/20260810-active-order-progress-allocation/bug-regression-evidence.md
- doc/tasks/20260810-active-order-progress-allocation/read-only-active-order-progress-e2e.cjs
- doc/tasks/20260810-active-order-progress-allocation/mes-test-classpath.txt
- doc/tasks/20260810-active-order-progress-allocation/javac-target-test.args
- doc/tasks/20260810-active-order-progress-allocation/junit-target-test.args
- doc/tasks/20260810-active-order-progress-allocation/junit-target-test.stdout.txt
- doc/tasks/20260810-active-order-progress-allocation/junit-target-test.stderr.txt
- doc/tasks/20260810-active-order-progress-allocation/isolated-test-runtime
- output/playwright/20260810-active-order-progress-allocation
- doc/tasks/20260810-active-order-progress-allocation/current-data-write-progress-e2e.cjs
- output/playwright/20260810-active-order-progress-allocation-write

## Cleanup Keep

- doc/tasks/20260810-active-order-progress-allocation/task.md
- doc/tasks/20260810-active-order-progress-allocation/execution-log.md
- doc/tasks/20260810-active-order-progress-allocation/verification-report.md
