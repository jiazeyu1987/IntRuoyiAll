# DCC Controlled File Terminology Repair

## Goal
修复当前 DCC 受控文件代码中仍把业务对象描述为“工作稿”“工作版本”“现行版”的用户可见文案，统一使用“受控文件”，并把审批、生效、待提交等状态作为进度描述。

## Current Status
ready_for_closeout

## Milestones
1. 静态扫描受控文件前后端用户可见文案。
2. 替换违反新业务规则的“工作稿/工作版本/现行版”文案，保留内部枚举状态语义。
3. 增加或更新静态合同，证明用户可见受控文件范围内不再出现旧业务称谓。
4. 运行目标静态合同、前端类型检查和构建，完成提交推送。

## Expected Verification
- 精确静态扫描 `IntRuoyiBackend/yudao-module-dcc/src/main/java`、`IntRuoyiFronted/src/views/dcc/controlled-file`、`IntRuoyiFronted/src/api/dcc/controlledFile` 不再包含用户可见旧称谓。
- DCC 术语静态合同通过。
- 相关 DCC 前端脚本合同通过。
- `pnpm ts:check` 和 `pnpm build:local` 通过。
- `git diff --check` 通过。

## BDD
- BDD: controlled file terminology -> Given a user opens DCC upload, browser, detail, handling summary, or publication followup views; When labels, prompts, errors or statuses describe a file version; Then the business object is described as a controlled file and status wording describes submission/effective progress without using “工作稿”, “工作版本” or “现行版”.

## 设计约束检查
- 不改内部状态枚举、数据库状态或审批流状态机，只修复用户可见文案和覆盖合同。
- 不引入 fallback、兼容补丁或吞异常。
- 不执行真实页面 E2E、数据库写入、服务重启或部署，除非用户另行明确要求。

## Cleanup Keep
- doc/tasks/20260916-dcc-controlled-file-terminology-repair/task.md
- doc/tasks/20260916-dcc-controlled-file-terminology-repair/execution-log.md
- doc/tasks/20260916-dcc-controlled-file-terminology-repair/verification-report.md
