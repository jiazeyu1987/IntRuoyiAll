BDD: 访问规则页用户授权标签不应显示 undefined -> Given 管理员通过真实登录进入 `DCC访问规则` 页面 and `/admin-api/system/user/simple-list` 仅返回 `id`、`nickname`、`deptId`、`deptName` / When 页面渲染授权对象下拉与当前规则行 / Then 用户标签必须显示可读名称而不能拼出 `undefined`。

BDD: 访问规则目录区域应优先显示目录编码 -> Given 管理员通过真实登录进入 `DCC访问规则` 页面 and 目录树中存在 `name` 与 `code` 不同的目录 / When 用户查看左侧目录树与当前目录标题 / Then 主显示文本必须优先展示目录编码 / And 目录名称只能作为辅助上下文显示，不能替代目录编码。

## TDD / Verification Evidence

- M1: Completed. Previous frontend task `20260515-dcc-position-hide-source-remark` is already completed and does not block this resumed page fix.
- M2: Completed. This task directory, execution log, and existing script were identified before production code changes.
- PRECONDITION: `mvn -pl yudao-server -am -DskipTests package` -> PASS, rebuilt `yudao-server.jar` with `Main-Class` so runtime verification could start the local backend.
- PRECONDITION: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat` -> PASS, restarted frontend `8081` and backend `48081` to satisfy the real-page verification prerequisite.
- RED: `node doc/tasks/20260515-dcc-access-rules-undefined-user-label/scripts/verify-access-rules-source.mjs` -> FAIL, `tree_primary_display_is_not_code,title_missing_selected_directory_code`.
- RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-access-rules-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-access-rules-undefined-user-label\scripts\assert-access-rules-user-labels.mjs` -> FAIL, the real page selected a directory whose `name=3.DMR` and `code=INTAUTH-DFB695E6DDD14676933B747C6F0ECC1F0F9E43C8`, but the page title still displayed the name.
- GREEN: `node doc/tasks/20260515-dcc-access-rules-undefined-user-label/scripts/verify-access-rules-source.mjs` -> PASS.
- GREEN: `pnpm exec eslint src/views/dcc/controlled-file/access-rules/index.vue` -> PASS.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-access-rules-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-access-rules-undefined-user-label\scripts\assert-access-rules-user-labels.mjs` -> PASS, the same real page flow no longer hit the directory-title assertion or the `undefined` label assertion.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-access-rules-red eval "(() => { const title = document.querySelector('div.text-15px.font-600')?.textContent?.trim() || ''; const subtitle = Array.from(document.querySelectorAll('div')).map((el) => (el.textContent || '').trim()).find((text) => text.startsWith('目录名称：') || text === '当前目录编码已选中') || ''; const hasUndefined = document.body.innerText.includes('(undefined)') || document.body.innerText.includes('（undefined）'); return JSON.stringify({ url: location.href, title, subtitle, hasUndefined }); })()"` -> PASS, current runtime state is `{"url":"http://127.0.0.1:8081/dcc/controlled-file/access-rules","title":"INTAUTH-DFB695E6DDD14676933B747C6F0ECC1F0F9E43C8","subtitle":"目录名称：3.DMR","hasUndefined":false}`.
