package au.org.ai.breadmanager;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;

/**
 * Created by Benjamin on 05/11/2016.
 * Is the only way in which networking is done throughout the application.
 * All results are returned directly to the processQuery method within MainActivity
 */
class Networker extends AsyncTask<String, Integer, String> {

  public static final String CONNECT_SIGNAL = "connect_signal";
  public static final String REQUEST_ROUTES_SIGNAL = "request_routes_signal";
  private static final String DEFAULT_SIGNAL = "misc";

  private static final Hashtable<String, Boolean> activeTasks = new Hashtable<>(); // type -> active?

  private final MainActivity mainActivity;
  private String type;

  /**
   * @param mainActivity The Application's main activity.
   */
  Networker(MainActivity mainActivity) {
    this(mainActivity, DEFAULT_SIGNAL);
  }

  Networker(MainActivity mainActivity, String type) {
    this.mainActivity = mainActivity;
    this.type = type;
  }

  @Override protected String doInBackground(String... strings) {
    if (strings == null || strings[0].equals("")) {
      return "-1"; // invalid input.
    }

    if (taskAlreadyActive(type, false)) {
      return "-4"; // task already active
    }

    String query = strings[0];

    String response = "-3";
    //String TARGET_URL = "http://10.0.0.247/cityNet/BreadScript.php?";
    //String TARGET_URL = "http://stott.id.au/cityNet/BreadScript.php?";
    String TARGET_URL = "http://ai.org.au/ai-includes/BreadScript.php?";
    String urlString = (TARGET_URL + query).replace(" ", "%20");
    System.out.println(urlString);

    try {
      URL url = new URL(urlString);
      BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
      response = br.readLine();
      br.close();
    } catch (IOException e) {
      e.printStackTrace();
      response = "-2"; // networking issue
    }

    return response;
  }

  @Override protected void onPostExecute(String response) {
    super.onPostExecute(response);

    String[] parts = response.split(":", 5);

    if (parts[0].length() <= 4) {
      System.out.println("error in Networker: " + response);
    } else {
      mainActivity.processQueryResponse(parts);
      taskAlreadyActive(type, true);
    }
  }

  private static synchronized boolean taskAlreadyActive(String type, boolean finished) {
    if (type == null || type.equals(DEFAULT_SIGNAL)) {
      return false;
    } else if (finished) {
      activeTasks.put(type, false);
      return false;
    }

    System.out.println(type);
    System.out.println(activeTasks.get(type));

    if (activeTasks.containsKey(type) == false || activeTasks.get(type) == false) {
      // not yet active
      activeTasks.put(type, true);
      return false;
    } else {
      // already active
      System.out.println("cancelling networker for: " + type);
      return true;
    }
  }
}