package com.blackbox.pinspot.ui.main;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavDirections;
import com.blackbox.pinspot.R;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class MapFragmentDirections {
  private MapFragmentDirections() {
  }

  @NonNull
  public static ActionMapFragmentToPinInfoFragment actionMapFragmentToPinInfoFragment(
      @Nullable String PinID) {
    return new ActionMapFragmentToPinInfoFragment(PinID);
  }

  public static class ActionMapFragmentToPinInfoFragment implements NavDirections {
    private final HashMap arguments = new HashMap();

    @SuppressWarnings("unchecked")
    private ActionMapFragmentToPinInfoFragment(@Nullable String PinID) {
      this.arguments.put("PinID", PinID);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public ActionMapFragmentToPinInfoFragment setPinID(@Nullable String PinID) {
      this.arguments.put("PinID", PinID);
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    public Bundle getArguments() {
      Bundle __result = new Bundle();
      if (arguments.containsKey("PinID")) {
        String PinID = (String) arguments.get("PinID");
        __result.putString("PinID", PinID);
      }
      return __result;
    }

    @Override
    public int getActionId() {
      return R.id.action_mapFragment_to_pinInfoFragment;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public String getPinID() {
      return (String) arguments.get("PinID");
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
          return true;
      }
      if (object == null || getClass() != object.getClass()) {
          return false;
      }
      ActionMapFragmentToPinInfoFragment that = (ActionMapFragmentToPinInfoFragment) object;
      if (arguments.containsKey("PinID") != that.arguments.containsKey("PinID")) {
        return false;
      }
      if (getPinID() != null ? !getPinID().equals(that.getPinID()) : that.getPinID() != null) {
        return false;
      }
      if (getActionId() != that.getActionId()) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + (getPinID() != null ? getPinID().hashCode() : 0);
      result = 31 * result + getActionId();
      return result;
    }

    @Override
    public String toString() {
      return "ActionMapFragmentToPinInfoFragment(actionId=" + getActionId() + "){"
          + "PinID=" + getPinID()
          + "}";
    }
  }
}
