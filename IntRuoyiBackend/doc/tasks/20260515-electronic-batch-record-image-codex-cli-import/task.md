# Task: Electronic Batch Record Image Import With Codex CLI Correction

## Goal

Implement and verify the backend portion of image-based electronic batch-record report import so the system can analyze a production-record image, run `Codex CLI` structured recognition on the image, and fail fast when the image, recognition result, or prerequisites are invalid.

## Scope

- Check the latest backend task status before starting this task.
- Create the task document and execution log before editing production code.
- Verify `Codex CLI` and image-analysis prerequisites before writing behavior changes.
- Implement the minimal backend image-import report slice only in the MES generated-report module.
- Run a live test with the exact screenshot file when it becomes accessible locally.
- If a prerequisite or runtime limit blocks completion, record the exact blocker and impact.
- Do not introduce fallback or silent downgrade to the `.doc` flow.

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-electronic-batch-record-image-codex-cli-design/task.md`
- Status before this task: completed.
- Impact: the implementation task can use the confirmed design baseline.

## Milestones

- [x] M1: Confirm the previous backend task is completed and create this task document.
- [x] M2: Verify `Codex CLI` and image-analysis prerequisites and record the result.
- [x] M3: Record BDD and RED evidence for the backend correction flow.
- [x] M4: Implement the minimal backend image-import and correction slice.
- [x] M5: Complete GREEN verification and create the scoped backend commit.
- [x] M6: Run a live test with the exact screenshot file and record the observed runtime behavior.

## Expected Verification

- `codex.cmd --version`
- `codex.cmd exec --json --skip-git-repo-check --dangerously-bypass-approvals-and-sandbox -i <image> --output-schema <schema.json> "<prompt>"`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordCodexCliImageParserTest,MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package`
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`

## Current Status

Implemented and regression-tested. The exact screenshot file `C:\Users\BJB110\AppData\Local\Temp\ScreenShot_2026-05-15_170551_614.png` was also used for a live test. The request now reaches the backend correctly, and the earlier stdin-hang bug is fixed, but the full structured recognition for this specific image still does not complete within the current practical timeout budget.

## Blocker And Impact

- Blocker 1: `PaddleOCR` can be imported in Python but fails at runtime on this workstation with a OneDNN `fused_conv2d` error.
- Impact 1: this slice intentionally uses the verified `Codex CLI --image + output-schema` path directly instead of depending on the broken local PaddleOCR runtime.

- Blocker 2: the exact screenshot image test still exceeds the current structured-recognition time budget with the default `Codex CLI` model configuration.
- Impact 2: the feature is implemented and test-covered, but this specific complex image is not yet finishing end-to-end under the current default runtime settings.

## Final Verification Result

- `codex.cmd --version` -> PASS, local command available as `codex-cli 0.128.0`.
- `python -c "from paddleocr import PaddleOCR; print('PaddleOCR import ok')"` -> PASS, importable.
- direct PaddleOCR runtime probe on a generated sample image -> FAIL, `OneDnnContext does not have the input Filter`.
- `codex.cmd exec --json --skip-git-repo-check --dangerously-bypass-approvals-and-sandbox -i D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\.image\MySQL.jpg --output-schema D:\ProjectPackage\Int\IntRuoyi\tmp\codex-schema-test.json "根据图片返回 JSON。summary 用一句中文描述主体。"` -> PASS, returned structured JSON through `item.completed`.
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, initial image-import controller/service regression passed.
- RED live test on the exact screenshot file before the stdin fix:
  - authenticated `POST /admin-api/mes/pro/batch-record-report/import-image`
  - source file: `C:\Users\BJB110\AppData\Local\Temp\ScreenShot_2026-05-15_170551_614.png`
  - result: client-side timeout after roughly 424 seconds
  - backend access log showed `preHandle` for `/admin-api/mes/pro/batch-record-report/import-image` without completion, proving the request entered the service and then stalled.
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordCodexCliImageParserTest,MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`.
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -DskipTests package` -> PASS, rebuilt `yudao-server.jar`.
- `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS, restarted backend `48081` and frontend `8081`.
- RED live test on the exact screenshot file after rebuilding `yudao-server.jar` and restarting `48081/8081`:
  - authenticated `POST /admin-api/mes/pro/batch-record-report/import-image`
  - source file: `C:\Users\BJB110\AppData\Local\Temp\ScreenShot_2026-05-15_170551_614.png`
  - result: request still timed out after roughly 424 seconds
  - direct shell control test with the same image and the same `output-schema` also timed out, which isolates the remaining bottleneck to the `Codex CLI` recognition workload for this image instead of the HTTP or Java integration layer.
