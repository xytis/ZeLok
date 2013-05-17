package org.nolife4life.zelok;

import java.util.TimeZone;

/**
 * Each possible tracker command is wrapped with convenient functions.
 */
public class TrackerMessage {
	/**
	 * What data should be collected on alarm mode
	 * true -> GPS + GSM
	 * false -> only GSM
	 * @param full
	 * @return
	 */
	public static String AlarmData(boolean full) {
		if (full) {
			return "CFG_AD_CELL+GPS";
		} else {
			return "CFG_AD_CELL";
		}
	}
	
	public static String AlarmDurationTime(int seconds) {
		assert seconds != 0 : "Should not pass duration of 0 seconds!";
		return "CFG_ADT_" + seconds;
	}
	
	/**
	 * Reformats number to format understandable by tracker.s
	 * @param number
	 * @return formated number. On error returns empty string.
	 */
	public static String FormatPhoneNumber(String number) {
		String result = number;
		result.trim();	//No spaces
		result.replaceAll("[ -/*]", ""); //No hyphens, magic symbols or stuff.
		if (result.matches("^\\+370\\d{8}")) {
			return result;
		}
		if (result.matches("^\\8\\d{8}")) {
			result = "+370" + result.substring(1);
			return result;
		}
		
		result = "";
		return result;
	}
	
	public static String AlarmDestinationNumber(String number) {
		assert number.length() != 0 : "Passed number should not be empty!";
		return "CFG_ADP_" + number;
	}
	
	public static String AlarmDataSendingPeriod(int seconds) {
		assert seconds >= 30 : "Period should be larger than 30 seconds!";
		return "CFG_ADSP_" + seconds;
	}
	
	public static String CoordinatesLoggingPeriod(int mode) {
		assert mode >= 0 && mode <= 24 : "Mode should be in range [0:24]!";
		return "CFG_CLP_" + mode;
	}
	
	/**
	 * What data should be collected periodicaly
	 * true -> GPS + GSM
	 * false -> only GSM
	 * @param full
	 * @return
	 */
	public static String PeriodicalLoggingData(boolean full) {
		if (full) {
			return "CFG_PLD_CELL+GPS";
		} else {
			return "CFG_PLD_CELL";
		}
	}
	
	public static String DeviceTimeZone(TimeZone timezone) {
		int hours = timezone.getRawOffset() / 3600000;
		return "CFG_DTZ_" + hours;
	}
	
	public static String ChargeBatteryNotification(int percent, String recipient) {
		assert percent >= 30 : "Recomended minimal percentage is 30.";
		assert percent <= 100 : "Percent value out of range!";
		return "CFG_CBN_" + percent + "_" + recipient;
	}
	
	//CFG_SMS24 not implemented.
	
	//Queries:
	
	public static String WhereAmI() {
		return "WIM?";
	}
}
