package fr.bordeaux.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class TimeConvertorTest {

  @Test
  public void checkFormatTime() {
    assertEquals("00 : 00 : 00", TimeConvertor.formatTime(0));
    assertEquals("00 : 01 : 52", TimeConvertor.formatTime(1528));
    assertEquals("01 : 05", TimeConvertor.formatTime(65524));
  }

  @Test
  public void testNegativeAndZeroTime() {
    assertEquals("00 : 00 : 00", TimeConvertor.formatTime(0));
    assertEquals("00 : 00 : 00", TimeConvertor.formatTime(-1));
    assertEquals("00 : 00 : 00", TimeConvertor.formatTime(-1000));
  }

  @Test
  public void testMinuteBoundaries() {
    // 1 minute = 60,000 ms
    assertEquals("01 : 00", TimeConvertor.formatTime(60000));
    // Just below 1 minute should show seconds : centiseconds
    assertEquals("00 : 59 : 99", TimeConvertor.formatTime(59999));
    // Half a second
    assertEquals("00 : 00 : 50", TimeConvertor.formatTime(500));
  }

  @Test
  public void testMultipleMinutes() {
    // 2 min 5 sec = 125,000 ms
    assertEquals("02 : 05", TimeConvertor.formatTime(125000));
    // 10 min = 600,000 ms
    assertEquals("10 : 00", TimeConvertor.formatTime(600000));
  }
}
