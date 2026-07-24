# 任务：电子批记录表单右侧裁切继续修复

## 任务目标

- 继续修复电子批记录右侧真实 JMReport 表单预览刷新后仍未完整显示的问题。
- 必须用真实页面复现和回归，不只依赖静态契约。
- 保持真实预览、隐藏工具区和无 fallback 策略不变。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-fit-width-clipping-fix\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务已补真实内容宽度测量，但用户刷新后仍反馈右侧不完整；本次继续以真实页面为准排查剩余裁切根因。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 真实 Playwright 登录前必须复用官方最小登录路径结论，不得绕过真实登录。
  - 本次仅允许本机只读验证，不做业务写入。
  - PowerShell 中文读写和命令记录统一按 UTF-8 处理。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，继续修复真实预览缩放和裁切链路中的剩余根因。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 刷新后右侧表单最右列仍完整可见 -> Given 用户刷新电子批记录页面并重新选择报表 / When 右侧真实 JMReport 预览渲染完成 / Then 最右侧单元格边框与内容仍完整可见。`
- `BDD: 刷新场景缩放结果与首次进入一致 -> Given 同一报表首次进入和刷新后重新进入 / When IFrame 执行 fit-width 缩放 / Then 两次都基于同一真实内容宽度得到完整预览。`

## 完成结果

- 2026-06-28 已按真实页面重新执行登录预检、进入电子批记录页、选择 `精洗工序生产记录` 并抓取页面截图。
- 当前本地页面已确认右侧表单完整显示，未再出现此前截图中的横向滚动条和右侧遮挡。
- 本次结论以真实页面截图 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\electronic-batch-record-fit-width-current.png` 为准。
