# 默认签名楷体字体

- 字体：LXGW WenKai Regular（霞鹜文楷，楷书），v1.530。
- 来源：https://github.com/lxgw/LxgwWenKai/releases/tag/v1.530
- 文件：https://github.com/lxgw/LxgwWenKai/releases/download/v1.530/LXGWWenKai-Regular.ttf
- SHA-256：`2d73eea94ad808999d90c0000c239a84a33b09d4bc6ab6c5b8bbf1eda8a7fc8b`
- 授权：SIL Open Font License 1.1，保留同目录 `OFL.txt` 中的版权及许可证。

程序从 classpath 加载该固定字体，将正式用户姓名渲染为透明背景黑色 PNG，不依赖操作系统字体或 AI 服务。首次进入电子签名的“我的签名”或实际签署取图时初始化并启用；只有完全没有图片历史的用户才生成。已有上传、停用或被替换的历史均不重新生成，不覆盖已有图片，也不修改历史签署快照。

实际签署入口在身份锁和图片历史锁内检查并保存，重复请求复用已保存版本；图片生成或存储失败时签署失败。签署预检只核验无图片用户的姓名能否生成，不创建图片；历史证据读取继续只读，不为旧记录补造图片。
