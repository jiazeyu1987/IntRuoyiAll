from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
import fitz

output_dir = Path(__file__).resolve().parent
pdf_path = output_dir / 'SIM-1009200665-881MO082554-STERILIZATION.pdf'
pdfmetrics.registerFont(UnicodeCIDFont('STSong-Light'))
document = canvas.Canvas(str(pdf_path), pagesize=(595, 842))
document.setTitle('模拟灭菌报告 - 881MO082554 - 1009200665')
document.setFont('STSong-Light', 20)
document.drawString(50, 780, '模拟灭菌报告（软件测试专用）')
document.setFont('STSong-Light', 12)
report_lines = [
    '资料类别：灭菌报告',
    '场景：CASE-EDHR-001，2026-11-23 晚间本轮模拟测试',
    '环境：本地 localhost:8081 / 测试1',
    '生产订单：881MO082554',
    '活跃周期：1009200665',
    '产品：球囊扩张压力泵；型号：KDL-IN30',
    '生产批号：K202507136；本单生产数量：18019件',
    '批次执行：900000001360',
    '本人灭菌资料正式任务：3139；责任账号：dingjiamei（丁加美）',
    '用途：验证本人正式任务的灭菌附件上传、保存及刷新查回。',
    '模拟结果：仅模拟资料流程，不提供真实灭菌参数或质量结论。',
    '本文件不证明已实施灭菌，不替代真实检验或灭菌记录。',
    '无真实签章、无真人亲签，不作为真实产品上市放行依据。',
    '不修改本轮数量超限、参数异常和损耗记录。',
]
for line_number, report_line in enumerate(report_lines):
    document.drawString(50, 730 - line_number * 32, report_line)
document.save()
pdf_document = fitz.open(pdf_path)
pdf_document[0].get_pixmap(matrix=fitz.Matrix(1.3, 1.3)).save(output_dir / '模拟灭菌报告预览.png')
print(f'有效PDF页数：{len(pdf_document)}；文件：{pdf_path.name}')
