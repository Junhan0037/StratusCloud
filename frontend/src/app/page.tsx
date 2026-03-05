import { StatusCard } from "@/shared/ui/StatusCard";

const cards = [
  { title: "API 상태", value: "정상", description: "백엔드 health-check와 연결 예정" },
  { title: "활성 프로젝트", value: "0", description: "IAM/프로젝트 모듈 연동 예정" },
  { title: "오늘 이벤트", value: "0", description: "Audit 로그 파이프라인 연동 예정" }
];

// 1주차 MVP 기준의 콘솔 랜딩 페이지다.
export default function HomePage() {
  return (
    <main style={{ maxWidth: 1120, margin: "0 auto", padding: "48px 20px" }}>
      <section style={{ marginBottom: 28 }}>
        <h1 style={{ margin: 0, fontSize: 34 }}>StratusCloud Console</h1>
        <p style={{ marginTop: 10, color: "var(--muted)", maxWidth: 680 }}>
          IAM, Compute, Network, Storage, Governance를 일관된 운영 모델로 제공하는 클라우드 관리 콘솔.
        </p>
        <a
          href="/console/projects"
          style={{
            display: "inline-block",
            marginTop: 12,
            color: "#0f4dc2",
            fontWeight: 700
          }}
        >
          Week 3 IAM 콘솔로 이동
        </a>
      </section>
      <section
        style={{
          display: "grid",
          gap: 16,
          gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))"
        }}
      >
        {cards.map((card) => (
          <StatusCard key={card.title} title={card.title} value={card.value} description={card.description} />
        ))}
      </section>
    </main>
  );
}
