create table operations_job (
  id uuid primary key,
  job_type varchar(64) not null,
  submitted_by uuid,
  submitted_at timestamp with time zone not null,
  started_at timestamp with time zone,
  completed_at timestamp with time zone,
  duration_ms bigint,
  status varchar(32) not null,
  progress integer not null default 0,
  error_summary text,
  retry_count integer not null default 0,
  target_type varchar(64),
  target_id varchar(128),
  queue_key varchar(64),
  cancel_requested boolean not null default false,
  priority integer not null default 100,
  details text,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null
);

create index idx_operations_job_status on operations_job(status, submitted_at desc);
create index idx_operations_job_type on operations_job(job_type, submitted_at desc);
create index idx_operations_job_queue on operations_job(queue_key, status, priority desc);

create table operations_queue_state (
  queue_key varchar(64) primary key,
  paused boolean not null default false,
  paused_at timestamp with time zone,
  resumed_at timestamp with time zone,
  updated_at timestamp with time zone not null,
  updated_by uuid
);

create table operations_queue_item_override (
  id uuid primary key,
  queue_key varchar(64) not null,
  item_id varchar(128) not null,
  priority integer not null default 100,
  cancelled boolean not null default false,
  updated_at timestamp with time zone not null,
  updated_by uuid,
  constraint uq_operations_queue_item_override unique (queue_key, item_id)
);

create table operations_scheduler (
  scheduler_key varchar(64) primary key,
  display_name varchar(160) not null,
  description text,
  enabled boolean not null default true,
  run_interval_seconds bigint not null,
  next_execution_at timestamp with time zone,
  last_execution_at timestamp with time zone,
  last_status varchar(32),
  updated_at timestamp with time zone not null,
  updated_by uuid
);

create table operations_scheduler_execution (
  id uuid primary key,
  scheduler_key varchar(64) not null references operations_scheduler(scheduler_key) on delete cascade,
  triggered_by uuid,
  trigger_source varchar(32) not null,
  status varchar(32) not null,
  details text,
  started_at timestamp with time zone not null,
  completed_at timestamp with time zone
);

create index idx_operations_scheduler_execution_scheduler
  on operations_scheduler_execution(scheduler_key, started_at desc);

create table operations_metric (
  id uuid primary key,
  metric_group varchar(64) not null,
  metric_key varchar(120) not null,
  metric_value numeric(18,4) not null,
  metric_unit varchar(32),
  dimensions_json text,
  recorded_at timestamp with time zone not null
);

create index idx_operations_metric_group_key
  on operations_metric(metric_group, metric_key, recorded_at desc);

insert into operations_scheduler (scheduler_key, display_name, description, enabled, run_interval_seconds, next_execution_at, last_execution_at, last_status, updated_at)
values
  ('nightly-reindex', 'Nightly Reindex', 'Refresh retrieval projections across active customer knowledge.', true, 86400, now() + interval '1 day', null, null, now()),
  ('retention-evaluation', 'Retention Evaluation', 'Evaluate retention, disposal, and legal-hold eligibility.', true, 43200, now() + interval '12 hours', null, null, now()),
  ('embedding-refresh', 'Embedding Refresh', 'Regenerate stale embeddings for retrieval readiness.', true, 21600, now() + interval '6 hours', null, null, now()),
  ('quality-recalculation', 'Quality Recalculation', 'Recompute knowledge quality signals and stewardship queues.', true, 21600, now() + interval '6 hours', null, null, now()),
  ('orphan-cleanup', 'Orphan Cleanup', 'Identify orphaned projections, files, and queue records for cleanup.', true, 86400, now() + interval '1 day', null, null, now()),
  ('projection-rebuild', 'Projection Rebuild', 'Run a controlled projection rebuild for operational recovery.', false, 86400, now() + interval '1 day', null, null, now());
