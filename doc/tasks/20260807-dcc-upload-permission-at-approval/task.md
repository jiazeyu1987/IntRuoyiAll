# DCC 上传权限延后至审批任务

## Task Goal

允许具备 DCC 提交入口权限的用户完成文件上传和提交流程，不在上传阶段按文件类别 `UPLOAD` 权限阻断；文件类别权限在审批阶段执行并明确拒绝无权限审批动作。

## Milestones

- [x] M1：定位上传与审批权限校验链路并确定根因。
- [x] M2：先补充可重复失败的 BDD/TDD 回归测试。
- [x] M3：实现最小正式修复，使上传不受类别权限限制、审批仍受限制。
- [x] M4：完成定向回归、证据校验、提交、推送与任务收尾。

## Expected Verification

- 上传阶段：无文件类别 `UPLOAD` 权限的用户仍可选择类别并提交上传。
- 审批阶段：无对应文件类别审批权限的用户无法完成审批。
- 相关前端静态合同与后端单元测试通过。
- `git diff --check` 通过。

## Applicable Experience Gates

- 已读取 `docs/experience-index.md` 与 `docs/e2e-rules.md#DCC 文控审批处理入口门禁`。
- 不使用静态合同替代真实业务权限链路；上传和审批必须分别由各自测试证明。
- 不以 mock、默认成功或吞异常掩盖缺失的正式权限来源。
- 同一上传页和同一测试存在并行任务改动时停止写入，不覆盖或混合对方变更。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；拆分上传动作与审批动作的权限职责，避免上传页提前复用审批权限。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed：上传阶段的类别 `UPLOAD` 权限阻断已从前端和后端移除，审批阶段的参与人及 `review` / `approve` 权限限制保持不变；定向测试、类型检查、代码检查、本机真实页面、证据校验和任务清理均已通过，任务记录已完成。

## Root Cause

- 前端上传页通过 `canUpload=false` 过滤类别、表单校验和预检提示，在文件上传前阻断。
- 后端上传预览、路线预览和正式提交分别调用类别 `UPLOAD` 权限校验，因此仅隐藏提示不能满足“上传时不限制”。
- 审批动作已有当前任务参与人快照校验，以及 `dcc:controlled-file:review` / `dcc:controlled-file:approve` 阶段权限校验；该限制应保留并由独立回归测试证明。

## Resolved Concurrency Blocker

- 冲突文件：`IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue`、`IntRuoyiFronted/tests/e2e/dcc-upload-category-permission-static.spec.js`、`IntRuoyiFronted/tests/e2e/dcc-upload-category-leaf-real.e2e.js`。
- 并行任务当前目标是仅隐藏提示并保留上传阻断，与本任务“取消上传阶段限制”的业务目标不同。2026-08-07 用户明确要求继续后，按可区分改动继续：保留提示移除，重写权限合同为上传不限制。

## Cleanup Candidates

- `doc/tasks/20260807-dcc-upload-permission-at-approval/bug-regression-evidence.md`
- `output/playwright/20260807-dcc-upload-permission-at-approval/`
