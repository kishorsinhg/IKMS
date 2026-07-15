create table document_processing_job (
  id uuid primary key,
  document_id uuid not null references document(id) on delete cascade,
  document_version_id uuid not null references document_version(id) on delete cascade,
  client_id uuid,
  status varchar(32) not null,
  current_stage varchar(48) not null,
  language varchar(32),
  ocr_provider varchar(120),
  classification_provider varchar(120),
  overall_confidence numeric(5,4),
  ocr_confidence numeric(5,4),
  classification_confidence numeric(5,4),
  metadata_confidence numeric(5,4),
  business_reference_confidence numeric(5,4),
  validation_confidence numeric(5,4),
  duplicate_confidence numeric(5,4),
  retry_count integer not null default 0,
  last_error_code varchar(64),
  last_error_message text,
  reviewer_comment text,
  started_at timestamp with time zone,
  review_requested_at timestamp with time zone,
  approved_at timestamp with time zone,
  rejected_at timestamp with time zone,
  published_at timestamp with time zone,
  completed_at timestamp with time zone,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null
);

create index idx_document_processing_job_document on document_processing_job(document_id, created_at desc);
create index idx_document_processing_job_status on document_processing_job(status, current_stage);

create table document_processing_field (
  id uuid primary key,
  job_id uuid not null references document_processing_job(id) on delete cascade,
  field_key varchar(120) not null,
  field_label varchar(160) not null,
  field_type varchar(48) not null,
  business_reference_type varchar(48),
  extracted_value text,
  corrected_value text,
  approved_value text,
  confidence numeric(5,4),
  source_type varchar(64) not null,
  extraction_method varchar(64) not null,
  source_page integer,
  required_flag boolean not null default false,
  validation_state varchar(32) not null,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null
);

create index idx_document_processing_field_job on document_processing_field(job_id, field_key);

create table document_processing_finding (
  id uuid primary key,
  job_id uuid not null references document_processing_job(id) on delete cascade,
  finding_code varchar(96) not null,
  severity varchar(16) not null,
  stage varchar(48) not null,
  field_key varchar(120),
  message text not null,
  evidence_text text,
  source_page integer,
  confidence numeric(5,4),
  status varchar(16) not null,
  resolution_comment text,
  created_at timestamp with time zone not null,
  resolved_at timestamp with time zone
);

create index idx_document_processing_finding_job on document_processing_finding(job_id, stage, severity);
