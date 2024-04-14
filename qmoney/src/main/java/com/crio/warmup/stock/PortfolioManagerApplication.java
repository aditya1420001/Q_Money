
package com.crio.warmup.stock;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.Essentials;
import com.crio.warmup.stock.dto.Essentials.QueryParams;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class PortfolioManagerApplication {

  private static final Logger logger =
      Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final RestTemplate restTemplate = new RestTemplate();

  static {
    mapper.registerModule(new JavaTimeModule());
  }

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {

    List<PortfolioTrade> portfolioTrades = readTradesFromJson(args[0]);
    List<String> symbolList = fetchSymbolFieldListFromTrades(portfolioTrades);

    return symbolList;
  }

  private static List<String> fetchSymbolFieldListFromTrades(List<PortfolioTrade> portfolioTrades) {
    return portfolioTrades.parallelStream().map(PortfolioTrade::getSymbol)
        .collect(Collectors.toList());
  }


  private static void printJsonObject(Object object) throws IOException {
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI())
        .toFile();
  }


  // TODO: CRIO_TASK_MODULE_JSON_PARSING
  // Follow the instructions provided in the task documentation and fill up the correct values for
  // the variables provided. First value is provided for your reference.
  // A. Put a breakpoint on the first line inside mainReadFile() which says
  // return Collections.emptyList();
  // B. Then Debug the test #mainReadFile provided in PortfoliomanagerApplicationTest.java
  // following the instructions to run the test.
  // Once you are able to run the test, perform following tasks and record the output as a
  // String in the function below.
  // Use this link to see how to evaluate expressions -
  // https://code.visualstudio.com/docs/editor/debugging#_data-inspection
  // 1. evaluate the value of "args[0]" and set the value
  // to the variable named valueOfArgument0 (This is implemented for your reference.)
  // 2. In the same window, evaluate the value of expression below and set it
  // to resultOfResolveFilePathArgs0
  // expression ==> resolveFileFromResources(args[0])
  // 3. In the same window, evaluate the value of expression below and set it
  // to toStringOfObjectMapper.
  // You might see some garbage numbers in the output. Dont worry, its expected.
  // expression ==> getObjectMapper().toString()
  // 4. Now Go to the debug window and open stack trace. Put the name of the function you see at
  // second place from top to variable functionNameFromTestFileInStackTrace
  // 5. In the same window, you will see the line number of the function in the stack trace window.
  // assign the same to lineNumberFromTestFileInStackTrace
  // Once you are done with above, just run the corresponding test and
  // make sure its working as expected. use below command to do the same.
  // ./gradlew test --tests PortfolioManagerApplicationTest.testDebugValues

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "trades.json";
    String toStringOfObjectMapper = "ObjectMapper";
    String functionNameFromTestFileInStackTrace = "mainReadFile";
    String lineNumberFromTestFileInStackTrace = "";


    return Arrays.asList(
        new String[] {valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
            functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace});
  }


  // Note:
  // Remember to confirm that you are getting same results for annual.
  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

    List<PortfolioTrade> portfolioTrades = readTradesFromJson(args[0]);
    LocalDate endDate = LocalDate.parse(args[1]);

    List<TotalReturnsDto> totalReturnsDtos = new ArrayList<>();

    portfolioTrades.stream().forEach(trade -> {
      List<Candle> tiingoCandles = fetchCandles(trade, endDate);
      totalReturnsDtos.add(new TotalReturnsDto(trade.getSymbol(),
          tiingoCandles.get(tiingoCandles.size() - 1).getClose()));
    });

    Collections.sort(totalReturnsDtos, Comparator.comparing(TotalReturnsDto::getClosingPrice));

    return totalReturnsDtos.stream().map(TotalReturnsDto::getSymbol).collect(Collectors.toList());
  }

  private static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate) {
    final String token = getToken();
    return fetchCandles(trade, endDate, token);
  }

  public static String getToken() {
    return Essentials.TIINGO_API_TOKEN;
  }

  // TODO:
  // After refactor, make sure that the tests pass by using these two commands
  // ./gradlew test --tests PortfolioManagerApplicationTest.readTradesFromJson
  // ./gradlew test --tests PortfolioManagerApplicationTest.mainReadFile
  public static List<PortfolioTrade> readTradesFromJson(String filename)
      throws IOException, URISyntaxException {

    File input = resolveFileFromResources(filename);
    return mapper.readValue(input, new TypeReference<List<PortfolioTrade>>() {});
  }


  // TODO:
  // Build the Url using given parameters and use this function in your code to cann the API.
  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {

    // String url =
    // UriComponentsBuilder.fromHttpUrl("https://api.tiingo.com/tiingo/daily/CSCO/prices?startDate=2012-1-1&endDate=2016-1-1").build().toUriString();
    return UriComponentsBuilder
        .fromHttpUrl(
            "https://api.tiingo.com/tiingo/daily/".concat(trade.getSymbol()).concat("/prices"))
        .queryParam(QueryParams.START_DATE, trade.getPurchaseDate())
        .queryParam(QueryParams.END_DATE, endDate).queryParam(QueryParams.TOKEN, token)
        .toUriString();
  }



  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  // Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  // for the stocks provided in the Json.
  // Use the function you just wrote #calculateAnnualizedReturns.
  // Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.



  // TODO:
  // Ensure all tests are passing using below command
  // ./gradlew test --tests ModuleThreeRefactorTest
  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    return candles.stream().findFirst().map(Candle::getOpen).orElse(0.0D);
  }

  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.stream().reduce((first, second) -> second).map(Candle::getClose).orElse(0.0D);
  }


  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
    final String tiingoUrl = prepareUrl(trade, endDate, token);
    return Arrays.asList(restTemplate.getForObject(tiingoUrl, TiingoCandle[].class));
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {

    List<PortfolioTrade> portfolioTrades = readTradesFromJson(args[0]);
    LocalDate endDate = LocalDate.parse(args[1]);

    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();

    portfolioTrades.stream().forEach(trade -> {
      List<Candle> candles = fetchCandles(trade, endDate);

      Double buyPrice = getOpeningPriceOnStartDate(candles);
      Double sellPrice = getClosingPriceOnEndDate(candles);

      annualizedReturns.add(calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice));
    });

    Collections.sort(annualizedReturns,
        Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed());

    return annualizedReturns;
  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  // Return the populated list of AnnualizedReturn for all stocks.
  // Annualized returns should be calculated in two steps:
  // 1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
  // 1.1 Store the same as totalReturns
  // 2. Calculate extrapolated annualized returns by scaling the same in years span.
  // The formula is:
  // annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  // 2.1 Store the same as annualized_returns
  // Test the same using below specified command. The build should be successful.
  // ./gradlew test --tests PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {

    Double totalReturns = (sellPrice - buyPrice) / buyPrice;
    Double annualizedReturns = Math.pow((1.0 + totalReturns),
        ((1.0 / calculateYearsBetweenDates(trade.getPurchaseDate(), endDate)))) - 1.0;
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns);
  }


  private static Double calculateYearsBetweenDates(LocalDate startDate, LocalDate endDate) {
    Long value = endDate.toEpochDay() - startDate.toEpochDay();
    return (value.doubleValue() / 365.0);
  }

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Once you are done with the implementation inside PortfolioManagerImpl and
  // PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  // Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  // call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
    PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);
    LocalDate endDate = LocalDate.parse(args[1]);
    List<PortfolioTrade> portfolioTrades = readTradesFromJson(args[0]);
    return portfolioManager.calculateAnnualizedReturn(portfolioTrades, endDate);
  }

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainCalculateReturnsAfterRefactor(args));

  }
}

