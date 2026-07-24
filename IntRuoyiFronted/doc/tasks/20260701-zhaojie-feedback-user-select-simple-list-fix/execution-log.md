# Execution Log：待归属人员回显改为 simple-list（前端）

- `BDD: 缺少 system:user:query 时仍能回显已选归属人 -> Given 业务页面只需要展示已选用户标签 / When UserSelectV2 根据 ID 解析当前值 / Then 组件应通过 system/user/simple-list 回显昵称，而不是请求 system/user/list。`
- `BDD: 回显用户按输入 ID 集合过滤 -> Given 组件收到单个或多个用户 ID / When simple-list 返回候选集 / Then 只保留命中的用户项，不因缺失项抛异常。`
- `GREEN: experience-preflight -> PASS，已按门禁读取 docs\experience-index.md、docs\powershell-memory.md，并确认本轮只收口 UserSelectV2 回显合同，不修改待归属页布局或接口写入行为。`
- `RED: git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 show HEAD:src/views/system/user/components/UserSelectV2.vue -> FAIL，旧版 resolveItemById 仍直接调用 UserApi.getUserList(ids)，与“业务页无需 system:user:query 也能回显已选用户”的合同不一致。`
- `CHANGE: src/views/system/user/components/UserSelectV2.vue，将 resolveItemById 改为先读取 UserApi.getSimpleUserList()，再按当前 ID 集合映射命中用户项。`
- `CHANGE: tests/e2e/mes-feedback-user-select-permission-static.spec.js，新增“必须走 simple-list 回显、不再走 getUserList(ids)”的静态权限合同。`
- `GREEN: node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-user-select-permission-static.spec.js -> PASS。`
