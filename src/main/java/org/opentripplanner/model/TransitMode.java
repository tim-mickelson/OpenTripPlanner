package org.opentripplanner.model;

import org.opentripplanner.ext.transmodelapi.model.TransmodelTransportSubmode;
import org.opentripplanner.routing.core.TraverseMode;

import java.io.Serializable;
import java.util.Objects;

public class TransitMode implements Serializable {
  private final TraverseMode mode;
  private final TransmodelTransportSubmode submode;

  public TransitMode(TraverseMode mode, TransmodelTransportSubmode submode) {
    this.mode = mode;
    this.submode = submode;
  }

  public TraverseMode getMode() {
    return mode;
  }

  public TransmodelTransportSubmode getSubmode() {
    return submode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    TransitMode that = (TransitMode) o;
    return mode == that.mode && submode == that.submode;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mode, submode);
  }

  @Override
  public String toString() {
    return "ModeAndSubmode{" + "mode=" + mode + ", submode=" + submode + '}';
  }
}
