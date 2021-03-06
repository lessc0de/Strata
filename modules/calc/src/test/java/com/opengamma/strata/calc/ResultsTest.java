/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.result.Result;

/**
 * Test {@link Results}.
 */
@Test
public class ResultsTest {

  private static final ColumnHeader HEADER1 = ColumnHeader.of(ColumnName.of("A"), TestingMeasures.PRESENT_VALUE);
  private static final ColumnHeader HEADER2 = ColumnHeader.of(ColumnName.of("B"), TestingMeasures.PRESENT_VALUE);
  private static final ColumnHeader HEADER3 = ColumnHeader.of(ColumnName.of("C"), TestingMeasures.PRESENT_VALUE);

  public void test_empty() {
    Results test = Results.of(ImmutableList.of(), ImmutableList.of());
    assertEquals(test.getColumns(), ImmutableList.of());
    assertEquals(test.getRowCount(), 0);
    assertEquals(test.getColumnCount(), 0);
    assertThrows(() -> test.get(0, 0), IllegalArgumentException.class, "Row index must be greater than or.*");
  }

  public void nonEmpty() {
    Results test = Results.of(ImmutableList.of(HEADER1, HEADER2, HEADER3), results(1, 2, 3, 4, 5, 6));
    assertEquals(test.getColumns(), ImmutableList.of(HEADER1, HEADER2, HEADER3));
    assertEquals(test.getRowCount(), 2);
    assertEquals(test.getColumnCount(), 3);
    assertEquals(test.get(0, 0).getValue(), 1);
    assertEquals(test.get(1, 2).getValue(), 6);
    assertThrows(() -> test.get(-1, 0), IllegalArgumentException.class, "Row index must be greater than or.*");
    assertThrows(() -> test.get(2, 0), IllegalArgumentException.class, "Row index must be greater than or.*");
    assertThrows(() -> test.get(0, -1), IllegalArgumentException.class, "Column index must be greater than or.*");
    assertThrows(() -> test.get(0, 3), IllegalArgumentException.class, "Column index must be greater than or.*");
  }

  /**
   * Tests that it's not possible to create results with invalid combinations of row and column
   * count and number of items
   */
  public void createInvalid() {
    // Zero columns, non-zero cells
    assertThrowsIllegalArg(() -> Results.of(ImmutableList.of(), results(1)), "The number of cells.*");
    // More columns than cells
    assertThrowsIllegalArg(() -> Results.of(ImmutableList.of(HEADER1, HEADER2, HEADER3), results(1)), "The number of cells.*");
  }

  @SafeVarargs
  private static <T> List<Result<T>> results(T... items) {
    return Arrays.stream(items).map(Result::success).collect(toImmutableList());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Results test = Results.of(ImmutableList.of(HEADER1, HEADER2, HEADER3), results(1, 2, 3, 4, 5, 6));
    coverImmutableBean(test);
    Results test2 = Results.of(ImmutableList.of(HEADER1), results(9));
    coverBeanEquals(test, test2);
  }

}
