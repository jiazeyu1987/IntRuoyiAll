# PQC放行后批次执行可上市放行

## Current Status

ready_for_closeout

## Goal

按业务口径“详情里的单据就是正式业务事实”，PQC生产放行完成后，批次执行列表里的同一批次必须能找到正式上市放行事务并继续执行上市放行。

## Milestones

1. 复现并锁定缺口：PQC放行后缺少 `releaseTransactionId`。
2. 后端修复：PQC放行成功后创建或关联正式上市放行事务。
3. 验证：目标单元/静态测试通过。
4. 真实页面E2E：从重置指定测试订单开始，完成P1、P2、P3、PQC生产放行、上市放行，并在历史追溯验证活跃订单详情中的上市放行与上传文件事实。

## Expected Verification

1. 后端测试证明PQC放行成功后会初始化管理者/上市放行事务。
2. 后端测试证明前置缺失时明确失败，不默认成功。
3. 静态检查无空白或格式错误。
4. Playwright真实页面验证上市放行成功，历史追溯可见活跃订单全部历史事实。

## Latest Verification

2026-09-20：`node IntRuoyiFronted\\tests\\e2e\\stage1-p1-p2-boundary-real.e2e.cjs --with-p3 --with-market-release` 通过，runId `STAGE1-P1-P2-BOUNDARY-20260920094235`。从重置指定测试订单开始，P1、P2、通过详情页真实上传来料检/灭菌/成品检资料文件、P3、PQC生产放行、批次执行上市放行、历史追溯详情均通过；历史追溯详情显示同一活跃订单正式业务事实、PQC放行签名、上市放行事实和上传文件信息。

## 设计约束检查

1. 不使用fallback。
2. 不造假数据。
3. 不默认成功。
4. 不吞异常。
5. 不回退无关改动。
6. 不用SQL/API代替真实页面业务动作。
