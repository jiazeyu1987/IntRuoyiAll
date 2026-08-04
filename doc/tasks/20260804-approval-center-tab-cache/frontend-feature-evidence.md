# Frontend Feature Evidence

## Feature Goal And Non-Goals

- 目标：审批中心顶部页签切回时复用页面实例和已有状态，不重复初始化请求。
- 非目标：不改变审批数据、后端接口、权限、筛选语义、主动刷新和审批动作。

## Requirements And Acceptance

- AC1：首次进入审批中心正常加载。
- AC2：切走再切回不重复初始化。
- AC3：列表、筛选、分页状态保持。
- AC4：主动查询、刷新和有效路由筛选变化仍重新加载。

## UI Entry Points And Owned Files

- 入口：顶部页签“审批中心”。
- 路由：`src/router/modules/remaining.ts` 的审批中心四个列表子路由。
- 页面：`src/views/approval-center/index.vue`。
- 缓存基础设施：`src/layout/components/AppView.vue`、`src/store/modules/tagsView.ts`、`types/router.d.ts`。
- 测试：`tests/e2e/approval-center-tab-return-no-reload-static.spec.js`。

## API Contracts And Data States

- 保持现有审批中心列表与模块接口合同不变。
- 保持 loading、empty、error、permission 状态现有语义不变。

## BDD Scenarios

- Given 审批中心已加载并处于顶部页签缓存中，When 切换其它页签后返回，Then 保留实例和状态且不重复初始化请求。
- Given 用户主动查询、刷新或改变有效筛选，When 触发对应操作，Then 正常重新请求并展示正式结果或错误。

## RED

- `pnpm e2e:approval-center:tab-return-no-reload:static` -> FAIL。
- 预期失败：审批中心路由仍声明 `noCache: true`。

## GREEN

- 待执行。

## Responsive And State Checks

- 本任务不改布局。
- 需确认 loading、empty、error、permission、筛选可见性和键盘操作无回归。

## Verification Path

- 任务专用静态合同、相邻审批中心合同、`pnpm ts:check`。
- 若本地真实运行前置完整，再使用 Playwright 验证顶部页签切换及目标请求次数。

## Blockers And Follow-Up

- 当前无 blocker。
