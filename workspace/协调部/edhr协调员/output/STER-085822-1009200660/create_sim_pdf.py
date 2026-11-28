from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.cidfonts import UnicodeCIDFont
import fitz
from pypdf import PdfReader

folder = Path(__file__).resolve().parent
pdf_path = folder / "SIM-1009200660-881MO085822-STERILIZATION.pdf"
pdfmetrics.registerFont(UnicodeCIDFont("STSong-Light"))
document = canvas.Canvas(str(pdf_path), pagesize=(595, 842))
document.setTitle("软件模拟灭菌报告附件 - 881MO085822")
document.setFillColorRGB(0.72, 0.1, 0.1)
document.setFont("STSong-Light", 22)
document.drawString(48, 776, "软件模拟 - 非真实灭菌报告")
document.setFont("STSong-Light", 15)
document.drawString(48, 740, "不可作为真实质量依据")
document.setFillColorRGB(0.08, 0.08, 0.08)
document.setFont("STSong-Light", 13)
rows = ["用途：eDHR CASE001 正常单软件流程测试", "资料类别：灭菌报告（模拟附件）", "环境：本地 localhost:8081 / 测试1", "生产订单：881MO085822", "活跃订单周期：1009200660", "产品批号：K202510048", "批次数量：72件", "批次执行：900000001359", "关联PQC生产放行申请：350", "资料责任账号：dingjiamei（丁加美）", "本轮标识：2026-11-23 / CASE001 / 灭菌资料上传测试", "本文件仅用于验证附件上传、类别归属和任务完成状态。", "未实施真实灭菌，未提供实测数据、灭菌参数或质量结论。", "不含真实机构报告、真实签章或真人亲签。", "本附件不代表上市放行或归档批准。"]
for row_index, row_text in enumerate(rows):
    document.drawString(48, 686 - row_index * 32, row_text)
document.setFont("STSong-Light", 11)
document.setFillColorRGB(0.72, 0.1, 0.1)
document.drawString(48, 92, "软件模拟，非真实灭菌报告，不可作为真实质量依据")
document.drawString(48, 64, "仅本轮指定订单使用 | 1 / 1")
document.save()
rendered = fitz.open(pdf_path)
rendered[0].get_pixmap(matrix=fitz.Matrix(1.5, 1.5)).save(folder / "preview.png")
reader = PdfReader(pdf_path)
print({"path": str(pdf_path), "pages": len(reader.pages), "bytes": pdf_path.stat().st_size})
print(reader.pages[0].extract_text())
