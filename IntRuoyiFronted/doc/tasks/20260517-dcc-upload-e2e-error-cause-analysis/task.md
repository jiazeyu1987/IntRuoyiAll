# Task: DCC 上传 E2E 错误原因排查

## Goal

排查 DCC 上传叶子目录 E2E 复测中出现的两条 `系统内部错误` toast 与控制台 `@vite/client` `document is not defined` 的真实原因，确认它是否来自上传链路本身。

## Scope

- 先确认上一条相关前端验证任务已完成，再创建本任务记录。
- 使用真实前端入口和真实登录态复跑上传页，但这次重点抓控制台错误、失败请求与 toast 来源。
- 不改生产代码；仅做定位分析并记录结论。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-dcc-upload-leaf-directory-e2e-retest/task.md`
- Status before this task: completed.
- Impact: the upload path is already verified as pass, so this task can focus only on error-cause analysis.

## BDD

BDD: 排查 E2E 过程中出现的系统内部错误来源 -> Given DCC 真实上传路径可以成功提交 / When 诊断脚本捕获页面 toast、控制台错误与失败请求 / Then 应明确区分错误是来自上传 API、其他后台请求，还是前端 dev-server/HMR 运行时噪声。

## Milestones

- [x] M1: 创建任务文档与执行日志。
- [x] M2: 执行带诊断信息的真实上传复跑。
- [x] M3: 输出根因结论与剩余风险。

## Expected Verification

- 真实 Playwright 诊断复跑 `http://127.0.0.1:8081/dcc/controlled-file/upload`

## Current Status

Completed.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-e2e-error-cause-analysis run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-upload-e2e-error-cause-analysis\scripts\diagnose-dcc-upload-e2e-error-cause.mjs` -> PASS
- 真实诊断结果：
  - 上传提交接口成功：`POST /admin-api/dcc/controlled-files/submit -> code=0`
  - 两条 `系统内部错误` toast 对应失败接口：
    - `GET /admin-api/dcc/controlled-files/upload-name-options?categoryId=9`
    - 返回 `code=500, message=系统内部错误`
  - 后端日志堆栈：
    - `java.lang.NullPointerException`
    - [DccControlledFileQueryServiceImpl.java](/abs/path/D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:213)
    - 触发行：`currentVersionMap.get(master.getCurrentActiveControlledFileId())`

## Root Cause

- `upload-name-options` 会遍历该类别历史主文件链 `masterList`。
- 其中至少一条 `master` 的 `currentActiveControlledFileId` 为 `null`。
- 当前实现把 `currentVersionMap` 构造成不可接收 `null key` 的不可变 `Map`，随后直接执行 `currentVersionMap.get(null)`，触发 `NullPointerException`。
- 所以前端在类别切换后加载历史文件名称建议时收到 500，并弹出两条 `系统内部错误` toast；这与最终上传提交成功是两条独立链路。

## Residual Risk

- 先前 E2E 中控制台出现的 `@vite/client` `document is not defined` 在本次诊断复跑中未再出现。
- 当前可确认的用户可见错误根因，是 `upload-name-options` 的后端空指针。
- `@vite/client` 错误更像独立的 dev/HMR 噪声，暂时没有证据表明它是本次两条 toast 的直接来源。
