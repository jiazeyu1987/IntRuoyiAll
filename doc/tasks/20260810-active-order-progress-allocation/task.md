# 活跃订单生产进度按工序分配修复

## Task Goal

修复活跃订单池生产进度：FIFO 自动分配或手动分配满某个工序数量时，只按该产品正式工序总数累加进度；组长减少已分配工序数量后，进度同步回退。

## Milestones

- [x] 建立 BDD 场景和回归测试
- [x] 定位生产进度计算链路和错误根因
- [x] 实现最小正式修复，不引入 fallback
- [x] 运行后端隔离回归、前端静态验证和只读真实路径 E2E
- [x] 确认生产代码已进入 `origin/int_main`
- [ ] 使用确认的测试租户和任务自有数据运行写入型分配减少 E2E

## Expected Verification

- 后端回归覆盖分配满 1 个工序为 1/N、分配满多个工序按数量累计、减少后回退。
- 前端静态回归覆盖分配数量允许 0 或空且空按 0 处理。
- 真实前端只读路径核对活跃订单池生产进度列与正式接口一致。
- 写入型减少数量 E2E 必须使用确认的测试租户、账号和可清理任务数据；缺少前置时保持阻塞。
- 检查无静默降级、无吞异常、无默认成功值。

## Experience Gate Summary

- `docs/backend-development.md#fifo-自动分配当前工序快照边界`：正式分配必须使用 `activeOrderId + routeProcessId + processId` 和正数目标数量快照，指定订单缺失时 fail-fast。
- `docs/backend-development.md#工序共享分配池与旧报工终结链路边界`：FIFO 和手动分配共享同一正式分配事实链路，不以来源工序或前端展示补丁替代。
- `docs/e2e-rules.md` 与 `docs/login-access.md`：写入型 E2E 禁止使用 admin 基线数据，必须使用确认的测试租户和任务自有数据。
- 已执行 `project-experience-consolidation` 检查；上述已有长期经验文档已经覆盖本次经验，无需创建或修改长期经验文档。

## Current Status

blocked - 生产代码和回归测试已进入 `origin/int_main`，后端隔离 JUnit 37/37、前端静态回归和只读 Playwright 均通过；缺少可安全写入的测试租户、账号和任务自有分配数据，不能执行“减少已分配数量”的写入型真实 E2E。

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

## Cleanup Keep

- doc/tasks/20260810-active-order-progress-allocation/task.md
- doc/tasks/20260810-active-order-progress-allocation/execution-log.md
- doc/tasks/20260810-active-order-progress-allocation/verification-report.md
