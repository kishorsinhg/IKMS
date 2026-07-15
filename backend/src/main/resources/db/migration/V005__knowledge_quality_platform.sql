create table if not exists knowledge_quality_snapshot (
  id uuid primary key,
  client_id uuid not null unique references client(id) on delete cascade,
  overall_score numeric(5,4) not null,
  completeness_score numeric(5,4) not null,
  business_reference_score numeric(5,4) not null,
  linkage_score numeric(5,4) not null,
  duplicate_score numeric(5,4) not null,
  timeline_score numeric(5,4) not null,
  version_score numeric(5,4) not null,
  retrieval_readiness_score numeric(5,4) not null,
  ai_quality_score numeric(5,4) not null,
  issue_count integer not null default 0,
  open_issue_count integer not null default 0,
  readiness_state varchar(32) not null,
  summary_text text,
  evaluated_at timestamptz not null,
  updated_at timestamptz not null
);

create table if not exists knowledge_quality_issue (
  id uuid primary key,
  snapshot_id uuid references knowledge_quality_snapshot(id) on delete cascade,
  client_id uuid references client(id) on delete cascade,
  source_type varchar(32),
  source_id uuid,
  category varchar(64) not null,
  issue_type varchar(64) not null,
  severity varchar(16) not null,
  status varchar(16) not null,
  title varchar(255) not null,
  detail_text text,
  recommendation_type varchar(64),
  recommendation_detail text,
  business_reference_key varchar(64),
  score_impact numeric(5,4),
  created_at timestamptz not null,
  updated_at timestamptz not null
);

create index if not exists idx_quality_issue_client on knowledge_quality_issue(client_id, status, severity);
create index if not exists idx_quality_issue_source on knowledge_quality_issue(source_type, source_id);
