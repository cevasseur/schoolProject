package fr.bordeaux.util;

import java.util.concurrent.TimeUnit;

/** Time conversion utility for Quoridor game timers. */
public final class TimeConvertor {

  /** Private constructor to prevent instantiation of this utility class. */
  private TimeConvertor() {}

  /**
   * Formats duration in milliseconds to "MM : SS" or "SS : ms".
   *
   * @param timeMilli duration to format in milliseconds.
   * @return a formatted {@link String} representing the time.
   */
  public static String formatTime(final long timeMilli) {

    final long minutes = TimeUnit.MILLISECONDS.toMinutes(timeMilli);
    final long seconds =
        TimeUnit.MILLISECONDS.toSeconds(timeMilli) - TimeUnit.MINUTES.toSeconds(minutes);
    final long milliseconds =
        timeMilli - TimeUnit.SECONDS.toMillis(seconds) - TimeUnit.MINUTES.toMillis(minutes);

    final String result;

    if (timeMilli <= 0) {
      result = "00 : 00 : 00";
    } else if (minutes <= 0) {
      result = String.format("00 : %02d : %02d", seconds, milliseconds / 10);
    } else {
      result = String.format("%02d : %02d", minutes, seconds);
    }

    return result;
  }
}
