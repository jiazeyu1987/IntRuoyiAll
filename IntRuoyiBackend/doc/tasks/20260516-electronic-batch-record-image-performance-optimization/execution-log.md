BDD: the exact screenshot remains the benchmark input -> Given this task is an optimization pass, When experiments run, Then they must use the exact screenshot file `C:\Users\BJB110\AppData\Local\Temp\ScreenShot_2026-05-15_170551_614.png` so elapsed-time comparisons stay meaningful.

BDD: a faster configuration must still return structured JSON -> Given a candidate model or prompt change is tested, When `Codex CLI` finishes, Then the result must still satisfy the existing output schema instead of trading away structural validity for speed.

BDD: backend defaults change only when an experiment is measurably better -> Given multiple `Codex CLI` configurations are compared, When one option is both faster and structurally valid, Then only that option should be promoted into the backend parser configuration.

BDD: default parser runs should not silently opt into repo context -> Given `codexWorkingDirectory` is not explicitly configured, When the parser builds the `codex exec` command, Then it must not append `-C`.

BDD: explicit working-directory configuration should stay opt-in -> Given `codexWorkingDirectory` is explicitly configured, When the parser builds the `codex exec` command, Then it must append `-C <workingDir>`.

BDD: timeout should stay conservative without stronger evidence -> Given the fastest successful control experiment finished in about `82147 ms`, When no custom timeout is configured, Then the parser should keep the default timeout at `600000 ms` instead of extending the fail-fast budget to `900000 ms`.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordCodexCliImageParserTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `codexWorkingDirectoryProperty_defaultsToBlankSoCFlagNeedsExplicitConfig` still saw `${...:#{systemProperties['user.dir']}}` and `timeoutDefault_restoresConservativeBudget` still saw `900000`, proving the default parser configuration did not match the accepted experiment contract.
GREEN: exact screenshot run without `-C`, with `--ephemeral`, and with minimal reasoning -> PASS, valid structured JSON after about `82147 ms`.
GREEN: exact screenshot run with `-C D:\ProjectPackage\Int\IntRuoyi` -> PASS, valid structured JSON after about `293113 ms`.
GREEN: exact screenshot run with `-C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` -> PASS, valid structured JSON after about `308173 ms`.
GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordCodexCliImageParserTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`.
