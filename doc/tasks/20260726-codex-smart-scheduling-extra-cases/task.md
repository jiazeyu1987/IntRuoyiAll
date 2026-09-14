# 智能排产额外测试项

## 任务目标

- 根据智能排产的真实用户操作场景，在 `系统管理 > 测试管理` 中额外新增 3 个可执行测试项。
- 通过真实前端页面完成新增并复核测试项名称、测试方法项和测试目标项。

## 里程碑

- [x] M1：读取任务、登录、本地运行、数据库、E2E、PowerShell 与收尾规则。
- [x] M2：保存任务开始前的脏工作区基线，确认本机前后端、目标租户和测试管理现有数据。
- [x] M3：梳理智能排产真实场景并通过测试管理页面新增 3 个测试项。
- [x] M4：复核新增结果并完成验证、经验检查；Git 提交/推送等待共享分支协调。

## 预期验证

- 本机前端 `http://127.0.0.1:8081` 可访问。
- 本机后端 `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- 使用 `芋道源码/admin` 身份通过真实前端进入 `系统管理 > 测试管理`。
- 页面中可按可见业务名称检索到本任务新增的 3 个智能排产测试项。
- 每个测试项均包含与真实智能排产路径一致的测试方法项和可核验的测试目标项。

## 经验门禁

- 测试管理页面出现 `系统异常` 时，先核对 `system_codex_test_case` 当前 schema 和分页接口，不得隐藏错误、切换数据源或 mock 成功。
- 写入必须通过 Playwright 操作真实前端页面；API 只允许最终只读核验，不得代替页面新增。
- 写入前必须确认本机入口、`芋道源码/admin` 身份标签和目标数据范围；不得写入远端或未授权租户。
- Element Plus 下拉和表格操作必须按页面可见业务唯一文本定位并断言真实选中，不得使用数组下标、隐藏值或坐标猜测。
- 测试项名称需可追踪且不能与现有项目重复；新增内容是用户要求的正式本机测试管理数据，不作为临时 E2E 数据清理。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；先核对智能排产现有真实入口和业务行为，再编写可执行、可核验的测试项。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

## Current Blockers

- Git closeout is blocked by shared-branch coordination: current workspace contains unrelated task files and local commits on `int_main`; this task must not stage, commit, push, clean, or revert files outside `doc/tasks/20260726-codex-smart-scheduling-extra-cases/`.

## Cleanup Keep

- doc/tasks/20260726-codex-smart-scheduling-extra-cases/task.md
- doc/tasks/20260726-codex-smart-scheduling-extra-cases/execution-log.md
- doc/tasks/20260726-codex-smart-scheduling-extra-cases/verification-report.md
