from pathlib import Path
import sys
from datetime import datetime
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
import fitz

root = Path(__file__).resolve().parent
pdfmetrics.registerFont(UnicodeCIDFont("STSong-Light"))
order, active, batch, execution, quantity = sys.argv[1:6]
for category in ("成品检验报告", "成品检验记录"):
    output = root / f"SIM-R2-{active}-{category}-软件模拟.pdf"
    if output.exists():
        raise FileExistsError(output)
    document = canvas.Canvas(str(output), pagesize=(595, 842))
    document.setTitle(f"第二轮软件模拟 - {order} - {category}")
    document.setFont("STSong-Light", 18)
    document.setFillColorRGB(0.75, 0.05, 0.05)
    document.drawString(42, 790, "模拟测试附件 - 软件模拟")
    document.setFont("STSong-Light", 13)
    document.drawString(42, 760, f"非真实{category}，不可作为真实质量依据")
    document.setFillColorRGB(0, 0, 0)
    document.setFont("STSong-Light", 14)
    document.drawString(42, 720, f"第二轮：{category}上传流程测试")
    lines = [f"生产订单：{order}", f"活跃实例：{active}", f"生产批号：{batch}", f"批次执行：{execution}", f"计划生产数量：{quantity}（仅对象识别，不代表实测合格数）", "资料责任账号：qianfengqin（钱凤琴）", "测试范围：仅验证成品资料分类、上传保存和任务流转。", "来源：本轮获授权的软件模拟及当前订单页面身份信息。", "未填写真实实测值、检验合格结论、机构证明、印章或真人签名。", "PQC生产放行状态不等于本文件作出质量批准或上市放行。", "本文件不替代正式检验原始数据、责任决定及真实质量依据。"]
    if active == "1009200653":
        lines += ["原交接保留：组装I首检检验损耗1，整体不合格。", "保留原值，不改判合格，不补造处置批准。"]
    else:
        lines += ["交接注明本轮检验损耗0；该信息不构成实测或合格结论。"]
    lines += [f"生成时间（本机北京时间）：{datetime.now():%Y-%m-%d %H:%M:%S}", "非真实实测／质量批准；禁止用于真实产品质量判定。"]
    vertical = 685
    document.setFont("STSong-Light", 11)
    for line in lines:
        document.drawString(42, vertical, line)
        vertical -= 28
    document.setFont("STSong-Light", 10)
    document.drawString(42, 48, "软件模拟专用 | 本轮独立生成 | 第1页／共1页")
    document.save()
    with fitz.open(output) as rendered:
        assert len(rendered) == 1
        text = rendered[0].get_text()
        assert order in text and active in text and "软件模拟" in text
        rendered[0].get_pixmap(matrix=fitz.Matrix(1, 1)).save(output.with_suffix(".png"))
    print(output.name)
