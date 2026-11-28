from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
import fitz

output_dir = Path(__file__).resolve().parent
pdf_path = output_dir / 'SIM-CASE001-1009200660-881MO085822-K202510048-来料检验报告.pdf'
pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
document = canvas.Canvas(str(pdf_path), pagesize=(595, 842))
document.setTitle('软件模拟来料检验报告 - 881MO085822')
document.setFillColorRGB(0.7, 0.05, 0.05)
document.setFont('STSong-Light', 19)
document.drawString(45, 785, '软件模拟 - 来料检验报告')
document.setFont('STSong-Light', 13)
document.drawString(45, 751, '非真实报告，不可作为真实质量依据')
document.drawString(45, 727, '模拟测试，不可用于实际质量或上市依据')
document.setFillColorRGB(0.1, 0.1, 0.1)
document.setFont('STSong-Light', 12)
lines = [
    '报告编号：SIM-CASE001-1009200660-INCOMING',
    '测试环境：localhost:8081 / 测试1',
    '测试场景：CASE-EDHR-001 正常生产至上市放行 / 本轮来料资料',
    '生产订单：881MO085822',
    '订单周期：1009200660',
    '生产批号：K202510048',
    '产品：数显球囊扩张压力泵 / INT-IDE-243',
    '本轮订单数量：72件（非实际抽样或实测数量）',
    '批次执行：900000001359；PQC申请：350',
    '资料类别：来料检报告；正式待办任务：3131',
    '编制日期：2026-11-23（北京时间）',
    '用途：仅验证软件中来料资料的上传、归属、保存与任务流转。',
    '检验数据：本文件未实施来料检验，不记录或虚构实测值。',
    '质量结论：不作真实合格判定，不替代受控真实来料检验报告。',
    '已知差异：旧检验详情与最新过程检验汇总存在差异，保留原记录。',
    '生产超量、参数异常及检测用报废损耗均不在本次资料任务中修改。',
    '本文件无真实检验机构结论、报告章或真人签章。',
    '后续上市放行应由相应责任岗按正式任务及本轮模拟授权办理。',
]
for line_number, line in enumerate(lines):
    document.drawString(45, 682 - line_number * 27, line)
document.setFont('STSong-Light', 10)
document.drawString(45, 105, '软件模拟专用附件 | 订单881MO085822 / 批K202510048 | 1 / 1')
document.save()
rendered = fitz.open(pdf_path)
rendered[0].get_pixmap(matrix=fitz.Matrix(1.5, 1.5)).save(output_dir / '085822-来料模拟报告预览.png')
print(str(pdf_path))
print('PDF pages:', len(rendered))
