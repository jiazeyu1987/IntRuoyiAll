# Verification Report

## Summary

- `D:\IntRuoyiWorktree\20260902-route-process-input-output-materials` 的工艺路线输入/输出物料改动已进入 `E:\IntRuoyi` 主工作区，目标文件逐项 hash 一致。
- 后端保存、读取、发布快照和一线报工输出物料读取通过定向 Maven 测试。
- 前端静态合同、字段引用回归、类型检查和端口契约均已通过。

## Commands

- PASS: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteFlowConfigServiceImplTest,MesProRouteServiceImplTest,MesFrontlineProcessMaterialServiceTest" test` -> 71 tests, 0 failures, 0 errors。
- PASS: `node IntRuoyiFronted\tests\e2e\route-process-input-output-materials-static.spec.cjs`。
- PASS: `rg -n "inputMaterialIds|outputMaterialIds|frontlineReportMaterialIds" IntRuoyiBackend\yudao-module-mes\src IntRuoyiFronted\src IntRuoyiFronted\tests -S`。
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false`。
- PASS: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1`。

## Notes

- 首次未设置 Node heap 的 `vue-tsc` 因 4GB OOM 失败；已按前端规则用 8GB 单实例重跑通过。
- 主工作区仍存在与本任务无关的注册证、数据库规则、登录文档、批记录解析器等脏改，未纳入本任务融合范围。
