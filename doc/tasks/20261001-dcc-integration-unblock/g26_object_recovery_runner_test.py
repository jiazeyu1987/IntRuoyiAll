import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec=importlib.util.spec_from_file_location('g26runner',Path(__file__).with_name('g26-object-recovery-runner.py'))
runner=importlib.util.module_from_spec(spec);spec.loader.exec_module(runner)

class RecoveryAuthorizationBoundary(unittest.TestCase):
    def test_absent_actual_approval_never_opens_database_or_object_client(self):
        with tempfile.TemporaryDirectory() as name:
            directory=Path(name)
            with patch.object(runner,'BACKUPS',directory),patch.object(runner,'verify_delivery',return_value={}),patch.object(runner.subprocess,'run',side_effect=AssertionError('No actual operation permitted')),patch('sys.argv',['test','--mode','recover','--output-directory',str(directory/'new')]):
                with self.assertRaisesRegex(ValueError,'authorization receipt'):runner.main()
                self.assertFalse((directory/'new').exists())

    def test_wrong_specific_scope_is_refused_before_database(self):
        with tempfile.TemporaryDirectory() as name:
            directory=Path(name);auth=directory/'g26-object-recovery-authorization.json'
            auth.write_text(json.dumps({'actualUserApproval':True,'maximumUniqueObjectPuts':4,'sourceFileIds':list(runner.IDS),'sourceMessageReference':'actual-reply'}))
            with patch.object(runner,'MAIN',directory),patch.object(runner,'BACKUPS',directory),patch.object(runner,'verify_delivery',return_value={}),patch.object(runner.subprocess,'run',side_effect=AssertionError('No actual operation permitted')),patch('sys.argv',['test','--mode','recover','--output-directory',str(directory/'new'),'--authorization',str(auth)]):
                with self.assertRaisesRegex(ValueError,'Specific actual object approval'):runner.main()

    def test_no_output_overwrite_even_with_real_scope(self):
        with tempfile.TemporaryDirectory() as name:
            with patch('sys.argv',['test','--mode','prepare','--output-directory',name]),patch.object(runner.subprocess,'run',side_effect=AssertionError('No operation permitted')):
                with self.assertRaisesRegex(ValueError,'New exact protected child'):runner.main()

if __name__=='__main__':unittest.main()
