# ERP 账套连接切换验证报告

## 结论

通过。页面提供测试账套和正式账套两个固定连接选项，选择后只有点击“保存连接”才更新后端持久化状态；页面始终明确展示实际生效的当前连接。验证结束后已恢复测试账套。

## 自动化验证

- 后端：`mvn "-Dtest=ErpKingdeeConfigServiceImplTest,ErpKingdeeConfigControllerTest" test` -> PASS，17/17。
- 前端合同：`node tests/e2e/profile-erp-table-auto-sync-static.spec.js` -> PASS。
- 前端质量：目标文件 ESLint 和 Prettier -> PASS。
- 差异检查：任务代码文件 `git diff --check` -> PASS。
- TypeScript 全量检查：`pnpm ts:check` 未通过，唯一错误位于本任务未修改的 `FrontlineFixedTemplatePanel.vue:2772`，与本功能无关；目标文件定向检查已通过。

## 真实路径验证

- 测试 -> 正式：选择后未保存时实际标签不变并出现“待保存”；保存后标签切换；刷新后保持正式账套。
- 正式 -> 测试：选择后未保存时实际标签不变并出现“待保存”；保存后标签切换；刷新后保持测试账套。
- 最终数据库状态：`yudao.erp.kingdee.connection.active=TEST`。
- 正式连接配置：隐藏、JSON 有效、连接必需字段完整；报告未记录凭据值。
- 桌面和 390x844 窄屏均完成截图检查，连接区域无重叠或溢出。
- 最新 E2E 控制台无错误。

## 运行状态

- 前端：`http://localhost:8081`，监听正常。
- 后端：`http://127.0.0.1:48081`，健康状态 `UP`。
- 本任务真实 E2E 使用的运行包 SHA-256：`0A4C30B257A8C9ACEA2CF171A8F60234702CEDD07B077532133B689C2EB43868`，该包保留在稳定运行目录。
- 收尾期间并行任务更新了 `48081` 主运行态；只读审计确认新运行包仍包含本任务 Controller、Service、VO 和连接枚举类，健康状态为 `UP`。最终运行包 SHA-256：`A8695B9B0E0E06249B7424A37474984DC5A4DD44A1C7B158CF1016099C3EC244`。

## 残余风险

- 工作区已有与本任务无关的 TypeScript 类型错误，因此无法声明全仓 `ts:check` 通过；该错误没有触及本任务目标文件和验证路径。
