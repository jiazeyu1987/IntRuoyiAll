from pathlib import Path
import hashlib
import json
import fitz
from pypdf import PdfReader
from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph

root = Path(__file__).resolve().parent
file = root / "SIM-1009200625-来料检验报告-模拟测试.pdf"
pdfmetrics.registerFont(TTFont("Chinese", "C:/Windows/Fonts/simsun.ttc", subfontIndex=0))
body = ParagraphStyle("Body", fontName="Chinese", fontSize=11, leading=18, spaceAfter=12)
title = ParagraphStyle("Title", parent=body, fontSize=20, leading=28, alignment=1)
warning = ParagraphStyle("Warning", parent=body, fontSize=13, leading=22, alignment=1, textColor=colors.HexColor("#B42318"))
contents = [
    ("来料检验报告（模拟测试）", title),
    ("模拟测试、非真实报告记录<br/>不可作为真实质量或上市依据", warning),
    ("文件编号：SIM-INCOMING-1009200625；类别：来料检验报告。<br/>环境：本地8081 / 测试1；本轮周期：1009200625。", body),
    ("工单：SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891<br/>关联工单数量：5000件（非抽样数、非检验合格数量）。", body),
    ("一、适用范围<br/>本文件仅为上述工单、本轮周期的来料报告附件流程测试而编制，不复用旧周期检验结论。未提供真实来料批号、供应商、材料规格、检验标准、抽样方案或实测数据；不填造上述内容，不作来料合格判定。", body),
    ("二、检验与结论声明<br/>未开展真实来料检验，无真实实测值、机构结论、报告签章或责任人签名。本附件不证明检验完成，不替代真实报告、质量处置、系统复核或批准。", body),
    ("三、已知问题不予掩盖<br/>协调交接提示本轮存在生产超限5501、参数0/1001及501待分配。本附件不将其解释为合格、不消除或掩盖相关问题；核验及处置由原业务链路完成。以上为交接信息，非本文件独立核实结论。", body),
    ("四、流程限制<br/>准备时交接状态：PQC检验已提交，尚待独立复核；PQC生产放行未发生。本文件仅离线准备，未上传、未修改业务记录、未办理批准。须待协调确认PQC生产放行正式成立，再另行办理上传。", body),
    ("五、时间与签署声明<br/>不声明现实检验时间，不由工单名称推断检验日期；无真实检验、审核、批准日期或签署。本附件不代表真人亲签、真实实测或真实质量批准。", body),
]
SimpleDocTemplate(str(file), pagesize=(595.28, 841.89), leftMargin=45, rightMargin=45, topMargin=35, bottomMargin=35, title="1009200625 来料报告 - 模拟测试").build([Paragraph(text, style) for text, style in contents])
reader = PdfReader(file, strict=True)
text = "".join(page.extract_text() for page in reader.pages)
required = ["1009200625", "5000", "模拟测试", "非真实报告记录", "不可作为真实质量", "5501", "0/1001", "501", "OPYAO451788352161891"]
assert all(value in text for value in required), "Required text missing"
assert len(reader.pages) == 1, "Expected one page"
rendered = fitz.open(file)
rendered[0].get_pixmap(matrix=fitz.Matrix(1.6, 1.6)).save(root / "SIM-1009200625-来料检验报告-版面核验.png")
result = {"file": str(file), "category": "来料检验报告", "cycle": "1009200625", "pages": len(reader.pages), "strict_pdf_parse": "PASS", "required_text": "PASS", "render": "PASS", "bytes": file.stat().st_size, "sha256": hashlib.sha256(file.read_bytes()).hexdigest(), "business_actions": "仅离线生成，未上传、未修改业务记录、未批准"}
(root / "SIM-1009200625-来料检验报告-格式检查.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(result, ensure_ascii=False, indent=2))

