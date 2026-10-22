# Verification Report

## Result

静态修复与定向单元验证完成：URB-001 至 URB-017 均已由代码 Review 和针对性验证覆盖，未进行 E2E。5 项 P1 与 12 项 P2 均有对应实现和测试证据；本报告不代表真实页面、运行服务、数据库迁移或跨账号流程已验证。

## Finding Status

| 编号 | 处理结果 | 主要证据 |
|---|---|---|
| URB-001 | fixed | `need_training` 写入条件 SQL；`DccWorkingSubmissionConditionTest`；DCC reactor suite |
| URB-002 | fixed | 三流程分支锁读/CAS/事务包围 BPM 推进；`DccControlledFileFinalizationServiceImplTest` |
| URB-003 | fixed | `triggerTask` 缺失/多 execution 显式失败；BPM trigger suite 4/4；DCC 训练/分发失败测试 |
| URB-004 | fixed | 附件 UPLOADING/READY/FAILED 状态和冻结提交快照；上传生命周期合同 |
| URB-005 | fixed | 源文件、PDF request sequence 与附件 generation 回写保护；上传生命周期合同 |
| URB-006 | fixed | 同 session 同源票据逻辑链冲突分类，保留服务端幂等提交；工作稿重试合同 |
| URB-007 | fixed | CheckinReplayPayload 包含 `needTraining`；DCC QueryServiceTest |
| URB-008 | fixed | 前驱 ID 全程字符串比较；升版发布 UX合同 |
| URB-009 | fixed | 浏览器到时间维护表单/审计 API 全程 String ID；修复合同 |
| URB-010 | fixed | 预览元数据、媒体、文本、PDF、watermark、错误和 loading 统一 request generation；修复合同与 `ts:check` |
| URB-011 | fixed | 签名分页 request sequence 保护成功/失败/finally；详情签名合同 |
| URB-012 | fixed | 无环文件详情授权守卫；纸质分发授权测试和 DCCPaperDistributionAckServiceTest |
| URB-013 | fixed | 创建任务时保存部门/负责人名称；历史投影只用快照名称；DCC QueryServiceTest |
| URB-014 | fixed | 内容批准节点写 `approvedTime`，最终发布只写 `publishedTime`，Mapper 使用 `COALESCE`；Finalization suite |
| URB-015 | fixed | 读取 BPM 历史 `DISTRIBUTION` 活动结束时间；驳回/最终化失败测试和分发摘要合同 |
| URB-016 | fixed | 检入成功与刷新失败分离，保留新版本 ID并提供重新加载；修复合同与检入状态合同 |
| URB-017 | fixed | 已生效 NEW 小版本转 REVISION，未生效 NEW 返工保留 UPLOAD；工作流单测 |

## Verification Commands

- `mvn -pl yudao-module-dcc -am -DskipTests compile -Dcheckstyle.skip=true` -> PASS。
- `mvn -pl yudao-module-bpm -Dtest=BpmTaskServiceImplTriggerTaskTest -DskipITs test -Dcheckstyle.skip=true` -> 4 tests PASS。
- `mvn -pl yudao-module-dcc -am -Dtest=DccControlledFileWorkflowServiceImplTest,DccControlledFileFinalizationServiceImplTest,DccPaperDistributionAckServiceTest,DccControlledFileDetailAuthorizationGuardTest,DccControlledFileQueryServiceTest,DccWorkingSubmissionConditionTest -Dsurefire.failIfNoSpecifiedTests=false -DskipITs test -Dcheckstyle.skip=true` -> 381 tests PASS。
- `pnpm exec node tests/dcc-static-004-005-upload-lifecycle-static.spec.mjs` -> PASS。
- `pnpm exec node tests/dcc-static-004-working-draft-retry-static.spec.mjs` -> PASS。
- `pnpm exec node tests/e2e/dcc-upload-optimization-static.spec.js` -> PASS。
- `pnpm exec node scripts/dcc-upload-revision-browse-fixes.test.mjs` -> 3/3 PASS。
- `pnpm exec node scripts/dcc-checkin-upload-state.test.mjs` -> 4/4 PASS。
- `pnpm exec node tests/e2e/dcc-revision-publish-ux-final-static.spec.cjs` -> PASS。
- `pnpm exec node tests/e2e/dcc-detail-signature-evidence-nonblocking-static.spec.js` -> PASS。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm run ts:check` -> PASS。
- `git diff --check` -> PASS。

## Boundaries And Remaining Risk

- 本轮没有执行 E2E、Playwright、真实页面动作、数据库迁移/写入、服务启动/重启、OnlyOffice 连通性或 Git 提交/推送。
- 运行态数据库 schema、BPMN seed 是否已部署、跨租户和真实权限矩阵仍需后续运行验证；不能用本报告替代这些证据。
- 工作区包含本任务之外的既有改动，未回滚、未整理、未提交。
