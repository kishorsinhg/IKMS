import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { getCurrentUser } from "../../api/auth";
import { createNote, getClient, listNotes } from "../../api/clients";
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

  if (!clientId) {
    return (
      <section style={{ display: "grid", gap: "1rem" }}>
        <header>
          <h2 style={{ marginBottom: "0.35rem" }}>Client Profile</h2>
          <p style={{ margin: 0, color: "#6d6253" }}>
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

  return (
    <section style={{ display: "grid", gap: "1.5rem" }}>
      <header>
        <h2 style={{ marginBottom: "0.35rem" }}>Client Profile</h2>
        <p style={{ margin: 0, color: "#6d6253" }}>
          {client.displayName} · {client.clientId}{client.clientIdTemporary ? " (Temporary ID)" : ""}
        </p>
      </header>

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
                <span>{note.noteText}</span>
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
  gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
  gap: "1rem",
};

const cardStyle: React.CSSProperties = {
  padding: "1rem",
  borderRadius: "1rem",
  background: "#fff8ee",
  border: "1px solid rgba(31, 28, 24, 0.1)",
};

const noteCardStyle: React.CSSProperties = {
  display: "grid",
  gap: "0.35rem",
  padding: "0.85rem",
  borderRadius: "0.85rem",
  background: "#f7efe0",
};

const buttonStyle: React.CSSProperties = {
  width: "fit-content",
  padding: "0.7rem 1rem",
  borderRadius: "999px",
  border: "none",
  background: "#1f1c18",
  color: "#fffaf0",
  fontWeight: 700,
  cursor: "pointer",
};

const backLinkStyle: React.CSSProperties = {
  width: "fit-content",
  color: "#1f1c18",
  fontWeight: 700,
  textDecoration: "none",
};
