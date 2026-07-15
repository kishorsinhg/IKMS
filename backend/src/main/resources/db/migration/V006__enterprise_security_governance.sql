alter table app_user
  add column if not exists business_unit varchar(120),
  add column if not exists department varchar(120),
  add column if not exists region varchar(120),
  add column if not exists country varchar(120),
  add column if not exists broker_office varchar(120),
  add column if not exists employment_role varchar(120),
  add column if not exists security_clearance varchar(64) not null default 'INTERNAL';

alter table document
  add column if not exists classification varchar(64) not null default 'INTERNAL',
  add column if not exists sensitivity_level varchar(64) not null default 'MODERATE',
  add column if not exists confidentiality varchar(120),
  add column if not exists data_residency varchar(120),
  add column if not exists lifecycle_state varchar(64) not null default 'ACTIVE',
  add column if not exists export_restricted boolean not null default false,
  add column if not exists watermark_required boolean not null default false,
  add column if not exists business_unit varchar(120),
  add column if not exists department varchar(120),
  add column if not exists region varchar(120),
  add column if not exists country varchar(120),
  add column if not exists broker_office varchar(120);

alter table retention_record
  add column if not exists hold_type varchar(64),
  add column if not exists retention_policy_key varchar(120),
  add column if not exists review_at timestamptz,
  add column if not exists archival_eligible_at timestamptz,
  add column if not exists disposal_eligible_at timestamptz;

create index if not exists idx_document_classification on document (classification);
create index if not exists idx_document_lifecycle_state on document (lifecycle_state);
create index if not exists idx_retention_record_legal_hold on retention_record (legal_hold, updated_at desc);
