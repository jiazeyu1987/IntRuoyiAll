# Task: DCC 访问规则仅显示目录名

## Goal

修正 `http://127.0.0.1:8081/dcc/controlled-file/access-rules` 页面的目录展示规则：

1. 左侧目录树只显示目录名，不显示目录编码
2. 当前目录标题只显示目录名，不显示目录编码
3. 同页用户授权标签继续保持不出现 `undefined`

## Scope

- 先检查同仓库上一条前端任务状态；若未完成，则显式阻塞后再启动本任务。
- 在生产代码修改前创建本任务目录、任务文档、执行日志和回归脚本。
- 使用真实前端入口、真实登录和真实页面数据复现“当前同时显示目录名与目录编码”的问题。
- 记录 BDD、RED、GREEN 证据，并说明上一轮需求理解偏差带来的影响。
- 仅修改 `DCC 访问规则` 页面目录展示相关前端代码；不改后端接口和目录数据。
- 运行至少一条真实页面回归验证和一条针对当前文件的静态验证。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-dcc-access-rules-undefined-user-label/task.md`
- Status before this task: completed.
- Impact: the previous task closed a misinterpreted “目录编码优先” implementation; this new task corrects the display rule to the user-confirmed “仅显示目录名”.

## Milestones

- [x] M1: Confirm the previous frontend task status and create this task directory before production code changes.
- [x] M2: Reproduce the real access-rules name/code display issue and capture RED evidence.
- [x] M3: Add reproducible regression checks for “目录名单显” and the existing `undefined` label guard.
- [x] M4: Apply the minimal frontend fix.
- [x] M5: Run GREEN verification, update evidence, and prepare a scoped frontend commit.

## Expected Verification

- A real browser path can log into `http://127.0.0.1:8081` and open `/dcc/controlled-file/access-rules`.
- Before the fix, runtime evidence proves the selected directory tree node or current title still contains the directory code together with the name.
- After the fix, the selected directory tree node and the current directory title show only the directory name for a directory whose `name` and `code` differ.
- After the fix, the page still does not render `undefined` in user authorization labels.
- A targeted static check against `src/views/dcc/controlled-file/access-rules/index.vue` passes.

## Current Status

Completed. The access-rules page now shows only directory names in the left tree
and current directory title, while the same page continues to guard against the
previous `undefined` user-label regression.

## Blocker And Impact

- Blocker: none.
- Impact: the page now matches the user-confirmed display rule and no longer exposes
  directory codes in the access-rules tree/header.

## Final Verification Result

- RED static regression:
  - `node doc/tasks/20260515-dcc-access-rules-name-only-display/scripts/verify-access-rules-directory-name-only-source.mjs`
  - Result: FAIL with `tree_still_displays_data_code,title_missing_selected_directory_name,title_still_displays_selected_directory_code,template_still_mentions_directory_code`
- RED runtime regression:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-access-rules-name-only-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-access-rules-name-only-display\scripts\assert-access-rules-directory-name-only.mjs`
  - Result: FAIL with `directory_tree_should_not_show_code:INTAUTH-DFB695E6DDD14676933B747C6F0ECC1F0F9E43C8:actual:INTAUTH-DFB695E6DDD14676933B747C6F0ECC1F0F9E43C8 3.DMR:name:3.DMR`
- GREEN static regression:
  - `node doc/tasks/20260515-dcc-access-rules-name-only-display/scripts/verify-access-rules-directory-name-only-source.mjs` -> PASS
- GREEN lint:
  - `pnpm exec eslint src/views/dcc/controlled-file/access-rules/index.vue` -> PASS
- GREEN runtime regression:
  - `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-access-rules-name-only-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-access-rules-name-only-display\scripts\assert-access-rules-directory-name-only.mjs` -> PASS
