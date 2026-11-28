from pathlib import Path
import sys

import fitz
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer


order_number, product_code, batch_number, task_number = sys.argv[1:]
folder = Path(__file__).resolve().parent
output_file = folder / f'SIM-{product_code}-{order_number}-STERILIZATION-round2.pdf'
pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
title_style = ParagraphStyle('Title', fontName='STSong-Light', fontSize=20, leading=30, textColor=colors.HexColor('#A02020'))
body_style = ParagraphStyle('Body', fontName='STSong-Light', fontSize=12, leading=22, spaceAfter=12)
document = SimpleDocTemplate(str(output_file), pagesize=(595, 842), rightMargin=48, leftMargin=48, topMargin=48, bottomMargin=48)
paragraphs = [
    Paragraph('灭菌报告 - 软件模拟附件', title_style),
    Spacer(1, 20),
    Paragraph('软件模拟，非真实实测／质量批准', title_style),
    Spacer(1, 24),
    Paragraph(f'生产订单：{order_number}<br/>产品编码：{product_code}<br/>生产批号：{batch_number}<br/>正式资料任务：{task_number}<br/>资料类别：灭菌报告<br/>模拟轮次：20261121B 第二轮', body_style),
    Paragraph('用途：仅验证获派灭菌资料任务的附件上传、保存、关联及展示流程。此文件不证明产品实施灭菌，不含真实灭菌参数、检测数据、合格判定、报告签章或真实质量批准。', body_style),
    Paragraph('结论：未作真实灭菌有效性或产品合格判定；不得作为真实生产、销售、临床使用或质量放行依据。', body_style),
    Paragraph('原始生产及检验记录保持不变；资料上传不替代异常处置、责任人审批、上市放行或归档。', body_style),
]
document.build(paragraphs)
pdf_document = fitz.open(output_file)
assert len(pdf_document) == 1
assert order_number in pdf_document[0].get_text()
pdf_document[0].get_pixmap(matrix=fitz.Matrix(1.2, 1.2)).save(str(output_file.with_suffix('.png')))
print(str(output_file))
print(f'PDF pages: {len(pdf_document)}; bytes: {output_file.stat().st_size}')
