#!/usr/bin/env python3
"""Validate framework structure, optionally execute reviewed project checks."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
from roadmap import validate as validate_roadmap
from project_zero import validate as validate_zero, fingerprint

ROOT = Path(__file__).resolve().parents[2]
CORE = ROOT / '.ai-framework'
ROLES = {'readme','agents','workflow','handoff','roadmap','architecture','decisions','tests','done','changelog'}

OPTIONAL_ROLES = {'ACCEPTANCE_CRITERIA.md', 'VISION.md', 'UI_RULES.md', 'LESSONS_LEARNED.md', 'RELEASE_PROCESS.md', 'POSTMORTEM.md', 'STYLE_GUIDE.md', 'REFACTORING.md', 'AI_CONTEXT.md', 'REQUIREMENTS.md', 'DESIGN_SYSTEM.md', 'PROMPTS.md', 'CODING_STANDARDS.md', 'RUNBOOK.md', 'MIGRATION.md', 'CONTRIBUTING.md', 'AI_EVALUATION.md', 'ACCESSIBILITY.md', 'DEPENDENCIES.md', 'THREAT_MODEL.md', 'MILESTONES.md', 'PERFORMANCE.md', 'FAQ.md', 'BACKUP.md', 'KNOWN_LIMITATIONS.md', 'QUICKSTART.md', 'ENVIRONMENT.md', 'DATA_MODEL.md', 'DEPLOYMENT.md', 'RECOVERY.md', 'TROUBLESHOOTING.md', 'UX_GUIDELINES.md', 'USER_GUIDE.md', 'INSTALLATION.md', 'API.md', 'PRIVACY.md', 'GLOSSARY.md', 'SECURITY.md', 'TESTING.md'}

def nonempty(v):
    return isinstance(v,str) and bool(v.strip())

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--strict', action='store_true', help='Require active configuration')
    p.add_argument('--run', action='store_true', help='Execute configured commands; review them first')
    p.add_argument('--ready', action='store_true', help='Require PROJECT_ZERO approval and active configuration')
    p.add_argument('--proposal', action='store_true', help='Validate review-stage proposal before approval')
    a = p.parse_args()
    if a.proposal and (a.ready or a.run): p.error('--proposal cannot be combined with --ready or --run')
    errors = []
    for file in ['PROJECT_ZERO.md','project-zero.json','tools/project_zero.py','ROADMAP_STANDARD.md','tools/roadmap.py','ENTRYPOINT.md','EXECUTION_CONTRACT.md','MODES.md','CONFIG_GUIDE.md','OPTIONAL_CATALOG.md','templates/CHECKPOINT.md','templates/TASK.md','templates/REPORT.md']:
        if not (CORE/file).is_file(): errors.append('Missing core file: '+file)
    try:
        cfg = json.loads((CORE/'project.json').read_text(encoding='utf-8'))
        if not isinstance(cfg,dict): raise ValueError('root must be an object')
    except (OSError,ValueError) as exc:
        print(f'FAIL configuration: {exc}'); return 1
    if cfg.get('schema_version') != 1: errors.append('Unsupported schema_version')
    status = cfg.get('status')
    if status not in ('draft','active'): errors.append('Invalid status')
    active = status == 'active'
    if (a.strict or a.run or a.ready or a.proposal) and not active: errors.append('Configuration is not active')
    if active:
        for k in ('project_name','release_policy','checkpoint_location'):
            if not nonempty(cfg.get(k)) or cfg[k].strip().lower() == 'unresolved': errors.append('Missing active field: '+k)
    sources=cfg.get('sources')
    if not isinstance(sources,dict): errors.append('sources must be an object'); sources={}
    optional=cfg.get('optional_sources')
    if not isinstance(optional,dict): errors.append('optional_sources must be an object'); optional={}
    if set(optional)-OPTIONAL_ROLES: errors.append('Unknown optional source roles: '+', '.join(sorted(set(optional)-OPTIONAL_ROLES)))
    all_sources={**{role:sources.get(role) for role in ROLES}, **{role:optional.get(role) for role in OPTIONAL_ROLES}}
    for role in sorted(all_sources):
        s=all_sources.get(role)
        if not isinstance(s,dict): errors.append('Missing source role: '+role); continue
        kind=s.get('kind'); value=s.get('value'); note=s.get('note')
        if kind=='file':
            if not nonempty(value) or Path(value).is_absolute(): errors.append('Invalid relative path: '+role); continue
            target=(ROOT/value).resolve()
            if ROOT not in target.parents or not target.is_file(): errors.append('Missing or outside repository: '+role)
        elif kind=='external':
            if not nonempty(value) or not nonempty(note): errors.append('External source needs value and note: '+role)
        elif kind=='not_applicable':
            if not nonempty(note) or value != '': errors.append('N/A needs reason and empty value: '+role)
            if role=='agents' and active: errors.append('Agent entrypoint cannot be N/A')
        elif kind=='unresolved':
            if active: errors.append('Unresolved role: '+role)
        else: errors.append('Invalid source kind: '+role)
    checks=cfg.get('checks')
    if not isinstance(checks,list): errors.append('checks must be an array'); checks=[]
    ids=set(); required=0
    for item in checks:
        if not isinstance(item,dict): errors.append('Invalid check object'); continue
        id_=item.get('id'); argv=item.get('argv'); req=item.get('required'); timeout=item.get('timeout_sec',120)
        if not nonempty(id_) or id_ in ids: errors.append('Missing or duplicate check id')
        else: ids.add(id_)
        if not isinstance(argv,list) or not argv or not all(nonempty(x) for x in argv): errors.append(f'Invalid argv: {id_}')
        if type(req) is not bool: errors.append(f'Invalid required: {id_}')
        elif req: required+=1
        if type(timeout) is not int or not 1<=timeout<=3600: errors.append(f'Invalid timeout: {id_}')
    if active and not required and not nonempty(cfg.get('checks_policy_note')): errors.append('No required checks or manual-check policy')
    if active and isinstance(cfg.get('sources'),dict):
        roadmap_errors, _ = validate_roadmap(ROOT,cfg)
        errors.extend(roadmap_errors)
        if cfg.get('roadmap_format') in ('existing','external'):
            print('SKIPPED: roadmap content requires tracker-specific verification')
    zero_errors, zero_state = validate_zero(ROOT,cfg,a.ready,proposal=a.proposal)
    errors.extend(zero_errors)
    if errors:
        for e in errors: print('FAIL:',e)
        return 1
    print(f'STRUCTURE PASS ({status}); external sources and document truth not verified', flush=True)
    if a.proposal: print('Proposal fingerprint: '+fingerprint(zero_state))
    if a.ready: print('PROJECT_ZERO readiness structure PASS; manual review of approval and actual requirements still required')
    if not a.run:
        print('NOT RUN: project checks'); return 0
    if not required: print('WARNING: no required automated checks; manual validation still needed')
    failed=False
    for item in checks:
        print(f'RUN {item["id"]}: {item["argv"]!r}',flush=True)
        try:
            result=subprocess.run(item['argv'],cwd=ROOT,timeout=item.get('timeout_sec',120),shell=False)
            ok=result.returncode==0
            print(f'{"PASS" if ok else "FAIL"} {item["id"]}: exit {result.returncode}',flush=True)
        except (OSError,ValueError,subprocess.TimeoutExpired) as exc:
            ok=False; print(f'BLOCKED {item["id"]}: {exc}',flush=True)
        if not ok and item['required']: failed=True
    print('Required configured automated checks failed' if failed else 'No required configured automated check failed; review remaining manual criteria')
    return int(failed)
if __name__=='__main__':
    sys.exit(main())
