# 20260611-runtime-console-backup-chain-display

## 任务目标

实现阶段 5 前端展示：运行控制台备份策略区域和备份点表格必须直接展示 DCC 全量/增量、对象新增/修改/删除/复用、链状态、演练状态和不可恢复原因。界面保持 IntPP 生产订单列表风格，紧凑、可扫描，不增加说明性营销文案。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。字段缺失按空值或不可恢复原因展示，不把缺失信息显示为成功。
- 是否从根因和长期维护角度解决：是。前端依赖后端 VO 类型字段，不自行解析 manifest。
- 是否存在临时补丁或绕过：否。本阶段只修改本地前端代码和类型。

## BDD 场景

- BDD: 运行控制台显示 DCC 链状态 -> Given 后端返回备份点 DCC 备份模式和 chainStatus / When 用户打开运行控制台 / Then 备份策略区域和备份点表格显示模式与链状态。
- BDD: 运行控制台显示对象变化数量 -> Given 后端返回新增、修改、删除、复用数量 / When 用户查看备份点表格 / Then 四类数量以紧凑数字列展示。
- BDD: 运行控制台显示演练状态 -> Given 后端返回 rehearsalStatus / When 用户查看备份点表格 / Then 显示演练状态标签。
- BDD: 运行控制台显示不可恢复原因 -> Given 后端返回 unrecoverableReasons / When 用户查看备份点表格 / Then 表格显示可悬停的原因摘要。

## 里程碑

- [x] M1：更新 API 类型。
- [x] M2：更新运行控制台展示。
- [x] M3：运行类型检查。

## 预期验证

- `pnpm ts:check`

## 当前状态

completed

## Verification Result

- `pnpm ts:check` -> FAIL，Node 默认堆达到 4GB 上限，进程 OOM。
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
