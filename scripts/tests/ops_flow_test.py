"""Test backup/restore control flow with a fake Docker CLI; no real DB or engine."""
from pathlib import Path
import os, subprocess, tempfile

with tempfile.TemporaryDirectory() as directory:
    root = Path(directory)
    (root / 'scripts').mkdir()
    source = Path(__file__).resolve().parents[1]
    for name in ('compose-common.sh', 'backup.sh', 'restore.sh'):
        (root / 'scripts' / name).write_text((source / name).read_text())
    (root / '.env').write_text('DB_PASSWORD=test\nMYSQL_ROOT_PASSWORD=test-root\n')
    (root / 'compose.yaml').write_text('services: {}\n')
    (root / 'bin').mkdir()
    stub = root / 'bin' / 'docker'
    stub.write_text('''#!/usr/bin/env python3
import os,sys,tarfile,io
from pathlib import Path
args=sys.argv[1:]
with open(os.environ['OPS_LOG'],'a') as log:log.write(' '.join(args)+'\\n')
if 'ps' in args:print('app')
if 'exec' in args:
 if os.environ.get('FAIL_DUMP')=='1':sys.exit(1)
 if 'mysqldump' in args[-1]:print('CREATE TABLE test(id INT);')
if 'run' in args and 'czf' in args:
 path=args[args.index('-v')+1].split(':')[0]
 with tarfile.open(Path(path)/'uploads.tgz','w:gz') as archive:
  entry=tarfile.TarInfo('uploads/');entry.type=tarfile.DIRTYPE;archive.addfile(entry)
''')
    stub.chmod(0o755)
    log = root / 'operations.log'
    env = {**os.environ, 'PATH':str(root/'bin')+':'+os.environ['PATH'], 'OPS_LOG':str(log)}
    def run(script,*args,extra=None):
        return subprocess.run(['bash',str(root/'scripts'/script),*map(str,args)],env={**env,**(extra or {})},capture_output=True,text=True)
    backup=root/'backup'
    result=run('backup.sh',backup)
    assert result.returncode==0,result.stderr
    assert (backup/'SHA256SUMS').exists()
    assert 'start app' in log.read_text()
    log.write_text('')
    result=run('backup.sh',root/'failed',extra={'FAIL_DUMP':'1'})
    assert result.returncode!=0
    assert 'start app' in log.read_text(), 'backup failure must resume paused app'
    assert not (root/'failed'/'SHA256SUMS').exists()
    log.write_text('')
    assert run('restore.sh',backup).returncode==2, 'restore requires explicit replacement flag'
    assert 'stop app' not in log.read_text()
    result=run('restore.sh',backup,'--confirm-replace')
    assert result.returncode==0,result.stderr
    log.write_text('')
    result=run('restore.sh',backup,'--confirm-replace',extra={'FAIL_DUMP':'1'})
    assert result.returncode!=0
    assert 'stop app' in log.read_text() and 'start app' not in log.read_text(), 'partial restore must stay offline'
    log.write_text('')
    (backup/'database.sql').write_text('corrupted')
    assert run('restore.sh',backup,'--confirm-replace').returncode!=0
    assert 'stop app' not in log.read_text(), 'bad checksum must fail before mutation'
print('PASS: backup success/failure, restore flag, restore failure and checksum guards (mock CLI)')
