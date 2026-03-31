export interface StockDto {
  id: string;
  ticker: string;
  market: string;
  name: string;
  tradeDate: string;
  openPrice: number;
  highPrice: number;
  lowPrice: number;
  closePrice: number;
  volume: number;
  marketCap: number;
  changeRate: number;
  ma5?: number;
  ma20?: number;
  ma60?: number;
  rsi14?: number;
  macd?: number;
  macdSignal?: number;
  macdHist?: number;
  isFavorite?: boolean;
}

export interface FavoriteDto {
  id: string;
  ticker: string;
  market: string;
  name: string;
  createdAt: string;
  latestStock?: StockDto;
}

export type Timeframe = "DAILY" | "WEEKLY" | "MONTHLY";

export interface TargetPrice {
  low: number;
  high: number;
}

export interface StockAnalysis {
  ticker: string;
  name: string;
  recommendation: string;
  targetPrice?: TargetPrice;
  stopLoss?: number;
  reasoning: string;
}

export interface StrategyAssessment {
  validity: string;
  score: number;
  reasoning: string;
}

export interface StockAnalysisReport {
  recommendation: string;
  score: number;
  targetPrice?: TargetPrice;
  stopLoss?: number;
  technicalAnalysis: string;
  fundamentalNote: string;
  globalContext: string;
  riskFactors: string;
  summary: string;
}

export interface SignalAnalysisResult {
  strategyAssessment?: StrategyAssessment;
  stockAnalyses?: StockAnalysis[];
  marketContext?: string;
}

export interface SignalDto {
  id: string;
  name: string;
  marketFilter: string;
  timeframe: Timeframe;
  conditions: SignalCondition;
  active: boolean;
  lastRunAt?: string;
  lastResults?: StockDto[];
  lastAnalysis?: SignalAnalysisResult;
  createdAt: string;
}

export interface SignalCondition {
  version?: string;
  logic: "AND" | "OR";
  conditions: (SignalLeaf | SignalGroup)[];
}

export interface SignalLeaf {
  id: string;
  field: string;
  operator: ">" | ">=" | "<" | "<=" | "==" | "!=" | "crossover" | "crossunder";
  value?: number;
  compareField?: string;
}

export interface SignalGroup {
  id: string;
  logic: "AND" | "OR";
  conditions: (SignalLeaf | SignalGroup)[];
}

export interface NewsDto {
  id: number;
  title: string;
  summary: string;
  url: string;
  source: string;
  market: string;
  relatedTicker?: string;
  publishedAt: string;
}

export interface DashboardDto {
  tradeDate: string;
  favorites: StockDto[];
  topVolumeKr: StockDto[];
  topVolumeUs: StockDto[];
  latestNews: NewsDto[];
  activeSignals: SignalDto[];
  krSummary: MarketSummary;
  usSummary: MarketSummary;
}

export interface MarketSummary {
  market: string;
  totalStocks: number;
  risingStocks: number;
  fallingStocks: number;
  avgChangeRate: number;
}

export interface ApiResponse<T> {
  data: T;
  meta: {
    timestamp: string;
    page?: number;
    size?: number;
    totalElements?: number;
    totalPages?: number;
  };
}
