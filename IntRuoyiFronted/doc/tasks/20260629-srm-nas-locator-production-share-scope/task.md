# 任务：SRM NAS定位 前端同步双共享范围

## 任务目标

- 将 `NAS定位` 页面状态区中的共享范围提示从单共享更新为双共享范围。
- 不改页面交互结构，只同步真实后端状态语义与静态契约。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-srm-nas-locator\task.md`
- 状态：`COMPLETED`
- 处理说明：上一前端任务已完成单共享页面交付；本次只做双共享范围同步。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接消费后端状态字段并同步固定提示文案，不保留旧单共享说明。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 页面状态区展示双共享范围 -> Given 页面读取到最新 NAS定位 状态 / When 渲染状态卡片和提示条 / Then 用户必须同时看到 质量体系文件 与 生产部 两个共享范围。`
- `BDD: 页面在 RUNNING 时展示真实进度 -> Given 状态接口返回运行中任务与阶段性进度 / When 页面每 3 秒轮询状态 / Then 状态区必须展示当前共享、当前目录与已扫描数量，而不是只有“刷新中”。`

## 里程碑

1. M1：补前端任务文档与静态契约 RED。`COMPLETED`
2. M2：实现文案与状态字段同步。`COMPLETED`
3. M3：运行静态契约回归。`COMPLETED`
4. M4：接入运行中进度展示与轮询可视化。`COMPLETED`

## 预期验证

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js`

## 最终验证结果

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js` -> PASS

## 当前结论

- 页面状态区“共享范围”与提示条已同步展示 `质量体系文件` 和 `生产部` 两个共享。
- 本轮不改页面交互，仅同步正式后端语义。
- 真实 E2E 脚本已修正为“先等 RUNNING，再等 RUNNING 结束”，避免误把旧成功状态当作新一轮刷新完成。
- 用户已追加要求：`RUNNING` 时必须看到真实进度，本轮将在不改搜索主流程的前提下补充状态卡片可视化。
- 已完成：页面新增 `运行进度` 卡片，在轮询期间展示共享阶段、当前共享、当前目录与已扫描目录/文件数量。
