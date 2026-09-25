import json
import tempfile
import unittest
from unittest.mock import patch
from pathlib import Path

import numpy as np
from scipy.special import expit

from experiment import anchors, connected, convert, fit, load, run
from generate_fixture import generate
from collect_api import collect


class ExperimentTest(unittest.TestCase):
    def test_api_collection_anonymizes_users_and_preserves_source_rank(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            def response(base, path, attempts=4):
                if path.startswith('/api/v1/ratings/charts'):
                    level = int(path.split('level=')[1].split('&')[0])
                    chart = {'chartId': level, 'songId': level, 'songName': f'song{level}',
                             'genreName': 'genre', 'jacketUrl': None, 'level': level,
                             'difficulty': 4, 'upper': False}
                    return {'snapshotId': 'snapshot', 'rankings': [{'chart': chart}], 'held': []}
                if path.startswith('/api/v1/users?'):
                    return {'items': [{'id': '1111-2222-3333'}], 'hasNext': False}
                return {'playdata': [{'chartId': 49, 'allTimeBest': {'score': 95000, 'rankCode': 4},
                                      'medal': {'code': 6}}]}
            with patch('collect_api.get', side_effect=response):
                metadata = collect(root, 'https://example.invalid', workers=1)
            content = (root/'records.jsonl').read_text('utf-8')
            self.assertNotIn('1111-2222-3333', content)
            self.assertEqual(4, json.loads(content)['allTimeRankCode'])
            self.assertEqual(1, metadata['recordCount'])

    def test_full_pipeline_exports_calibrated_intervals_and_extrapolated_constants(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            generate(root/'input')
            report = run(root/'input', root/'output', bootstrap=30)
            self.assertEqual(report['inputKind'], 'SYNTHETIC')
            self.assertEqual(report['bootstrapConverged'], 30)
            self.assertTrue(report['finalFit']['converged'])
            rows = json.loads((root/'output'/'achievement-ratings.json').read_text('utf-8'))
            bundle = json.loads((root/'output'/'achievement-import.json').read_text('utf-8'))
            self.assertEqual('MEDAL', bundle['axis'])
            self.assertEqual('achievement-v1', bundle['modelVersion'])
            self.assertEqual(rows, bundle['constants'])
            self.assertEqual(len(rows), 18*5)
            visible = [r for r in rows if r['status'] == 'EXPERIMENTAL']
            self.assertTrue(visible)
            self.assertTrue(all(np.isfinite(r['difficultyConstant']) for r in visible))
            self.assertTrue(all(len(r['interval']) == 2 for r in visible))
            for metric in report['evaluation'].values():
                self.assertLess(metric['commonSkill']['logLoss'], metric['levelOnly']['logLoss'])
            missing_rank = run(root/'input', root/'rank-output', axis='rank', bootstrap=0)
            self.assertEqual(missing_rank['status'], 'BLOCKED')

    def test_recovers_cross_chart_target_reversal_without_reversing_nested_targets(self):
        rng = np.random.default_rng(7)
        skill = np.linspace(-3, 3, 250)
        truth = np.array([[-1.5, -.2, .2, 1., 2.], [-1.5, -.8, .9, 1.5, 2.5],
                          [-2, -1, 0, 1, 2], [-1, 0, 1, 2, 3]])
        u = np.repeat(np.arange(len(skill)), len(truth))
        c = np.tile(np.arange(len(truth)), len(skill))
        # One shared draw per owned record, maintaining nested achievements.
        y = (rng.random((len(u), 1)) < expit(skill[u, None]-truth[c])).astype(float)
        _, estimated, info = fit(u, c, y, len(skill), len(truth), .5)
        self.assertTrue(info['converged'])
        self.assertTrue(np.all(np.diff(estimated, axis=1) >= 0))
        self.assertGreater(estimated[0, 1], estimated[1, 1])
        self.assertLess(estimated[0, 2], estimated[1, 2])

    def test_calibration_rejects_reversed_sparse_and_out_of_range_anchors(self):
        d = np.repeat(np.array([[0.], [1.], [2.]]), 3, axis=0)
        levels = np.repeat([48, 49, 50], 3)
        eligible = np.ones_like(d, dtype=bool)
        points = anchors(d, levels, eligible, 0)
        self.assertEqual(convert(.5, points), 48.5)
        self.assertEqual(convert(3, points), 51)
        self.assertEqual(convert(-1, points), 47)
        self.assertIsNone(anchors(-d, levels, eligible, 0))
        eligible[-1] = False
        self.assertIsNone(anchors(d, levels, eligible, 0))

    def test_graph_disconnected_is_detected(self):
        self.assertFalse(connected([0, 1], [0, 1], 2, 2))
        self.assertTrue(connected([0, 0, 1], [0, 1, 1], 2, 2))

    def test_no_play_assist_hidden_duplicates_and_missing_rank_are_not_failures(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root/'catalog.json').write_text(json.dumps([{'chartId': 1, 'level': 49}]))
            base = dict(chartId=1, userExists=True, profileExists=True, chartExists=True, songExists=True)
            rows = [dict(base, userId=i, medal=m) for i, m in enumerate([7, 8, 11, 12, 13])]
            rows += [dict(base, userId=5, medal=7, hidden=True),
                     dict(base, userId=6, medal=7), dict(base, userId=6, medal=8)]
            (root/'records.jsonl').write_text('\n'.join(json.dumps(r) for r in rows))
            _, valid, quality = load(root, 'medal')
            self.assertEqual(valid, [(0, 1, 7), (1, 1, 8)])
            self.assertEqual(quality['duplicateKeys'], 1)
            self.assertEqual(load(root, 'rank')[1], [])


if __name__ == '__main__':
    unittest.main()
