# 工艺路线设备列表显示产能

## 任务目标

在 MES 工艺路线详情的“组成工序”设备列表弹窗中，展示设备工序单台产能和按绑定数量计算的总产能。

## 前置任务状态

- 已检查最近同主题前端任务 `20260608-mes-route-process-machinery-column`：状态为 blocked，阻塞点是测试租户设备详情权限，不是本次产能列展示。
- 本次前端改动只增加设备列表弹窗展示列和接口类型，不改设备详情权限、不新增测试数据、不写入 admin 租户数据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少设备工序产能时显示 `未配置`，不从设备主档或产品产能兜底。
- `是否从根因和长期维护角度解决`：是。前端展示后端按设备+工序返回的产能字段，与资源大表和排程预算来源一致。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 设备列表显示单台和总产能 -> Given 工艺路线工序绑定了设备且设备+工序产能已配置 / When 用户打开该工序设备列表 / Then 每台设备显示单台产能和数量计算后的总产能。
- BDD: 缺少设备工序产能不兜底 -> Given 工位设备缺少设备+工序产能 / When 用户查看设备列表 / Then 单台产能和总产能显示未配置，不使用设备主档产能替代。

## 里程碑

- [x] M1：创建任务文档，记录 BDD 与设计约束。
- [x] M2：补充接口类型中的产能字段。
- [x] M3：设备列表弹窗新增 `单台产能/h` 与 `总产能/h` 两列。
- [x] M4：运行类型检查和真实页面只读验证。
- [x] M5：运行 task-closeout-cleanup 预览，仅提交本任务相关改动。

## 当前状态

已完成。

## 最终验证结果

- PASS：`node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- PASS：Playwright 登录本机 `芋道源码/admin`，只读验证 `/mes/pro/route?openId=900026` 中 `B010 吹球囊成型` 设备列表弹窗显示 `单台产能/h`、`总产能/h` 和 `A03190` 的 `9.52381`。
- PASS：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-route-process-machinery-capacity-list --mode preview`，delete/blocked/warnings 均为空。
