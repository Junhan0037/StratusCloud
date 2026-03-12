import { StatusCard } from "@/shared/ui/StatusCard";

const cards = [
  { title: "API 상태", value: "정상", description: "백엔드 health-check와 연결 예정" },
  { title: "활성 프로젝트", value: "0", description: "IAM/프로젝트 모듈 연동 예정" },
  { title: "오늘 이벤트", value: "0", description: "Audit 로그 파이프라인 연동 예정" }
];

// 1주차 MVP 기준의 콘솔 랜딩 페이지다.
export default function HomePage() {
  return (
    <div className="space-y-6">
      <div className="flex flex-col space-y-2">
        <h1 className="text-2xl font-bold tracking-tight text-slate-900">StratusCloud Console</h1>
        <p className="text-slate-500 max-w-2xl">
          IAM, Compute, Network, Storage, Governance를 일관된 운영 모델로 제공하는 클라우드 관리 콘솔.
        </p>
        <div className="pt-2">
          <a
            href="/console/projects"
            className="inline-flex items-center text-sm font-semibold text-indigo-600 hover:text-indigo-500"
          >
            Week 3 IAM 콘솔로 이동 &rarr;
          </a>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {cards.map((card) => (
          <StatusCard key={card.title} title={card.title} value={card.value} description={card.description} />
        ))}
      </div>
    </div>
  );
}
