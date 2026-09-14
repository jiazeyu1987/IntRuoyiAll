# Task: eDHR 损耗单填写人真实 E2E 验证

## Task Goal

通过本机真实前端路径验证批次执行详情右侧“损耗单”单据卡片能显示后端返回的每个单据填写人。

## Milestones

- [x] M1: 读取 E2E、登录、运行态、任务收尾和端口门禁。
- [x] M2: 准备只读 Playwright E2E 脚本与任务证据文件。
- [x] M3: 运行真实前端路径 E2E 并记录结果。
- [x] M4: 收尾清理、提交并推送验证记录。

## Expected Verification

- GREEN: 本机前端 `http://localhost:8081` 和后端 `http://127.0.0.1:48081` 可访问。
- GREEN: Playwright 登录本机前端，打开目标批次详情页。
- GREEN: 页面右侧当前工序单据卡片中，“损耗单”显示 `张可莹`，且对应详情接口 `fillableUsers` 非空。

## Current Status

completed

## 经验门禁

- `docs/e2e-rules.md#任务专用-e2e-环境变量与证据文件门禁`：真实 E2E 必须写入当前任务证据，不覆盖历史证据。
- `docs/e2e-rules.md#edhr-历史执行只读验证门禁`：只读验证必须走真实前端页面，API 只用于只读辅助核验。
- `docs/local-runtime.md`：`int_main` 使用前端 `8081`、后端 `48081`，不得换端口。
- `docs/backend-development.md#edhr-详情回填门禁`：不得从当前登录人、创建人、更新人或角色 ID 推断填写人。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务只做真实路径验证，不改实现。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260725-edhr-route-form-filler-e2e/readonly-filler-display.e2e.cjs`
- `doc/tasks/20260725-edhr-route-form-filler-e2e/real-e2e-evidence.md`
- `doc/tasks/20260725-edhr-route-form-filler-e2e/right-rail-loss-filler.png`
