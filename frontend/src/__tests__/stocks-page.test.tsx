/**
 * PR #1 기능 테스트: 주식 검색 페이지 + Navbar
 *
 * Test Plan:
 * 1. /stocks 접속 시 전체 종목 목록 표시 확인
 * 2. 마켓 탭 전환 (ALL/KOSPI/KOSDAQ/NYSE/NASDAQ) 정상 동작 확인
 * 3. 종목코드·종목명 검색 후 결과 표시 확인
 * 4. 초기화 버튼으로 검색 상태 리셋 확인
 * 5. 페이지네이션 (처음/이전/다음/마지막) 동작 확인
 * 6. Navbar 주식 메뉴 항목 존재 확인
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

// --- 모킹 ---

// next/navigation
vi.mock("next/navigation", () => ({
  usePathname: () => "/stocks",
}));

// next/link
vi.mock("next/link", () => ({
  default: ({ href, children, ...props }: { href: string; children: React.ReactNode; [key: string]: unknown }) => (
    <a href={href} {...props}>{children}</a>
  ),
}));

// next-themes
vi.mock("next-themes", () => ({
  useTheme: () => ({ theme: "light", setTheme: vi.fn() }),
}));

// DataRefreshButton
vi.mock("@/components/ui/DataRefreshButton", () => ({
  DataRefreshButton: () => <button>새로고침</button>,
}));

// StockTable
vi.mock("@/components/stocks/StockTable", () => ({
  StockTable: ({ stocks }: { stocks: unknown[] }) => (
    <table data-testid="stock-table">
      <tbody>
        {stocks.map((s: unknown) => {
          const stock = s as { ticker: string; name: string };
          return (
            <tr key={stock.ticker}>
              <td>{stock.ticker}</td>
              <td>{stock.name}</td>
            </tr>
          );
        })}
      </tbody>
    </table>
  ),
}));

// useStocks hook
const mockMutate = vi.fn();
const mockUseStocks = vi.fn();

vi.mock("@/hooks/useStocks", () => ({
  useStocks: (...args: unknown[]) => mockUseStocks(...args),
}));

// 테스트용 mock 데이터
const mockStocks = [
  { ticker: "005930", name: "삼성전자", market: "KOSPI" },
  { ticker: "000660", name: "SK하이닉스", market: "KOSPI" },
  { ticker: "AAPL", name: "Apple Inc.", market: "NASDAQ" },
];

const makeMeta = (page: number, totalPages: number, totalElements: number) => ({
  page,
  totalPages,
  totalElements,
  size: 20,
});

// --- 컴포넌트 import (모킹 이후) ---
import StocksPage from "@/app/stocks/page";
import { Navbar } from "@/components/ui/Navbar";

// =========================================================

describe("PR #1 기능 테스트", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  // -------------------------------------------------------
  // TC-1: /stocks 접속 시 전체 종목 목록 표시
  // -------------------------------------------------------
  describe("TC-1: /stocks 접속 시 전체 종목 목록 표시", () => {
    it("데이터 로드 완료 후 종목 목록이 렌더링된다", () => {
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(0, 1, 3),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });

      render(<StocksPage />);

      expect(screen.getByTestId("stock-table")).toBeInTheDocument();
      expect(screen.getByText("삼성전자")).toBeInTheDocument();
      expect(screen.getByText("SK하이닉스")).toBeInTheDocument();
      expect(screen.getByText("Apple Inc.")).toBeInTheDocument();
    });

    it("총 종목 수가 헤더에 표시된다", () => {
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(0, 1, 3),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });

      render(<StocksPage />);
      expect(screen.getByText(/3개 종목/)).toBeInTheDocument();
    });

    it("초기 로딩 중에는 스피너가 표시된다", () => {
      mockUseStocks.mockReturnValue({
        stocks: undefined,
        meta: undefined,
        isLoading: true,
        error: null,
        mutate: mockMutate,
      });

      render(<StocksPage />);
      // 종목 테이블이 없어야 함
      expect(screen.queryByTestId("stock-table")).not.toBeInTheDocument();
    });

    it("에러 시 오류 메시지가 표시된다", () => {
      mockUseStocks.mockReturnValue({
        stocks: undefined,
        meta: undefined,
        isLoading: false,
        error: new Error("Network error"),
        mutate: mockMutate,
      });

      render(<StocksPage />);
      expect(screen.getByText("데이터를 불러오지 못했습니다.")).toBeInTheDocument();
    });
  });

  // -------------------------------------------------------
  // TC-2: 마켓 탭 전환 (ALL/KOSPI/KOSDAQ/NYSE/NASDAQ)
  // -------------------------------------------------------
  describe("TC-2: 마켓 탭 전환", () => {
    beforeEach(() => {
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(0, 1, 3),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });
    });

    it("기본 탭은 전체(ALL)이다", () => {
      render(<StocksPage />);
      // 첫 번째 useStocks 호출이 market='ALL'로 이루어졌는지 확인
      expect(mockUseStocks).toHaveBeenCalledWith("ALL", undefined, 0, 20);
    });

    it.each(["KOSPI", "KOSDAQ", "NYSE", "NASDAQ"])(
      "탭 %s 클릭 시 해당 market으로 useStocks가 호출된다",
      async (tab) => {
        render(<StocksPage />);
        const tabButton = screen.getByText(tab);
        await userEvent.click(tabButton);

        // 탭 변경 후 page=0으로 리셋되어 호출되어야 함
        expect(mockUseStocks).toHaveBeenCalledWith(tab, undefined, 0, 20);
      }
    );

    it("탭 전환 시 page가 0으로 리셋된다", async () => {
      // page=2 상태 시뮬레이션을 위해 먼저 totalPages=5로 세팅
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(2, 5, 100),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });

      render(<StocksPage />);

      // 다음 버튼 클릭해서 page 1로 이동
      const nextBtn = screen.getByText("다음");
      await userEvent.click(nextBtn);

      // KOSPI 탭 클릭
      await userEvent.click(screen.getByText("KOSPI"));

      // page=0으로 호출되었어야 함
      const calls = mockUseStocks.mock.calls;
      const lastCall = calls[calls.length - 1];
      expect(lastCall[0]).toBe("KOSPI");
      expect(lastCall[2]).toBe(0);
    });
  });

  // -------------------------------------------------------
  // TC-3: 검색 기능
  // -------------------------------------------------------
  describe("TC-3: 종목코드·종목명 검색", () => {
    beforeEach(() => {
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(0, 1, 3),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });
    });

    it("검색어 입력 후 검색 버튼 클릭 시 query가 적용된다", async () => {
      render(<StocksPage />);

      const input = screen.getByPlaceholderText("종목코드 또는 종목명 검색");
      await userEvent.type(input, "삼성");

      const searchBtn = screen.getByText("검색");
      await userEvent.click(searchBtn);

      expect(mockUseStocks).toHaveBeenCalledWith("ALL", "삼성", 0, 20);
    });

    it("검색어 입력 후 Enter 키로도 검색된다", async () => {
      render(<StocksPage />);

      const input = screen.getByPlaceholderText("종목코드 또는 종목명 검색");
      await userEvent.type(input, "AAPL{Enter}");

      expect(mockUseStocks).toHaveBeenCalledWith("ALL", "AAPL", 0, 20);
    });

    it("검색 시 page가 0으로 리셋된다", async () => {
      render(<StocksPage />);

      const input = screen.getByPlaceholderText("종목코드 또는 종목명 검색");
      await userEvent.type(input, "테스트{Enter}");

      const calls = mockUseStocks.mock.calls;
      const lastCall = calls[calls.length - 1];
      expect(lastCall[2]).toBe(0); // page=0
    });

    it("공백만 입력한 경우 검색어가 trim되어 빈 값으로 처리된다", async () => {
      render(<StocksPage />);

      const input = screen.getByPlaceholderText("종목코드 또는 종목명 검색");
      await userEvent.type(input, "   {Enter}");

      // trim 결과 빈 문자열 → undefined로 전달
      expect(mockUseStocks).toHaveBeenCalledWith("ALL", undefined, 0, 20);
    });
  });

  // -------------------------------------------------------
  // TC-4: 초기화 버튼
  // -------------------------------------------------------
  describe("TC-4: 초기화 버튼", () => {
    beforeEach(() => {
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(0, 1, 3),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });
    });

    it("검색어 없을 때 초기화 버튼이 표시되지 않는다", () => {
      render(<StocksPage />);
      expect(screen.queryByText("초기화")).not.toBeInTheDocument();
    });

    it("검색어 입력 시 초기화 버튼이 나타난다", async () => {
      render(<StocksPage />);

      const input = screen.getByPlaceholderText("종목코드 또는 종목명 검색");
      await userEvent.type(input, "삼성");

      expect(screen.getByText("초기화")).toBeInTheDocument();
    });

    it("초기화 버튼 클릭 시 검색어와 query가 모두 리셋된다", async () => {
      render(<StocksPage />);

      const input = screen.getByPlaceholderText("종목코드 또는 종목명 검색");
      await userEvent.type(input, "삼성{Enter}");

      // 초기화 클릭
      await userEvent.click(screen.getByText("초기화"));

      // 입력 필드가 비어있어야 함
      expect(input).toHaveValue("");

      // 초기화 후 query=undefined, page=0 으로 useStocks 호출되어야 함
      const calls = mockUseStocks.mock.calls;
      const lastCall = calls[calls.length - 1];
      expect(lastCall[1]).toBeUndefined(); // query
      expect(lastCall[2]).toBe(0); // page
    });

    it("초기화 후 초기화 버튼이 사라진다", async () => {
      render(<StocksPage />);

      const input = screen.getByPlaceholderText("종목코드 또는 종목명 검색");
      await userEvent.type(input, "삼성");
      await userEvent.click(screen.getByText("초기화"));

      expect(screen.queryByText("초기화")).not.toBeInTheDocument();
    });
  });

  // -------------------------------------------------------
  // TC-5: 페이지네이션
  // -------------------------------------------------------
  describe("TC-5: 페이지네이션", () => {
    it("totalPages가 1 이하면 페이지네이션이 표시되지 않는다", () => {
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(0, 1, 3),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });

      render(<StocksPage />);
      expect(screen.queryByText("처음")).not.toBeInTheDocument();
    });

    it("totalPages > 1이면 페이지네이션 버튼이 4개 모두 표시된다", () => {
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(0, 5, 100),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });

      render(<StocksPage />);
      expect(screen.getByText("처음")).toBeInTheDocument();
      expect(screen.getByText("이전")).toBeInTheDocument();
      expect(screen.getByText("다음")).toBeInTheDocument();
      expect(screen.getByText("마지막")).toBeInTheDocument();
    });

    it("첫 페이지(0)에서 처음·이전 버튼이 비활성화된다", () => {
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(0, 5, 100),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });

      render(<StocksPage />);
      expect(screen.getByText("처음")).toBeDisabled();
      expect(screen.getByText("이전")).toBeDisabled();
      expect(screen.getByText("다음")).not.toBeDisabled();
      expect(screen.getByText("마지막")).not.toBeDisabled();
    });

    it("마지막 버튼 클릭 후 다음·마지막 버튼이 비활성화된다", async () => {
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(0, 5, 100),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });

      render(<StocksPage />);
      // "마지막" 버튼 클릭 → page state = totalPages-1 = 4
      await userEvent.click(screen.getByText("마지막"));

      expect(screen.getByText("처음")).not.toBeDisabled();
      expect(screen.getByText("이전")).not.toBeDisabled();
      expect(screen.getByText("다음")).toBeDisabled();
      expect(screen.getByText("마지막")).toBeDisabled();
    });

    it("다음 버튼 클릭 시 page+1로 이동한다", async () => {
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(0, 5, 100),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });

      render(<StocksPage />);
      await userEvent.click(screen.getByText("다음"));

      const calls = mockUseStocks.mock.calls;
      const lastCall = calls[calls.length - 1];
      expect(lastCall[2]).toBe(1);
    });

    it("마지막 버튼 클릭 시 마지막 페이지(totalPages-1)로 이동한다", async () => {
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(0, 5, 100),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });

      render(<StocksPage />);
      await userEvent.click(screen.getByText("마지막"));

      const calls = mockUseStocks.mock.calls;
      const lastCall = calls[calls.length - 1];
      expect(lastCall[2]).toBe(4); // totalPages - 1 = 4
    });

    it("처음 버튼 클릭 시 page=0으로 이동한다", async () => {
      // 중간 페이지부터 시작
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(3, 5, 100),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });

      render(<StocksPage />);
      await userEvent.click(screen.getByText("처음"));

      const calls = mockUseStocks.mock.calls;
      const lastCall = calls[calls.length - 1];
      expect(lastCall[2]).toBe(0);
    });

    it("현재 페이지/전체 페이지가 표시된다", async () => {
      mockUseStocks.mockReturnValue({
        stocks: mockStocks,
        meta: makeMeta(0, 5, 100),
        isLoading: false,
        error: null,
        mutate: mockMutate,
      });

      const { container } = render(<StocksPage />);

      // 기본 page=0 → 표시는 "1 / 5" (텍스트가 여러 노드로 분리되므로 textContent로 확인)
      const pageSpan = container.querySelector("span.text-sm.text-muted-foreground.px-2");
      expect(pageSpan?.textContent?.replace(/\s/g, "")).toBe("1/5");

      // 다음 버튼 클릭 → page=1 → 표시는 "2 / 5"
      await userEvent.click(screen.getByText("다음"));
      expect(pageSpan?.textContent?.replace(/\s/g, "")).toBe("2/5");
    });
  });

  // -------------------------------------------------------
  // TC-6: Navbar 주식 메뉴
  // -------------------------------------------------------
  describe("TC-6: Navbar 주식 메뉴", () => {
    it("Navbar에 주식 메뉴 링크가 존재한다", () => {
      render(<Navbar />);
      const stockLink = screen.getByRole("link", { name: /주식/ });
      expect(stockLink).toBeInTheDocument();
      expect(stockLink).toHaveAttribute("href", "/stocks");
    });

    it("/stocks 경로에서 주식 메뉴가 활성 스타일을 가진다", () => {
      // usePathname이 '/stocks'를 반환하도록 이미 모킹됨
      render(<Navbar />);
      const stockLink = screen.getByRole("link", { name: /주식/ });
      // 활성 상태: bg-primary text-primary-foreground 클래스
      expect(stockLink.className).toContain("bg-primary");
    });

    it("Navbar의 모든 메뉴 항목이 표시된다", () => {
      render(<Navbar />);
      expect(screen.getByRole("link", { name: /대시보드/ })).toBeInTheDocument();
      expect(screen.getByRole("link", { name: /주식/ })).toBeInTheDocument();
      expect(screen.getByRole("link", { name: /즐겨찾기/ })).toBeInTheDocument();
      expect(screen.getByRole("link", { name: /거래량TOP/ })).toBeInTheDocument();
      expect(screen.getByRole("link", { name: /시그널/ })).toBeInTheDocument();
      expect(screen.getByRole("link", { name: /뉴스/ })).toBeInTheDocument();
    });
  });
});
