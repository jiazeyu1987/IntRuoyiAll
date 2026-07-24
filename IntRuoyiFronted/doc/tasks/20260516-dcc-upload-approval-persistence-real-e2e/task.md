# Task: DCC 上传审批落盘真实 E2E

## Goal

新增 1 条真实 Playwright E2E，用真实 DCC PDF、真实前端页面、真实审批矩阵和真实后端接口，跑通 `上传 -> 预览路线 -> 提交审批 -> 四段审批 -> 终审落盘` 全链路，并在最终校验中确认受控文件已经成为可预览的 `ACTIVE` 版本，且其 `publishedFileId` 对应的文件元数据与下载内容都可读。

## Scope

- 检查前端仓上一条未完成任务状态，并在开始本任务前显式记录暂停原因。
- 在生产代码变更前创建当前任务目录、`task.md`、`execution-log.md` 和脚本目录。
- 复用已稳定的真实 DCC 上传/审批链路，补上“最终落盘”断言：
  - 详情接口返回 `ACTIVE`
  - `publishedFileId` 已写入
  - `publishedTime` 已写入
  - 对应 `infra file` 元数据可读
  - 对应文件下载内容非空且保持 PDF 头
- 如真实链路暴露缺失前置数据，可修复 live runtime 的真实类别、岗位、目录、矩阵、分配和本地文件存储主配置后重跑。
- 不使用 mock 数据，不增加 fallback，不用接口捷径替代上传/审批前端路径；接口仅用于前置修复和最终落盘校验。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260516-dcc-position-hide-combined-role/task.md`
- Status before this task: blocked by user reprioritization.
- Impact: the combined-role filtering task is paused independently, so this task can proceed because it only adds new DCC real E2E coverage and does not depend on that list change.

## Milestones

- [x] M1: Confirm previous frontend task state and create this task package.
- [x] M2: Record BDD scenarios and RED evidence for the missing upload-to-approval-to-persistence coverage.
- [x] M3: Add the real Playwright E2E script for upload, approval, and final persistence verification.
- [x] M4: Run GREEN verification and update task evidence with exact results or blockers.
- [ ] M5: Commit only this task's QA/E2E files if verification fully passes.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-approval-persistence-e2e-green3 run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-upload-approval-persistence-real-e2e\scripts\verify-dcc-upload-approval-persistence-real-e2e.mjs`
- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-upload-approval-persistence-real-e2e\qa-test-suite-evidence.md`

## Current Status

Completed. The real Playwright E2E now runs through upload, route preview, submit, four live approvals, and final persistence verification against the local runtime.

## Runtime Repairs Performed

- Started the local `IntAuth` backend on `http://127.0.0.1:8020` with the shared internal token.
- Imported the 48 live IntAuth DCC file categories into tenant `1`.
- Imported the 31 live IntAuth DCC approval positions into tenant `1`.
- Restored fixed local positions `900333 / 900334` as `部门负责人 / 部门授权代表`.
- Granted category `产品技术要求` the required user-scoped permission rules for `admin`.
- Bound category `产品技术要求` to directory `3.DMR`.
- Saved the live four-stage approval matrix for category `产品技术要求`.
- Replaced the required DCC position assignments with real local user `admin`.
- Switched `infra_file_config.id=4` to the DB-backed master file store so `upload-preview` no longer uses the broken sample S3 config.
- Rebuilt and restarted `yudao-server` so the approval signature enum and latest DCC code were present in the runtime jar.

## Blocker And Impact

- Blocker: scoped Git commit is still pending because this repository already has many unrelated in-progress frontend changes outside this task package.
- Impact: the new E2E case is green and documented, but a task-only commit should be created later from a cleaner write set.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-approval-persistence-e2e-green3 run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-upload-approval-persistence-real-e2e\scripts\verify-dcc-upload-approval-persistence-real-e2e.mjs` -> PASS
- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-upload-approval-persistence-real-e2e\qa-test-suite-evidence.md` -> PASS
- Real result:
  - controlled file id: `11`
  - final detail status: `现行`
  - final API status: `ACTIVE`
  - published file id: `2217`
  - published file config id: `4`
  - published file path: `dcc/original/20260516/审核会签.pdf`
  - screenshot: `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/dcc-upload-approval-persistence-real-e2e-20260516.png`
