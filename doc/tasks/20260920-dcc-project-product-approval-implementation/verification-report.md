# DCC项目代码与产品目录联合新建审批验证报告

## 验证范围

- 产品目录页发起联合申请。
- 项目代码页移除直接新建入口。
- 申请字段仅包含项目名称、项目代码、项目负责人、产品编码、产品名称、分类、备注。
- `admin` 独立完成审核和批准两个节点。
- 批准后在事务内写入 DCC 项目代码、DCC 产品目录和一对一关系。
- 项目代码、产品编码、产品名称的正式数据与申请身份占用防重。
- 正式写入失败回滚，并在独立事务中标记 `WRITE_FAILED`。
- 旧项目代码和产品目录直接新增服务入口拒绝绕过审批。

## Given/When/Then

### Given

- 联合申请表、关系表、身份占用表已完成迁移。
- 申请状态为 `PENDING_REVIEW`，审核和批准均固定由用户名 `admin` 执行。

### When

- 用户从 DCC 产品目录提交申请。
- `admin` 审核通过后，`admin` 批准通过。
- 正式写入过程中任一对象插入或关系更新失败。

### Then

- 审批前不生成正式项目代码或产品目录。
- 批准成功生成一个项目代码、一个产品目录和一条一对一关系，并将申请置为 `COMPLETED`。
- 写入异常导致正式事务回滚，申请在新事务中置为 `WRITE_FAILED`，并保留错误码和错误信息。
- 直接调用旧创建服务不会绕过联合审批。

## 执行结果

| 检查项 | 命令/方式 | 结果 |
|---|---|---|
| 后端业务静态合同 | `node yudao-module-dcc/src/test/js/dcc-project-product-approval-static.spec.cjs` | PASS |
| 前端页面/API 静态合同 | `node tests/e2e/dcc-project-product-approval-static.spec.js` | PASS |
| 前端类型检查 | `NODE_OPTIONS=--max-old-space-size=8192 pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json` | PASS |
| 变更空白检查 | `git diff --check` | PASS |
| 后端 Maven 编译 | `mvn -pl yudao-module-dcc -am -DskipTests compile` | BLOCKED：环境没有 `mvn` 或 `mvnw` |
| 真实页面 E2E | 未执行 | NOT RUN：本轮未明确要求 E2E，且仓库规则要求明确授权 |

## 结论

代码合同、前端类型和静态结构验证通过；后端 Maven 编译和真实页面 E2E 仍需在具备 Maven、后端依赖及授权的环境中执行。
