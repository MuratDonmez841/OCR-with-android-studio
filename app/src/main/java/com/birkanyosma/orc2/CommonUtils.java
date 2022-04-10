package com.birkanyosma.orc2;

import java.io.File;

import android.os.Environment;
import android.util.Log;

/**
 *
 kesinlikle değişiklik yapmayın
 */
public class CommonUtils {
	public static String TAG = "COMPA";
	public static String EXAM_CFG = "exam.txt";
	public static String REGEX_QUESTION_ANSWER = ":|=";
	public static String REGEX_QUESTION = "\\.|,";
	public static String APP_PATH = Environment.getExternalStorageDirectory() + "/RecognizeTextOCR/";

	/**
	 * geçici resim klasörünü temizliyoruz
	 */
	public static void cleanFolder() {
		String datapath = APP_PATH;
		File tenpPath = new File(datapath);
		if (!tenpPath.exists()) {
			if (!tenpPath.mkdir()) {
				// path oluşturulmassa
			}
		} else {
			for (File child : tenpPath.listFiles()) {
				// config dosyaları
				if (!child.getName().contains(".txt")) {
					child.delete();
				}
			}
		}
	}

	public static void info(Object msg) {
		Log.i(TAG, msg.toString());
	}

}
