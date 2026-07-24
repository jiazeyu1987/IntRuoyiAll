# 任务：修复运行控制台总览超时

## 任务目标

- 修复运行控制台顶部 `运维矩阵：timeout of 70000ms exceeded`。
- 保持运行控制台真实探测，不用 mock、静默跳过或默认成功掩盖远程环境问题。
- 总览接口应在前端 70 秒超时前返回，并对慢探测项展示明确错误状态。

## 前序任务检查

- 已确认上一任务 `doc/tasks/20260601-backup-runtime-status-disk/task.md` 状态为 `completed`。
- 当前仓库存在无关未跟踪 `runtime/`，本任务不触碰、不提交。

## BDD 场景

- BDD: 总览接口不被单个慢探测拖到前端超时 -> Given 运行控制台需要展示 4 个环境和 4 个组件 / When 某个状态探测超过总览预算 / Then `/infra/runtime-control/overview` 应在前端 70 秒超时前返回，并将超时项标记为错误。
- BDD: 慢探测错误必须显式展示 -> Given 某个远程状态脚本超时 / When 控制台展示状态矩阵 / Then 对应单元格应显示 `error` 与超时原因，不得静默降级为运行中。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：复现 70 秒前端请求超时并定位后端总览耗时来源。
- [x] M3：增加失败回归测试并最小修复。
- [x] M4：验证 API 与页面不再超时，记录证据。
- [x] M5：收尾清理预览并提交本任务改动。

## 预期验证

- 修复前能用真实本机 API 复现 70 秒请求超时。
- 回归测试先 RED 后 GREEN。
- `RuntimeControlServiceImplTest` 相关用例通过。
- 登录态请求 `/admin-api/infra/runtime-control/overview` 能在 70 秒前返回。
- 页面刷新后不再显示 `运维矩阵：timeout of 70000ms exceeded`。

## 已完成工作

- 复现 `/admin-api/infra/runtime-control/overview` 在 70 秒客户端超时内未返回。
- 定位到 `RuntimeControlServiceImpl.queryStatusesConcurrently()` 的 Stream 写法导致 `CompletableFuture` 逐个提交、逐个等待，实际串行执行 16 个状态探测。
- 增加并发回归测试，要求总览状态探测存在重叠执行。
- 修改实现为先提交全部 future，再统一 join 收集结果。
- 重新打包并重启本机 48081 后端，真实 `/overview` 请求 16.55 秒返回。

## 验证结果

- RED 已复现：新增并发测试在修复前失败。
- GREEN 已通过：新增并发测试通过。
- REGRESSION 已通过：`RuntimeControlServiceImplTest` 32 个用例通过。
- 真实 API 已验证：`/overview` 在 70 秒客户端超时前返回，`seconds=16.55`。
- Bug 回归证据校验通过：`validate_bug_regression.py --evidence .../bug-regression-evidence.md`。
- TDD 合规检查通过：`verify_tdd_compliance.py --task-dir ... --all-changed`。
- 收尾清理预览通过：无删除项、无阻塞、保留任务文档与 bug 证据。

## Cleanup Keep

- `doc/tasks/20260601-runtime-control-overview-timeout/bug-regression-evidence.md`

## 当前状态

status: completed
