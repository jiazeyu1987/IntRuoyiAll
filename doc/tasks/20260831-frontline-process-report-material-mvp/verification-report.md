# Verification Report

## Objective

独立核对工序多物料一线报工 MVP 是否满足确认需求、测试合同、真实页面路径和清理门禁。

## Decision

PASS。工序多物料一线报工 MVP 已在独立 worktree 完成实现与验证，真实测试数据已清理。

## Requirement Coverage

- 冻结路线产品 BOM 为唯一工序物料来源，运行态与会话快照一致。
- 页面按物料隔离数量、损耗、设备参数和计量状态草稿。
- 页签只有灰色/绿色；完成数量明确填写即绿色，`0` 也为绿色。
- 正式提交一次携带全部物料；缺少任一物料完成数量时不发送请求。
- 物料完成数 `5/3` 时，工序池进度和初始分配为 `3`，逐物料事实保留 `5/3`。
- 批号只读系统同步领料单；有数据展示去重批号，无数据保持空白。
- 主报工满足数量守恒，逐物料损耗不会造成主报工合格数量为负。

## Evidence Reviewed

- Backend: 17 个聚焦测试类，70 项 PASS，0 失败。
- Database: 迁移合同 2/2 PASS；release migration policy gate PASS，迁移总数 548。
- Frontend: ESLint PASS；`pnpm ts:check` PASS；4 个直接静态合同 PASS。
- Adjacent regression: 96 个 frontline 静态合同与基线对比，`CURRENT_ONLY` 为空。
- Real E2E: `1600x900` 页面验证 PASS；两物料 `5/3`、同步批号有/无、一次正式提交、最小值 `3` 均成立，目标错误为空。
- Runtime: worktree slot 56，前端 `8311`，后端 `48311`，health `UP`。
- Cleanup: `status=CLEAN`，`remainingTaskDataCount=0`。

## Verification Commands

- Maven 聚焦回归覆盖运行态、快照、批号查询、逐物料持久化、正式提交、回滚和工序池闭环。
- Python pytest 覆盖新迁移；release migration policy gate 覆盖完整迁移根。
- ESLint、Vue TypeScript 检查和 frontend 静态合同覆盖页面/API 契约。
- Playwright 通过真实登录和一线生产页面执行任务自有两物料正式提交。

## Findings

- 未发现阻断性缺陷。
- 浏览器首次验证发现并修正了泛型箭头函数被 Vite 模板解析器误判的问题。
- 聚焦复审发现并修正了主报工数量守恒问题，避免多物料损耗合计导致负合格数量。

## Residual Risks

- 批号展示依赖系统每日同步时效；同步表尚无目标记录时按正式规则显示为空，不会切换数据源。
- 当前旧 frontline 静态合同集合存在既有漂移，但基线差异证明本任务未新增失败。

## Follow-Up Actions

- 融合前运行分支端口守卫和精确差异检查。
- 融合后在 `int_main` 重新运行关键静态、类型、后端和迁移门禁。
