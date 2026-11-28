from pathlib import Path
import fitz
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
from reportlab.pdfgen import canvas

output_dir = Path(__file__).resolve().parent
pdf_path = output_dir / 'SIM-CASE001-1009200665-881MO082554-来料检验报告-模拟测试.pdf'
pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
document = canvas.Canvas(str(pdf_path), pagesize=A4)
document.setTitle('来料检验报告 - 模拟测试 - 881MO082554')
document.setFont('STSong-Light', 20)
document.drawString(48, 782, '来料检验报告（模拟测试）')
document.setFillColorRGB(0.8, 0, 0)
document.setFont('STSong-Light', 12)
document.drawString(48, 752, '模拟测试、非真实报告、不可用于实际质量或上市依据')
document.setFillColorRGB(0, 0, 0)
lines = [
    '测试用途：CASE001 来料资料上传、保存及查回验证',
    '报告编号：SIM-CASE001-1009200665-INCOMING',
    '生成日期：2026-11-23（北京时间）',
    '测试环境：本地 localhost:8081 / 测试1',
    '生产订单：881MO082554',
    '活跃周期：1009200665',
    '生产批号：K202507136',
    '产品名称：球囊扩张压力泵',
    '型号规格：KDL-IN30',
    '生产数量：18019件（订单信息，不代表检验样本数）',
    '批次执行：900000001360',
    '来料资料正式任务：3138',
    '资料类别：来料检报告',
    '模拟结论：仅供软件资料流程测试，不构成真实来料合格判定。',
    '本文件未实施真实取样、检验或测量，无真实检验数据。',
    '本文件不包含真实签章，不代表检验员或批准人亲签。',
    '上传本文件不替代生产、检验、PQC批准或上市放行。',
]
for index, line in enumerate(lines):
    document.drawString(48, 710 - index * 29, line)
document.setFont('STSong-Light', 10)
document.drawString(48, 76, '模拟测试附件 / 仅供指定本轮测试对象使用 / 第1页，共1页')
document.save()
with fitz.open(pdf_path) as verification:
    assert len(verification) == 1
    text = verification[0].get_text()
    assert '881MO082554' in text and '1009200665' in text
    verification[0].get_pixmap(matrix=fitz.Matrix(1.3, 1.3)).save(output_dir / '模拟来料报告预览.png')
print(pdf_path)
print(f'Valid PDF: {pdf_path.stat().st_size} bytes, 1 page')
