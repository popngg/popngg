"""Offline, non-publishing experiment for common achievement difficulty scales."""
import argparse
import csv
import hashlib
import json
from pathlib import Path
from datetime import datetime, timezone

import numpy as np
from scipy.optimize import minimize
from scipy.special import expit

TARGETS = {
    "medal": [("CLEAR", 7), ("BRONZE_DIAMOND", 6), ("BRONZE_STAR", 5),
              ("FULL_COMBO", 4), ("PERFECT", 1)],
    "rank": [("AA", 5), ("AA_PLUS", 4), ("AAA", 3), ("S", 2), ("S_PLUS", 1)],
}


def load(directory, axis):
    catalog = {int(c["chartId"]): c for c in json.loads((directory / "catalog.json").read_text("utf-8-sig"))
               if 48 <= c["level"] <= 50}
    rows, seen, duplicate = [], set(), set()
    counts = {"raw": 0, "excluded": 0, "missingRank": 0}
    with (directory / "records.jsonl").open(encoding="utf-8-sig") as stream:
        for line in stream:
            if not line.strip():
                continue
            r = json.loads(line)
            counts["raw"] += 1
            key = (r["userId"], r["chartId"])
            if key in seen:
                duplicate.add(key)
            seen.add(key)
            valid = all(r.get(k, False) for k in ("userExists", "profileExists", "chartExists", "songExists"))
            valid &= not any(r.get(k, False) for k in ("bot", "hidden", "deleted", "duplicate"))
            if not valid or r["chartId"] not in catalog:
                counts["excluded"] += 1
                continue
            code = r.get("medal" if axis == "medal" else "allTimeRankCode")
            if axis == "rank" and code is None:
                counts["missingRank"] += 1
            if not isinstance(code, int) or not 1 <= code <= (10 if axis == "medal" else 12):
                counts["excluded"] += 1
                continue
            rows.append((int(r["userId"]), int(r["chartId"]), code))
    rows = sorted(r for r in rows if r[:2] not in duplicate)
    counts["duplicateKeys"] = len(duplicate)
    counts["valid"] = len(rows)
    return catalog, rows, counts


def fit(u, c, y, users, charts, penalty, weights=None):
    """Cumulative binary composite likelihood, ordered per-chart thresholds.

    Targets share one user skill. Positive softplus increments permit chart-specific
    gaps. This is an experimental composite objective, not independent attempts.
    """
    targets = y.shape[1]
    weights = np.ones(len(u)) if weights is None else weights
    total = max(weights.sum(), 1)
    initial = np.zeros(users + charts * targets)
    initial[users:] = np.tile(np.r_[0., np.repeat(-.5, targets - 1)], charts)

    def unpack(x):
        raw = x[users:].reshape(charts, targets)
        d = np.column_stack((raw[:, 0], np.logaddexp(0, raw[:, 1:])))
        return x[:users], raw, np.cumsum(d, axis=1)

    def objective(x):
        skill, raw, difficulty = unpack(x)
        z = skill[u, None] - difficulty[c]
        residual = (expit(z) - y) * weights[:, None] / targets
        loss = (weights[:, None] * (np.logaddexp(0, z) - y * z)).sum() / targets
        gs = np.bincount(u, weights=residual.sum(axis=1), minlength=users)
        gd = np.zeros_like(difficulty)
        np.add.at(gd, c, -residual)
        # Shrink chart differences within each target; do not force common target gaps.
        centered = difficulty - difficulty.mean(axis=0)
        loss += penalty / 2 * (np.dot(skill, skill) + (centered ** 2).sum())
        gs += penalty * skill
        gd += penalty * centered
        gr = np.flip(np.cumsum(np.flip(gd, axis=1), axis=1), axis=1)
        gr[:, 1:] *= expit(raw[:, 1:])
        return loss / total, np.r_[gs, gr.ravel()] / total

    result = minimize(objective, initial, jac=True, method="L-BFGS-B",
                      options={"maxiter": 800, "ftol": 1e-10, "gtol": 1e-6})
    skill, _, difficulty = unpack(result.x)
    return skill, difficulty, {"converged": bool(result.success), "iterations": result.nit}


def metrics(y, p):
    p = np.clip(p, 1e-12, 1 - 1e-12)
    return {"logLoss": float(-(y * np.log(p) + (1-y) * np.log(1-p)).mean()),
            "brier": float(((p-y)**2).mean()), "count": len(y)}


def anchors(difficulty, levels, eligible, reference):
    result = []
    for level in (48, 49, 50):
        indexes = np.flatnonzero((levels == level) & eligible[:, reference])
        if len(indexes) < 3:
            return None
        result.append(float(np.median(difficulty[indexes, reference])))
    return result if np.all(np.diff(result) > 1e-4) else None


def convert(value, points):
    if points is None:
        return None
    if value < points[0]:
        return float(48 + (value-points[0])/(points[1]-points[0]))
    if value > points[2]:
        return float(50 + (value-points[2])/(points[2]-points[1]))
    return float(np.interp(value, points, [48, 49, 50]))


def connected(u, c, user_count, chart_count):
    parent = list(range(user_count + chart_count))
    def root(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x
    for a, b in zip(u, c):
        parent[root(int(a))] = root(user_count + int(b))
    return len({root(user_count + k) for k in range(chart_count)}) == 1


def run(directory, output, axis="medal", bootstrap=30, seed=20260925, source_snapshot_id=None):
    if directory.resolve() == output.resolve():
        raise ValueError("Output must not overwrite the source snapshot")
    catalog, rows, quality = load(directory, axis)
    output.mkdir(parents=True, exist_ok=True)
    if not rows:
        report = {"status": "BLOCKED", "reason": "NO_VALID_RECORDS", "quality": quality}
        (output / "report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
        return report
    ids = sorted({r[1] for r in rows})
    users = sorted({r[0] for r in rows})
    ui, ci = {v: i for i, v in enumerate(users)}, {v: i for i, v in enumerate(ids)}
    u = np.array([ui[r[0]] for r in rows])
    c = np.array([ci[r[1]] for r in rows])
    codes = np.array([r[2] for r in rows])
    y = np.array([codes <= threshold for _, threshold in TARGETS[axis]]).T.astype(float)
    levels = np.array([catalog[i]["level"] for i in ids])
    count = np.bincount(c, minlength=len(ids))
    successes = np.zeros((len(ids), y.shape[1]))
    np.add.at(successes, c, y)
    eligible = (count[:, None] >= 50) & (successes >= 5) & (count[:, None]-successes >= 5)
    # All thresholds of a user-chart observation stay in the same split.
    rng = np.random.default_rng(seed)
    order = rng.permutation(len(rows))
    split = np.empty(len(rows), dtype=int)
    split[order] = np.where(np.arange(len(rows)) < .7*len(rows), 0,
                           np.where(np.arange(len(rows)) < .85*len(rows), 1, 2))
    train = split == 0
    supported = np.isin(u, u[train]) & np.isin(c, c[train])
    validation, test = (split == 1) & supported, (split == 2) & supported
    if not validation.any() or not test.any():
        raise ValueError("Insufficient supported validation/test interactions")
    trials = []
    for penalty in (.5, 2., 8.):
        skill, d, info = fit(u[train], c[train], y[train], len(users), len(ids), penalty)
        loss = metrics(y[validation], expit(skill[u[validation], None]-d[c[validation]]))["logLoss"]
        trials.append((loss if info["converged"] else float("inf"), penalty, skill, d, info))
    _, penalty, skill, d, info = min(trials, key=lambda t: t[0])
    if not info["converged"]:
        raise RuntimeError("No candidate converged")
    level_index = levels[c]-48
    baseline_trials = []
    for bp in (.5, 2., 8.):
        bu, bd, bi = fit(u[train], level_index[train], y[train], len(users), 3, bp)
        score = metrics(y[validation], expit(bu[u[validation], None]-bd[level_index[validation]]))["logLoss"]
        baseline_trials.append((score if bi["converged"] else float("inf"), bp, bu, bd, bi))
    _, baseline_penalty, baseline_u, baseline_d, baseline_info = min(baseline_trials, key=lambda t: t[0])
    if not baseline_info["converged"]:
        raise RuntimeError("User+level baseline did not converge")
    evaluation = {}
    for t, (name, _) in enumerate(TARGETS[axis]):
        # Level-only empirical baseline fitted exclusively to training records.
        rates = {l: (y[train & (levels[c] == l), t].sum()+1) /
                     ((train & (levels[c] == l)).sum()+2) for l in (48, 49, 50)}
        evaluation[name] = {
            "levelOnly": metrics(y[test, t], np.array([rates[l] for l in levels[c[test]]])),
            "userLevel": metrics(y[test, t], expit(baseline_u[u[test]]-baseline_d[level_index[test], t])),
            "commonSkill": metrics(y[test, t], expit(skill[u[test]]-d[c[test], t]))}
    # Final all-record refit happens only after the held-out evaluation above.
    _, d, final_info = fit(u, c, y, len(users), len(ids), penalty)
    reference = 0 if axis == "medal" else 2  # CLEAR / provisional AAA.
    graph_connected = connected(u, c, len(users), len(ids))
    points = anchors(d, levels, eligible, reference) if graph_connected and final_info["converged"] else None
    samples = []
    bootstrap_converged = 0
    for _ in range(bootstrap):
        # Cluster bootstrap: all charts and targets of a sampled user share weight.
        multiplicity = rng.multinomial(len(users), np.full(len(users), 1/len(users)))
        weights = multiplicity[u]
        _, bd, bi = fit(u, c, y, len(users), len(ids), penalty, weights)
        if not bi["converged"]:
            continue
        bootstrap_converged += 1
        # Unique observed users, not bootstrap repetitions, determine eligibility.
        bc = np.bincount(c[weights > 0], minlength=len(ids))
        bs = np.zeros_like(successes)
        np.add.at(bs, c[weights > 0], y[weights > 0])
        be = (bc[:, None] >= 50) & (bs >= 5) & (bc[:, None]-bs >= 5)
        replicate_connected = connected(u[weights > 0], c[weights > 0], len(users), len(ids))
        bp = anchors(bd, levels, be, reference) if replicate_connected else None
        samples.append([[convert(v, bp) for v in chart] for chart in bd])
    results = []
    for j, chart_id in enumerate(ids):
        for t, (name, _) in enumerate(TARGETS[axis]):
            reasons = []
            if count[j] < 50: reasons.append("INSUFFICIENT_PLAYERS")
            if successes[j, t] < 5: reasons.append("INSUFFICIENT_ACHIEVERS")
            if count[j]-successes[j, t] < 5: reasons.append("INSUFFICIENT_NON_ACHIEVERS")
            if not graph_connected: reasons.append("DISCONNECTED_GRAPH")
            if not final_info["converged"]: reasons.append("MODEL_NOT_CONVERGED")
            value = convert(d[j, t], points)
            if points is None: reasons.append("UNSTABLE_LEVEL_ANCHORS")
            valid = [s[j][t] for s in samples if s[j][t] is not None]
            interval = np.quantile(valid, [.025, .975]).tolist() if bootstrap >= 30 and len(valid) >= .9*bootstrap else None
            if interval is None: reasons.append("INSUFFICIENT_BOOTSTRAP_SUPPORT")
            results.append({"chartId": chart_id, "songName": catalog[chart_id].get("songName"),
                            "level": int(levels[j]), "axis": axis, "target": name,
                            "rawDifficulty": float(d[j, t]), "difficultyConstant": value if not reasons else None,
                            "interval": interval if not reasons else None,
                            "playerCount": int(count[j]), "achievedCount": int(successes[j, t]),
                            "status": "HOLD" if reasons else "EXPERIMENTAL", "holdReasons": reasons})
    source_hashes = {p: hashlib.sha256((directory/p).read_bytes()).hexdigest()
                     for p in ("records.jsonl", "catalog.json")}
    generated_at = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    source_id = source_snapshot_id or "files:" + source_hashes["records.jsonl"][:32]
    input_kind = ("SYNTHETIC" if (directory/'FIXTURE_NOT_PRODUCTION.txt').exists() else
                  "PUBLIC_API" if (directory/'API_COLLECTION_METADATA.json').exists() else "UNVERIFIED_SOURCE")
    report = {"status": "EXPERIMENT_COMPLETE", "publicationStatus": "NOT_VALIDATED", "axis": axis,
              "inputKind": input_kind,
              "quality": quality, "users": len(users), "charts": len(ids), "seed": seed,
              "penalty": penalty, "finalFit": final_info, "evaluation": evaluation,
              "userLevelPenalty": baseline_penalty,
              "validationTrials": [{"penalty": t[1], "converged": t[4]["converged"],
                                    "logLoss": t[0] if np.isfinite(t[0]) else None} for t in trials],
              "testUnsupported": int(((split == 2) & ~supported).sum()),
              "connected": graph_connected, "referenceTarget": TARGETS[axis][reference][0],
              "levelAnchors": points, "bootstrapRuns": bootstrap, "bootstrapConverged": bootstrap_converged,
              "sourceSnapshotId": source_id, "generatedAt": generated_at,
              "sourceHashes": source_hashes,
              "limitations": ["Observed best-owned states; not attempt probabilities",
                              "Selection bias and static ability assumptions",
                              "No user holdout or existing production CPI/SPI comparison yet",
                              "Minimum outcome counts and calibration rules are provisional",
                              "No DB writes, S3 publication, or operational rating replacement"]}
    for filename, data in (("report.json", report), ("achievement-ratings.json", results)):
        (output/filename).write_text(json.dumps(data, ensure_ascii=False, indent=2, allow_nan=False), encoding="utf-8")
    bundle = {"sourceSnapshotId": source_id, "modelVersion": "achievement-v1",
              "modelStatus": "EXPERIMENTAL", "generatedAt": generated_at,
              "axis": axis.upper(), "constants": results}
    (output/"achievement-import.json").write_text(
        json.dumps(bundle, ensure_ascii=False, indent=2, allow_nan=False), encoding="utf-8")
    with (output/"achievement-ratings.csv").open("w", newline="", encoding="utf-8-sig") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(results[0]))
        writer.writeheader()
        writer.writerows(results)
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--snapshot", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--axis", choices=TARGETS, default="medal")
    parser.add_argument("--bootstrap", type=int, default=30)
    parser.add_argument("--source-snapshot-id")
    args = parser.parse_args()
    print(json.dumps(run(args.snapshot, args.output, args.axis, args.bootstrap,
                         source_snapshot_id=args.source_snapshot_id), ensure_ascii=False, indent=2))
