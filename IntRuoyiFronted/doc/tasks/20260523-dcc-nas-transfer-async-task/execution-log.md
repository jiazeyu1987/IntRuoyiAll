# Execution Log：DCC NAS 转移前端异步任务化

BDD: 用户发起 NAS 转移后不应再等待同步长请求完成 -> Given 用户在 `转移到 DCC` 对话框中已选择目录、模板类别和生效日期 / When 用户点击 `确认转移` / Then 前端应只提交“创建转移任务”请求并快速进入任务状态展示，而不是等待整批转移完成

BDD: 前端必须明确展示后台转移任务状态 -> Given 后端已返回有效任务编号与状态 / When 前端开始轮询任务状态 / Then 页面必须展示任务编号、当前状态、已创建数量、失败数量与最近错误，不得静默隐藏任务失败

BDD: 任务完成后前端必须停止轮询并展示最终结果 -> Given 后台转移任务已进入 `COMPLETED` 或 `FAILED` 终态 / When 前端收到终态状态响应 / Then 前端必须停止轮询，保留最终统计与失败报告入口，不得继续无限请求

RED: 用户真实页面截图 -> FAIL，`转移到 DCC` 对话框在提交 `1. QMS documents/5.STM实验室规程` 时直接报 `timeout of 30000ms exceeded`，说明前端仍把整批 NAS 转移当作一次同步长请求等待

GREEN: `node --test scripts/system-nas-management.test.mjs` -> PASS，静态契约确认 NAS 页面已接入 `getNasTransferTaskState`、任务轮询与任务状态展示文案

GREEN: `pnpm exec eslint src/views/system/nas/index.vue src/api/dcc/controlledFile/workflow.ts scripts/system-nas-management.test.mjs --format stylish` -> PASS

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS

BLOCKED: 真实 `http://localhost:8081/system/nas` 用户路径验证 -> BLOCKED，当前运行中的本地后端尚未应用异步任务新表与新接口

GREEN: 真实 Playwright 用户路径验证 -> PASS，使用 `芋道源码 / admin / admin123` 登录 `http://127.0.0.1:8081/system/nas` 后，完成 `测试连接 -> 刷新目录 -> 选择 -> 展开 1. QMS documents -> 勾选 5.STM实验室规程 -> 打开 转移到 DCC -> 确认开始`，页面立即显示 `转移任务` 状态块，未再出现 `timeout of 30000ms exceeded`

GREEN: 同一真实页面轮询结果 -> PASS，任务 `id=1` 在页面上从 `待处理条目=533 / 成功文件=113` 自动刷新到 `待处理条目=278 / 成功文件=381`，证明前端已按任务态持续轮询而非同步等待单次长请求
