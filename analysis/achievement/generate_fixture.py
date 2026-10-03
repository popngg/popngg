"""Deterministic multi-target fixture. Never represents popn.gg users."""
import argparse
import json
from pathlib import Path

import numpy as np
from scipy.special import expit


def generate(output):
    output.mkdir(parents=True, exist_ok=True)
    rng = np.random.default_rng(42)
    catalog, records = [], []
    skills = rng.normal(0, 1.7, 300)
    for level in (48, 49, 50):
        for offset in range(6):
            chart = (level-48)*6+offset+1
            clear = (level-49)*1.3 + (offset-2.5)*.13
            difficulty = clear + np.r_[0., np.cumsum([.3+offset*.08, .65-offset*.08, .6, .8])]
            catalog.append(dict(chartId=chart, songId=chart, level=level,
                                songName=f'SYNTHETIC Lv{level} chart {offset+1}'))
            for user, skill in enumerate(skills, 1):
                reached = int((rng.random() < expit(skill-difficulty)).sum())
                medal = [8, 7, 6, 5, 4, 1][reached]
                records.append(dict(userId=user, chartId=chart, level=level, medal=medal,
                                    userExists=True, profileExists=True, chartExists=True, songExists=True,
                                    bot=False, hidden=False, deleted=False, duplicate=False))
    (output/'catalog.json').write_text(json.dumps(catalog), encoding='utf-8')
    (output/'records.jsonl').write_text('\n'.join(json.dumps(r) for r in records), encoding='utf-8')
    (output/'FIXTURE_NOT_PRODUCTION.txt').write_text('Synthetic ordered-target benchmark. No real users.\n', encoding='utf-8')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    generate(parser.parse_args().output)
