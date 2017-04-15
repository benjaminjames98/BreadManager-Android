package au.org.ai.breadmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Hashtable;

/**
 * The main activity, shown at startup. Everything is centered on this application.
 */
public class MainActivity extends AppCompatActivity {

    private MainActivity mainActivity = this;
    private boolean connected = false;
    private Hashtable<String, Route> routes = new Hashtable<>(); // PK -> Route

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences preferences = this.getPreferences(Context.MODE_PRIVATE);
        String savedRoute = preferences.getString("saved_route", "REGION CODE");
        ((TextView) this.findViewById(R.id.main_region_code_field)).setText(savedRoute);

        findViewById(R.id.main_connect_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = getRegionCode() + ":" + getMacAddress() + ":RequestAssignedRoutes::";
                new Networker(mainActivity, Networker.CONNECT_SIGNAL).execute(query);
            }
        });

        findViewById(R.id.main_request_routes_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String query = getRegionCode() + ":" + getMacAddress() + ":RequestAvailableRoutes::";
                new Networker(mainActivity, Networker.REQUEST_ROUTES_SIGNAL).execute(query);
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (!connected)
            return super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.refresh:
                refreshUI();
                return true;
            case R.id.relinquish_groups:
                if (routes.size() == 0) {
                    createAlertDialogue(
                            "Hey there", "It doesn't seem that you have any routes to relinquish.");
                    return true;
                }

                String[] PKs = new String[routes.size()];
                String[] names = new String[routes.size()];

                int j = 0;
                for (String pk : routes.keySet()) {
                    PKs[j] = pk;
                    names[j++] = routes.get(pk).getRouteName();
                }

                System.out.println(PKs[0]);
                System.out.println(names[0]);

                Intent i = new Intent(this, RequestActivity.class);
                i.putExtra("requestCode", 2);
                i.putExtra("PKs", Utilities.implode("~", PKs));
                i.putExtra("names", Utilities.implode("~", names));
                startActivityForResult(i, 2);
                return true;
            case R.id.logout:
                connected = false;
                if (routes.size() > 0) {
                    String[] pks = routes.keySet().toArray(new String[routes.size()]);
                    String pkString = Utilities.implode("~", pks);
                    String query =
                            getRegionCode() + ":" + getMacAddress() + ":RelinquishRoutes:" + pkString + ":";
                    new Networker(this).execute(query);
                } else {
                    processQueryResponse(new String[]{"RelinquishRoutes", "", "", "", ""});
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * return statement from RequestActivity
     *
     * @param requestCode 1 = request, 2 = relinquish
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_CANCELED)
            return;

        String[] pks = data.getStringArrayExtra("PKs");
        System.out.println("activity result- " + Arrays.toString(pks));
        String routes = Utilities.implode("~", pks);

        if (requestCode == 1) {
            String query = getRegionCode() + ":" + getMacAddress() + ":RequestRoutes:" + routes + ":";
            new Networker(this).execute(query);
        } else if (requestCode == 2) {
            String query =
                    getRegionCode() + ":" + getMacAddress() + ":RelinquishRoutes:" + routes + ":";
            new Networker(this).execute(query);
        }
    }

    /**
     * Called by the Networker after a successful communication.
     *
     * @param input The string to be processed by this method
     */
    void processQueryResponse(String[] input) {
        if (input == null)
            return;
        System.out.println("processing - " + Arrays.toString(input));

        // first time connecting
        if (!connected) {
            if (input[0].equals("Error") && input[1].equals("REGIONCODE does not exist")) {
                createAlertDialogue("Whoops...", "That region code doesn't seem to exist");
                return;
            } else if (input[0].equals("RequestAssignedRoutes")) {
                connected = true;
            } else if (!input[0].equals("RelinquishRoutes")) {
                return;
            }
        }

        findViewById(R.id.main_connect_button).setEnabled(!connected);
        findViewById(R.id.main_region_code_field).setEnabled(!connected);
        findViewById(R.id.main_request_routes_button).setEnabled(connected);

        // processing and delegation
        if (input[0].equals("RequestAssignedRoutes")) {
            String newRegionCode = ((TextView) findViewById(R.id.main_region_code_field)).getText().toString();
            SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("saved_route", newRegionCode);
            editor.apply();

            if (!input[1].equals("")) {
                String[] routePKs = input[1].split("~");
                for (String pk : routePKs) {
                    String query = getRegionCode() + ":" + getMacAddress() + ":RequestRouteInfo:" + pk + ":";
                    new Networker(this).execute(query);
                }
            }
        } else if (input[0].equals("RequestRouteInfo")) {
            String routePK = input[1];
            Route route = new Route(this, routePK);

            String[] churches = input[2].split("~");
            for (String church : churches) {
                church = church.replace("%58", ":");
                String[] info = church.split("\\|");

                int i = 0;
                route.addChurch(info[i++], info[i++], info[i++], info[i++], info[i]);
            }
            route.setRouteName(input[3]);
            routes.put(routePK, route);
        } else if (input[0].equals("RequestAvailableRoutes")) {
            if (input[1].equals(""))
                createAlertDialogue("Slight Hiccup...", "There aren't any unclaimed routes left");
            else {
                Intent i = new Intent(this, RequestActivity.class);
                i.putExtra("PKs", input[1]);
                i.putExtra("names", input[2]);
                startActivityForResult(i, 1);
            }
        } else if (input[0].equals("RequestRoutes")) {
            if (!input[1].equals("")) {
                refreshUI();
            }
        } else if (input[0].equals("ConfirmDeliveries")) {
            if (!input[1].equals("") && !input[2].equals("")) {
                Route route = routes.get(input[1]);
                route.deliveryConfirmed(input[2]);
            }
        } else if (input[0].equals("RelinquishRoutes")) {
            refreshUI();
        }
    }

    /**
     * Wipes the Gui clean, and then sends a RequestAssignedRoutes network request.
     */
    private void refreshUI() {
        LinearLayout layout = (LinearLayout) findViewById(R.id.main_route_holder);
        layout.removeAllViews();
        routes = new Hashtable<>();
        if (connected) {
            String query = getRegionCode() + ":" + getMacAddress() + ":RequestAssignedRoutes::";
            new Networker(mainActivity).execute(query);
        }
    }

    /**
     * Returns the region code displayed in the gui.
     *
     * @return The currently shown region code
     */
    String getRegionCode() {
        return ((EditText) findViewById(R.id.main_region_code_field)).getText().toString();
    }

    /**
     * Returns the internally generated MAC address, which is persistent.
     *
     * @return the internally generated MAC address
     */
    String getMacAddress() {
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        String address = preferences.getString("MAC_ADDRESS", null);
        if (address == null) {

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                if (i != 0)
                    builder.append("-");
                int rand = (int) (Math.random() * 255);
                String hexString = String.format("%02X", rand);
                builder.append(hexString);
            }

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("MAC_ADDRESS", builder.toString());
            editor.apply();
            address = preferences.getString("MAC_ADDRESS", null);
        }

        return address;
    }

    /**
     * Shows a simple, one-button alert dialogue.
     *
     * @param title   The displayed title
     * @param message The displayed message
     */
    void createAlertDialogue(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }
}
