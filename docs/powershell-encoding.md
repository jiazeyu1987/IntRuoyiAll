# IntRuoyi PowerShell And Encoding Rules

## 触发场景

- PowerShell 命令涉及中文文本、Markdown、JSON、SQL、CSV、日志、here-string、SSH/MySQL stdin 或多行脚本时，必须先读取本文件。
- 读写中文文件、任务文档、规则文档或生成文档时，必须按本文件执行。

## PowerShell 命令规则

- PowerShell 命令不得使用 `&&`。
- 需要串联命令时使用分行或分号。
- 命令失败必须按真实失败处理，不得静默忽略退出码。
- 执行高风险命令前必须明确工作目录、目标路径和退出码检查方式。

## UTF-8 读写规则

- 中文文本默认使用 UTF-8。
- 读取中文文件使用 `Get-Content -Encoding utf8`、`python -X utf8`、Node UTF-8 API 或 `rg`。
- 写中文文件优先使用 `apply_patch` 或显式 UTF-8 API。
- 不得用默认 `Set-Content`、`Add-Content`、`Out-File`、`>`、`>>` 写中文文本。
- 不得用 `cmd /c echo`、`type`、`more` 或未声明编码的批处理方式生成中文文件。

## 禁止做法

- 禁止发现乱码后静默重写文件。
- 禁止把 UTF-8 no-BOM 文件误转成 ANSI、GBK、UTF-16LE 或混合编码。
- 禁止在命令日志中记录密码、token、私钥、连接串密钥或其他凭据。

## 验证方式

- 写入后用 `python -X utf8` 或等效 UTF-8 方式重新读取。
- 对关键文档记录 UTF-8 校验结果。
- 多行脚本记录退出码和关键输出，不记录敏感明文。
