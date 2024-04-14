
package com.crio.warmup.stock.portfolio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  // TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  // Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  // into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  // clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  // CHECKSTYLE:OFF

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  // CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Extract the logic to call Tiingo third-party APIs to a separate function.
  // Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
    final String tiingoUrl = buildUri(symbol, from, to);
    return Arrays.asList(restTemplate.getForObject(tiingoUrl, TiingoCandle[].class));
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {

    String uriTemplate = "https:api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
        + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";

    uriTemplate = uriTemplate.replace("$SYMBOL", symbol).replace("$STARTDATE", startDate.toString())
        .replace("$ENDDATE", endDate.toString());
    return uriTemplate;
  }

  private Double getOpeningPriceOnStartDate(List<Candle> candles) {
    return candles.stream().findFirst().map(Candle::getOpen).orElse(0.0D);
  }

  private Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.stream().reduce((first, second) -> second).map(Candle::getClose).orElse(0.0D);
  }

  private AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {

    Double totalReturns = (sellPrice - buyPrice) / buyPrice;
    Double annualizedReturns = Math.pow((1.0 + totalReturns),
        ((1.0 / calculateYearsBetweenDates(trade.getPurchaseDate(), endDate)))) - 1.0;

    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns);
  }


  private Double calculateYearsBetweenDates(LocalDate startDate, LocalDate endDate) {

    Long value = endDate.toEpochDay() - startDate.toEpochDay();

    return (value.doubleValue() / 365.0);
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) {

    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();

    portfolioTrades.stream().forEach(trade -> {
      List<Candle> candles = null;
      try {
        candles = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }

      Double buyPrice = getOpeningPriceOnStartDate(candles);
      Double sellPrice = getClosingPriceOnEndDate(candles);

      annualizedReturns.add(calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice));
    });

    Collections.sort(annualizedReturns, getComparator());

    return annualizedReturns;

  }
}
