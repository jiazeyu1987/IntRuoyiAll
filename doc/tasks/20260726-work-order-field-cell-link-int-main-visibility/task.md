# 20260726 Work Order Field Cell Link Int Main Visibility

## Task Goal

让 `int_main` 本机环境中 `芋道源码/admin` 在批次执行单元格链接配置的源选择框里可以直接看到并选择 `生产工单`，选择后可继续选择生产工单字段带入批次执行。

## Milestones

- [x] 核对已完成修复分支与 `int_main` 当前差异
- [x] 将生产工单源选项与字段联动能力同步到 `int_main`
- [x] 运行静态契约、类型检查、后端目标测试和真实只读可见性 E2E
- [ ] 完整字段矩阵 E2E 需要 `48081` 后端加载 `E:\IntRuoyi` 当前代码后复跑
- [ ] 记录验证证据并完成提交推送

## Expected Verification

- `node tests\e2e\mes\batch-record-cell-link-static.spec.js`
- `node --check tests\e2e\mes\batch-record-cell-link-work-order-field-readonly.e2e.mjs`
- `pnpm ts:check`
- `芋道源码/admin` 通过真实前端路径看到源选择框中的 `生产工单`
- `scripts\preflight\branch-runtime-port-guard.ps1`

## Current Status

blocked_by_runtime_conflict

## 经验门禁

- Element Plus 下拉选择门禁：真实 E2E 必须打开可见 `.el-select-dropdown__item` 并按业务文本 `生产工单` 选择，不得只依赖接口值或数组下标。
- 官方登录前置与 admin-only 全量验证门禁：`芋道源码/admin` 只做只读验证，不在 admin 基线数据上保存单元格链接。
- Worktree 隔离运行态 URL 门禁：真实 E2E 的前端/后端端口必须属于同一运行态；当前 `8081` 前端属于 `E:\IntRuoyi`，但 `48081` 后端 PID 57744 属于 `D:\IntRuoyiWorktree\edhr-release-dossier-e2e-20260726`，完整字段矩阵 E2E 阻塞。
- PowerShell 编排门禁：中文与多行脚本采用 UTF-8 路径，不记录登录密码或密钥。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，目标是将源选项模型本身补齐到 `int_main`，不是临时改文案或绕过接口。
- `是否存在临时补丁或绕过`：否
