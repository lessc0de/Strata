/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import java.util.regex.Pattern;

import org.joda.convert.FromString;

import com.opengamma.strata.collect.TypedString;

/**
 * Identifies the source of observable market data, for example Bloomberg or Reuters.
 * <p>
 * The meaning of a source is deliberately abstract, identified only by name.
 * While it may refer to a major system, such as Bloomberg, it might refer to any
 * other system or sub-system, such as data from a specific broker.
 */
public final class ObservableSource
    extends TypedString<ObservableSource> {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;
  /**
   * Pattern for checking the name.
   * It must only contains the characters A-Z, a-z, 0-9 and -.
   */
  private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9-]+");

  //-------------------------------------------------------------------------
  /**
   * A market data source used when the application does not care about the source.
   */
  public static final ObservableSource NONE = of("None");

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Source names must only contains the characters A-Z, a-z, 0-9 and -.
   *
   * @param name  the name of the source
   * @return a source with the specified name
   */
  @FromString
  public static ObservableSource of(String name) {
    return new ObservableSource(name);
  }

  /**
   * Creates an instance.
   * 
   * @param name  the name of the source
   */
  private ObservableSource(String name) {
    super(name, NAME_PATTERN, "Source name must only contain the characters A-Z, a-z, 0-9 and -");
  }

}
