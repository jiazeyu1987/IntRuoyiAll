# DCC 文件双版本上传与下载权限需求

## 1. 目标

DCC 普通文件上传支持两个独立版本：

| 版本 | 是否必须 | 用途 |
| --- | --- | --- |
| 不可编辑版本 | 必须 | 在线浏览的唯一文件，格式为 PDF |
| 可编辑版本 | 非必须 | 授权用户下载后编辑，保留原始文件格式 |

系统不负责把可编辑文件自动转换成 PDF，也不允许因缺少某一版本而回退到另一版本。

## 2. 上传合同

提交请求使用两个独立 ticket：

- `readOnlyUploadTicket`：必填，上传 purpose 为 `READ_ONLY_VIEW`，必须是真实 PDF。
- `editableUploadTicket`：选填，上传 purpose 为 `EDITABLE_SOURCE`，允许 doc/docx/xls/xlsx/dwg/sldprt/sldasm/slddrw 等可编辑源文件。

存储后分别写入：

- `readOnlyFileId`：不可编辑浏览文件。
- `editableFileId`：可编辑下载文件，可为空。

缺少 `readOnlyUploadTicket` 时，提交直接失败；可编辑 ticket 缺失不影响提交。

## 3. 在线浏览

普通受控文件浏览、OnlyOffice 只读入口和预览元数据统一解析 `readOnlyFileId`。

在线浏览只检查 `dcc:controlled-file:preview` / `dcc:controlled-file:query` 对应的浏览能力，不因为用户拥有下载权限而自动放开浏览，也不因为用户拥有浏览权限而放开下载。

## 4. 下载权限

下载权限必须拆成两个权限码：

- `dcc:controlled-file:download-read-only`：下载不可编辑 PDF。
- `dcc:controlled-file:download-editable`：下载可编辑源文件。

接口：

- `GET /dcc/controlled-files/{id}/download/read-only`
- `GET /dcc/controlled-files/{id}/download/editable`

两个接口都必须保留现有受控下载留痕、权限校验、文件存在校验和失败状态记录。缺少对应权限时返回无权限，不得降级到另一版本。

## 5. 验收标准

1. 只上传可编辑文件，不能提交。
2. 只上传不可编辑 PDF，可以提交并在线浏览。
3. 两个版本都上传后，在线浏览读取不可编辑 PDF。
4. 只有不可编辑下载权限的用户不能下载可编辑版本。
5. 只有可编辑下载权限的用户不能下载不可编辑版本。
6. 只有在线浏览权限的用户可以浏览，但两个下载接口都被拒绝。
