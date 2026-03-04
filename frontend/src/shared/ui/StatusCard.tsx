interface StatusCardProps {
  title: string;
  value: string;
  description: string;
}

// 콘솔 대시보드에서 공통으로 재사용할 상태 카드 컴포넌트다.
export function StatusCard({ title, value, description }: StatusCardProps) {
  return (
    <article
      style={{
        background: "var(--surface)",
        borderRadius: 16,
        padding: 20,
        boxShadow: "0 10px 25px rgba(20, 43, 78, 0.08)"
      }}
    >
      <p style={{ margin: 0, color: "var(--muted)", fontSize: 14 }}>{title}</p>
      <strong style={{ display: "block", marginTop: 8, fontSize: 24 }}>{value}</strong>
      <p style={{ marginTop: 8, marginBottom: 0, color: "var(--muted)", fontSize: 13 }}>{description}</p>
    </article>
  );
}
