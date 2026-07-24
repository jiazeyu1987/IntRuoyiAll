# Execution Log：DCC 批量识别项目代码基础数据匹配失败修复

BDD: DCC 项目代码识别结果规范化后匹配基础数据 -> Given Codex/内容识别返回的项目代码文本与基础数据编码只存在大小写、空格、下划线、连字符或全角符号差异 / When 批量识别验证候选结果 / Then 应匹配唯一启用的 DCC 项目代码并写入识别账本。
BDD: 仍然拒绝非唯一或不存在候选 -> Given 识别结果规范化后没有唯一启用项目代码或项目名称 / When 执行识别 / Then 必须 fail fast 并记录失败账本，不得写入错误关联。

INFO: 已读取 PowerShell 经验、项目经验索引、bug-regression-fix-loop 与 bug-contract。
INFO: 截图症状为批量识别最后错误 `DCC project-code recognition returned no DCC basic-data match`。

RED: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_codexProjectCodeMatchIgnoresCommonSeparators" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected reason: validateProjectCodeMatch required exact projectCode == matchText, so `RE-STM-MM-017-04` could not match recognized `re_stm mm 017 04`.
FIX: validateProjectCodeMatch now compares project code candidates and recognized match text using a normalized alphanumeric uppercase key, including full-width ASCII letters/digits, while still requiring exactly one enabled candidate for the normalized key.
GREEN: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#recognizeProjectCode_codexProjectCodeMatchIgnoresCommonSeparators" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 1 test.
GREEN: mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 27 tests.
