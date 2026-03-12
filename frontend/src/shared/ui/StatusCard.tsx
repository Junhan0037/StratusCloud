interface StatusCardProps {
  title: string;
  value: string;
  description: string;
}

// 콘솔 대시보드에서 공통으로 재사용할 상태 카드 컴포넌트다.
export function StatusCard({ title, value, description }: StatusCardProps) {
  return (
    <div className="bg-white overflow-hidden rounded-lg shadow-sm border border-slate-200">
      <div className="px-4 py-5 sm:p-6">
        <dt className="text-sm font-medium text-slate-500 truncate">{title}</dt>
        <dd className="mt-1 text-3xl font-semibold tracking-tight text-slate-900">{value}</dd>
        <dd className="mt-2 text-sm text-slate-400">{description}</dd>
      </div>
    </div>
  );
}
