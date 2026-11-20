import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
spec=importlib.util.spec_from_file_location('package',Path(__file__).resolve().parents[1]/'runtime/backend-package.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class ProvenanceTest(unittest.TestCase):
 def test_success_exit_without_new_package_cannot_certify_old_package(self):
  with tempfile.TemporaryDirectory() as tmp:
   root=Path(tmp); (root/'pom.xml').write_text('<project/>')
   jar=root/'yudao-server/target/yudao-server-exec.jar';jar.parent.mkdir(parents=True);jar.write_bytes(b'old package')
   receipt=jar.with_suffix('.provenance.json');receipt.write_text('{}')
   def interrupted(*args,**kwargs):
    self.assertFalse(jar.exists());self.assertFalse(receipt.exists())
   with patch('sys.argv',['backend-package.py','build','--root',str(root)]),patch.object(m.subprocess,'run',side_effect=interrupted):
    with self.assertRaisesRegex(RuntimeError,'did not produce a new'):m.main()
   self.assertFalse(receipt.exists())
 def test_only_newly_produced_package_is_certified(self):
  with tempfile.TemporaryDirectory() as tmp:
   root=Path(tmp); (root/'pom.xml').write_text('<project/>')
   jar=root/'yudao-server/target/yudao-server-exec.jar';jar.parent.mkdir(parents=True);jar.write_bytes(b'old package')
   receipt=jar.with_suffix('.provenance.json');receipt.write_text('{}')
   def build(*args,**kwargs):
    self.assertFalse(jar.exists());self.assertFalse(receipt.exists());jar.write_bytes(b'new package')
   with patch('sys.argv',['backend-package.py','build','--root',str(root)]),patch.object(m.subprocess,'run',side_effect=build):m.main()
   self.assertEqual(json.loads(receipt.read_text())['jarSha256'],m.digest(jar));m.verify(root,jar)
 def test_mutated_added_deleted_source_and_replaced_jar_are_rejected(self):
  with tempfile.TemporaryDirectory() as tmp:
   root=Path(tmp); (root/'pom.xml').write_text('<project/>'); source=root/'src/main/A.java';source.parent.mkdir(parents=True);source.write_text('original')
   jar=root/'app.jar';jar.write_bytes(b'package')
   receipt=jar.with_suffix('.provenance.json')
   with self.assertRaisesRegex(RuntimeError,'Missing'):m.verify(root,jar)
   receipt.write_text(json.dumps(dict(version=1,sourceSha256=m.fingerprint(root),jarSha256=m.digest(jar))))
   m.verify(root,jar)
   source.write_text('changed')
   with self.assertRaisesRegex(RuntimeError,'source differs'):m.verify(root,jar)
   source.write_text('original');extra=source.with_name('B.java');extra.write_text('added')
   with self.assertRaisesRegex(RuntimeError,'source differs'):m.verify(root,jar)
   extra.unlink();source.unlink()
   with self.assertRaises(RuntimeError):m.verify(root,jar)
   source.write_text('original');jar.write_bytes(b'other package')
   with self.assertRaisesRegex(RuntimeError,'package differs'):m.verify(root,jar)
if __name__=='__main__':unittest.main()
