# 任务：展厅产品导入结果弹窗换行与跳过提示核查

## 任务目标

修复或确认展厅产品管理导入结果弹窗中 `<br/>` 被当作普通文字展示的问题，并核查截图中 `product_001` 仍出现在跳过产品列表是否来自旧后端进程。

## 前序任务检查

- 已检查前端相关任务 `doc/tasks/20260531-showroom-product-import-blank-keep-current/task.md`，状态为 completed，不阻塞本任务。
- 已看到前端仓库存在无关改动 `src/views/showroom-admin/shared/structuredError.ts`、`scripts/showroom-structured-network-error.test.mjs` 和一个已 Blocked 的运行控制台任务目录，本任务不触碰、不提交。
- 后端仓库当前仅有无关未跟踪 `runtime/`，本任务先从前端弹窗展示和本地后端进程版本核查开始。

## BDD 场景

- BDD: 导入结果弹窗按行展示统计 -> Given 导入接口返回包含换行的结果提示 / When 前端展示系统提示弹窗 / Then 用户看到分行统计文本，不看到原始 `<br/>` 标签。
- BDD: 图片差异产品不应被旧后端误导 -> Given `产品资料正式版.xlsx` 中 `product_001` 有产品图且与系统封面不同 / When 使用包含封面 hash 修复的新后端导入 / Then `product_001` 不应因固定 URL 复用被归为相同产品。

## 里程碑

- [x] M1：建立任务文档、BDD 场景与预期验证。
- [x] M2：定位弹窗 `<br/>` 来源和当前后端进程版本。
- [x] M3：补充 RED 回归测试。
- [x] M4：最小修复并运行 GREEN。
- [x] M5：收尾清理预览并提交本任务直接相关改动。

## 预期验证

- RED：前端导入结果格式化测试先证明 `<br/>` 会原样泄露到展示文本。
- GREEN：目标测试通过，导入结果展示文本不包含原始 HTML 标签。
- REGRESSION：`pnpm ts:check` 通过。

## Current Status

completed

## 当前状态

status: completed

已完成前端导入结果弹窗换行修复：使用安全 VNode 逐行渲染导入统计，不再把 `<br/>` 字符串传入普通 alert。已确认 8081 当前指向的 48081 后端仍是 2026-05-31 17:35 的旧 jar，早于后端封面 hash 修复，因此截图里的 `product_001` 跳过需要重启本地后端到新构建后再复验。已通过静态回归、类型检查、bug evidence 校验和 closeout 预览。
