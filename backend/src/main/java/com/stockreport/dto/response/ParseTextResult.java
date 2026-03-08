package com.stockreport.dto.response;

import com.stockreport.domain.stock.Timeframe;

public record ParseTextResult(String conditions, Timeframe timeframe) {}
