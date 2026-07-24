# 任务：展厅前台动态详情路由菜单收口

## 目标

修复数字展厅前台点击菜单进入 `展厅产品`/`产品详情` 时出现 `加载展厅前台数据失败：展厅前台接口缺少数值字段：route.params.hallId` 的问题，确保动态详情路由不再作为无参菜单入口暴露。

## 非目标

- 不修改展厅前台真实数据接口契约。
- 不新增 mock、fallback 或静默降级逻辑。
- 不改动展厅后台页签、讲解生成或 Phase 2 范围功能。

## 里程碑

- [x] M1：确认上一个前端任务已完成并定位报错根因
- [x] M2：记录 RED 回归，证明动态详情路由被错误暴露为菜单入口
- [x] M3：收口动态详情路由配置并保持真实详情访问能力
- [x] M4：运行定向验证并更新执行证据
- [x] M5：完成任务收尾记录

## 预期验证

- `node --test scripts/showroom-frontstage.test.mjs`
- `pnpm exec eslint src/router/modules/showroom.ts src/views/showroom-frontstage/index.vue scripts/showroom-frontstage.test.mjs`

## 当前状态

Blocked. 当前对话已转向新的“展厅主页页签与公司页签报错修复”任务；本任务虽然验证已通过，但仍受前序 shared-foundation 任务混入同批文件的提交边界阻塞，暂不继续推进独立提交。

## 验证结果

- PASS: `node --test scripts/showroom-frontstage.test.mjs`
- PASS: `pnpm exec eslint src/router/modules/showroom.ts src/views/showroom-frontstage/index.vue scripts/showroom-frontstage.test.mjs`

## Remaining Blockers

- 当前仓库已存在另一个未完成 closeout 的 staged showroom frontstage 任务 `20260519-showroom-frontstage-shared-foundation`，且与本任务共享同一批文件；在不混入前序 staged 改动前，无法安全生成“仅当前任务”的提交。
- 当前缺少 `SHOWROOM_E2E_TENANT_NAME`、`SHOWROOM_E2E_FRONTSTAGE_USERNAME`、`SHOWROOM_E2E_FRONTSTAGE_PASSWORD` 环境变量，无法对 `http://localhost:8081` 执行带认证的真实前台 E2E 复核。

## 备注

- 当前 Git 仓库为 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`。
- 工作区存在其他未提交改动；本任务只处理展厅前台动态详情路由与对应回归脚本。
