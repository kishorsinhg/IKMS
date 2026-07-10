INSERT INTO client (
  id,
  client_id,
  client_id_temporary,
  client_type,
  status,
  display_name,
  legal_name,
  primary_email,
  primary_phone,
  contact_person,
  city,
  state_or_region,
  country,
  created_at,
  updated_at
) VALUES (
  '11111111-1111-1111-1111-111111111111',
  'CL-ACME-001',
  FALSE,
  'BUSINESS',
  'ACTIVE',
  'Acme Manufacturing',
  'Acme Manufacturing Pvt Ltd',
  'broker-contact@acme.example',
  '+1-202-555-0101',
  'Jordan Pike',
  'Chicago',
  'Illinois',
  'USA',
  '2026-07-10T09:00:00Z',
  '2026-07-10T09:00:00Z'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO client (
  id,
  client_id,
  client_id_temporary,
  client_type,
  status,
  display_name,
  legal_name,
  primary_email,
  primary_phone,
  contact_person,
  city,
  state_or_region,
  country,
  created_at,
  updated_at
) VALUES (
  '22222222-2222-2222-2222-222222222222',
  'TMP-IND-001',
  TRUE,
  'INDIVIDUAL',
  'ACTIVE',
  'Avery Stone',
  'Avery Stone',
  'avery.stone@example.test',
  '+1-202-555-0112',
  'Avery Stone',
  'Austin',
  'Texas',
  'USA',
  '2026-07-10T09:05:00Z',
  '2026-07-10T09:05:00Z'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO note (
  id,
  client_id,
  note_text,
  status,
  created_at,
  updated_at
) VALUES (
  '33333333-3333-3333-3333-333333333333',
  '11111111-1111-1111-1111-111111111111',
  'Seed note for quickstart validation: renewal discussion scheduled with the broker team.',
  'ACTIVE',
  '2026-07-10T09:10:00Z',
  '2026-07-10T09:10:00Z'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO document_type (
  id,
  name,
  description,
  active,
  created_at
) VALUES (
  '44444444-4444-4444-4444-444444444444',
  'Policy Schedule',
  'Seed document type for broker policy documents.',
  TRUE,
  '2026-07-10T09:00:00Z'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO metadata_field (
  id,
  field_key,
  label,
  pii,
  active,
  created_at
) VALUES (
  '55555555-5555-5555-5555-555555555555',
  'insured_email',
  'Insured Email',
  TRUE,
  TRUE,
  '2026-07-10T09:00:00Z'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO document (
  id,
  client_id,
  document_type_id,
  title,
  current_version_id,
  processing_status,
  review_status,
  client_match_confidence,
  classification_confidence,
  extraction_confidence,
  source,
  created_at
) VALUES (
  '66666666-6666-6666-6666-666666666666',
  '11111111-1111-1111-1111-111111111111',
  '44444444-4444-4444-4444-444444444444',
  'Acme Policy Schedule 2026',
  '77777777-7777-7777-7777-777777777777',
  'READY',
  'APPROVED',
  0.9800,
  0.9400,
  0.9300,
  'MANUAL_UPLOAD',
  '2026-07-10T09:20:00Z'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO document_version (
  id,
  document_id,
  version_number,
  file_hash,
  file_name,
  mime_type,
  file_size_bytes,
  original_storage_path,
  redacted_storage_path,
  extracted_text,
  ocr_provider,
  embedding_model,
  language,
  redaction_status,
  is_current,
  created_at
) VALUES (
  '77777777-7777-7777-7777-777777777777',
  '66666666-6666-6666-6666-666666666666',
  1,
  'seed-hash-acme-policy-v1',
  'acme-policy-schedule.pdf',
  'application/pdf',
  204800,
  'seed/originals/acme-policy-schedule.pdf',
  'seed/redacted/acme-policy-schedule.pdf',
  'Policy renewal due 2026-08-31 with insured email broker-contact@acme.example.',
  'tesseract',
  'seed-keyword-index',
  'en',
  'REDACTED',
  TRUE,
  '2026-07-10T09:20:00Z'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO email_item (
  id,
  client_id,
  message_id,
  thread_id,
  subject,
  sender,
  recipients,
  received_at,
  body_text,
  processing_status,
  review_status,
  created_at
) VALUES (
  '88888888-8888-8888-8888-888888888888',
  '11111111-1111-1111-1111-111111111111',
  'seed-message-acme-001',
  'seed-thread-acme-001',
  'Acme renewal reminder',
  'servicing@carrier.example',
  'broker@ikms.local',
  '2026-07-10T08:45:00Z',
  'Please review the attached renewal schedule and confirm any underwriting changes.',
  'INDEXED',
  'APPROVED',
  '2026-07-10T08:45:00Z'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO embedding_chunk (
  id,
  client_id,
  source_type,
  source_id,
  chunk_text,
  embedding_reference,
  created_at
) VALUES (
  '99999999-9999-9999-9999-999999999999',
  '11111111-1111-1111-1111-111111111111',
  'DOCUMENT',
  '66666666-6666-6666-6666-666666666666',
  'Acme Policy Schedule 2026 renewal due 2026-08-31 with servicing contact details.',
  'seed-embedding-acme-001',
  '2026-07-10T09:25:00Z'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO ai_interaction (
  id,
  client_id,
  question,
  answer,
  status,
  cited_sources,
  helpful_feedback,
  feedback_comment,
  created_at
) VALUES (
  'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
  '11111111-1111-1111-1111-111111111111',
  'When is the Acme renewal due?',
  'The current policy schedule shows the renewal is due on 2026-08-31.',
  'Answered',
  'Document: Acme Policy Schedule 2026',
  TRUE,
  'Seed interaction for search and AI validation.',
  '2026-07-10T09:30:00Z'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO review_setting (
  id,
  mode,
  low_confidence_threshold,
  updated_at
) VALUES (
  'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
  'confidence',
  0.75,
  '2026-07-10T09:00:00Z'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO ai_provider_setting (
  id,
  provider_name,
  model_name,
  ocr_provider,
  active,
  updated_at
) VALUES (
  'cccccccc-cccc-cccc-cccc-cccccccccccc',
  'mistral',
  'mistral-small',
  'tesseract',
  TRUE,
  '2026-07-10T09:00:00Z'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO audit_log (
  id,
  occurred_at,
  retained_until,
  actor_user_id,
  actor_username,
  client_id,
  category,
  action,
  outcome,
  target_type,
  target_id,
  pii_access,
  details
) VALUES (
  'dddddddd-dddd-dddd-dddd-dddddddddddd',
  '2026-07-10T09:35:00Z',
  '2033-07-09T09:35:00Z',
  NULL,
  'seed-system',
  '11111111-1111-1111-1111-111111111111',
  'SEARCH',
  'CLIENT_SEARCH',
  'SUCCESS',
  'Client',
  '11111111-1111-1111-1111-111111111111',
  FALSE,
  '{"query":"renewal","seed":"true"}'
) ON CONFLICT (id) DO NOTHING;
