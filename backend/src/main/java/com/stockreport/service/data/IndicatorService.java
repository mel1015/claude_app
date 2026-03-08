package com.stockreport.service.data;

import com.stockreport.domain.stock.StockDailyCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DecimalNum;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@Slf4j
public class IndicatorService {

    public void calculateAndSetIndicators(List<StockDailyCache> history, StockDailyCache latest) {
        if (history == null || history.isEmpty()) return;

        try {
            BarSeries series = new BaseBarSeries();
            for (int i = history.size() - 1; i >= 0; i--) {
                StockDailyCache s = history.get(i);
                if (s.getOpenPrice() == null || s.getHighPrice() == null ||
                        s.getLowPrice() == null || s.getClosePrice() == null ||
                        s.getVolume() == null) continue;

                ZonedDateTime dt = s.getTradeDate().atStartOfDay(ZoneId.of("Asia/Seoul"));
                series.addBar(dt,
                        DecimalNum.valueOf(s.getOpenPrice()),
                        DecimalNum.valueOf(s.getHighPrice()),
                        DecimalNum.valueOf(s.getLowPrice()),
                        DecimalNum.valueOf(s.getClosePrice()),
                        DecimalNum.valueOf(s.getVolume()));
            }

            if (series.getBarCount() == 0) return;

            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            int lastIdx = series.getEndIndex();

            if (series.getBarCount() >= 5) {
                SMAIndicator ma5 = new SMAIndicator(closePrice, 5);
                latest.setMa5(ma5.getValue(lastIdx).doubleValue());
            }
            if (series.getBarCount() >= 10) {
                SMAIndicator ma10 = new SMAIndicator(closePrice, 10);
                latest.setMa10(ma10.getValue(lastIdx).doubleValue());
            }
            if (series.getBarCount() >= 20) {
                SMAIndicator ma20 = new SMAIndicator(closePrice, 20);
                latest.setMa20(ma20.getValue(lastIdx).doubleValue());
            }
            if (series.getBarCount() >= 60) {
                SMAIndicator ma60 = new SMAIndicator(closePrice, 60);
                latest.setMa60(ma60.getValue(lastIdx).doubleValue());
            }
            if (series.getBarCount() >= 14) {
                RSIIndicator rsi = new RSIIndicator(closePrice, 14);
                latest.setRsi14(rsi.getValue(lastIdx).doubleValue());
            }
            if (series.getBarCount() >= 26) {
                MACDIndicator macdIndicator = new MACDIndicator(closePrice, 12, 26);
                EMAIndicator macdSignalIndicator = new EMAIndicator(macdIndicator, 9);
                double macdValue = macdIndicator.getValue(lastIdx).doubleValue();
                double macdSignalValue = macdSignalIndicator.getValue(lastIdx).doubleValue();
                latest.setMacd(macdValue);
                latest.setMacdSignal(macdSignalValue);
                latest.setMacdHist(macdValue - macdSignalValue);
            }

        } catch (Exception e) {
            log.warn("Failed to calculate indicators for {}: {}", latest.getTicker(), e.getMessage());
        }
    }
}
