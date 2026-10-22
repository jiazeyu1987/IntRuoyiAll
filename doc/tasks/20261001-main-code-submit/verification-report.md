# 验证报告

## Scope
int_main 既有变更的定向回归、Git 提交推送和本任务记录清理；不包含真实 E2E 或部署。

## Results
- PASS: 初始 int_main / origin/int_main 为 0 ahead、0 behind。
- PASS: 分支运行端口门禁，int_main/int_main 8081/48081。
- PASS: 前端 9 个变更测试文件 56 tests / 0 failures / 0 skipped。
- PASS: Python 迁移依赖闭包测试 1 passed；后端重置静态合同 PASS。
- PASS: 冻结的 54 文件 SHA-256 和 staged 范围一致，无未解决冲突，git diff --cached --check PASS；新增行凭据检查未命中。
- PASS: 经验合并至既有 docs/powershell-encoding.md，UTF-8、标题和 Git 空白核验通过。
- PASS: 54 文件基线提交 5264c8e48469263098a5cdb7f6485e5807feac5a 已推送 origin/int_main。
- PASS: 经验实现提交 4fb95c4b22b1f6dbbac3dc5943dea4ec298a958d 仅包含 docs/powershell-encoding.md。
- PASS: cleanup preview/apply，keep=3、delete=2、blocked/warnings=0，仅删除当前任务临时清单和原始日志。
- PASS: 代码基线和经验提交均已推送，推送后工作区干净且无 ahead/behind。

- PASS: Maven BUILD SUCCESS; 14 fresh Surefire XML reports, 344 tests / 0 failures / 0 errors / 0 skipped. Summary runner had a log-decoding error; actual Maven result confirmed independently without rerunning or changing tests.

## Final Verification

全部代码及文档定向门禁 PASS；真实 E2E 和部署不在本轮范围。最终收尾提交仅含三份核心记录，推送后再次核对工作区干净、ahead/behind=0/0、本地 HEAD 与 origin/int_main 和 GitHub refs/heads/int_main 一致；如失败须记录实际 blocker，不能将推送写成 PASS。
