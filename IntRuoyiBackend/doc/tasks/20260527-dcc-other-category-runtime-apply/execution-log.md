# 执行日志：运行库补齐 DCC 类别其他

BDD: 运行库补齐其他类别 -> Given 运行库存在唯一启用的 `产品技术要求` 且具备启用审批路线 / When 执行幂等初始化 SQL / Then 对应租户存在唯一启用的 `其他`，并复制源模板治理配置。

BDD: NAS 转移弹窗可选择真实其他 -> Given `芋道源码/admin` 登录 NAS 管理 / When 打开 `转移到 DCC` 弹窗 / Then 模板类别下拉默认命中真实启用的 `其他`，缺失时必须 fail fast。

GREEN: `node script\tests\dcc-other-template-sql.test.mjs` -> PASS，既有 SQL 静态断言通过。

RED: 测试服执行前查询 -> FAIL，`芋道源码 tenant_id=1` 存在启用的 `产品技术要求`，但没有启用的 `其他`；源模板治理配置为权限规则 5 条、分发规则 1 条、培训规则 1 条、启用审批路线 1 条、审批节点 12 个。

RED: 全租户前置检查 -> FAIL，测试服存在 `tenant_id=0` 的旧 `产品技术要求`，但 `system_tenant` 无 `id=0` 且无启用审批路线；直接执行全租户脚本会被 fail-fast 规则阻断。

RED: `node script\tests\dcc-other-template-sql.test.mjs` -> FAIL，新增真实租户作用域断言后，当前 SQL 未 JOIN `system_tenant`。

GREEN: `node script\tests\dcc-other-template-sql.test.mjs` -> PASS，SQL 只处理 `system_tenant.status=0` 且 `deleted=0` 的真实租户，旧 `tenant_id=0` 行不再进入 NAS 转移模板初始化范围。

GREEN: 测试服执行 `sql/mysql/20260526_dcc_other_template_category.sql` -> PASS，无数据库错误。

GREEN: 结果查询 -> PASS，`tenant_id=1` 新增 `DCC_OTHER_TEMPLATE_900250 / 其他`，唯一启用；权限规则 5 条、分发规则 1 条、培训规则 1 条、启用审批路线 1 条、审批节点 12 个，均与 `产品技术要求` 对齐。

GREEN: 幂等复验 -> PASS，重复执行 SQL 后 `tenant_id=1` 与 `tenant_id=122` 的启用 `其他` 均仍为 1 条。

GREEN: `node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` -> PASS，环境为 `http://172.30.30.58:8081`、`芋道源码/admin`，只读路径没有提交转移、保存映射或应用恢复写请求。

CLEANUP: `task_closeout.py --task-id 20260527-dcc-other-category-runtime-apply --mode preview` -> BLOCKED，delete 为 `<none>`；阻塞原因为当前 linked worktree 不能 fast-forward 合并到 `int_main`，且同一 worktree 仍存在较大范围 NAS ACL 在途改动。未执行清理。
