from pathlib import Path
import fitz
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.pdfgen import canvas

output_dir = Path(__file__).resolve().parent
pdfmetrics.registerFont(UnicodeCIDFont("STSong-Light"))
for revision in (1, 2):
    target_path = output_dir / f"dcc_main_20261126_v{revision}.pdf"
    document = canvas.Canvas(str(target_path), pagesize=A4)
    document.setTitle(f"DCC-MAIN-20261126 revision {revision} TEST ONLY")
    document.setFont("STSong-Light", 18)
    document.drawString(45, 780, "文控主链流程测试文件（不得用于真实业务）")
    document.setFont("STSong-Light", 12)
    lines = [
        "文件标识：DCC-MAIN-20261126",
        "环境：本地 localhost:8081，测试1租户",
        f"测试内容修订：{revision}",
        "目的：验证上传、主管审核、三部门指派及会签、批准、文控审核。",
        "随后验证生效、搜索、查看、下载、检出修改和检入。",
        "本文件无真实产品参数，不构成真实质量批准或生产指令。",
        "签名仅用于本次获授权的软件流程测试身份验证。",
        "初次内容：验证首次受控生效。" if revision == 1 else "修改后内容：验证检入仅产生工作版本，不自动更新正式版本。",
        "不得用于生产、检验、注册申报或产品放行。",
    ]
    for line_index, line_text in enumerate(lines):
        document.drawString(45, 730 - line_index * 34, line_text)
    document.setFont("Helvetica", 10)
    document.drawString(45, 55, f"TEST ONLY | DCC-MAIN-20261126 | revision {revision} | 1/1")
    document.save()
    with fitz.open(target_path) as rendered_document:
        rendered_document[0].get_pixmap().save(output_dir / f"dcc_main_20261126_v{revision}.png")
    print(target_path)
