import nextCoreWebVitals from "eslint-config-next/core-web-vitals";

// Next.js 16이 제공하는 Flat Config를 그대로 사용해 일관된 린트 규칙을 유지한다.
export default [
  ...nextCoreWebVitals,
  {
    ignores: [".next/**", "node_modules/**", "next-env.d.ts"]
  }
];
