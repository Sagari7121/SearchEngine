# Search Engine (Built From Scratch)

A web crawler and search engine built in Java/Spring Boot — **no Lucene, Elasticsearch, or any off-the-shelf search library**. The inverted index, BM25F ranking, phrase search, segment storage, and PageRank are all implemented from first principles.

---

## Table of Contents

- [Why build this from scratch](#why-build-this-from-scratch)
- [Architecture overview](#architecture-overview)
- [Core components](#core-components)
  - [Crawler](#crawler)
  - [Indexing pipeline](#indexing-pipeline)
  - [Segment storage engine](#segment-storage-engine)
  - [Ranking: BM25F + PageRank](#ranking-bm25f--pagerank)
  - [Phrase search](#phrase-search)
- [Data model](#data-model)
- [API](#api)
- [Running locally](#running-locally)
- [Known limitations / roadmap](#known-limitations--roadmap)

---

## Why build this from scratch

Wiring together `Elasticsearch` or a vector DB + embeddings API gets you a working search box in an afternoon, but it doesn't demonstrate much beyond "can call an API." This project implements the actual data structures and algorithms search engines are built on:

- An **inverted index** (term → postings), the foundational structure behind every text search system
- **BM25F** relevance scoring — the same ranking family used by real search engines, extended with per-field weighting
- **Immutable, segment-based storage** with background merging — the same design pattern behind Lucene, RocksDB, Cassandra, and other LSM-tree-based systems
- **PageRank**, computed from the crawled link graph via iterative convergence

## Architecture overview

```
                    ┌─────────────┐
                    │   Crawler   │  robots.txt-aware, rate-limited, retry with backoff
                    └──────┬──────┘
                           │ writes
                           ▼
                ┌────────────────────┐
                │   page_metadata    │  crawled content + status
                │   pending_crawl    │  crawl queue
                │   page_link        │  link graph (source → target)
                │   domain           │  per-domain politeness state
                └──────────┬─────────┘
                           │ indexStatus = PENDING
                           ▼
                ┌────────────────────┐
                │  Indexing Scheduler │  batches pending pages
                └──────────┬─────────┘
                           │ tokenize (title + text fields)
                           ▼
                ┌────────────────────┐
                │   Segment Writer    │  immutable, atomic flush
                └──────────┬─────────┘
                           │
                           ▼
              ┌──────────────────────────┐
              │   On-disk segments        │◄── background merge (compaction)
              │   segment_1, segment_2... │
              └──────────┬────────────────┘
                           │
                           ▼
              ┌──────────────────────────┐
              │   doc_location (tombstone) │  which segment currently owns each doc
              └──────────┬────────────────┘
                           │
                           ▼
              ┌──────────────────────────┐
              │   Search query            │  BM25F + PageRank boost
              │   (multi-segment scan)    │
              └────────────────────────────┘
```

## Core components

### Crawler

- Pulls due URLs from `pending_crawl` with `FOR UPDATE SKIP LOCKED` for multi-worker safety
- Enforces per-domain politeness via `Domain.nextAvailableAt` / `crawlDelayMs`
- Fetches and honors `robots.txt` per-domain (cached, 24h TTL): `User-agent` group resolution, longest-match `Allow`/`Disallow` precedence, `Crawl-delay`
- Deduplicates discovered URLs with a Bloom filter before they ever hit the database
- SSRF-checks every outbound link before queuing it
- Exponential backoff on failed fetches; gives up after a configurable attempt cap
- Detects unchanged content via SHA-256 hashing, so an unchanged re-crawl does zero re-indexing work

### Indexing pipeline

- Runs on a schedule, batching `PENDING` pages
- Tokenizes **title and body text as separate fields** (needed for field-weighted ranking, see below)
- Tracks token *positions* within the text field, enabling exact-phrase queries
- Writes each batch as one immutable **segment** (see below) rather than mutating a live index in place

### Segment storage engine

Modeled after the LSM-tree pattern used by Lucene, RocksDB, and Cassandra:

- Each segment is **written once, never modified** — new content always goes into a new segment
- Segments are written to a temp directory and **atomically renamed** into place, so a crash mid-write can never leave a corrupt/partial segment visible to readers
- A background `MergePolicy` periodically folds many small segments into one larger segment once a threshold is crossed, reclaiming space from stale/deleted docs along the way
- **Soft deletes via tombstones**: a `doc_location` table tracks which segment currently "owns" each document. Re-indexing a page (e.g. after a content change) simply writes a new segment and updates this table — no in-place mutation of old segment files needed. Search and merge both consult this table to ignore stale copies.
- Segment ids are assigned from a single monotonic counter (shared by flush *and* merge) and loaded numerically (not alphabetically) on startup, so ordering stays correct past `segment_9` → `segment_10`

### Ranking: BM25F + PageRank

- **BM25F**: per-field term frequencies (title, body text) are blended into one pseudo-TF *before* the BM25 saturation curve is applied, with separate length-normalization (`b`) per field — a title match is weighted more heavily than the same term buried in body text, without corrupting the corpus-wide IDF calculation
- **PageRank**: computed nightly via iterative convergence over the `page_link` graph (damping factor 0.85, convergence threshold on total score delta)
- The two signals are combined **multiplicatively**, not additively: `finalScore = bm25fScore × (1 + weight × normalizedPageRank)`, with PageRank log-scaled and normalized into `[0, 1]` first. This means link authority can only amplify genuine text relevance — a page with zero matching terms stays at zero regardless of how authoritative it is.

### Phrase search

- A query wrapped in double quotes (`"exact phrase"`) is checked for **positional adjacency**, not just co-occurrence
- Candidate documents are filtered to those containing the phrase *before* BM25F scoring runs, so ranking is computed only over genuinely matching documents

## Data model

| Table | Purpose |
|---|---|
| `domain` | Per-domain crawl politeness state (delay, page budget, next-available time) |
| `pending_crawl` | Crawl queue — retry count, backoff timestamp |
| `page_metadata` | Crawled page content, content hash, index status |
| `page_link` | Link graph edges (source → target), feeds PageRank |
| `doc_location` | Tombstone table — which segment currently owns each document |
| `page_rank` | Latest computed PageRank score per page |

All page-identity keys are UUIDs assigned by application code at *discovery* time (not DB auto-increment), so a page's identity stays stable as it moves from queued → crawled → indexed → re-indexed, and every foreign-key-style reference (`page_link.targetPageId`, `doc_location.pageId`) stays valid across that lifecycle without rewriting.

## API

```
GET /api/search?q={query}&limit={n}
```
Returns ranked results with URL, title, snippet, and score. Wrap `q` in double quotes for an exact-phrase query.

```json
{
  "query": "machine learning",
  "resultCount": 8,
  "tookMillis": 14,
  "results": [
    { "url": "...", "title": "...", "snippet": "...", "score": 4.82 }
  ]
}
```

## Running locally

```bash
# prerequisites: JDK 21+, PostgreSQL, Redis (for the Bloom filter)

cd search-engine

# configure application.properties / application-dev.properties:
#   - datasource url/credentials
#   - search.index.data-dir (segment storage path)

./mvnw spring-boot:run
```

## Known limitations / roadmap

This is a working prototype built to learn how search engines work internally. The items below are the gaps I know about, roughly in the order I would tackle them.

### Storage engine

- **Segments are loaded fully into memory.** Each segment's postings and document lengths are read into heap on startup, so index size is bounded by RAM. Next step: a sorted term dictionary with on-disk postings that are read on demand (memory-mapped files or block-level reads).
- **Segment files are plain text.** Postings are stored as pipe-delimited lines, which is easy to debug but large and slow to parse. Next step: a binary format with delta-encoded document ids and variable-byte compression for positions.
- **No explicit `fsync` before the atomic rename.** The temp-directory-then-rename flush prevents readers from seeing a partial segment, but durability across a power loss is not guaranteed until the files and the parent directory are synced.
- **No skip pointers.** Multi-term and phrase queries walk full postings lists. Skip lists would let intersections jump ahead.
- **Merge policy is a simple threshold.** A tiered policy (merge segments of similar size) would reduce write amplification as the index grows.

### Text analysis

- **The tokenizer is a single regex** (`[a-z0-9]+` after lowercasing). There is no stemming, stop-word handling, or synonym support, so "running" does not match "run".
- **English/ASCII only.** Non-Latin scripts and accented characters are dropped. Next step: Unicode-aware tokenization and normalisation.

### Ranking and query features

- **BM25F parameters and the PageRank weight are hand-set**, not tuned against a relevance benchmark. Next step: evaluate with a labelled query set (nDCG / MRR) and tune from there.
- **Query language is minimal**: free text and quoted phrases only. No boolean operators, field-scoped queries (`title:`), or typo tolerance.
- **PageRank is recomputed from scratch nightly** over the whole link graph. An incremental update would scale better.

### Crawler

- **HTML only.** No JavaScript rendering, sitemap discovery, or handling of non-HTML content types.
- **Near-duplicate detection is exact-hash only** (SHA-256). SimHash or MinHash would catch pages that differ only in boilerplate.
- **Crawl prioritisation is first-due, first-served.** Prioritising by domain authority or freshness would use the crawl budget better.

### Engineering

- **Test coverage is minimal.** Priority: unit tests for the segment writer/reader round trip, merge with tombstones, phrase matching, and the robots.txt parser.
- **Configuration and secrets** should come entirely from environment variables rather than `application.properties`.
- **No metrics or benchmarks yet.** Planned: indexing throughput, query latency percentiles, and index size per 1,000 pages, measured on a fixed corpus.
- **Single node.** The index lives on one machine's disk; sharding and replication are out of scope for now.
