# 执行记录：DCC 访问规则已绑定目录列表 + 树形新增

- BDD: 左侧仅显示已绑定目录路径 -> Given 管理员打开 DCC 访问规则页 and 系统中只有部分目录存在访问规则 When 页面加载完成 Then 左侧只显示这些已绑定目录，并以完整路径字符串展示，而不是目录树节点。
- BDD: 新增目录时未绑定目录先进入草稿态 -> Given 管理员点击新增目录 and 选择一个尚无访问规则的目录 When 页面切换到该目录 Then 右侧进入空规则草稿态，保存成功前左侧不出现该目录。
- BDD: 新增目录选择已绑定目录只切换不重复 -> Given 管理员点击新增目录 and 选择一个已经在左侧列表中的目录 When 页面切换 Then 左侧不新增重复项，只高亮并加载该目录现有规则。
- BDD: 左侧删除删除整个目录规则集合 -> Given 管理员在左侧列表删除一个已绑定目录 When 删除成功 Then 该目录全部访问规则被移除，刷新后左侧不再显示该目录。
- BDD: 当前目录规则保存契约保持不变 -> Given 管理员在当前目录内新增、修改或删除单条规则 When 点击保存规则 Then 页面继续提交原有真实字段，不改变查看、预览、下载、启用绑定。

- INFO: task-created -> 已创建前端任务文档，准备补访问规则左侧目录列表 RED 静态合同。
- RED: `node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js` -> FAIL, `package.json must expose the access-rule bound directory list static script`，说明前端静态合同入口尚未接入。
- RED: `node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js` -> FAIL, `directory api must expose a bound access-rule directory list request`，说明前端尚未补已绑定目录列表与整组删除 API 封装。
- GREEN: `node tests/e2e/dcc-access-rule-bound-directory-list-static.spec.js` -> PASS
- GREEN: `node tests/e2e/dcc-access-rule-header-context-static.spec.js` -> PASS
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- GREEN: experience-preflight -> PASS，本轮准备执行真实登录最小路径，只做本机页面可达性与权限入口验证，不进行写入。
- GREEN: `node D:\\ProjectPackage\\Int\\IntRuoyi\\scripts\\preflight\\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /dcc/controlled-file/access-rules --target-text 访问规则` -> PASS，真实登录已进入访问规则页面。
