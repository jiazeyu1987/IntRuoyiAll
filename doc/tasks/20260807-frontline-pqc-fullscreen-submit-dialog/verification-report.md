# Verification Report

## Summary

- 一线PQC提交签名弹框不会被浏览器全屏层覆盖；弹框仍作为 `data-pqc-fullscreen-root` 的子树渲染，并保持 `position:absolute; inset:0; z-index:70` 覆盖全屏填写区。
- 新增静态合同锁定 fullscreen root、签名弹框 DOM 后代关系、禁止 body teleport，以及弹框覆盖层样式。
- 真实 Playwright 已在本机 `http://127.0.0.1:8081` 使用 `芋道源码/admin` 验证：进入全屏后点击提交，签名弹框可见、命中最上层，且没有调用正式 PQC submit 接口。

## Commands

- `RED: node tests\e2e\frontline-pqc-fullscreen-submit-dialog-static.spec.cjs -> FAIL, missing data-pqc-fullscreen-root`
- `GREEN: node tests\e2e\frontline-pqc-fullscreen-submit-dialog-static.spec.cjs -> PASS`
- `GREEN: node tests\e2e\frontline-pqc-formal-submit-static.spec.js -> PASS`
- `GREEN: node tests\e2e\edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs -> PASS`
- `GREEN: node tests\e2e\mes-frontline-pqc-fullscreen-preload-static.spec.js -> PASS`
- `GREEN: node doc\tasks\20260807-frontline-pqc-fullscreen-submit-dialog\pqc-fullscreen-submit-dialog-real-check.cjs -> PASS`
- `GREEN: pnpm ts:check -> PASS`
- `GREEN: git diff --check -- <task-owned paths> -> PASS`

## Real E2E Evidence

- `fullscreenRoot=true`，`dialogVisible=true`，`dialogInsideRoot=true`，`topElementInsideDialog=true`。
- `formalSubmitRequestCount=0`；真实检查只触发提交前 payload validate，不输入签名密码、不确认签名、不写入正式PQC提交。
- Evidence JSON: `doc/tasks/20260807-frontline-pqc-fullscreen-submit-dialog/evidence/pqc-fullscreen-submit-dialog-real-check.json`。
- Screenshot: `doc/tasks/20260807-frontline-pqc-fullscreen-submit-dialog/evidence/pqc-fullscreen-submit-dialog-real-check.png`。
- 真实页面仍出现一条既有上下文提示 `设备账号上下文不完整或不一致：activeOrder.processSnapshot.activeOrderId=30`；它未阻止 payload validate 和签名弹框展示，本任务未修改该数据上下文链路。

## Final Status

completed
