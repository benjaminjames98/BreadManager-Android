package au.org.ai.breadmanager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;


/**
 * requires: PKs and Names in String form
 * i.e. "pkey1~pkey2~pkey3~pkey4~pkey5" = "PKs"
 * i.e. "name1~name2~name3~name4~name5" = "names"
 * <p>
 * Also requires a requestCode code, which will determine the UI.
 * If not provided, will be set to 1.
 * 1 = Request Routes
 * 2 = Relinquish Routes
 * <p>
 * returns: selected String Array of Route PKs
 * i.e. {"pk1", "name1", "pk2", "name2", "pk3", "name2", "pk4", "name2"} = "pks"
 */
public class RequestActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle inputBundle) {
    super.onCreate(inputBundle);
    setContentView(R.layout.activity_request);

    Intent intent = getIntent();
    String pkString = intent.getStringExtra("PKs");
    String nameString = intent.getStringExtra("names");
    final int requestCode = intent.getIntExtra("requestCode", 1);

    final HashMap<Integer, View> layouts = new HashMap<>();
    String[] PKs = pkString.split("~");
    String[] names = nameString.split("~");

    final LinearLayout layout = (LinearLayout) findViewById(R.id.request_route_holder);
    Context context = layout.getContext();
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(
        Context.LAYOUT_INFLATER_SERVICE);

    for (int i = 0; i < names.length; i++) {
      View v = inflater.inflate(R.layout.activity_request_route_layout, null);

      ((TextView) v.findViewById(R.id.request_internal_route_name)).setText(names[i]);
      layout.addView(v);
      layouts.put(Integer.parseInt(PKs[i]), v);
    }

    int id = (requestCode == 2) ? R.string.relinquish_routes : R.string.request_routes;
    Button confirmButton = ((Button) findViewById(R.id.request_confirm_button));
    confirmButton.setText(getApplicationContext().getString(id));

    confirmButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        HashSet<String> toReturn = new HashSet<>();
        for (int i : layouts.keySet()) {
          Switch sw = (Switch) layouts.get(i).findViewById(R.id.request_internal_switch);
          if (sw.isChecked()) {
            toReturn.add(Integer.toString(i));
          }
        }

        if (toReturn.isEmpty()) {
          setResult(RESULT_CANCELED);
        } else {

          Intent data = new Intent();
          data.putExtra("PKs", toReturn.toArray(new String[0]));
          setResult(RESULT_OK, data);
        }

        finish();
      }
    });

    findViewById(R.id.request_cancel_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        setResult(RESULT_CANCELED);
        finish();
      }
    });
  }
}
