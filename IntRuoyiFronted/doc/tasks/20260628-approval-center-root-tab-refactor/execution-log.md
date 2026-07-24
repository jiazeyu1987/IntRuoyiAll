# 执行日志：审批中心一级入口重构

- `BDD: 审批中心以一级标签打开 -> Given 用户进入审批中心任一入口 / When 路由命中审批中心 / Then 页面以“审批中心”一级容器打开，而不是旧的单工作台隐藏页。`
- `BDD: 子页签由子路由驱动 -> Given 用户访问 /approval-center/todo 或其他审批中心子路径 / When 页面渲染 / Then 激活对应子页签并映射到正确的审批视图类型。`
- `BDD: 旧 query 入口自动归一 -> Given 用户访问 /approval-center?moduleCode=DCC&viewType=TODO / When 页面初始化 / Then 自动规范到 /approval-center/todo 并保留筛选参数。`
- `BDD: 非法审批页签直接报错 -> Given 用户访问不支持的审批中心子路径或非法 viewType / When 页面解析路由 / Then 直接显示明确错误提示，不静默降级到默认页签。`
- `GREEN: previous-task-check -> PASS, yudao-ui-admin-vue3/doc/tasks/20260628-electronic-batch-record-remove-outer-card/task.md 为 COMPLETED。`
- `GREEN: experience-preflight-not-required -> PASS, 本轮仅做前端静态改造与本地验证，不涉及真实 E2E、服务器写入、发布、恢复或其他高风险动作。`
- `RED: node scripts/approval-center-page-contract.test.mjs -> FAIL, 旧路由仍是单页 /approval-center，未声明一级容器子路由。`
- `RED: node tests/e2e/approval-center-root-tab-static.spec.mjs -> FAIL, 旧审批中心未声明一级容器子路由与 query 归一路径。`
- `GREEN: node scripts/approval-center-page-contract.test.mjs -> PASS`
- `GREEN: node tests/e2e/approval-center-root-tab-static.spec.mjs -> PASS`
- `GREEN: node tests/e2e/approval-center-phase2-static.spec.mjs -> PASS`
- `GREEN: node tests/e2e/approval-center-phase4-static.spec.mjs -> PASS`
- `GREEN: node tests/e2e/approval-center-phase5-retirement-static.spec.mjs -> PASS`
- `GREEN: node tests/e2e/approval-center-phase8-mes-feedback-static.spec.mjs -> PASS`
- `BDD: cutMenu 二级菜单跟随审批中心路由切换 -> Given 用户通过一级标签、旧链接归一或站内跳转进入 /approval-center/* / When 当前路由切换到审批中心子路由 / Then 左侧二级菜单自动同步为审批中心的五个子项，而不是停留在上一个模块。`
- `BDD: 审批中心顶部标签统一收敛 -> Given 用户在审批中心内切换 待办/已办/我发起的/抄送我的/签名待处理 任一子路由 / When TagsView 记录当前打开页签 / Then 顶部只保留一个名为“审批中心”的一级标签，不新增多个子路由标签。`
