# 任务：运行库补齐 DCC 类别其他

## 任务目标

- 在测试服务器运行库中补齐真实启用的 DCC 文件类别 `其他`。
- 使用已验证的幂等初始化脚本 `sql/mysql/20260526_dcc_other_template_category.sql`，从启用的 `产品技术要求` 复制治理配置。
- 补齐后复验 `芋道源码/admin` 打开 NAS 转移弹窗时可使用真实 `其他` 类别。

## 边界

- 不新增前端假选项，不改变 `POST /dcc/controlled-files/nas-transfer` 请求结构。
- 不创建空模板；缺少唯一启用的 `产品技术要求` 或缺少启用审批路线时必须失败。
- 本任务只执行和验证 DCC `其他` 类别数据，不修改 NAS 权限恢复生产代码。

## BDD 场景

- BDD: 运行库补齐其他类别 -> Given 运行库存在唯一启用的 `产品技术要求` 且具备启用审批路线 / When 执行幂等初始化 SQL / Then 对应租户存在唯一启用的 `其他`，并复制源模板治理配置。
- BDD: NAS 转移弹窗可选择真实其他 -> Given `芋道源码/admin` 登录 NAS 管理 / When 打开 `转移到 DCC` 弹窗 / Then 模板类别下拉默认命中真实启用的 `其他`，缺失时必须 fail fast。

## 里程碑

- [x] M1：建立运行库应用任务文档，明确边界和 BDD。
- [x] M2：记录执行前 RED/前置检查，确认目标租户缺少启用的 `其他`。
- [x] M3：运行已验证 SQL，补齐 DCC `其他` 类别。
- [x] M4：验证治理规则数量和 NAS 弹窗真实路径。
- [x] M5：更新任务证据并收尾。

## 预期验证

- RED：执行前查询显示 `芋道源码` 租户缺少启用的 `其他`。
- GREEN：`node script/tests/dcc-other-template-sql.test.mjs` -> PASS。
- GREEN：测试服务器 SQL 应用完成，`其他` 唯一启用且治理规则与 `产品技术要求` 对齐。
- GREEN：`node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` -> PASS。

## 当前状态

- 状态：completed。
- 已完成：任务文档初始化；补齐 SQL 真实租户作用域；测试服务器已为 `芋道源码` 补齐唯一启用的 DCC 类别 `其他`；治理规则数量与 `产品技术要求` 对齐；`admin-readonly` 真实 E2E 通过。
- 阻塞：无。

## 最终验证

- RED：执行前查询 -> FAIL，`tenant_id=1` 只有启用的 `产品技术要求`，缺少启用的 `其他`。
- RED：`node script\tests\dcc-other-template-sql.test.mjs` -> FAIL，脚本未限定 `system_tenant` 中的真实启用租户，测试服旧 `tenant_id=0` 源类别会阻断全租户执行。
- GREEN：`node script\tests\dcc-other-template-sql.test.mjs` -> PASS，脚本只处理启用且未删除的真实租户。
- GREEN：测试服执行 `sql/mysql/20260526_dcc_other_template_category.sql` -> PASS。
- GREEN：结果查询 -> PASS，`tenant_id=1` 的 `其他` 唯一启用，权限规则 5 条、分发规则 1 条、培训规则 1 条、启用审批路线 1 条、审批节点 12 个，均与 `产品技术要求` 一致。
- GREEN：重复执行 SQL -> PASS，`tenant_id=1` 与 `tenant_id=122` 的启用 `其他` 均仍为 1 条。
- GREEN：`node tests\e2e\dcc-nas-permission-real-data.e2e.js --mode=admin-readonly` -> PASS，`baseUrl=http://172.30.30.58:8081`。
- CLEANUP：`task_closeout.py --task-id 20260527-dcc-other-category-runtime-apply --mode preview` -> BLOCKED，delete 为 `<none>`；当前 linked worktree 不能 fast-forward 合并到 `int_main`，且仍存在较大范围 NAS ACL 在途改动，因此未执行清理。
