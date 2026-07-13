import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { ui } from "../../app/ui";
import { getCurrentUser } from "../../api/auth";
import { createNote, deleteNote, getClient, listNotes, updateNote } from "../../api/clients";
import { listClientDocuments, listClientEmails } from "../../api/intake";
import { ClientSearchPanel } from "../search/ClientSearchPanel";
import { DocumentsSection, EmailsSection } from "./knowledge/KnowledgeSections";

const clientQueryKey = (clientId: string) => ["clients", "profile", clientId];
const notesQueryKey = (clientId: string) => ["clients", clientId, "notes"];
const documentsQueryKey = (clientId: string) => ["clients", clientId, "documents"];
const emailsQueryKey = (clientId: string) => ["clients", clientId, "emails"];

export function ClientProfilePage() {
  const { clientId } = useParams();
  const queryClient = useQueryClient();
  const [noteText, setNoteText] = useState("");
  const [editingNoteId, setEditingNoteId] = useState<string | null>(null);
  const [editingNoteText, setEditingNoteText] = useState("");
  const currentUserQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: getCurrentUser,
    retry: false,
  });

  const clientQuery = useQuery({
    queryKey: clientId ? clientQueryKey(clientId) : ["clients", "profile", "empty"],
    queryFn: () => getClient(clientId!),
    enabled: Boolean(clientId),
  });
  const notesQuery = useQuery({
    queryKey: clientId ? notesQueryKey(clientId) : ["clients", "notes", "empty"],
    queryFn: () => listNotes(clientId!),
    enabled: Boolean(clientId),
  });
  const documentsQuery = useQuery({
    queryKey: clientId ? documentsQueryKey(clientId) : ["clients", "documents", "empty"],
    queryFn: () => listClientDocuments(clientId!),
    enabled: Boolean(clientId),
  });
  const emailsQuery = useQuery({
    queryKey: clientId ? emailsQueryKey(clientId) : ["clients", "emails", "empty"],
    queryFn: () => listClientEmails(clientId!),
    enabled: Boolean(clientId),
  });
  const noteMutation = useMutation({
    mutationFn: (text: string) => createNote(clientId!, { noteText: text }),
    onSuccess: async () => {
      setNoteText("");
      await queryClient.invalidateQueries({ queryKey: notesQueryKey(clientId!) });
    },
  });
  const updateNoteMutation = useMutation({
    mutationFn: ({ noteId, text }: { noteId: string; text: string }) => updateNote(noteId, { noteText: text }),
    onSuccess: async () => {
      setEditingNoteId(null);
      setEditingNoteText("");
      await queryClient.invalidateQueries({ queryKey: notesQueryKey(clientId!) });
    },
  });
  const deleteNoteMutation = useMutation({
    mutationFn: (noteId: string) => deleteNote(noteId),
    onSuccess: async () => {
      setEditingNoteId(null);
      setEditingNoteText("");
      await queryClient.invalidateQueries({ queryKey: notesQueryKey(clientId!) });
    },
  });

  if (!clientId) {
    return (
      <section style={ui.page}>
        <header style={ui.pageHeader}>
          <h2 style={ui.pageTitle}>Client Profile</h2>
          <p style={ui.pageDescription}>
            The client workspace is the central broker view for linked documents, emails, notes, AI Q&A, and activity.
          </p>
        </header>
        <Link to="/clients" style={backLinkStyle}>Return to client workspace</Link>
        <div style={sectionGrid}>
          <ProfileSection title="Client Profile" description="Core identity, contact, and account fields." />
          <ProfileSection title="Documents" description="Client-linked document knowledge and versions." />
          <ProfileSection title="Emails" description="Mailbox and attachment history for the selected client." />
          <ProfileSection title="Notes" description="Manual broker notes and follow-up context." />
          <ProfileSection title="AI Q&A" description="Client-scoped evidence-based answers only." />
          <ProfileSection title="Audit/Activity" description="Client-level audit and operational events." />
        </div>
      </section>
    );
  }

  if (clientQuery.isLoading || notesQuery.isLoading || documentsQuery.isLoading || emailsQuery.isLoading || currentUserQuery.isLoading) {
    return <div>Loading client profile...</div>;
  }

  if (clientQuery.isError || currentUserQuery.isError) {
    return <div>Unable to load the selected client profile.</div>;
  }

  const client = clientQuery.data!;
  const currentUser = currentUserQuery.data!;

  function handleCreateNote(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!noteText.trim()) {
      return;
    }
    noteMutation.mutate(noteText.trim());
  }

  function startEditing(noteId: string, text: string) {
    setEditingNoteId(noteId);
    setEditingNoteText(text);
  }

  function handleUpdateNote(event: FormEvent<HTMLFormElement>, noteId: string) {
    event.preventDefault();
    if (!editingNoteText.trim()) {
      return;
    }
    updateNoteMutation.mutate({ noteId, text: editingNoteText.trim() });
  }

  return (
    <section style={ui.page}>
      <section style={heroStyle}>
        <div style={ui.pageHeader}>
          <h2 style={ui.pageTitle}>Client Profile</h2>
          <p style={ui.pageDescription}>
            {client.displayName} · {client.clientId}{client.clientIdTemporary ? " (Temporary ID)" : ""}
          </p>
        </div>
        <div style={ui.metricRow}>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{client.clientType}</strong>
            <span style={metricLabelStyle}>Client type</span>
          </div>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{client.status}</strong>
            <span style={metricLabelStyle}>Record status</span>
          </div>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{documentsQuery.data?.length ?? 0}</strong>
            <span style={metricLabelStyle}>Documents</span>
          </div>
          <div style={ui.metricCard}>
            <strong style={metricValueStyle}>{emailsQuery.data?.length ?? 0}</strong>
            <span style={metricLabelStyle}>Emails</span>
          </div>
        </div>
      </section>

      <Link to="/clients" style={backLinkStyle}>Return to client workspace</Link>

      <div style={sectionGrid}>
        <ProfileSection
          title="Client Profile"
          description={`${client.clientType} · ${client.status}${client.primaryEmail ? ` · ${client.primaryEmail}` : ""}`}
        />
        <DocumentsSection documents={documentsQuery.data} permissions={currentUser.permissions} />
        <EmailsSection emails={emailsQuery.data} />
        <section style={cardStyle}>
          <h3 style={{ marginTop: 0 }}>Notes</h3>
          <form onSubmit={handleCreateNote} style={{ display: "grid", gap: "0.75rem", marginBottom: "1rem" }}>
            <textarea
              rows={4}
              value={noteText}
              onChange={(event) => setNoteText(event.target.value)}
              placeholder="Add a broker note"
            />
            <button type="submit" style={buttonStyle} disabled={noteMutation.isPending}>
              {noteMutation.isPending ? "Saving..." : "Add note"}
            </button>
          </form>
          <div style={{ display: "grid", gap: "0.75rem" }}>
            {notesQuery.data?.map((note) => (
              <div key={note.id} style={noteCardStyle}>
                <strong>{new Date(note.createdAt).toLocaleString()}</strong>
                {editingNoteId === note.id ? (
                  <form onSubmit={(event) => handleUpdateNote(event, note.id)} style={noteEditFormStyle}>
                    <textarea
                      rows={3}
                      value={editingNoteText}
                      onChange={(event) => setEditingNoteText(event.target.value)}
                    />
                    <div style={noteActionRowStyle}>
                      <button type="submit" style={buttonStyle} disabled={updateNoteMutation.isPending}>
                        {updateNoteMutation.isPending ? "Saving..." : "Save note"}
                      </button>
                      <button
                        type="button"
                        style={secondaryButtonStyle}
                        onClick={() => {
                          setEditingNoteId(null);
                          setEditingNoteText("");
                        }}
                      >
                        Cancel
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <span>{note.noteText}</span>
                    <div style={noteActionRowStyle}>
                      <button type="button" style={secondaryButtonStyle} onClick={() => startEditing(note.id, note.noteText)}>
                        Edit note
                      </button>
                      <button
                        type="button"
                        style={dangerButtonStyle}
                        disabled={deleteNoteMutation.isPending}
                        onClick={() => deleteNoteMutation.mutate(note.id)}
                      >
                        Delete note
                      </button>
                    </div>
                  </>
                )}
              </div>
            ))}
            {notesQuery.data?.length === 0 ? <span>No notes yet.</span> : null}
          </div>
        </section>
        <ClientSearchPanel clientId={clientId} />
        <ProfileSection title="Audit/Activity" description="Client-level audit and operational events." />
      </div>
    </section>
  );
}

function ProfileSection({ title, description }: { title: string; description: string }) {
  return (
    <section style={cardStyle}>
      <h3 style={{ marginTop: 0 }}>{title}</h3>
      <p style={{ marginBottom: 0 }}>{description}</p>
    </section>
  );
}

const sectionGrid: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
  gap: "1rem",
};

const cardStyle: React.CSSProperties = {
  ...ui.card,
};

const noteCardStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.35rem",
  padding: "0.95rem 1rem",
  borderRadius: "0.95rem",
  background: "var(--panel-muted)",
  border: "1px solid rgba(191, 208, 226, 0.72)",
};

const buttonStyle: React.CSSProperties = {
  ...ui.primaryButton,
};

const secondaryButtonStyle: React.CSSProperties = {
  ...ui.secondaryButton,
};

const dangerButtonStyle: React.CSSProperties = {
  ...ui.dangerButton,
};

const noteActionRowStyle: React.CSSProperties = {
  display: "flex",
  gap: "0.75rem",
  flexWrap: "wrap",
  marginTop: "0.35rem",
};

const noteEditFormStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.75rem",
};

const backLinkStyle: React.CSSProperties = {
  width: "fit-content",
  color: "var(--accent)",
  fontWeight: 700,
  textDecoration: "none",
};

const heroStyle: React.CSSProperties = {
  ...ui.heroCard,
  display: "grid",
  gap: "1.25rem",
};

const metricValueStyle: React.CSSProperties = {
  fontSize: "1.2rem",
  letterSpacing: "-0.02em",
};

const metricLabelStyle: React.CSSProperties = {
  color: "var(--muted)",
  fontSize: "0.84rem",
};
