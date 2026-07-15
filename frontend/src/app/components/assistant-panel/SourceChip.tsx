import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import EmailOutlinedIcon from "@mui/icons-material/EmailOutlined";
import EventNoteOutlinedIcon from "@mui/icons-material/EventNoteOutlined";
import GroupsOutlinedIcon from "@mui/icons-material/GroupsOutlined";
import PersonOutlinedIcon from "@mui/icons-material/PersonOutlined";
import PolicyOutlinedIcon from "@mui/icons-material/PolicyOutlined";
import { Chip } from "@mui/material";
import type { AssistantSourceReference } from "./assistantTypes";

export function SourceChip({ source }: { source: AssistantSourceReference }) {
  return (
    <Chip
      size="small"
      variant="outlined"
      icon={getSourceIcon(source)}
      label={source.label}
      onClick={source.onClick}
      disabled={source.disabled ?? !source.onClick}
      aria-label={`${buildAriaPrefix(source)} source ${source.label}`}
    />
  );
}

function getSourceIcon(source: AssistantSourceReference) {
  switch (source.kind) {
    case "DOCUMENT":
      return <DescriptionOutlinedIcon fontSize="small" />;
    case "EMAIL":
      return <EmailOutlinedIcon fontSize="small" />;
    case "METADATA":
      return getSourceIconForReferenceType(source.referenceType);
    case "CUSTOMER":
      return <PersonOutlinedIcon fontSize="small" />;
    case "NOTE":
      return <GroupsOutlinedIcon fontSize="small" />;
    default:
      return <DescriptionOutlinedIcon fontSize="small" />;
  }
}

const sourceReferenceIconForMetadata = <PolicyOutlinedIcon fontSize="small" />;

function getSourceIconForReferenceType(referenceType?: AssistantSourceReference["referenceType"]) {
  if (referenceType === "claim") {
    return <EventNoteOutlinedIcon fontSize="small" />;
  }
  return sourceReferenceIconForMetadata;
}

function buildAriaPrefix(source: AssistantSourceReference) {
  if (source.kind === "METADATA" && source.referenceType) {
    return `${source.referenceType} reference`;
  }
  return source.kind.toLowerCase();
}
