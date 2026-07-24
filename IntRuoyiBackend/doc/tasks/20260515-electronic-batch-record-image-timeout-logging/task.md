# Task: Electronic Batch Record Image Timeout And Logging Tuning

## Goal

Increase the backend timeout budget for image-based electronic batch-record recognition and add phase-level logs so the team can distinguish between request entry, `Codex CLI` process start, model wait time, stdout drain, and structured output parsing.

## Scope

- Check the latest backend task status before starting this task.
- Create the task document and execution log before editing production code.
- Add BDD and strict TDD evidence for timeout and observability behavior.
- Adjust only the backend image-recognition runtime behavior in the MES generated-report module.
- Restore the local backend runtime after the change when possible.
- Do not redesign prompts, add fallback OCR engines, or change frontend behavior in this task.

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-electronic-batch-record-image-codex-cli-import/task.md`
- Status before this task: completed.
- Impact: the import feature exists and this task can focus on runtime tuning and observability only.

## Milestones

- [x] M1: Confirm the previous backend task is completed and create this task document.
- [x] M2: Record BDD and RED evidence for timeout and logging expectations.
- [x] M3: Implement configurable larger timeout and phase-level logs.
- [x] M4: Run targeted regression tests.
- [x] M5: Update evidence and create a scoped backend commit.

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordCodexCliImageParserTest,MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`

## Current Status

Completed. The backend image parser now uses a larger default timeout budget, applies timeout control to the whole `Codex CLI` process lifecycle instead of only after stdout is drained, emits phase-level logs for start, process launch, timeout, process finish, and structured-result summary, and now returns a clear timeout JSON when the same complex screenshot still overruns the budget.

## Blocker And Impact

- Blocker 1: the normal `yudao-server` package path with `-DskipTests` is currently blocked by a pre-existing unrelated test compile error in `MesProBatchRecordReportLayoutCalibratorTest`.
- Impact 1: a production jar cannot be rebuilt through the standard package path until that unrelated test issue is cleaned up, so this task used `-Dmaven.test.skip=true` after targeted regression tests had already passed.

- Blocker 2: the exact screenshot image test still exceeds the current 600-second structured-recognition time budget.
- Impact 2: the system now fails explicitly and logs the timeout phase, but this specific image still does not finish end-to-end under the current model/runtime configuration.

## Final Verification Result

- RED baseline:
  - the previous image-import task had already proven the old timeout behavior could leave a request hanging in practice.
- GREEN targeted regression:
  - `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordCodexCliImageParserTest,MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: PASS, `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0`.
- RED full package attempt:
  - `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package`
  - Result: FAIL, blocked by unrelated pre-existing test compile error `MesProBatchRecordReportLayoutCalibratorTest` in the MES module.
- GREEN deployment package workaround:
  - `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
  - Result: PASS, rebuilt `yudao-server.jar` after skipping unrelated test compilation.
- GREEN restart after root-script fix:
  - `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
  - Result: PASS, restarted backend `48081` and frontend `8081`.
- GREEN direct control test on the exact screenshot file:
  - `codex.cmd exec --json --skip-git-repo-check --dangerously-bypass-approvals-and-sandbox -i C:\Users\BJB110\AppData\Local\Temp\ScreenShot_2026-05-15_170551_614.png --output-schema D:\ProjectPackage\Int\IntRuoyi\tmp\codex-image-report-schema.json "<prompt>"`
  - Result: PASS in about 245 seconds, proving the image can be recognized successfully when run directly.
- GREEN live backend timeout diagnosis:
  - authenticated `POST /admin-api/mes/pro/batch-record-report/import-image`
  - source file: `C:\Users\BJB110\AppData\Local\Temp\ScreenShot_2026-05-15_170551_614.png`
  - Result: explicit timeout JSON `{"success":false,"message":"Codex CLI 图片识别超时","code":500,...}` after about 602 seconds.
  - Backend logs now clearly show:
    - report import start
    - image recognition start
    - child process start with pid
    - timeout at about `600038 ms`
    - no structured stdout payload before timeout
