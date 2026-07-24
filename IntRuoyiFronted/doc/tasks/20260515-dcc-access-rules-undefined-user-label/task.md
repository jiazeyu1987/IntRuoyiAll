# Task: DCC 访问规则页面显示修复

## Goal

修复 `http://127.0.0.1:8081/dcc/controlled-file/access-rules` 页面的两个显示问题：

1. 授权对象中的用户标签不再拼出 `undefined`
2. 目录选择区与当前目录标题优先显示目录编码，而不是目录名称

## Scope

- 先检查同仓库上一条前端任务状态；若未完成，则显式阻塞后再启动本任务。
- 复用现有任务目录、执行日志与回归脚本，不新建第二个同页任务。
- 在生产代码修改前先补齐本次“目录编码显示”场景的 BDD、RED、GREEN 目标。
- 使用真实前端入口、真实登录和真实页面数据复现 `DCC 访问规则` 页面显示异常。
- 记录 BDD、RED、GREEN 证据，并补充缺陷根因说明。
- 仅修改与 `DCC 访问规则` 页面目录展示和授权对象标签展示直接相关的前端代码。
- 运行至少一条真实页面回归验证和一条针对当前文件的静态验证。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-dcc-position-hide-source-remark/task.md`
- Status before this task: completed.
- Impact: the previous DCC position-column task is closed and does not block this resumed access-rules page fix.

## Milestones

- [x] M1: Check the previous frontend task state.
- [x] M2: Create or identify this task directory before production code changes.
- [x] M3: Reproduce the real access-rules display bugs and capture RED evidence.
- [x] M4: Add reproducible regression checks for both the `undefined` label and directory-code display symptoms.
- [x] M5: Apply the minimal frontend fix.
- [x] M6: Run GREEN verification, update evidence, and prepare a scoped frontend commit.

## Expected Verification

- A real browser path can log into `http://127.0.0.1:8081` and open `/dcc/controlled-file/access-rules`.
- Before the fix, runtime evidence proves the page title still prefers directory name over directory code when a directory has different `name` and `code`.
- Before or during RED, runtime or contract evidence proves the user subject label logic can produce `undefined`.
- After the fix, the same runtime check shows the selected directory title prefers `code`, while still preserving readable directory-name context.
- After the fix, the same runtime check no longer finds `undefined` on the page.
- A targeted static check against `src/views/dcc/controlled-file/access-rules/index.vue` passes.

## Current Status

Completed. The `DCC 访问规则` page now shows directory `code` as the primary
tree/title label, keeps directory `name` as secondary context only when needed,
and no longer builds user labels from the missing `username` field.

## Blocker And Impact

- Blocker: none.
- Impact: the access-rules page now uses the expected directory-code-first display
  and the same scoped regression checks can catch both display regressions.

## Final Verification Result

- RED static regression:
  - `node doc/tasks/20260515-dcc-access-rules-undefined-user-label/scripts/verify-access-rules-source.mjs`
  - Result: FAIL with `tree_primary_display_is_not_code,title_missing_selected_directory_code`
- Runtime prerequisite recovery:
  - `mvn -pl yudao-server -am -DskipTests package` -> PASS
  - `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS, frontend `8081` and backend `48081` returned HTTP 200
- RED runtime regression:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-access-rules-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-access-rules-undefined-user-label\scripts\assert-access-rules-user-labels.mjs`
  - Result: FAIL with `directory_title_expected_code:INTAUTH-DFB695E6DDD14676933B747C6F0ECC1F0F9E43C8:actual:3.DMR:name:3.DMR`
- GREEN static regression:
  - `node doc/tasks/20260515-dcc-access-rules-undefined-user-label/scripts/verify-access-rules-source.mjs` -> PASS
- GREEN lint:
  - `pnpm exec eslint src/views/dcc/controlled-file/access-rules/index.vue` -> PASS
- GREEN runtime regression:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-access-rules-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-access-rules-undefined-user-label\scripts\assert-access-rules-user-labels.mjs` -> PASS
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-access-rules-red eval "(() => { const title = document.querySelector('div.text-15px.font-600')?.textContent?.trim() || ''; const subtitle = Array.from(document.querySelectorAll('div')).map((el) => (el.textContent || '').trim()).find((text) => text.startsWith('目录名称：') || text === '当前目录编码已选中') || ''; const hasUndefined = document.body.innerText.includes('(undefined)') || document.body.innerText.includes('（undefined）'); return JSON.stringify({ url: location.href, title, subtitle, hasUndefined }); })()"` -> PASS with `{"url":"http://127.0.0.1:8081/dcc/controlled-file/access-rules","title":"INTAUTH-DFB695E6DDD14676933B747C6F0ECC1F0F9E43C8","subtitle":"目录名称：3.DMR","hasUndefined":false}`
