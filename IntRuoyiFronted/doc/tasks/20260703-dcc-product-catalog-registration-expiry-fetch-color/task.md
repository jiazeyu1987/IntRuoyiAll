# DCC 产品目录注册证有效期抓取失败颜色修复

## 任务目标

修复 DCC 产品目录点击“注册证有效期”后，将外站抓取失败 `FETCH_FAILED` 误显示为红色不一致的问题。只有真实 `MISMATCH` 才显示红色；`MATCH` 保持绿色；抓取失败改为中性提示并通过 tooltip 暴露原因。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`，命令显式使用 UTF-8，不使用 `&&`。
- 前端样式：已读取 `D:\\ProjectPackage\\Int\\IntPP\\FRONTEND_STYLE.md`，保持现有表格风格，仅调整状态颜色语义。
- 缺陷修复：按回归测试先行，补静态契约测试后再改组件逻辑。
- 高风险动作：本轮不执行真实 E2E、登录写入、服务器操作或数据库写入。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，纠正前端状态语义，避免把外站抓取失败伪装成日期不一致。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 注册证有效期真实不一致才显示红色 -> Given 比对状态为 `MISMATCH` / When 列表渲染有效期单元格 / Then 有效期文字显示红色并提示当前值与外站值不一致。
- BDD: 外站抓取失败不应显示红色 -> Given 比对状态为 `FETCH_FAILED` / When 列表渲染有效期单元格 / Then 有效期文字不显示红色，只通过 tooltip 展示抓取失败原因。

## 里程碑

1. 建立任务文档与执行日志。completed
2. 补静态回归测试锁定 `FETCH_FAILED` 不得走红色类名。completed
3. 调整产品目录有效期颜色映射与 tooltip 文案。completed
4. 运行前端静态验证并收尾。completed

## 预期验证

- `node tests/e2e/dcc-basic-data-product-catalog-static.spec.js`

## 当前状态

completed

## 最终验证

- `node tests/e2e/dcc-basic-data-product-catalog-static.spec.js` -> PASS
