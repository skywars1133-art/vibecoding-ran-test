#!/usr/bin/env python3
"""Build the daily rating-band puzzle packs for ChessLock (SPEC 2).

The Lichess puzzle database is CC0: https://database.lichess.org/#puzzles

Usage (the GitHub Action runs this, but it also works locally):

    curl -O https://database.lichess.org/lichess_db_puzzle.csv.zst
    zstdcat lichess_db_puzzle.csv.zst | python3 scripts/build-puzzle-packs.py \
        --output-dir puzzle-packs --day 20261102

Output: one `puzzles-<center>.csv` per 200-point band center from 400 to 2800. Each file contains
150 puzzles from center-300 to center+300, chosen from a larger pool of reliable/popular puzzles.
The daily salt rotates the selection, so a new download normally brings new material even when the
Lichess dump itself has not changed.

The app's `PuzzlePackPlanner` uses the same centers and spread (`AppConfig`). Keep these defaults in
sync when changing them. Stdlib only; the candidate pool is bounded so the full dump is streamed.
"""
import argparse
import csv
import hashlib
import heapq
import os
import sys
from datetime import datetime, timezone

HEADER = ["PuzzleId", "FEN", "Moves", "Rating", "RatingDeviation", "Popularity", "NbPlays",
          "Themes", "GameUrl", "OpeningTags"]


def band_centers(min_rating: int, max_rating: int, width: int) -> list[int]:
    if width <= 0 or max_rating < min_rating:
        raise SystemExit("invalid rating band range")
    return list(range(min_rating, max_rating + 1, width))


def daily_key(day: str, puzzle_id: str) -> bytes:
    return hashlib.blake2b(f"{day}:{puzzle_id}".encode("utf-8"), digest_size=8).digest()


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--output-dir", required=True)
    ap.add_argument("--day", default=datetime.now(timezone.utc).strftime("%Y%m%d"),
                    help="rotation salt; defaults to today's UTC date (YYYYMMDD)")
    ap.add_argument("--min-rating", type=int, default=400, help="lowest band center")
    ap.add_argument("--max-rating", type=int, default=2800, help="highest band center")
    ap.add_argument("--band-width", type=int, default=200, help="spacing between band centers")
    ap.add_argument("--spread", type=int, default=300, help="points covered on each side of a center")
    ap.add_argument("--pack-size", type=int, default=150, help="puzzles written per file")
    ap.add_argument("--pool-per-band", type=int, default=3000,
                    help="candidate puzzles retained per band before the daily rotation")
    ap.add_argument("--max-deviation", type=int, default=100, help="skip puzzles with an unreliable rating")
    ap.add_argument("--min-popularity", type=int, default=90)
    ap.add_argument("--min-plays", type=int, default=2000)
    ap.add_argument("--max-moves", type=int, default=8, help="skip very long puzzles (UCI plies incl. opponent's)")
    ap.add_argument("--require-full", action="store_true",
                    help="fail instead of warning when a band has fewer than --pack-size puzzles")
    args = ap.parse_args()

    centers = band_centers(args.min_rating, args.max_rating, args.band_width)
    # Each band's heap keeps its best `pool-per-band` candidates. The tuple is ordered so the
    # min-heap root is the *worst* retained candidate: high deviation, low popularity/plays.
    pools: dict[int, list[tuple[int, int, int, int, list[str]]]] = {c: [] for c in centers}
    sequence = 0
    rows_read = 0
    rows_kept = 0

    reader = csv.reader(sys.stdin)
    for row in reader:
        if not row or row[0] == "PuzzleId" or len(row) < 9:
            continue
        rows_read += 1
        try:
            rating, dev, pop, plays = int(row[3]), int(row[4]), int(row[5]), int(row[6])
        except ValueError:
            continue
        moves = row[2].split()
        if dev > args.max_deviation or pop < args.min_popularity or plays < args.min_plays:
            continue
        if len(moves) < 2 or len(moves) % 2 != 0 or len(moves) > args.max_moves:
            continue

        matching = [
            c for c in centers
            if c - args.spread <= rating < c + args.spread
        ]
        if not matching:
            continue

        sequence += 1
        item = (-dev, pop, plays, sequence, row[:10])
        for center in matching:
            heap = pools[center]
            if len(heap) < args.pool_per_band:
                heapq.heappush(heap, item)
                rows_kept += 1
            else:
                # Keep the better candidate; heappushpop returns the worst of the old/new pair.
                heapq.heappushpop(heap, item)

    os.makedirs(args.output_dir, exist_ok=True)
    total_written = 0
    warnings = 0
    for center in centers:
        heap = pools[center]
        ordered = sorted(heap, key=lambda item: daily_key(args.day, item[4][0]))
        chosen: list[list[str]] = []
        seen_ids: set[str] = set()
        for item in ordered:
            puzzle_id = item[4][0]
            if puzzle_id in seen_ids:
                continue
            seen_ids.add(puzzle_id)
            chosen.append(item[4])
            if len(chosen) >= args.pack_size:
                break

        if len(chosen) < args.pack_size:
            message = (
                f"band {center} ({center - args.spread}..{center + args.spread - 1}) "
                f"has only {len(chosen)} puzzles (pool {len(heap)})"
            )
            if args.require_full:
                raise SystemExit(f"error: {message}; refusing to publish an incomplete pack")
            warnings += 1
            print(f"warning: {message}", file=sys.stderr)
        if not chosen:
            continue

        chosen.sort(key=lambda r: (int(r[3]), r[0]))
        path = os.path.join(args.output_dir, f"puzzles-{center}.csv")
        with open(path, "w", newline="", encoding="utf-8") as out:
            writer = csv.writer(out, lineterminator="\n")
            writer.writerow(HEADER)
            for r in chosen:
                writer.writerow(r[:10] + [""] * (10 - len(r)))
        total_written += len(chosen)

    print(
        f"day={args.day}: read {rows_read} rows, kept up to {args.pool_per_band} per band, "
        f"wrote {total_written} puzzles across {len(centers)} bands ({warnings} warnings)",
        file=sys.stderr,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
