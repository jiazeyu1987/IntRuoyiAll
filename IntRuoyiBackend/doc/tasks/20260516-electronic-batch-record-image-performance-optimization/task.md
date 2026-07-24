# Task: Electronic Batch Record Image Performance Optimization

## Goal

Find and apply a faster `Codex CLI` recognition configuration for the exact electronic batch-record screenshot so the backend image-import path can complete more reliably within the service timeout budget.

## Scope

- Check the latest backend task status before starting this task.
- Create the task document and execution log before changing production code.
- Use the exact screenshot file as the only optimization benchmark.
- Run comparative `Codex CLI` experiments before changing backend defaults.
- Apply only the minimal backend configuration and prompt changes justified by the experiment results.
- Do not introduce fallback OCR engines or frontend changes in this task.

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-electronic-batch-record-image-timeout-logging/task.md`
- Status before this task: completed.
- Impact: timeout handling and logs are already improved, so this task can focus on speed optimization.

## Milestones

- [x] M1: Confirm the previous backend task is completed and create this task document.
- [x] M2: Record BDD scenarios and RED baseline for the current slow configuration.
- [x] M3: Run comparative `Codex CLI` experiments on the exact screenshot file.
- [x] M4: Apply the best-performing backend configuration.
- [x] M5: Run targeted verification for the scoped parser regression fix.

## Expected Verification

- direct `codex.cmd exec --image` comparisons on the exact screenshot file
- targeted backend regression tests after configuration changes

## Current Status

Completed for the scoped NO-GO fix. The accepted control experiment is now the temp-directory run without `-C`, with `--ephemeral`, and with minimal reasoning, which returned valid structured JSON in about `82147 ms`. The backend parser now keeps that path as the default, only appends `-C` when `codexWorkingDirectory` is explicitly configured, and restores the default timeout to `600000 ms` because the current evidence does not justify extending the fail-fast budget to `900000 ms`.

## Blocker And Impact

- Blocker: none for the scoped parser, test, and task-document fix.
- Impact: the default runtime now stays on the fastest validated path, while explicit `codexWorkingDirectory` configuration still allows slower repo-context runs when a future operator wants that tradeoff deliberately.

## Final Verification Result

- exact screenshot run without `-C`, with `--ephemeral`, and with minimal reasoning
  - Result: valid structured JSON after about `82147 ms`.
- exact screenshot run with current prompt and `-C D:\ProjectPackage\Int\IntRuoyi`
  - Result: valid structured JSON after about `293113 ms`.
- exact screenshot run with current prompt and `-C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
  - Result: valid structured JSON after about `308173 ms`.
- local source change applied:
  - `MesProBatchRecordCodexCliImageParser` now defaults `codexWorkingDirectory` to blank, appends `-C <workingDir>` only when it is explicitly configured, and restores the default timeout to `600000 ms`.
  - `MesProBatchRecordCodexCliImageParserTest` now covers blank-vs-explicit `codexWorkingDirectory` command building and locks the conservative timeout default.
- targeted Maven verification:
  - `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordCodexCliImageParserTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - Result: PASS, `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`.
