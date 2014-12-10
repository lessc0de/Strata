/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.rate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.basics.index.IborIndex;
import com.opengamma.collect.ArgChecker;

/**
 * Defines the calculation of a rate of interest based on the average of multiple
 * fixings of a single IBOR-like floating rate index.
 * <p>
 * An interest rate determined from a single IBOR-like index observed on multiple dates.
 * For example, the average of three fixings of 'GBP-LIBOR-3M'.
 */
@BeanDefinition
public final class IborAveragedRate
    implements Rate, ImmutableBean, Serializable {

  /**
   * The IBOR-like index.
   * <p>
   * The rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final IborIndex index;
  /**
   * The list of fixings.
   * <p>
   * A fixing will be taken for each reset period, with the final rate
   * being an average of the fixings.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final ImmutableList<IborAveragedFixing> fixings;

  //-------------------------------------------------------------------------
  /**
   * Creates an {@code IborAveragedRate} from an index and fixings.
   * 
   * @param index  the index
   * @param fixings  the weighted fixings
   * @return the averaged IBOR rate
   */
  public static IborAveragedRate of(IborIndex index, List<IborAveragedFixing> fixings) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(fixings, "fixings");
    return IborAveragedRate.builder()
        .index(index)
        .fixings(ImmutableList.copyOf(fixings))
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code IborAveragedRate}.
   * @return the meta-bean, not null
   */
  public static IborAveragedRate.Meta meta() {
    return IborAveragedRate.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(IborAveragedRate.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static IborAveragedRate.Builder builder() {
    return new IborAveragedRate.Builder();
  }

  private IborAveragedRate(
      IborIndex index,
      List<IborAveragedFixing> fixings) {
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notEmpty(fixings, "fixings");
    this.index = index;
    this.fixings = ImmutableList.copyOf(fixings);
  }

  @Override
  public IborAveragedRate.Meta metaBean() {
    return IborAveragedRate.Meta.INSTANCE;
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
  /**
   * Gets the IBOR-like index.
   * <p>
   * The rate to be paid is based on this index
   * It will be a well known market index such as 'GBP-LIBOR-3M'.
   * @return the value of the property, not null
   */
  public IborIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the list of fixings.
   * <p>
   * A fixing will be taken for each reset period, with the final rate
   * being an average of the fixings.
   * @return the value of the property, not empty
   */
  public ImmutableList<IborAveragedFixing> getFixings() {
    return fixings;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      IborAveragedRate other = (IborAveragedRate) obj;
      return JodaBeanUtils.equal(getIndex(), other.getIndex()) &&
          JodaBeanUtils.equal(getFixings(), other.getFixings());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndex());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFixings());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("IborAveragedRate{");
    buf.append("index").append('=').append(getIndex()).append(',').append(' ');
    buf.append("fixings").append('=').append(JodaBeanUtils.toString(getFixings()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code IborAveragedRate}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<IborIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", IborAveragedRate.class, IborIndex.class);
    /**
     * The meta-property for the {@code fixings} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<IborAveragedFixing>> fixings = DirectMetaProperty.ofImmutable(
        this, "fixings", IborAveragedRate.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "index",
        "fixings");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -843784602:  // fixings
          return fixings;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public IborAveragedRate.Builder builder() {
      return new IborAveragedRate.Builder();
    }

    @Override
    public Class<? extends IborAveragedRate> beanType() {
      return IborAveragedRate.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborIndex> index() {
      return index;
    }

    /**
     * The meta-property for the {@code fixings} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<IborAveragedFixing>> fixings() {
      return fixings;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return ((IborAveragedRate) bean).getIndex();
        case -843784602:  // fixings
          return ((IborAveragedRate) bean).getFixings();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code IborAveragedRate}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<IborAveragedRate> {

    private IborIndex index;
    private List<IborAveragedFixing> fixings = new ArrayList<IborAveragedFixing>();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(IborAveragedRate beanToCopy) {
      this.index = beanToCopy.getIndex();
      this.fixings = new ArrayList<IborAveragedFixing>(beanToCopy.getFixings());
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          return index;
        case -843784602:  // fixings
          return fixings;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 100346066:  // index
          this.index = (IborIndex) newValue;
          break;
        case -843784602:  // fixings
          this.fixings = (List<IborAveragedFixing>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public IborAveragedRate build() {
      return new IborAveragedRate(
          index,
          fixings);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code index} property in the builder.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(IborIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the {@code fixings} property in the builder.
     * @param fixings  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder fixings(List<IborAveragedFixing> fixings) {
      JodaBeanUtils.notEmpty(fixings, "fixings");
      this.fixings = fixings;
      return this;
    }

    /**
     * Sets the {@code fixings} property in the builder
     * from an array of objects.
     * @param fixings  the new value, not empty
     * @return this, for chaining, not null
     */
    public Builder fixings(IborAveragedFixing... fixings) {
      return fixings(Arrays.asList(fixings));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("IborAveragedRate.Builder{");
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("fixings").append('=').append(JodaBeanUtils.toString(fixings));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
