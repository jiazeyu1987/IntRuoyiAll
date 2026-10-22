# Verification Report

## Result

PASS

## Scope

验证业务口径：详情里的单据就是正式业务事实；PQC生产放行之后，批次执行允许上市放行；上市放行后进入批次执行历史追溯，历史追溯详情继续显示同一活跃订单详情的正式事实，包括PQC放行、上市放行和上传文件信息。

## Command

`node IntRuoyiFronted\\tests\\e2e\\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release`

## Evidence

- Run ID: `STAGE1-P1-P2-BOUNDARY-20260920094235`
- Result file: `output/playwright/stage1-p1-p2-boundary-real/STAGE1-P1-P2-BOUNDARY-20260920094235/result.json`
- P1: 生产进度 `100%`，检验进度 `100%`。
- P2: 批次执行 `900000001112` 生成，过程检验记录 `76` 行，生产/PQC原始提交数量未被P2新增。
- 资料上传: 通过活跃订单详情页真实上传三类资料文件：
  - `STAGE1-P1-P2-BOUNDARY-20260920094235-incoming-inspection.txt`
  - `STAGE1-P1-P2-BOUNDARY-20260920094235-sterilization.txt`
  - `STAGE1-P1-P2-BOUNDARY-20260920094235-finished-product.txt`
- PQC生产放行: 申请 `104` 已生产放行，签名为 `瑛泰管理员（2026-09-20 17:44:55）`。
- 上市放行: `releaseTransactionId=170`，`releaseStatus=RELEASED`，批次状态 `40`。
- 历史追溯详情: 同一详情中显示PQC放行签名、`活跃订单资料上传`、`PQC生产放行`、`批记录上市放行` 操作事实，并显示三条上传文件的文件名、上传人和上传时间。

## Notes

E2E写入动作均由Playwright在真实前端页面完成；未使用接口、SQL或脚本替代业务动作。结果中出现的外部百度统计请求 `net::ERR_ABORTED` 与业务链路无关，页面错误列表为空。
