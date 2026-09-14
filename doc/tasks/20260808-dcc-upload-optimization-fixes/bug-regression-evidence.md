# DCC 文件上传优化回归证据

## Bug Summary

DCC 文件上传页在历史文件升版、编号冲突、版本格式和生效日期校验上存在前后端状态不一致：页面可能同时显示“升版”“新建 master”和“可提交”，但提交阶段返回编号链路冲突；非法版本号和过去生效日期也未在前端预检中明确拦截或说明。

## Expected Behavior

历史文件升版、新建 master、编号冲突、版本格式和生效日期规则必须形成一致状态；“未分类”发布为允许规则，仅优化提示，不作为错误。

## Reproduction

Use the documented path `/dcc/controlled-file/upload` and static regression contracts around upload page state and submit error mapping.

## Root Cause

前端上传页把历史文件选择、同编号现行版本查询和新建 master 提示拆成多个独立状态，没有把“选择历史文件但无法定位现行主档”作为阻断态；文件编号/版本预检只检查非空、重复版本和修改中状态，未纳入现行版本查询错误、升版目标缺失和版本号格式错误；提交错误映射只把包含“版本/version”的错误挂到版本字段，导致编号链冲突英文原文直接弹出。

## Regression Test

新增 `IntRuoyiFronted/tests/e2e/dcc-upload-optimization-static.spec.js`，并在 `IntRuoyiFronted/package.json` 增加 `e2e:dcc:upload-optimization:static`。该合同锁定未分类允许文案、版本格式校验、历史升版与新建 master 互斥、编号链冲突中文化、生效日期允许补录提示和提交前强制现行版本校验。

## RED

RED: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-optimization:static` -> FAIL，首个失败为 `unclassified directory landing must be shown as an allowed business rule`，证明旧页面仍把“未分类”表达成自动兜底，后续合同覆盖的升版/编号/版本/日期状态也缺失。

## GREEN

GREEN: `pnpm --dir IntRuoyiFronted e2e:dcc:upload-optimization:static` -> PASS。

## Verification

- `pnpm --dir IntRuoyiFronted e2e:dcc:upload-current-version:static` -> PASS。
- `pnpm --dir IntRuoyiFronted e2e:dcc:upload-project-taxonomy-revision:static` -> PASS。
- `pnpm --dir IntRuoyiFronted e2e:dcc:upload-category-taxonomy-binding:static` -> PASS。
- `pnpm --dir IntRuoyiFronted e2e:dcc:upload-governance-ux:static` -> PASS。
- `pnpm --dir IntRuoyiFronted e2e:dcc:upload-name-version-autofill:static` -> PASS。
- `pnpm --dir IntRuoyiFronted e2e:dcc:upload-layout:static` -> PASS。
- `pnpm --dir IntRuoyiFronted ts:check` -> PASS。

## Risk And Regression Scope

变更范围限制在 DCC 上传页前端状态、字段校验、错误映射和相关静态合同；未改后端错误码、版本链服务、目录落位服务、数据库 seed 或真实业务数据。风险集中在上传页提交前多一次现行版本查询，已通过 `ts:check` 与相邻 DCC 上传合同确认没有破坏现有上传契约。

## Blockers And Follow-Up

Blockers: None。

Follow-up: DHF文件清单无正式类别、市场调研报告多正式类别、`smokeplan1` 昵称乱码属于配置/数据治理项，本任务未直接写数据库或远端测试服数据；建议拆成独立数据治理任务处理。
