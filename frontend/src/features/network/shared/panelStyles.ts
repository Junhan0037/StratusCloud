import { CSSProperties } from "react";

export const panelStyle: CSSProperties = {
  background: "#ffffff",
  borderRadius: 16,
  padding: 20,
  boxShadow: "0 10px 20px rgba(11, 34, 68, 0.08)"
};

export const inputStyle: CSSProperties = {
  height: 40,
  border: "1px solid #d8e1ee",
  borderRadius: 10,
  padding: "0 12px"
};

export const textareaStyle: CSSProperties = {
  border: "1px solid #d8e1ee",
  borderRadius: 10,
  padding: "10px 12px",
  resize: "vertical"
};

export const buttonStyle: CSSProperties = {
  height: 40,
  border: "none",
  borderRadius: 10,
  background: "#0f4dc2",
  color: "#ffffff",
  fontWeight: 600,
  cursor: "pointer",
  padding: "0 12px"
};

export const secondaryButtonStyle: CSSProperties = {
  height: 40,
  border: "1px solid #b5c5dc",
  borderRadius: 10,
  background: "#ffffff",
  color: "#183961",
  fontWeight: 600,
  cursor: "pointer",
  padding: "0 12px"
};

export const dangerButtonStyle: CSSProperties = {
  border: "none",
  borderRadius: 8,
  background: "#d3415d",
  color: "#ffffff",
  fontWeight: 600,
  cursor: "pointer",
  padding: "6px 10px"
};

export const inlineButtonStyle: CSSProperties = {
  border: "none",
  borderRadius: 8,
  background: "#e8eefc",
  color: "#0f4dc2",
  fontWeight: 600,
  cursor: "pointer",
  padding: "6px 10px"
};

export const subTextStyle: CSSProperties = {
  marginTop: 6,
  color: "#5d6b7d",
  fontSize: 14
};

export const errorStyle: CSSProperties = {
  marginTop: 12,
  color: "#b42318",
  fontWeight: 600
};

export const gridStyle: CSSProperties = {
  display: "grid",
  gap: 8,
  gridTemplateColumns: "repeat(2, minmax(0, 1fr))"
};
