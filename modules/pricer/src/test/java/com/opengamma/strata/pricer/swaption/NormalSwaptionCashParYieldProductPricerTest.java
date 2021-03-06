/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static com.opengamma.strata.product.swap.type.FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;
import com.opengamma.strata.pricer.impl.option.NormalFunctionData;
import com.opengamma.strata.pricer.impl.option.NormalPriceFunction;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swaption.CashSettlement;
import com.opengamma.strata.product.swaption.CashSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSettlement;
import com.opengamma.strata.product.swaption.ResolvedSwaption;
import com.opengamma.strata.product.swaption.Swaption;

/**
 * Test {@link NormalSwaptionCashParYieldProductPricer}.
 */
@Test
public class NormalSwaptionCashParYieldProductPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = RatesProviderDataSets.VAL_DATE_2014_01_22;
  private static final LocalDate SWAPTION_EXERCISE_DATE = VAL_DATE.plusYears(5);
  private static final LocalDate SWAPTION_PAST_EXERCISE_DATE = VAL_DATE.minusYears(1);
  private static final LocalTime SWAPTION_EXPIRY_TIME = LocalTime.of(11, 0);
  private static final ZoneId SWAPTION_EXPIRY_ZONE = ZoneId.of("America/New_York");
  private static final LocalDate SWAP_EFFECTIVE_DATE =
      USD_LIBOR_3M.calculateEffectiveFromFixing(SWAPTION_EXERCISE_DATE, REF_DATA);
  private static final int SWAP_TENOR_YEAR = 5;
  private static final Period SWAP_TENOR = Period.ofYears(SWAP_TENOR_YEAR);
  private static final LocalDate SWAP_MATURITY_DATE = SWAP_EFFECTIVE_DATE.plus(SWAP_TENOR);
  private static final double STRIKE = 0.01;
  private static final double NOTIONAL = 100_000_000;
  private static final Swap SWAP_REC = USD_FIXED_6M_LIBOR_3M
      .toTrade(VAL_DATE, SWAP_EFFECTIVE_DATE, SWAP_MATURITY_DATE, SELL, NOTIONAL, STRIKE).getProduct();
  private static final ResolvedSwap RSWAP_REC = SWAP_REC.resolve(REF_DATA);
  private static final Swap SWAP_PAY = USD_FIXED_6M_LIBOR_3M
      .toTrade(VAL_DATE, SWAP_EFFECTIVE_DATE, SWAP_MATURITY_DATE, BUY, NOTIONAL, STRIKE).getProduct();
  private static final ResolvedSwap RSWAP_PAY = SWAP_PAY.resolve(REF_DATA);
  private static final Swap SWAP_REC_PAST = USD_FIXED_6M_LIBOR_3M // Only for checks; no actual computation on that swap
      .toTrade(SWAPTION_PAST_EXERCISE_DATE, SWAPTION_PAST_EXERCISE_DATE, SWAPTION_PAST_EXERCISE_DATE.plusYears(10),
          SELL, NOTIONAL, STRIKE).getProduct();
  private static final Swap SWAP_PAY_PAST = USD_FIXED_6M_LIBOR_3M // Only for checks; no actual computation on that swap
      .toTrade(SWAPTION_PAST_EXERCISE_DATE, SWAPTION_PAST_EXERCISE_DATE, SWAPTION_PAST_EXERCISE_DATE.plusYears(10),
          BUY, NOTIONAL, STRIKE).getProduct();
  private static final LocalDate SETTLE_DATE = USD_LIBOR_3M.getEffectiveDateOffset().adjust(SWAPTION_EXERCISE_DATE, REF_DATA);
  private static final CashSettlement PAR_YIELD = CashSettlement.builder()
      .cashSettlementMethod(CashSettlementMethod.PAR_YIELD)
      .settlementDate(SETTLE_DATE).build();
  private static final ResolvedSwaption SWAPTION_REC_LONG = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_REC_SHORT = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.SHORT)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_LONG = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_SHORT = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.SHORT)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_REC_LONG_AT_EXPIRY = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(VAL_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_SHORT_AT_EXPIRY = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(VAL_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.SHORT)
      .underlying(SWAP_PAY)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_REC_LONG_PAST = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(SWAPTION_PAST_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_REC_PAST)
      .build()
      .resolve(REF_DATA);
  private static final ResolvedSwaption SWAPTION_PAY_SHORT_PAST = Swaption.builder()
      .swaptionSettlement(PAR_YIELD)
      .expiryDate(AdjustableDate.of(SWAPTION_PAST_EXERCISE_DATE))
      .expiryTime(SWAPTION_EXPIRY_TIME)
      .expiryZone(SWAPTION_EXPIRY_ZONE)
      .longShort(LongShort.LONG)
      .underlying(SWAP_PAY_PAST)
      .build()
      .resolve(REF_DATA);
  // volatility and rate providers
  private static final ImmutableRatesProvider RATE_PROVIDER = RatesProviderDataSets.multiUsd(VAL_DATE);
  private static final NormalSwaptionExpiryTenorVolatilities VOL_PROVIDER =
      SwaptionNormalVolatilityDataSets.NORMAL_VOL_SWAPTION_PROVIDER_USD_STD;
  private static final NormalSwaptionVolatilities VOL_PROVIDER_FLAT =
      SwaptionNormalVolatilityDataSets.NORMAL_VOL_SWAPTION_PROVIDER_USD_FLAT;
  // test parameters
  private static final double FD_EPS = 1.0E-7;
  private static final double TOL = 1.0e-12;
  // pricers
  private static final NormalPriceFunction NORMAL = new NormalPriceFunction();
  private static final NormalSwaptionCashParYieldProductPricer PRICER_SWAPTION =
      NormalSwaptionCashParYieldProductPricer.DEFAULT;
  private static final DiscountingSwapProductPricer PRICER_SWAP = DiscountingSwapProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FINITE_DIFFERENCE_CALCULATOR =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    CurrencyAmount pvRecComputed = PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvPayComputed = PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double volatility = VOL_PROVIDER.volatility(SWAPTION_REC_LONG.getExpiry(), SWAP_TENOR_YEAR, STRIKE, forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    NormalFunctionData normalData = NormalFunctionData.of(forward, annuityCash * discount, volatility);
    double expiry = VOL_PROVIDER.relativeTime(SWAPTION_REC_LONG.getExpiry());
    EuropeanVanillaOption optionRec = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.PUT);
    EuropeanVanillaOption optionPay = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.CALL);
    double pvRecExpected = NORMAL.getPriceFunction(optionRec).apply(normalData);
    double pvPayExpected = -NORMAL.getPriceFunction(optionPay).apply(normalData);
    assertEquals(pvRecComputed.getCurrency(), USD);
    assertEquals(pvRecComputed.getAmount(), pvRecExpected, NOTIONAL * TOL);
    assertEquals(pvPayComputed.getCurrency(), USD);
    assertEquals(pvPayComputed.getAmount(), pvPayExpected, NOTIONAL * TOL);
  }

  public void test_presentValue_at_expiry() {
    CurrencyAmount pvRec = PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvPay = PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    assertEquals(pvRec.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(pvPay.getAmount(), discount * annuityCash * (STRIKE - forward), NOTIONAL * TOL);
  }

  public void test_presentValue_after_expiry() {
    CurrencyAmount pvRec = PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvPay = PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvRec.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(pvPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValue_parity() {
    CurrencyAmount pvRecLong = PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvRecShort = PRICER_SWAPTION.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvPayLong = PRICER_SWAPTION.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvPayShort = PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvRecLong.getAmount(), -pvRecShort.getAmount(), NOTIONAL * TOL);
    assertEquals(pvPayLong.getAmount(), -pvPayShort.getAmount(), NOTIONAL * TOL);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    double expected = discount * annuityCash * (forward - STRIKE);
    assertEquals(pvPayLong.getAmount() - pvRecLong.getAmount(), expected, NOTIONAL * TOL);
    assertEquals(pvPayShort.getAmount() - pvRecShort.getAmount(), -expected, NOTIONAL * TOL);
  }

  public void test_physicalSettlement() {
    Swaption swaption = Swaption.builder()
        .swaptionSettlement(PhysicalSettlement.DEFAULT)
        .expiryDate(AdjustableDate.of(SWAPTION_EXERCISE_DATE))
        .expiryTime(SWAPTION_EXPIRY_TIME)
        .expiryZone(SWAPTION_EXPIRY_ZONE)
        .longShort(LongShort.LONG)
        .underlying(SWAP_REC)
        .build();
    assertThrowsIllegalArg(() -> PRICER_SWAPTION.presentValue(swaption.resolve(REF_DATA), RATE_PROVIDER, VOL_PROVIDER));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueDelta() {
    CurrencyAmount pvDeltaRecComputed =
        PRICER_SWAPTION.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaPayComputed =
        PRICER_SWAPTION.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double volatility = VOL_PROVIDER.volatility(SWAPTION_REC_LONG.getExpiry(), SWAP_TENOR_YEAR, STRIKE, forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    NormalFunctionData normalData = NormalFunctionData.of(forward, annuityCash * discount, volatility);
    double expiry = VOL_PROVIDER.relativeTime(SWAPTION_REC_LONG.getExpiry());
    EuropeanVanillaOption optionRec = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.PUT);
    EuropeanVanillaOption optionPay = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.CALL);
    double pvDeltaRecExpected = NORMAL.getDelta(optionRec, normalData);
    double pvDeltaPayExpected = -NORMAL.getDelta(optionPay, normalData);
    assertEquals(pvDeltaRecComputed.getCurrency(), USD);
    assertEquals(pvDeltaRecComputed.getAmount(), pvDeltaRecExpected, NOTIONAL * TOL);
    assertEquals(pvDeltaPayComputed.getCurrency(), USD);
    assertEquals(pvDeltaPayComputed.getAmount(), pvDeltaPayExpected, NOTIONAL * TOL);
  }

  public void test_presentValueDelta_at_expiry() {
    CurrencyAmount pvDeltaRec =
        PRICER_SWAPTION.presentValueDelta(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaPay =
        PRICER_SWAPTION.presentValueDelta(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    assertEquals(pvDeltaRec.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(pvDeltaPay.getAmount(), -discount * annuityCash, NOTIONAL * TOL);
  }

  public void test_presentValueDelta_after_expiry() {
    CurrencyAmount pvDeltaRec = PRICER_SWAPTION.presentValueDelta(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaPay = PRICER_SWAPTION.presentValueDelta(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvDeltaRec.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(pvDeltaPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValueDelta_parity() {
    CurrencyAmount pvDeltaRecLong = PRICER_SWAPTION.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaRecShort = PRICER_SWAPTION.presentValueDelta(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaPayLong = PRICER_SWAPTION.presentValueDelta(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvDeltaPayShort = PRICER_SWAPTION.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvDeltaRecLong.getAmount(), -pvDeltaRecShort.getAmount(), NOTIONAL * TOL);
    assertEquals(pvDeltaPayLong.getAmount(), -pvDeltaPayShort.getAmount(), NOTIONAL * TOL);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    double expected = discount * annuityCash;
    assertEquals(pvDeltaPayLong.getAmount() - pvDeltaRecLong.getAmount(), expected, NOTIONAL * TOL);
    assertEquals(pvDeltaPayShort.getAmount() - pvDeltaRecShort.getAmount(), -expected, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueGamma() {
    CurrencyAmount pvGammaRecComputed =
        PRICER_SWAPTION.presentValueGamma(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaPayComputed =
        PRICER_SWAPTION.presentValueGamma(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double volatility = VOL_PROVIDER.volatility(SWAPTION_REC_LONG.getExpiry(), SWAP_TENOR_YEAR, STRIKE, forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    NormalFunctionData normalData = NormalFunctionData.of(forward, annuityCash * discount, volatility);
    double expiry = VOL_PROVIDER.relativeTime(SWAPTION_REC_LONG.getExpiry());
    EuropeanVanillaOption optionRec = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.PUT);
    EuropeanVanillaOption optionPay = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.CALL);
    double pvGammaRecExpected = NORMAL.getGamma(optionRec, normalData);
    double pvGammaPayExpected = -NORMAL.getGamma(optionPay, normalData);
    assertEquals(pvGammaRecComputed.getCurrency(), USD);
    assertEquals(pvGammaRecComputed.getAmount(), pvGammaRecExpected, NOTIONAL * TOL);
    assertEquals(pvGammaPayComputed.getCurrency(), USD);
    assertEquals(pvGammaPayComputed.getAmount(), pvGammaPayExpected, NOTIONAL * TOL);
  }

  public void test_presentValueGamma_at_expiry() {
    CurrencyAmount pvGammaRec =
        PRICER_SWAPTION.presentValueGamma(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaPay =
        PRICER_SWAPTION.presentValueGamma(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvGammaRec.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(pvGammaPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValueGamma_after_expiry() {
    CurrencyAmount pvGammaRec = PRICER_SWAPTION.presentValueGamma(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaPay = PRICER_SWAPTION.presentValueGamma(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvGammaRec.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(pvGammaPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValueGamma_parity() {
    CurrencyAmount pvGammaRecLong = PRICER_SWAPTION.presentValueGamma(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaRecShort = PRICER_SWAPTION.presentValueGamma(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaPayLong = PRICER_SWAPTION.presentValueGamma(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvGammaPayShort = PRICER_SWAPTION.presentValueGamma(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvGammaRecLong.getAmount(), -pvGammaRecShort.getAmount(), NOTIONAL * TOL);
    assertEquals(pvGammaPayLong.getAmount(), -pvGammaPayShort.getAmount(), NOTIONAL * TOL);
    assertEquals(pvGammaPayLong.getAmount(), pvGammaRecLong.getAmount(), NOTIONAL * TOL);
    assertEquals(pvGammaPayShort.getAmount(), pvGammaRecShort.getAmount(), NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueTheta() {
    CurrencyAmount pvThetaRecComputed =
        PRICER_SWAPTION.presentValueTheta(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaPayComputed =
        PRICER_SWAPTION.presentValueTheta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double volatility = VOL_PROVIDER.volatility(SWAPTION_REC_LONG.getExpiry(), SWAP_TENOR_YEAR, STRIKE,
        forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    NormalFunctionData normalData = NormalFunctionData.of(forward, annuityCash * discount, volatility);
    double expiry = VOL_PROVIDER.relativeTime(SWAPTION_REC_LONG.getExpiry());
    EuropeanVanillaOption optionRec = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.PUT);
    EuropeanVanillaOption optionPay = EuropeanVanillaOption.of(STRIKE, expiry, PutCall.CALL);
    double pvThetaRecExpected = NORMAL.getTheta(optionRec, normalData);
    double pvThetaPayExpected = -NORMAL.getTheta(optionPay, normalData);
    assertEquals(pvThetaRecComputed.getCurrency(), USD);
    assertEquals(pvThetaRecComputed.getAmount(), pvThetaRecExpected, NOTIONAL * TOL);
    assertEquals(pvThetaPayComputed.getCurrency(), USD);
    assertEquals(pvThetaPayComputed.getAmount(), pvThetaPayExpected, NOTIONAL * TOL);
  }

  public void test_presentValueTheta_at_expiry() {
    CurrencyAmount pvThetaRec =
        PRICER_SWAPTION.presentValueTheta(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaPay =
        PRICER_SWAPTION.presentValueTheta(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvThetaRec.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(pvThetaPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValueTheta_after_expiry() {
    CurrencyAmount pvThetaRec = PRICER_SWAPTION.presentValueTheta(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaPay = PRICER_SWAPTION.presentValueTheta(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvThetaRec.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(pvThetaPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValueTheta_parity() {
    CurrencyAmount pvThetaRecLong = PRICER_SWAPTION.presentValueTheta(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaRecShort = PRICER_SWAPTION.presentValueTheta(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaPayLong = PRICER_SWAPTION.presentValueTheta(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvThetaPayShort = PRICER_SWAPTION.presentValueTheta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvThetaRecLong.getAmount(), -pvThetaRecShort.getAmount(), NOTIONAL * TOL);
    assertEquals(pvThetaPayLong.getAmount(), -pvThetaPayShort.getAmount(), NOTIONAL * TOL);
    assertEquals(pvThetaPayLong.getAmount(), pvThetaRecLong.getAmount(), NOTIONAL * TOL);
    assertEquals(pvThetaPayShort.getAmount(), pvThetaRecShort.getAmount(), NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------  
  public void test_currencyExposure() {
    MultiCurrencyAmount computedRec = PRICER_SWAPTION.currencyExposure(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount computedPay = PRICER_SWAPTION.currencyExposure(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pointRec =
        PRICER_SWAPTION.presentValueSensitivityStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER));
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(USD).getAmount(), expectedRec.getAmount(USD).getAmount(), NOTIONAL * TOL);
    PointSensitivityBuilder pointPay =
        PRICER_SWAPTION.presentValueSensitivityStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER));
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(USD).getAmount(), expectedPay.getAmount(USD).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_at_expiry() {
    MultiCurrencyAmount computedRec =
        PRICER_SWAPTION.currencyExposure(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount computedPay =
        PRICER_SWAPTION.currencyExposure(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pointRec =
        PRICER_SWAPTION.presentValueSensitivityStickyStrike(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER));
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(USD).getAmount(), expectedRec.getAmount(USD).getAmount(), NOTIONAL * TOL);
    PointSensitivityBuilder pointPay =
        PRICER_SWAPTION.presentValueSensitivityStickyStrike(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER));
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(USD).getAmount(), expectedPay.getAmount(USD).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_after_expiry() {
    MultiCurrencyAmount computedRec =
        PRICER_SWAPTION.currencyExposure(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount computedPay =
        PRICER_SWAPTION.currencyExposure(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(USD).getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(USD).getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_impliedVolatility() {
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double expected = VOL_PROVIDER.volatility(SWAPTION_REC_LONG.getExpiry(), SWAP_TENOR_YEAR, STRIKE, forward);
    double computedRec = PRICER_SWAPTION.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    double computedPay = PRICER_SWAPTION.impliedVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(computedRec, expected, TOL);
    assertEquals(computedPay, expected, TOL);
  }

  public void test_impliedVolatility_at_expiry() {
    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    double expected = VOL_PROVIDER.volatility(
        VAL_DATE.atTime(SWAPTION_EXPIRY_TIME).atZone(SWAPTION_EXPIRY_ZONE), SWAP_TENOR_YEAR, STRIKE, forward);
    double computedRec = PRICER_SWAPTION.impliedVolatility(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    double computedPay = PRICER_SWAPTION.impliedVolatility(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(computedRec, expected, TOL);
    assertEquals(computedPay, expected, TOL);
  }

  public void test_impliedVolatility_after_expiry() {
    assertThrowsIllegalArg(() -> PRICER_SWAPTION.impliedVolatility(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOL_PROVIDER));
    assertThrowsIllegalArg(() -> PRICER_SWAPTION.impliedVolatility(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOL_PROVIDER));
  }

  //-------------------------------------------------------------------------
  public void implied_volatility_round_trip() { // Compute pv and then implied vol from PV and compare with direct implied vol
    CurrencyAmount pvLongRec =
        PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    double impliedLongRecComputed =
        PRICER_SWAPTION.impliedVolatilityFromPresentValue(SWAPTION_REC_LONG, RATE_PROVIDER,
            VOL_PROVIDER.getDayCount(), pvLongRec.getAmount());
    double impliedLongRecInterpolated =
        PRICER_SWAPTION.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(impliedLongRecComputed, impliedLongRecInterpolated, TOL);

    CurrencyAmount pvLongPay =
        PRICER_SWAPTION.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER);
    double impliedLongPayComputed =
        PRICER_SWAPTION.impliedVolatilityFromPresentValue(SWAPTION_PAY_LONG, RATE_PROVIDER,
            VOL_PROVIDER.getDayCount(), pvLongPay.getAmount());
    double impliedLongPayInterpolated =
        PRICER_SWAPTION.impliedVolatility(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(impliedLongPayComputed, impliedLongPayInterpolated, TOL);

    CurrencyAmount pvShortRec =
        PRICER_SWAPTION.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    double impliedShortRecComputed =
        PRICER_SWAPTION.impliedVolatilityFromPresentValue(SWAPTION_REC_SHORT, RATE_PROVIDER,
            VOL_PROVIDER.getDayCount(), pvShortRec.getAmount());
    double impliedShortRecInterpolated =
        PRICER_SWAPTION.impliedVolatility(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(impliedShortRecComputed, impliedShortRecInterpolated, TOL);
  }

  public void implied_volatility_wrong_sign() {
    CurrencyAmount pvLongRec =
        PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    assertThrowsIllegalArg(() -> PRICER_SWAPTION.impliedVolatilityFromPresentValue(SWAPTION_REC_LONG, RATE_PROVIDER,
        VOL_PROVIDER.getDayCount(), -pvLongRec.getAmount()));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityStickyStrike() {
    PointSensitivities pointRec = PRICER_SWAPTION
        .presentValueSensitivityStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER_FLAT).build();
    CurrencyParameterSensitivities computedRec = RATE_PROVIDER.parameterSensitivity(pointRec);
    CurrencyParameterSensitivities expectedRec = FINITE_DIFFERENCE_CALCULATOR.sensitivity(
        RATE_PROVIDER, (p) -> PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, p, VOL_PROVIDER_FLAT));
    assertTrue(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 200d));
    PointSensitivities pointPay = PRICER_SWAPTION
        .presentValueSensitivityStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER_FLAT).build();
    CurrencyParameterSensitivities computedPay = RATE_PROVIDER.parameterSensitivity(pointPay);
    CurrencyParameterSensitivities expectedPay = FINITE_DIFFERENCE_CALCULATOR.sensitivity(
        RATE_PROVIDER, (p) -> PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT, p, VOL_PROVIDER_FLAT));
    assertTrue(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_EPS * 200d));
  }

  public void test_presentValueSensitivityStickyStrike_at_expiry() {
    PointSensitivities pointRec = PRICER_SWAPTION.presentValueSensitivityStickyStrike(
        SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER).build();
    for (PointSensitivity sensi : pointRec.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
    PointSensitivities pointPay = PRICER_SWAPTION.presentValueSensitivityStickyStrike(
        SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER).build();
    CurrencyParameterSensitivities computedPay = RATE_PROVIDER.parameterSensitivity(pointPay);
    CurrencyParameterSensitivities expectedPay = FINITE_DIFFERENCE_CALCULATOR.sensitivity(
        RATE_PROVIDER, (p) -> PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT_AT_EXPIRY, p, VOL_PROVIDER_FLAT));
    assertTrue(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_EPS * 100d));
  }

  public void test_presentValueSensitivityStickyStrike_after_expiry() {
    PointSensitivityBuilder pointRec = PRICER_SWAPTION
        .presentValueSensitivityStickyStrike(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pointPay = PRICER_SWAPTION
        .presentValueSensitivityStickyStrike(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pointRec, PointSensitivityBuilder.none());
    assertEquals(pointPay, PointSensitivityBuilder.none());
  }

  public void test_presentValueSensitivityStickyStrike_parity() {
    CurrencyParameterSensitivities pvSensiRecLong = RATE_PROVIDER.parameterSensitivity(
        PRICER_SWAPTION.presentValueSensitivityStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER).build());
    CurrencyParameterSensitivities pvSensiRecShort = RATE_PROVIDER.parameterSensitivity(
        PRICER_SWAPTION.presentValueSensitivityStickyStrike(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER).build());
    CurrencyParameterSensitivities pvSensiPayLong = RATE_PROVIDER.parameterSensitivity(
        PRICER_SWAPTION.presentValueSensitivityStickyStrike(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER).build());
    CurrencyParameterSensitivities pvSensiPayShort = RATE_PROVIDER.parameterSensitivity(
        PRICER_SWAPTION.presentValueSensitivityStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER).build());
    assertTrue(pvSensiRecLong.equalWithTolerance(pvSensiRecShort.multipliedBy(-1d), NOTIONAL * TOL));
    assertTrue(pvSensiPayLong.equalWithTolerance(pvSensiPayShort.multipliedBy(-1d), NOTIONAL * TOL));

    double forward = PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER);
    PointSensitivityBuilder forwardSensi = PRICER_SWAP.parRateSensitivity(RSWAP_REC, RATE_PROVIDER);
    double annuityCash = PRICER_SWAP.getLegPricer().annuityCash(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double annuityCashDeriv = PRICER_SWAP.getLegPricer()
        .annuityCashDerivative(RSWAP_REC.getLegs(SwapLegType.FIXED).get(0), forward);
    double discount = RATE_PROVIDER.discountFactor(USD, SETTLE_DATE);
    PointSensitivityBuilder discountSensi = RATE_PROVIDER.discountFactors(USD).zeroRatePointSensitivity(SETTLE_DATE);
    PointSensitivities expecedPoint = discountSensi.multipliedBy(annuityCash * (forward - STRIKE)).combinedWith(
        forwardSensi.multipliedBy(discount * annuityCash + discount * annuityCashDeriv * (forward - STRIKE))).build();
    CurrencyParameterSensitivities expected = RATE_PROVIDER.parameterSensitivity(expecedPoint);
    assertTrue(expected.equalWithTolerance(pvSensiPayLong.combinedWith(pvSensiRecLong.multipliedBy(-1d)),
        NOTIONAL * TOL));
    assertTrue(expected.equalWithTolerance(pvSensiRecShort.combinedWith(pvSensiPayShort.multipliedBy(-1d)),
        NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivityNormalVolatility() {
    SwaptionSensitivity computedRec = PRICER_SWAPTION
        .presentValueSensitivityVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvRecUp = PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER,
        SwaptionNormalVolatilityDataSets.normalVolSwaptionProviderUsdStsShifted(FD_EPS));
    CurrencyAmount pvRecDw = PRICER_SWAPTION.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER,
        SwaptionNormalVolatilityDataSets.normalVolSwaptionProviderUsdStsShifted(-FD_EPS));
    double expectedRec = 0.5 * (pvRecUp.getAmount() - pvRecDw.getAmount()) / FD_EPS;
    assertEquals(computedRec.getCurrency(), USD);
    assertEquals(computedRec.getSensitivity(), expectedRec, FD_EPS * NOTIONAL);
    assertEquals(computedRec.getConvention(), SwaptionNormalVolatilityDataSets.USD_1Y_LIBOR3M);
    assertEquals(computedRec.getExpiry(), SWAPTION_REC_LONG.getExpiry());
    assertEquals(computedRec.getTenor(), SWAP_TENOR_YEAR, TOL);
    assertEquals(computedRec.getStrike(), STRIKE, TOL);
    assertEquals(computedRec.getForward(), PRICER_SWAP.parRate(RSWAP_REC, RATE_PROVIDER), TOL);
    SwaptionSensitivity computedPay = PRICER_SWAPTION
        .presentValueSensitivityVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvUpPay = PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER,
        SwaptionNormalVolatilityDataSets.normalVolSwaptionProviderUsdStsShifted(FD_EPS));
    CurrencyAmount pvDwPay = PRICER_SWAPTION.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER,
        SwaptionNormalVolatilityDataSets.normalVolSwaptionProviderUsdStsShifted(-FD_EPS));
    double expectedPay = 0.5 * (pvUpPay.getAmount() - pvDwPay.getAmount()) / FD_EPS;
    assertEquals(computedPay.getCurrency(), USD);
    assertEquals(computedPay.getSensitivity(), expectedPay, FD_EPS * NOTIONAL);
    assertEquals(computedPay.getConvention(), SwaptionNormalVolatilityDataSets.USD_1Y_LIBOR3M);
    assertEquals(computedPay.getExpiry(), SWAPTION_PAY_SHORT.getExpiry());
    assertEquals(computedPay.getTenor(), SWAP_TENOR_YEAR, TOL);
    assertEquals(computedPay.getStrike(), STRIKE, TOL);
    assertEquals(computedPay.getForward(), PRICER_SWAP.parRate(RSWAP_PAY, RATE_PROVIDER), TOL);
  }

  public void test_presentValueSensitivityNormalVolatility_at_expiry() {
    SwaptionSensitivity sensiRec =
        PRICER_SWAPTION.presentValueSensitivityVolatility(SWAPTION_REC_LONG_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(sensiRec.getSensitivity(), 0d, NOTIONAL * TOL);
    SwaptionSensitivity sensiPay =
        PRICER_SWAPTION.presentValueSensitivityVolatility(SWAPTION_PAY_SHORT_AT_EXPIRY, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(sensiPay.getSensitivity(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValueSensitivityNormalVolatility_after_expiry() {
    SwaptionSensitivity sensiRec =
        PRICER_SWAPTION.presentValueSensitivityVolatility(SWAPTION_REC_LONG_PAST, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSensitivity sensiPay =
        PRICER_SWAPTION.presentValueSensitivityVolatility(SWAPTION_PAY_SHORT_PAST, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(sensiRec.getSensitivity(), 0.0d, NOTIONAL * TOL);
    assertEquals(sensiPay.getSensitivity(), 0.0d, NOTIONAL * TOL);
  }

  public void test_presentValueSensitivityNormalVolatility_parity() {
    SwaptionSensitivity pvSensiRecLong =
        PRICER_SWAPTION.presentValueSensitivityVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSensitivity pvSensiRecShort =
        PRICER_SWAPTION.presentValueSensitivityVolatility(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSensitivity pvSensiPayLong =
        PRICER_SWAPTION.presentValueSensitivityVolatility(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSensitivity pvSensiPayShort =
        PRICER_SWAPTION.presentValueSensitivityVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvSensiRecLong.getSensitivity(), -pvSensiRecShort.getSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiPayLong.getSensitivity(), -pvSensiPayShort.getSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiRecLong.getSensitivity(), pvSensiPayLong.getSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiPayShort.getSensitivity(), pvSensiPayShort.getSensitivity(), NOTIONAL * TOL);
  }

}
