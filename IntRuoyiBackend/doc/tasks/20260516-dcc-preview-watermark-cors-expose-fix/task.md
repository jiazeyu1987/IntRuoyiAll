# Task: DCC 预览水印响应头跨域暴露修复

## Goal

修复 DCC 受控预览接口虽然已返回 `X-DCC-Preview-Watermark`，但浏览器跨域
JS 仍无法读取该响应头的问题，使统一受控阅读页能真正显示基于后端权威元数据
的 badge 和 overlay。

## Scope

- 在后端仓库创建任务文档、执行日志和回归证据。
- 仅修改 DCC 预览响应头输出及其定向测试。
- 不改变预览/下载权限，不改二进制 body，不引入 fallback 水印头。

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260516-dcc-finalizing-active-status-race-fix/task.md`
- Status before this task: completed.
- Impact: the final-status race fix is already complete and does not block this
  follow-up CORS/header exposure repair.

## Milestones

- [x] M1: Create task package and record the live browser blocker.
- [ ] M2: Add RED regression coverage for preview watermark header exposure.
- [ ] M3: Implement the minimal header exposure fix.
- [ ] M4: Run GREEN verification and update bug evidence.
- [ ] M5: Commit only this backend task's files if verification fully passes.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFilePreviewDownloadApiTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Status

Blocked pending user reprioritization. The backend RED state and intended CORS
header fix remain valid, but this task is paused so the electronic batch record
import-analysis tab work can proceed first without mixing unrelated backend
changes into the same completion cycle.

## Blocker And Impact

- Blocker: user reprioritized to the electronic batch record import-analysis
  tab task before M2-M5 for this DCC CORS fix were completed.
- Impact: the DCC preview watermark response-header fix is still pending and
  must be resumed in its own task later; it is intentionally not mixed into the
  current electronic batch record implementation.
