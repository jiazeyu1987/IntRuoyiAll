from pathlib import Path
from uuid import uuid4

import fitz
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer


output = Path(__file__).parent / "output" / "pdf"
output.mkdir(parents=True, exist_ok=True)
destination = output / "SIM-1009200593-2984-sterilization-test.pdf"
pdfmetrics.registerFont(UnicodeCIDFont("STSong-Light"))
style = ParagraphStyle("body", fontName="STSong-Light", fontSize=12, leading=19, spaceAfter=10)
warning = ParagraphStyle("warning", parent=style, textColor=colors.darkred, fontSize=16, leading=24)
content = [
    Paragraph("模拟灭菌资料 / SIMULATION ONLY", warning),
    Paragraph("模拟测试附件，非真实报告，不可用于实际质量/上市依据。", warning),
    Spacer(1, 14),
    Paragraph("环境：本地 localhost:8081 / 测试1", style),
    Paragraph("本轮活跃订单：1009200593；正式资料任务：2984", style),
    Paragraph("批次执行：900000001329；类别：灭菌文件（灭菌报告）", style),
    Paragraph("产品：AI30/5000；生产指令：881XSDD80927", style),
    Paragraph("工单/批号：", style),
    Paragraph("SIM-COPY-CODX-PQC-20260807-SP-WO-05-<br/>OPYAO451788352161891", style),
    Paragraph(f"随机测试标识：{uuid4()}", style),
    Paragraph("用途：仅验证本轮灭菌资料上传、保存、关联及待办状态。", style),
    Paragraph("本文不包含真实灭菌参数、检测值、实测结论、机构签章或人员签字。", style),
    Paragraph("不证明灭菌有效，不代替质量审批、生产放行或上市放行。", style),
]
SimpleDocTemplate(str(destination), pagesize=(595.28, 841.89), rightMargin=45, leftMargin=45, topMargin=45, bottomMargin=45).build(content)
reader = PdfReader(str(destination))
assert len(reader.pages) == 1
assert "1009200593" in reader.pages[0].extract_text()
document = fitz.open(destination)
preview = output / "SIM-1009200593-2984-sterilization-test-preview.png"
document[0].get_pixmap(matrix=fitz.Matrix(1.5, 1.5)).save(preview)
print(f"PDF: {destination.resolve()}")
print(f"Preview: {preview.resolve()}")
print(f"Pages: {len(reader.pages)}; Bytes: {destination.stat().st_size}")
