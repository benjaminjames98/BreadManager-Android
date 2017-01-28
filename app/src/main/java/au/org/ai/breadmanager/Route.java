package au.org.ai.breadmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Hashtable;

/**
 * Created by Benjamin on 05/11/2016.
 * <p>
 * Represents a route containing churches information. Tied to the gui.
 */
class Route {

  private final MainActivity mainActivity;
  private final String routePK;
  private HashSet<Church> churches = new HashSet<>();
  private Hashtable<String, View> views = new Hashtable<>();
  private String routeName = "N/A";

  /**
   * @param mainActivity
   *     The mainActivity
   * @param routePK
   *     The given private key of the route
   */
  Route(MainActivity mainActivity, String routePK) {
    this.mainActivity = mainActivity;
    this.routePK = routePK;
  }

  /**
   * Used to add a church to this route.
   *
   * @param pk
   *     The given private key
   * @param name
   *     The churches name
   * @param time
   *     The churches assigned delivery time
   * @param address
   *     The churches delivery address
   * @param delivered
   *     Has the church been delivered too?
   */
  void addChurch(String pk, String name, String time, String address, String delivered) {
    System.out.println(
        "adding " + pk + " - " + name + " - " + time + " - " + address + " - " + delivered + " - ");
    final Church church = new Church();
    church.pk = pk;
    church.name = name;
    church.time = time.substring(0, 5);
    church.address = address;
    church.delivered = delivered;
    churches.add(church);

    //create view, add it too route_holder
    LinearLayout layout = (LinearLayout) mainActivity.findViewById(R.id.main_route_holder);
    Context context = layout.getContext();
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);

    View v = inflater.inflate(R.layout.activity_main_route_layout, null);
    ((TextView) v.findViewById(R.id.main_internal_church_name)).setText(church.name);
    ((TextView) v.findViewById(R.id.main_internal_delivery_time)).setText(church.time);
    v.findViewById(R.id.main_internal_info_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        String line0 = "Name: " + church.name + "\n";
        String line1 = "Time: " + church.time + "\n";
        String line2 = "Address: " + church.address + "\n";
        String line3 = "delivered: " + church.delivered + "\n";
        String line4 = "pk: " + church.pk;
        mainActivity.createAlertDialogue(
            "Word on the Street", line0 + line1 + line2 + line3 + line4);
      }
    });
    v.findViewById(R.id.main_internal_done_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        String query = mainActivity.getRegionCode() + ":" + mainActivity.getMacAddress() +
            ":ConfirmDeliveries:" + routePK + ":" + church.pk + "~delivered";
        new Networker(mainActivity).execute(query);
      }
    });

    //finish up
    layout.addView(v);
    views.put(church.pk, v);

    if (delivered.equals("delivered")) {
      deliveryConfirmed(pk);
    }
  }

  /**
   * Shades the gui element of the given church.
   *
   * @param churchPK
   *     The private key of the church.
   */
  void deliveryConfirmed(String churchPK) {
    View view = views.get(churchPK);
    view.setBackgroundColor(mainActivity.getResources().getColor(R.color.colorLighterGrey));
    view.findViewById(R.id.main_internal_done_button).setEnabled(false);
  }


  String getRoutePK() {
    return routePK;
  }

  public void setRouteName(String routeName) {
    this.routeName = routeName;
  }

  public String getRouteName() {
    return routeName;
  }

  /**
   * Container for storing the information of the church.
   */
  private class Church {

    String pk = "-1";
    String name = "NA";
    String time = "00:00";
    String address = "NA";
    String delivered = "not delivered";
  }
}
