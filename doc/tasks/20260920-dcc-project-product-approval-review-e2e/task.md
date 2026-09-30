# DCC项目代码与产品目录联合新建审批静态审查与E2E

## Goal

对 DCC 项目代码与产品目录联合新建审批实现进行静态代码审查，修复发现的问题；静态验证无问题后，用 Playwright 通过真实前端页面完成一次完整 E2E 验证。

## Milestones

1. 读取仓库规则、运行态规则、既有实现记录和当前工作区状态。
2. RED：用静态审查和合同测试识别事务、并发、权限、字段、UI 和迁移问题。
3. GREEN：修复本任务相关问题，不触碰并行无关改动。
4. REGRESSION：重跑后端/前端静态合同、类型检查、diff 检查，具备运行态后执行真实页面 E2E。
5. 收尾记录验证证据、运行态边界和清理状态。

## Expected Verification

- 后端联合审批静态合同通过。
- 前端联合审批静态合同通过。
- 前端 TypeScript 检查通过。
- 静态代码审查无剩余阻断问题。
- Playwright E2E 使用真实页面、租户“芋道源码”、用户 `admin`，完成申请、审核、批准，并通过页面或只读核验确认正式数据出现。

## Current Status

completed

## Review Findings

1. `WRITING` 状态未进入待处理列表，写入中或写入失败转换前可能在审批面板短暂消失；已纳入待处理查询。
2. 批准后的正式写入阶段只校验正式表和活跃申请，未再次校验身份占用表；已补充项目代码、产品编码、产品名称 claim 校验。
3. 驳回释放身份占用使用逻辑删除，会与 `(tenant_id, identity_type, identity_value, deleted)` 唯一索引在重复驳回/重提时冲突；已改为按申请物理删除 claim。
4. E2E 暴露新申请表主键没有自增，导致 MyBatis-Plus 普通 `@TableId` 插入失败；已将三张新表主键改为 `AUTO_INCREMENT`，并补静态合同。
5. 整包编译暴露 `DccControlledFileWorkflowServiceImpl.applyOriginalVersionFile()` 未携带附件文件列表；已做最小修复，保留 `submitFiles.attachmentFiles()`。

## Verification So Far

- PASS: 后端联合审批静态合同。
- PASS: 前端联合审批静态合同。
- PASS: 前端 `vue-tsc --noEmit -p tsconfig.relaxed.json`。
- PASS: 相关 diff 空白检查。
- PASS: `mvn -pl yudao-module-dcc -am -DskipTests compile`。
- PASS: 用户授权后执行本任务迁移和依赖迁移，重启 `int_qms` 后端/前端。
- PASS: `mvn -pl yudao-server -am -DskipTests package`。
- PASS: Playwright 真实页面 E2E，`芋道源码/admin` 从 DCC 产品目录提交申请、审核通过、批准通过，产品目录页面可见；DB 只读确认申请 `COMPLETED`、正式项目代码和正式产品目录已写入。

## 设计约束检查

- 只处理 DCC 项目代码与产品目录联合新建审批相关文件。
- 不回滚、不覆盖工作区中其它并行改动。
- E2E 只通过真实前端页面执行业务动作，不用 API/DB 承担被验收动作。
- API/DB 如需使用，仅做只读核验。
- 不执行 Git 提交/推送，除非用户后续明确要求。
