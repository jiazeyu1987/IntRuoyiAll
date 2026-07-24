# Execution Log

BDD: 浏览页默认显示最新版本 -> Given 目录下同一受控文件存在当前版与历史版 When 用户进入 DCC 受控浏览列表 Then 列表默认只显示该文件的最新版本一行

BDD: 浏览页可从版本下拉切换历史版本 -> Given 某个列表行携带多个可见版本 When 用户展开版本下拉并选择旧版本 Then 该行展示的版本号、状态标签与行内操作目标切换到所选历史版本

BDD: 我的受控文件保持原有记录语义 -> Given 提交人存在多次受控文件修订记录 When 用户进入我的受控文件 Then 页面仍按提交记录展示，不被浏览页的新语义改写

RED: `node --test scripts/dcc-controlled-browser-version-selector.test.mjs` -> FAIL, 浏览页源码仍直接按 `row.id / row.versionNo` 展示，没有 `latestVersionOnly` 请求参数、版本下拉或按所选版本执行操作的逻辑。

GREEN: `node --test scripts/dcc-controlled-browser-version-selector.test.mjs` -> PASS, 浏览页已请求 `latestVersionOnly=true`，版本列已使用下拉并通过 `getSelectedVersion(row)` 驱动展示与操作。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm run ts:check` -> PASS, 前端放宽配置下的 Vue/TypeScript 静态检查通过；首次默认堆内存不足已通过显式 8GB Node 堆配置排除环境噪声。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-browser-latest-version-default open http://127.0.0.1:8081/login?redirect=%2Fdcc%2Fcontrolled-file%2Fbrowser` + `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-browser-latest-version-default run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-dcc-browser-latest-version-default\scripts\verify-dcc-browser-latest-version-default.mjs` -> PASS，真实页面请求已携带 `latestVersionOnly=true`，并可在受控浏览列表中切换到历史版本，`output/playwright/dcc-browser-latest-version-default.png` 已生成。
