"use client";

import { useEffect, useState } from "react";
import {
  getOperationsSummary,
  HttpMetricItemResponse,
  listOperationsHttpMetrics,
  OperationsSummaryResponse
} from "@/shared/lib/api/client";
import { StatusCard } from "@/shared/ui/StatusCard";

export function OperationsPanel() {
  const [summary, setSummary] = useState<OperationsSummaryResponse | null>(null);
  const [metrics, setMetrics] = useState<HttpMetricItemResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    void loadOperations();
  }, []);

  const loadOperations = async () => {
    setIsLoading(true);
    setErrorMessage("");
    try {
      const [latestSummary, latestMetrics] = await Promise.all([
        getOperationsSummary(),
        listOperationsHttpMetrics()
      ]);
      setSummary(latestSummary);
      setMetrics(latestMetrics.items);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "운영 메트릭을 불러오지 못했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <section style={panelStyle}>
      <div style={headerStyle}>
        <div>
          <h2 style={{ margin: 0 }}>운영 안정화</h2>
          <p style={captionStyle}>Week 11 릴리스 게이트용 health, p95, 오류율, 권한 거부 현황을 보여줍니다.</p>
        </div>
        <button type="button" onClick={() => void loadOperations()} style={buttonStyle} disabled={isLoading}>
          {isLoading ? "갱신 중..." : "운영 메트릭 갱신"}
        </button>
      </div>

      {summary ? (
        <div style={cardGridStyle}>
          <StatusCard title="서비스 상태" value={summary.serviceStatus} description="Actuator health 기반 상태" />
          <StatusCard
            title="읽기 p95"
            value={formatLatency(summary.coreReadP95Ms)}
            description="GET /v1/projects/{projectId}"
          />
          <StatusCard
            title="쓰기 p95"
            value={formatLatency(summary.coreWriteP95Ms)}
            description="POST /v1/projects"
          />
          <StatusCard
            title="5xx 오류율"
            value={`${summary.serverErrorRate.toFixed(2)}%`}
            description={`최근 샘플 ${summary.requestCount}건 기준`}
          />
          <StatusCard
            title="권한 거부"
            value={`${summary.deniedCountLast15m}건`}
            description="최근 15분 denied audit 집계"
          />
        </div>
      ) : null}

      <div style={{ marginTop: 18 }}>
        <h3 style={{ marginBottom: 12 }}>원시 HTTP 메트릭</h3>
        <div style={listStyle}>
          {metrics.map((metric) => (
            <article key={`${metric.method}-${metric.uri}`} style={metricRowStyle}>
              <div>
                <strong>
                  {metric.method} {metric.uri}
                </strong>
                <p style={metricMetaStyle}>
                  count={metric.count} max={metric.maxMs.toFixed(2)}ms p95={formatLatency(metric.p95Ms)} error={metric.errorCount}
                </p>
              </div>
            </article>
          ))}
          {!isLoading && !metrics.length && !errorMessage ? (
            <p style={emptyStyle}>아직 수집된 메트릭이 없습니다. 핵심 API를 몇 번 호출한 뒤 다시 갱신하세요.</p>
          ) : null}
        </div>
      </div>

      {errorMessage ? <p style={errorStyle}>{errorMessage}</p> : null}
    </section>
  );
}

function formatLatency(value: number | null): string {
  return value == null ? "n/a" : `${value.toFixed(2)}ms`;
}

const panelStyle: React.CSSProperties = {
  background: "#ffffff",
  borderRadius: 16,
  padding: 20,
  boxShadow: "0 10px 20px rgba(11, 34, 68, 0.08)",
  gridColumn: "1 / -1"
};

const headerStyle: React.CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  gap: 12
};

const captionStyle: React.CSSProperties = {
  marginTop: 8,
  marginBottom: 0,
  color: "#5d6b7d",
  fontSize: 13
};

const buttonStyle: React.CSSProperties = {
  minWidth: 150,
  height: 40,
  border: "none",
  borderRadius: 10,
  background: "#123f9f",
  color: "#ffffff",
  fontWeight: 600,
  cursor: "pointer"
};

const cardGridStyle: React.CSSProperties = {
  display: "grid",
  gap: 12,
  marginTop: 18,
  gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))"
};

const listStyle: React.CSSProperties = {
  display: "grid",
  gap: 10
};

const metricRowStyle: React.CSSProperties = {
  border: "1px solid #d8e1ee",
  borderRadius: 12,
  padding: 14
};

const metricMetaStyle: React.CSSProperties = {
  marginTop: 6,
  marginBottom: 0,
  color: "#5d6b7d",
  fontSize: 13
};

const emptyStyle: React.CSSProperties = {
  margin: 0,
  color: "#5d6b7d",
  fontSize: 13
};

const errorStyle: React.CSSProperties = {
  color: "#d22c2c",
  marginTop: 12,
  marginBottom: 0,
  fontSize: 13
};
