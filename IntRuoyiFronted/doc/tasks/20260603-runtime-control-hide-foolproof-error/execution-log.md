# 执行日志：运行控制台隐藏傻瓜式运维顶部错误

BDD: 傻瓜式运维超时不进入顶部错误条 -> Given 运行控制台傻瓜式运维数据接口超时 / When 页面聚合加载错误 / Then 顶部错误条不得显示 `傻瓜式运维：timeout ...`，避免与旁侧已有超时提示重复。

BDD: 运维矩阵错误仍显示 -> Given 运行控制台运维矩阵接口失败 / When 页面聚合加载错误 / Then 顶部错误条仍显示 `运维矩阵：...`，保留关键连接错误提示。

BDD: 傻瓜式运维失败仍影响连接状态 -> Given 傻瓜式运维数据加载失败 / When 页面刷新完成 / Then 页面连接状态不得被错误标记为完全正常。

RED: `node tests/e2e/runtime-control-hide-foolproof-error-static.spec.js` -> FAIL, 页面仍包含 `傻瓜式运维：`，且缺少 `foolproofLoadFailed` 状态标记。

GREEN: `node tests/e2e/runtime-control-hide-foolproof-error-static.spec.js` -> PASS。

GREEN: `node tests/e2e/runtime-control-static.spec.js` -> PASS。

CHECK: `node tests/e2e/runtime-control-foolproof-static.spec.js` -> 本次断言已通过，但脚本仍因既有无关断言失败：`.env.local` 期望 `8098/48098` 而当前为 `8081/48081`，以及旧 `镜像标签` 文案断言。

CHECK: `pnpm ts:check` -> FAIL, Node 默认堆内存不足，进程因 out of memory 退出。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

GREEN: `git diff --check` 目标文件 -> PASS，仅 Windows 换行提示。
