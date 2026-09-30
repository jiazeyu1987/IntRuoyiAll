# Execution Log - DCC项目代码与产品目录联合新建审批静态审查与E2E

## 2026-09-20

- 读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/local-runtime.md`、`docs/branch-runtime-ports.md`。
- 确认当前分支为 `int_qms`，工作区存在大量并行未提交改动；本任务只审查和修改联合审批相关文件。
- BDD: Given 用户从 DCC 产品目录提交联合申请，When admin 审核并批准，Then 系统创建一个 DCC 项目代码、一个 DCC 产品目录及一条一对一关系，并阻断重复数据。
- BDD: Given 正式写入任一步失败，When 批准动作触发写入，Then 正式数据回滚，申请标记为 `WRITE_FAILED`，错误向调用方暴露。
- BDD: Given 用户进入 DCC 项目代码页，When 查看工具栏，Then 不存在直接新建项目代码入口。
- RED: 静态审查发现 `WRITING` 状态未进入待处理查询，写入中/写入失败边界存在列表丢失风险。
- GREEN: `DccProjectProductCreateRequestMapper.selectPendingList()` 增加 `WRITING` 状态，后端静态合同补充断言。
- RED: 静态审查发现正式写入阶段缺少 identity claim 再校验，审批通过写入时可能未显式阻断已被其它申请占用的项目代码、产品编码或产品名称。
- GREEN: `DccProjectProductCreateWriteService.validateUnique()` 增加项目代码、产品编码、产品名称 claim 冲突校验。
- RED: 静态审查发现驳回释放 identity claim 使用逻辑删除，重复驳回/重提会与唯一键 `(tenant_id, identity_type, identity_value, deleted)` 冲突。
- GREEN: `DccProjectProductIdentityClaimMapper.deleteByRequestId()` 改为物理删除，后端静态合同补充断言。
- GREEN: `node yudao-module-dcc/src/test/js/dcc-project-product-approval-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/dcc-project-product-approval-static.spec.js` -> PASS。
- GREEN: `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` -> PASS。
- GREEN: `git diff --check` scoped to related files and task docs -> PASS。
- GREEN: `mvn -pl yudao-module-dcc -am -DskipTests compile` -> PASS。
- VERIFY: `8061/48061` 当前监听进程均归属 `int_qms` 本地分支运行时，未触碰 `int_main`。
- BLOCKED: 只读查询 `information_schema.tables` 返回 0，说明本地库缺少 `dcc_project_product_create_request`、`dcc_project_product_identity_claim`、`dcc_project_product_relation` 三张表；完整 E2E 需要先明确授权执行本任务 SQL 迁移，当前不能擅自写库。
- AUTHORIZED: 用户明确授权本地数据库迁移。
- GREEN: 执行 `20260920_dcc_project_product_create_approval.sql` -> PASS；只读核验三张新增表存在。
- RED: `mvn -pl yudao-server -am -DskipTests package` -> FAIL，`DccControlledFileWorkflowServiceImpl.applyOriginalVersionFile()` 构造 `ResolvedSubmitFiles` 时少传 `attachmentFiles`。
- GREEN: 最小修复 `applyOriginalVersionFile()`，保留 `submitFiles.attachmentFiles()`；`mvn -pl yudao-server -am -DskipTests package` -> PASS。
- RUNTIME: 重启 `int_qms` 后端到 `output/runtime/int_qms/branch-backend-runtime-20260920-171253.jar`；补充 DCC 签名证据和 MES 独立收货本地必需环境变量后，`/actuator/health` -> `UP`。
- RUNTIME: 重启 `int_qms` 前端 8061，Vite ready，首页 HTTP 200。
- RED: Playwright E2E 第一轮直接访问 `/dcc/controlled-file/basic-data/product-catalog` 未进入真实菜单页；只读菜单表确认真实路径为 `/mdm/product-catalog` 与 `/mdm/project-code`。
- RED: Playwright E2E 第二轮提交申请失败；后端日志显示 `dcc_data_relation` 表缺失和 `dcc_project_product_create_request.id` 无默认值。
- GREEN: 执行依赖迁移 `20260903_dcc_explicit_data_relation.sql`；修正本任务迁移中三张表 `id BIGINT NOT NULL AUTO_INCREMENT`，并 ALTER 本地表为自增；只读结构核验通过。
- GREEN: Playwright E2E 第三/四/五轮完成业务链路，脚本逐步修正页面校验 strict mode 和项目代码页分页/列配置问题。
- GREEN: Playwright 真实页面 E2E 最终 PASS：数据 `E2E-PP-1789897401637` / `E2E-PROD-1789897401637`；页面完成申请提交、审核通过、批准通过、产品目录可见；DB 只读核验 request `COMPLETED`、project `ENABLE`、product 已写入。
- GREEN: 补充迁移 `AUTO_INCREMENT` 静态合同后，`node yudao-module-dcc/src/test/js/dcc-project-product-approval-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/dcc-project-product-approval-static.spec.js` -> PASS。
- GREEN: `git diff --check` scoped to related files and task docs -> PASS。
- GREEN: `node --check doc/tasks/20260920-dcc-project-product-approval-review-e2e/real-e2e.cjs` -> PASS。
