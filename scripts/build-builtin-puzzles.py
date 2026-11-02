#!/usr/bin/env python3
"""Build the app's built-in puzzle set (SPEC 2: about 50 puzzles across ratings) from the real Lichess dump.

The Lichess puzzle database is CC0: https://database.lichess.org/#puzzles

Usage (needs network once, on your own machine):

    curl -O https://database.lichess.org/lichess_db_puzzle.csv.zst
    zstdcat lichess_db_puzzle.csv.zst | python3 scripts/build-builtin-puzzles.py \
        > app/src/main/assets/builtin_puzzles.csv

Selection is deterministic: the lowest rating deviation first (most reliable rating), then the most popular,
then the most played. One bucket per rating band, a fixed number of puzzles per bucket.
Stdlib only. Parameters below are build-time choices for this data file, not app constants.
"""
import argparse
import csv
import sys

HEADER = ["PuzzleId", "FEN", "Moves", "Rating", "RatingDeviation", "Popularity", "NbPlays",
          "Themes", "GameUrl", "OpeningTags"]


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--min-rating", type=int, default=600)
    ap.add_argument("--max-rating", type=int, default=2600)
    ap.add_argument("--bucket", type=int, default=200, help="rating band width")
    ap.add_argument("--per-bucket", type=int, default=5)
    ap.add_argument("--max-deviation", type=int, default=100, help="skip puzzles with an unreliable rating")
    ap.add_argument("--min-popularity", type=int, default=90)
    ap.add_argument("--min-plays", type=int, default=2000)
    ap.add_argument("--max-moves", type=int, default=8, help="skip very long puzzles (UCI plies incl. opponent's)")
    args = ap.parse_args()

    reader = csv.reader(sys.stdin)
    buckets: dict[int, list[list[str]]] = {}
    for row in reader:
        if not row or row[0] == "PuzzleId" or len(row) < 9:
            continue
        try:
            rating, dev, pop, plays = int(row[3]), int(row[4]), int(row[5]), int(row[6])
        except ValueError:
            continue
        moves = row[2].split()
        if not (args.min_rating <= rating < args.max_rating):
            continue
        if dev > args.max_deviation or pop < args.min_popularity or plays < args.min_plays:
            continue
        if len(moves) < 2 or len(moves) % 2 != 0 or len(moves) > args.max_moves:
            continue
        bucket = (rating - args.min_rating) // args.bucket
        rows = buckets.setdefault(bucket, [])
        rows.append(row)
        rows.sort(key=lambda r: (int(r[4]), -int(r[5]), -int(r[6]), r[0]))
        # Keep only the best candidates instead of retaining the whole multi-gigabyte dump.
        del rows[args.per_bucket:]

    out = csv.writer(sys.stdout, lineterminator="\n")
    out.writerow(HEADER)
    total = 0
    expected_buckets = (args.max_rating - args.min_rating + args.bucket - 1) // args.bucket
    for b in range(expected_buckets):
        rows = buckets.get(b, [])
        rows.sort(key=lambda r: (int(r[4]), -int(r[5]), -int(r[6]), r[0]))
        chosen = rows[: args.per_bucket]
        if len(chosen) < args.per_bucket:
            lo = args.min_rating + b * args.bucket
            print(f"warning: rating band {lo}-{lo + args.bucket - 1} has only {len(chosen)} puzzles",
                  file=sys.stderr)
        # keep the file ordered by rating so the app can pick the nearest quickly
        for r in sorted(chosen, key=lambda r: int(r[3])):
            out.writerow(r[:10] + [""] * (10 - len(r)))
            total += 1
    print(f"wrote {total} puzzles", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
