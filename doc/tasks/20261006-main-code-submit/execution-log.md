# Execution Log

## Request and Authorization

- 用户原话：提交推送主干代码。
- 范围：当前 int_main 已有提交及起始未提交代码的快照保存、推送；本任务记录独立提交。
- 已读取根 AGENTS.md、docs/task-closeout-rules.md、docs/worktree-restrictions.md、docs/branch-runtime-ports.md 及收尾与经验技能。
- 起始 HEAD=00897b976；origin=https://github.com/jiazeyu1987/IntRuoyiAll.git；起始 ahead=7，dirty=18。
- 验证边界：本轮执行 Git 完整性和提交安全核验；不重新声明已有改动的业务验收结论。

## Evidence

- 冻结基线 HEAD=00897b976c9e61317bb9b24e098e811c174d4989，共 18 个文件；git diff --check、UTF-8/文件类型、大小与新增差异敏感信息检查均 PASS；起始暂存区为空。
- pwsh -NoProfile -File scripts/preflight/branch-runtime-port-guard.ps1 -> PASS，int_main/int_main，8081/48081。
- git fetch origin int_main -> PASS；首次网络等待较长，仅处理本任务已确认的fetch子进程，原会话最终exit=0；随后以相同origin与分支、30秒低速超时及非交互凭据设置复核fetch，exit=0。未改变网络或Git持久配置。
- git rev-list --left-right --count origin/int_main...int_main -> 0/7，远端没有未合入提交。
- 暂存18个精确路径，前后SHA-256未漂移；Git过滤后的工作文件对象与索引对象逐项相等；staged路径、HEAD、git diff --cached --check均PASS。

## Frozen Baseline Files

| Path | SHA-256 | Bytes |
| --- | --- | ---: |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/batchrecord/MesProEdhrWorkTaskMapper.java | 5302a067dc0a96fe05f9c9fef6d2f2d4179c44754ce4d7e0e42311273c94e235 | 32186 |
| IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffService.java | b052d6d402c4ad14f48a0cae1fcbbd0a00672c79f60c518d54fa32bfd35258a6 | 45818 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesMarketReleaseDoneVisibilityTest.java | 0afa101671ef49be110efd3a58c4c890a66f456aae2071a73fa636d0ef2ddd50 | 25598 |
| IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/handoff/MesActiveOrderHandoffLifecycleTest.java | 8d5882f98054062f8bceb4d788a52a3762f6f1178140ca08036bdc2e44a2447b | 47934 |
| IntRuoyiFronted/build/vite/index.ts | ddf787a204e6f8b1b1657274b43efe008f32c8a39a2fa27838771030355b670a | 3407 |
| IntRuoyiFronted/build/vite/windowsReadFileLimit.mjs | 63e37ab33dafa6f94ca0399fbd5422b66805746ebcf496b008a10531e41b1ab2 | 1861 |
| IntRuoyiFronted/scripts/purge-source-scope.test.mjs | 344dd96b1a7d9ac95b9daf63442abbf739a598bc3fe6aeeec60f7ba426b3875b | 5261 |
| IntRuoyiFronted/scripts/windows-read-file-limit.test.mjs | 214ac5ae294c829957929f9dc1bf9a9fde3d0a14d8c6e6e2cdecab400703ffa4 | 8158 |
| IntRuoyiFronted/src/api/mes/pro/handoff/index.ts | 5181ca93892a3f71b91122d860a9e8b0e5e2b6e859c26dbdff11acdaa6df6e35 | 3415 |
| IntRuoyiFronted/src/utils/activeOrderHandoffNavigation.ts | 8aad0bce9ffb00be5778026374126a37accc5cc669e420f51972f5a93d70341e | 5336 |
| IntRuoyiFronted/src/views/mes/pro/handoff/ActiveOrderHandoffPanel.vue | 3aedea4466545cbfe04f259f1c6e46c4165588006372bc8f20db6de1c8bbe8f7 | 14323 |
| IntRuoyiFronted/src/views/mes/pro/processpool/components/ActiveOrderSubmissionDetailPanel.vue | fc5be584a835e89276ac420c08d01cd5a2440ad602a0a11323e238ea05699143 | 227893 |
| IntRuoyiFronted/src/views/mes/pro/production-release/PqcProductionReleasePage.vue | e22f35138db1252b1b6092e7bb0b05d269b2674ca6a17f747ff3191a7f285302 | 31292 |
| IntRuoyiFronted/tests/e2e/active-order-dossier-context-race.spec.cjs | 43b78f3f90a27399025ea51581c2b27472978bd1fedacc46863a0885a7da04a0 | 17800 |
| IntRuoyiFronted/tests/e2e/active-order-handoff-behavior.spec.cjs | ffe503654c98cbd49ec7b9297aa19c74615970adf94602da10aa8e42eb6d179d | 18529 |
| IntRuoyiFronted/tests/e2e/pqc-release-exact-task-route-behavior.spec.cjs | 3f51bed99af2ba2441cf6e05d816a292746b36cee8a677696e51e13747709a3e | 22382 |
| IntRuoyiFronted/vite.config.ts | aad9bd20f7d67ffdb39587ae5d22380640de824e2e8863fdcec60503770cd031 | 9568 |
| docs/worktree-memory.md | 8a9dad8994e6db09bda1346c1e46acbc1fade7b6eb85b7091b10122cbf9954b4 | 95983 |

## Commit and Closeout

- 基线提交：1b0bf53d51bc89cc8527449be52aebe0a335511c；文件清单为上表18项，1005 insertions、25 deletions。Git pre-commit端口钩子PASS。
- project-experience-consolidation：核对已有docs/worktree-memory.md的Dirty/Untracked分类门禁，在原节归并“主干快照提交”的冻结与验证边界经验；未新建长期经验文档。
- 当前任务没有业务实现变更；本任务实现提交范围为上述经验文档及4个任务记录。任务实现提交hash待记录。
- 状态已同步为ready_for_closeout；cleanup preview/apply与推送结果待记录。
- 任务文档结构验证：UTF-8、必需标题、task.md/task-state.json状态一致、18行冻结指纹清单均PASS；归并经验后的git diff --check PASS。
