# eDHR 并行第一组当前工序真实 E2E Evidence

- BDD: 开始节点并行第一组全部显示当前运行态 -> Given 芋道源码/admin 打开球囊扩张压力泵批次详情 When 详情接口返回工序开始后的第一组可执行任务 Then 粗洗工序、清洗工序、清洁工序三个工序组在真实页面均为黄色背景，组装Ⅰ工序不提前标黄。
- 前端入口：`http://127.0.0.1:8093`
- 后端入口：`http://127.0.0.1:48093`
- 租户/账号：`芋道源码/admin`
- 批次执行：`900000000903 / EDHRB-1785252397713 / 34126020001`
- 当前可执行工序组：`粗洗工序:928609`、`清洗工序:928611`、`清洁工序:928612`
- 页面黄色工序：`粗洗工序`、`清洗工序`、`清洁工序`
- 截图：`D:\IntRuoyiWorktree\20260729-edhr-parallel-highlight-e2e\doc\tasks\20260729-edhr-parallel-start-process-highlight\parallel-current-process-highlight.png`
- MES 写请求数：`0`
- RESULT: PASS
