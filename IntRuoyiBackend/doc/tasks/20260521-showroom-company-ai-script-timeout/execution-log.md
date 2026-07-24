# Execution Log

BDD: Codex CLI 子进程未退出时也必须按超时失败 -> Given 本地 Codex CLI 子进程持续占用 stdout 且未在配置时间内退出 / When 后端通过 `CodexCliChatModel` 调用该进程 / Then 调用必须在配置 timeout 内失败并抛出明确超时错误，不得一直卡住线程。

BDD: 展厅公司 AI生成介绍 不得无限 loading -> Given `/showroom/company` 当前公司已存在真实发布版本 / When 用户点击 `AI生成介绍` / Then 后端必须在合理时间内返回成功结果或明确失败原因，不得因为本地 Codex CLI stdout 阻塞而让前端长期 loading。

RED: `mvn --% -pl yudao-module-ai -Dtest=CodexCliChatModelTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，新增回归 `callShouldHonorTimeoutEvenWhenCliKeepsStdoutOpen` 在 2 秒外层限时内超时，堆栈卡在 `CodexCliChatModel.executePrompt()` 的 `BufferedReader.readLine()`，证明当前实现先阻塞读 stdout，后置 `waitFor(timeout)` 无法生效。

GREEN: `mvn --% -pl yudao-module-ai -Dtest=CodexCliChatModelTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，重构为 stdout 重定向到临时文件并先 `waitFor(timeout)` 后，2 条定向测试全部通过，超时断言恢复生效。

GREEN: `mvn --% -pl yudao-server -am -DskipTests package` -> PASS，最新 `CodexCliChatModel` 已打入 `yudao-server.jar`。

GREEN: `cmd /c D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS，本地 `48081/8081` 已重启到最新运行时。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-ai-script-error-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-ai-script-error\scripts\reproduce-showroom-company-ai-script-error.mjs` -> PASS，真实页面点击 `AI生成介绍` 已返回 `code=0` 并回填中英文介绍，不再出现长时间无响应。
