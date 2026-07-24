# 任务：补齐本机 DCC 其他模板类别

## 任务目标

修复本机 DCC NAS 转移前置数据缺口：当 DCC 模板类别缺少启用的 `其他` 时，先在 DCC 文件类别中补齐启用的 `其他` 及其治理配置，再继续允许转移路径验证。

## 上一任务检查

- 上一个后端任务 `20260602-backend-docker-build-dns-blocker` 已标记 `completed`。
- 当前任务只处理本机运行库和任务记录；未经用户明确授权，不操作测试服或正式服。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少唯一启用的源模板类别、审批路线或数据库前置表时必须失败。
- `是否从根因和长期维护角度解决`：是。复用已验证、幂等的正式 SQL，从启用的 `产品技术要求` 复制治理配置到 `其他`，不创建空类别。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

BDD: 本机补齐 DCC 其他模板类别 -> Given 本机运行库存在唯一启用的 `产品技术要求` 类别及审批/权限治理配置 / When 执行已验证的 `sql/mysql/20260526_dcc_other_template_category.sql` / Then 本机运行库存在唯一启用的 `其他` 文件类别，并拥有权限、分发、培训、审批路线和审批节点治理数据。

BDD: 缺少正式源模板时失败 -> Given 本机运行库缺少唯一启用的 `产品技术要求` 或其启用审批路线 / When 执行补齐脚本 / Then SQL 必须明确失败，不得创建空 `其他` 或伪造治理规则。

## 里程碑

- [x] M1：建立任务文档并确认上一后端任务已完成。
- [x] M2：只读复现本机启用 `其他` 缺失或确认已存在。
- [x] M3：执行已验证的幂等 SQL，补齐本机 DCC 文件类别。
- [x] M4：只读校验 `其他` 唯一启用且治理数据齐全。
- [x] M5：运行静态/回归验证、收尾清理预览并提交本任务记录。

## 预期验证

- `node script/tests/dcc-other-template-sql.test.mjs`
- 本机 MySQL 只读查询：`dcc_file_category` 中启用 `其他` 唯一存在，且关联治理表存在记录。
- `task-closeout-cleanup` 预览无待删除临时产物。

## 当前状态

completed

本机真实租户 `tenant_id=1 / 芋道源码` 已补齐唯一启用的 `其他`，类别 ID `906104`，权限、分发、培训、审批路线和审批节点治理数据已复验。实际 NAS 转移未执行，因为本次用户消息只提供了前置缺失提示，未指定要转移的 NAS 路径。

## 最终验证结果

- `node script/tests/dcc-other-template-sql.test.mjs` -> PASS。
- 本机 MySQL 执行 `sql/mysql/20260526_dcc_other_template_category.sql` -> PASS。
- 本机只读复验 -> PASS，`tenant_id=1` 启用 `其他` 唯一存在，权限 5 条、分发 1 条、培训 1 条、启用审批路线 1 条、审批节点 4 个。
- 幂等复验 -> PASS，重复执行 SQL 后启用 `其他` 仍为 1 条。
- `task-closeout-cleanup --mode preview` -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。
