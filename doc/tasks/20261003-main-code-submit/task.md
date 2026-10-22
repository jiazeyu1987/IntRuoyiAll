# 2026-10-03 主干代码提交推送

## Goal
按用户“提交推送主干代码”的授权，将当前 int_main 待提交的前后端实现、对应测试、SQL 文件及既有经验文档，经定向验证后提交并推送 origin/int_main。

## Milestones
- M1 固定现有改动白名单、内容指纹和远端基线：completed（98 个文件，SHA256 已记录，暂存区为空，fetch 后 0/0）。
- M2 执行与改动匹配的前后端定向回归、源码及文档检查：completed（后端 35 类 722 tests，前端 10 个定向脚本及 21-spec 套件、源码编译/ESLint/文档检查 PASS）。
- M3 按白名单提交代码基线并推送：completed（18e814b4f，已推送 origin/int_main，远端身份核验一致）。
- M4 ready_for_closeout、cleanup preview/apply、收尾记录提交推送及同步核验：completed（cleanup PASS；最终核心记录随本次收尾提交推送，完成时复核远端）。

## Expected Verification
- 初始与提交前文件清单及 SHA256 一致，暂存区只包含授权白名单，无冲突标记、真实凭据或临时输出。
- MES 与 system 本次新增/修改测试类及相邻签名合同共 35 类定向 Maven 回归，依赖模块通过 -am 构建；事务测试仅使用明确隔离的 H2 测试数据库。另一任务拥有的固定 MySQL 集成测试仅参与 testCompile，不执行其写入，不作为本轮 PASS。
- 前端新增/修改的行为与静态合同测试、eDHR 静态套件、新组件 ESLint 及改动 Vue SFC 编译检查。
- SQL 仅做测试合同与静态检查，不执行迁移；Markdown UTF-8 与结构检查。
- 提交/推送前 branch-runtime-port-guard PASS，cleanup preview/apply PASS；最终本地及远端主干提交一致、ahead/behind 为 0/0。

## Current Status
completed

## 设计约束检查
- 本轮明确授权 Git 提交/推送，不包含部署、真实 E2E、数据库写入或共享服务重启。
- 主工作区 int_main，不建立备份分支，不修改其它任务状态或清理其它任务资产。
- 既有脏改动独立基线提交；本任务不改业务代码，若发现验证失败先记录原因并处理已授权范围。
- 遵守当前根 AGENTS.md：BDD/TDD 按需，不作为默认完成门禁；保留验证事实与证据边界。
- 命令与日志不得包含真实凭据；保持现有换行配置与 UTF-8 文档。
- 推送后出现的 docs/database-rules.md 与 docs/experience-index.md 新改动属于验证期间之后的并行经验增补，超出本次冻结白名单，不暂存、不覆盖；它们不影响本任务 98 文件提交与远端同步验收。

## Final Verification
后端 722 tests、前端定向 111 tests、21-spec 静态套件、11 个 SFC/TS 与新组件 ESLint、文档/端口/暂存检查、cleanup 均 PASS。代码已推送并确认远端同 hash、ahead/behind=0/0；真实 E2E、固定外部 MySQL 测试、迁移和部署未执行。
