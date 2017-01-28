package au.org.ai.breadmanager;

/**
 * Created by Benjamin on 06/11/2016.
 * <p>
 * To be used only for holding static utility methods.
 */
class Utilities {

  /**
   * Utility method for collapsing (imploding) an array into a string.
   *
   * @param separator
   *     The string to be put between each of data's elements
   * @param data
   *     The string array to be imploded
   *
   * @return A string containing the elements of the array, with the separator string in between
   */
  static String implode(String separator, String... data) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < data.length - 1; i++) {
      //data.length - 1 => to not add separator at the end
      if (!data[i].matches(" *")) {//empty string are ""; " "; "  "; and so on
        sb.append(data[i]);
        sb.append(separator);
      }
    }
    sb.append(data[data.length - 1].trim());
    return sb.toString();
  }
}
