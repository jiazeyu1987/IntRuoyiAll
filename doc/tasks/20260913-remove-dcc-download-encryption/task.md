# Remove DCC Download Encryption Requirement

## Task Goal

按用户要求去除 DCC 下载加密限制：不再要求 `DCC_DOWNLOAD_ENCRYPTION_CURRENT_KEY_VERSION` 和 `DCC_DOWNLOAD_ENCRYPTION_KEYRING`，移除下载链路中的加密包装逻辑，授权通过后直接返回原文件下载内容。

## Milestones

- [x] 读取项目后端规则、收尾规则、PowerShell 编码规则和后端交付技能。
- [x] 定位 DCC 下载加密配置、服务、控制器响应头、运行脚本和测试覆盖。
- [x] 移除下载加密代码、配置绑定和环境变量要求。
- [x] 更新/补充 BDD 与严格 TDD 证据。
- [x] 运行定向验证并记录结果。
- [x] 修复相邻 DCC 编译/权限顺序阻塞并重跑定向验证。
- [x] 按收尾规则完成 cleanup preview/apply。

## Expected Verification

- `rg -n "DCC_DOWNLOAD_ENCRYPTION|dcc\\.download\\.encryption|DccAesGcmDownloadEncryptionGateway|DccDownloadEncryptionProperties|DccDownloadEncryptionGateway|X-DCC-Encryption" IntRuoyiBackend scripts`
- `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest,DccControlledFilePreviewDownloadApiTest,DccProtectionSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `powershell -ExecutionPolicy Bypass -File .\IntRuoyiBackend\script\tests\test_dcc_download_encryption_runtime_config.ps1`
- `node .\scripts\tests\start-branch-backend-dcc-encryption-static.spec.cjs`
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_controlled_file_protection_sql.py`
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_restart_int_ruoyi_local_schema.py -k "dcc_download or encryption"`
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_runtime_control_scripts.py -k "dcc_download or local_restart_backend_does_not_pass"`
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_publish_int_ruoyi_to_test_tooling.py::<download-runtime-and-package-tests>`
- `node --check .\IntRuoyiFronted\tests\e2e\dcc-controlled-file-protection.e2e.js`

## Current Status

completed

下载加密移除实现、相邻编译阻塞修正、DCC 定向 Maven、脚本级静态合同、SQL 合同、backend evidence validator 和 cleanup apply 均已通过。实现和收尾记录已提交并融合进 `int_main`，远端 `origin/int_main` 已包含本次提交。

## Git Evidence

- 实现提交：`bc640fd96d407d284aab7ab2501215f85ac5bf14`
- 收尾记录提交：`43071eb7e530cb36d0dfd6d673660d12128b5426`
- 推送验证：`git fetch origin int_main` 后 `HEAD -> int_main, origin/int_main, origin/HEAD` 指向收尾记录提交。

## Design Constraints Check

- 下载授权、租户、业务文件归属和审计链路保持不变，只移除下载包加密与密钥环境变量依赖。
- 不添加 fallback、默认成功、mock 下载或兼容分支；缺失正式文件内容仍按既有 fail-fast 逻辑失败。
- 预览权限拒绝路径先完成正式权限判断，再解析二进制文件元数据，避免越权用户触发文件读取。
- Git 提交/推送仅按用户当轮“提交并融合进 int_main”授权执行；不执行远程服务器、数据库写入或 E2E。
