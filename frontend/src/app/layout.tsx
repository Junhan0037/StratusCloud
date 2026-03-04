import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "StratusCloud Console",
  description: "StratusCloud 운영 콘솔"
};

// 모든 페이지에 공통 레이아웃을 적용한다.
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
