/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.impl.light.LightMetaBean;

import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * A one-dimensional linear interpolator. The interpolated value of the function
 * <i>y</i> at <i>x</i> between two data points <i>(x<sub>1</sub>,
 * y<sub>1</sub>)</i> and <i>(x<sub>2</sub>, y<sub>2</sub>)</i> is given by:<br>
 * <i>y = y<sub>1</sub> + (x - x<sub>1</sub>) * (y<sub>2</sub> - y<sub>1</sub>)
 * / (x<sub>2</sub> - x<sub>1</sub>)</i>
 */
@BeanDefinition(style = "light", constructorScope = "public")
public final class LinearInterpolator1D
    extends Interpolator1D
    implements ImmutableBean {

  @Override
  public double interpolate(Interpolator1DDataBundle data, double value) {
    int lowerIndex = data.getLowerBoundIndex(value);
    double y1 = data.getValue(lowerIndex);
    // check if x-value is at or beyond the last node
    if (lowerIndex == data.size() - 1) {
      // return the value of the last node (flat extrapolation)
      return y1;
    }
    double x1 = data.getKey(lowerIndex);
    double x2 = data.getKey(lowerIndex + 1);
    double y2 = data.getValue(lowerIndex + 1);
    return y1 + (value - x1) / (x2 - x1) * (y2 - y1);
  }

  @Override
  public double firstDerivative(Interpolator1DDataBundle data, double value) {
    int lowerIndex = data.getLowerBoundIndex(value);
    // check if x-value is at or beyond the last node
    if (lowerIndex == data.size() - 1) {
      // return zero to match flat extrapolation in interpolate()
      if (value > data.lastKey()) {
        return 0d;
      }
      // if value is at last node, calculate the gradient from the previous interval
      lowerIndex--;
    }
    double x1 = data.getKey(lowerIndex);
    double y1 = data.getValue(lowerIndex);
    double x2 = data.getKey(lowerIndex + 1);
    double y2 = data.getValue(lowerIndex + 1);
    return (y2 - y1) / (x2 - x1);
  }

  @Override
  public double[] getNodeSensitivitiesForValue(Interpolator1DDataBundle data, double value) {
    int size = data.size();
    double[] result = new double[size];
    int lowerIndex = data.getLowerBoundIndex(value);
    // check if x-value is at or beyond the last node
    if (lowerIndex == size - 1) {
      // sensitivity is entirely to the last node
      result[size - 1] = 1d;
      return result;
    }
    double x1 = data.getKey(lowerIndex);
    double x2 = data.getKey(lowerIndex + 1);
    double dx = x2 - x1;
    double a = (x2 - value) / dx;
    result[lowerIndex] = a;
    result[lowerIndex + 1] = 1 - a;
    return result;
  }

  @Override
  public Interpolator1DDataBundle getDataBundle(double[] x, double[] y) {
    return new ArrayInterpolator1DDataBundle(x, y);
  }

  @Override
  public Interpolator1DDataBundle getDataBundleFromSortedArrays(double[] x, double[] y) {
    return new ArrayInterpolator1DDataBundle(x, y, true);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code LinearInterpolator1D}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(LinearInterpolator1D.class);

  /**
   * The meta-bean for {@code LinearInterpolator1D}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  /**
   * Creates an instance.
   */
  public LinearInterpolator1D() {
  }

  @Override
  public MetaBean metaBean() {
    return META_BEAN;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(32);
    buf.append("LinearInterpolator1D{");
    buf.append('}');
    return buf.toString();
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
