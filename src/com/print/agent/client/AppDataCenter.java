package com.print.agent.client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.itron.android.ftf.Util;
import com.itron.protol.android.CommandReturn;
import com.print.util.AssetsUtil;
import com.print.util.DateUtil;
import com.print.util.PhoneUtil;

public class AppDataCenter {

	// FSK相关
	private static String __PSAMNO = null; // psam卡号 8
	private static String __TERSERIALNO = null; // 终端序列号 - 20

	private static String __PSAMRANDOM = null; // 随机数
	private static String __PSAMPIN = null; // pin密文
	private static String __PSAMMAC = null; // mac密文
	private static String __PSAMTRACK = null; // 磁道密文
	private static String __PAN = null; // PAN
	private static String __ENCCARDNO = null; // 卡号密文
	private static String __VENDOR = null; // 商户号
	private static String __TERID = null; // 终端号
	private static String __CARDNO = null; // 磁卡卡号

	// 爱刷相关
	private static String __KSN = null;
	private static String __ENCTRACK = null;
	private static String __RANDOM = null;
	private static String __MASKEDPAN = null;

	// 其它参数
	private static String __TRACEAUDITNUM = null; // 系统追踪号
	private static String __BATCHNUM = null; // 批次号

	private static String __ADDRESS = "UNKNOWN";

	private static String __PHONENUM = null;

	private static String __CURRENTVERSION = null;

	private static int __VERSIONCODE = 0;

	// 默认为手机日期。如果没有此默认值，在登陆失败时，跳转到错误界面等的没有日期。
	private static String __SERVEREDATE = DateUtil.formatDate2(new Date());

	/*-------------------------------------------------------------------*/

	// transferCode --> transferName
	private static HashMap<String, String> transferNameMap = new HashMap<String, String>();
	private static HashMap<String, String> reversalMap = new HashMap<String, String>();

	// 必须全大写
	public static String getValue(String propertyName) {
		try {
			String fieldName = propertyName.trim().toUpperCase();
			if (fieldName.equals("__TRACEAUDITNUM")) {
				return getTraceAuditNum();
			} else if (fieldName.equals("__BATCHNUM")) {
				return getBatchNum();
			} else if (fieldName.equals("__YYYYMMDD")) {
				// return __SERVEREDATE; // 已由返回服务器更改为返回手机的日期
				return DateUtil.formatDate2(new Date()); // 返回手机本地日期

			} else if (fieldName.equals("__YYYY-MM-DD")) { // yyyy-MM-dd
				// return DateUtil.formatDateStr(__SERVEREDATE);
				return DateUtil.getSystemDate();

			} else if (fieldName.equals("__HHMMSS")) { // hhmmss
				return DateUtil.getSystemTime();

			} else if (fieldName.equals("__MMDD")) { // MMdd
				// return DateUtil.formatMonthDay(__SERVEREDATE);
				return DateUtil.getSystemMonthDay();

			} else if (fieldName.equals("__UUID")) {
				return getUUID();

			} else if (fieldName.equals("__PHONENUM")) {
				return getPhoneNum();

			} else if (fieldName.equals("__PHONENUMWITHLABEL")) {
				return getPhoneNumWithLabel();

			} else if (fieldName.equals("__CURRENTVERSION")) {
				return getCurrentVersion();

			} else if (fieldName.equals("__VERSIONCODE")) {
				return String.valueOf(__VERSIONCODE);

			} else if (fieldName.equals("__MERCHERNAME")) {
				if (Constant.isStatic) {
					return "中国联通";
				} else {
					return ApplicationEnvironment.getInstance().getPreferences().getString(Constant.MERCHERNAME, "");
				}
			}

			else {
				Class<?> thisClass = Class.forName("com.print.agent.client.AppDataCenter");
				Field field = thisClass.getDeclaredField(fieldName); // private
				return (String) field.get(thisClass);
			}

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return "";
	}

	// 取系统追踪号，6个字节数字定长域
	private static String getTraceAuditNum() {
		SharedPreferences preferences = ApplicationEnvironment.getInstance().getPreferences();

		int number = preferences.getInt(Constant.TRACEAUDITNUM, 1);

		Editor editor = preferences.edit();
		editor.putInt(Constant.TRACEAUDITNUM, (number + 1) == 1000000 ? 1 : (number + 1));
		editor.commit();

		__TRACEAUDITNUM = String.format("%06d", number);

		// ///////////////// for test
		/*
		 * Random rand = new Random(); int i = rand.nextInt(); i =
		 * rand.nextInt(100000); __TRACEAUDITNUM = String.format("%06d", i);
		 */
		// /////////////////

		return __TRACEAUDITNUM;
	}

	// 签到后
	public static void setBatchNum(String batchNum) {
		__BATCHNUM = batchNum;

		Editor editor = ApplicationEnvironment.getInstance().getPreferences().edit();
		editor.putString(Constant.BATCHNUM, batchNum);
		editor.commit();
	}

	private static String getBatchNum() {
		if (null != __BATCHNUM) {
			return __BATCHNUM;

		} else {
			SharedPreferences preferences = ApplicationEnvironment.getInstance().getPreferences();
			__BATCHNUM = preferences.getString(Constant.BATCHNUM, "000001");
			return __BATCHNUM;
		}
	}

	private static String getUUID() {
		return ApplicationEnvironment.getInstance().getPreferences().getString(Constant.UUIDSTRING, "");
	}

	public static void setAddress(String address) {
		__ADDRESS = address;
	}

	private static String getCurrentVersion() {
		int currentVersion = ApplicationEnvironment.getInstance().getPreferences().getInt(Constant.VERSION, 0);
		__CURRENTVERSION = String.valueOf(currentVersion);
		return __CURRENTVERSION;
	}

	/**
	 * 关于手机号的问题 一个容易忽视的情况是如果用户通过服务器更改了手机号，要能保证用户重新注册时也能够修改手机号。
	 * 所以现在是无论是从SIM卡中取值还是从本地保存的数据中取值，都是允许用户去更改这个值的，只不过如果和服务器中的手机号不匹配会注册或登陆失败。
	 */
	public static void setPhoneNum(String phoneNum) {
		__PHONENUM = phoneNum;

		Editor editor = ApplicationEnvironment.getInstance().getPreferences().edit();
		editor.putString(Constant.PHONENUM, phoneNum);
		editor.commit();
	}

	// 只在注册和登陆时使用该方法。
	private static String getPhoneNum() {
		if (__PHONENUM != null)
			return __PHONENUM;

		__PHONENUM = ApplicationEnvironment.getInstance().getPreferences().getString(Constant.PHONENUM, "");
		if (__PHONENUM != null && !"".equals(__PHONENUM))
			return __PHONENUM;

		__PHONENUM = PhoneUtil.getPhoneNum();
		if (__PHONENUM != null && !"".equals(__PHONENUM))
			return __PHONENUM;

		return "";
	}

	// 在交易页面调用
	private static String getPhoneNumWithLabel() {
		return "注册号码：" + getPhoneNum();
	}

	public static void setVersionCode(int versionCode) {
		__VERSIONCODE = versionCode;
	}

	// 设置取得服务器返回日期
	public static void setServerDate(String yyyyMMdd) {
		if (null != yyyyMMdd && yyyyMMdd.matches("^\\d{8}$")) {
			__SERVEREDATE = yyyyMMdd;
		}
	}

	public static String getServerDate() {
		return DateUtil.formatDateStr(__SERVEREDATE);
	}

	// 设置FSK的返回值。
	// 这里假设开始一项新的交易时，所用到的参数一定会是此交易所想要的参数，能及时更新到。理论上也是这样子的。
	public static void setFSKArgs(CommandReturn cmdReturn) {
		if (null != cmdReturn.Return_PSAMNo)
			__PSAMNO = Util.BytesToString(cmdReturn.Return_PSAMNo);

		if (null != cmdReturn.Return_TerSerialNo)
			__TERSERIALNO = Util.BcdToString(cmdReturn.Return_TerSerialNo);

		if (null != cmdReturn.Return_PSAMRandom)
			__PSAMRANDOM = Util.BinToHex(cmdReturn.Return_PSAMRandom, 0, cmdReturn.Return_PSAMRandom.length);

		if (null != cmdReturn.Return_PSAMPIN)
			__PSAMPIN = Util.BinToHex(cmdReturn.Return_PSAMPIN, 0, cmdReturn.Return_PSAMPIN.length);

		if (null != cmdReturn.Return_PSAMMAC)
			__PSAMMAC = Util.BytesToString(cmdReturn.Return_PSAMMAC);

		if (null != cmdReturn.Return_PSAMTrack)
			__PSAMTRACK = Util.BinToHex(cmdReturn.Return_PSAMTrack, 0, cmdReturn.Return_PSAMTrack.length);

		if (null != cmdReturn.Return_PAN)
			__PAN = Util.BinToHex(cmdReturn.Return_PAN, 0, cmdReturn.Return_PAN.length);

		if (null != cmdReturn.Return_ENCCardNo)
			__ENCCARDNO = Util.BinToHex(cmdReturn.Return_ENCCardNo, 0, cmdReturn.Return_ENCCardNo.length);

		if (null != cmdReturn.Return_Vendor)
			__VENDOR = Util.BytesToString(cmdReturn.Return_Vendor);

		if (null != cmdReturn.Return_TerID)
			__TERID = Util.BytesToString(cmdReturn.Return_TerID);

		if (null != cmdReturn.Return_CardNo) {
			__CARDNO = Util.BytesToString(cmdReturn.Return_CardNo);

			if (Constant.isStatic) {
				StaticNetClient.demo_accountNo = __CARDNO;
			}
		}

	}

	public static void setKSN(String ksn) {
		AppDataCenter.__KSN = ksn;
	}

	public static void setEncTrack(String str) {
		AppDataCenter.__ENCTRACK = str;
	}

	public static void setRandom(String random) {
		AppDataCenter.__RANDOM = random;
	}

	public static void setMaskedPAN(String pan) {
		AppDataCenter.__MASKEDPAN = pan;
	}
	
	public static String getMaskedPAN(){
		return AppDataCenter.__MASKEDPAN;
	}

	public static String getTransferName(String transferCode) {
		if (transferNameMap.size() > 0) {
			if (transferNameMap.containsKey(transferCode)) {
				return transferNameMap.get(transferCode);
			} else {
				return "未知";
			}
		} else {
			try {
				InputStream stream = AssetsUtil.getInputStreamFromPhone("transfername.xml");
				KXmlParser parser = new KXmlParser();
				parser.setInput(stream, "utf-8");
				int eventType = parser.getEventType();
				while (eventType != XmlPullParser.END_DOCUMENT) {
					switch (eventType) {
					case XmlPullParser.START_TAG:
						if ("item".equalsIgnoreCase(parser.getName())) {
							String code = parser.getAttributeValue(null, "code");
							String name = parser.getAttributeValue(null, "name");
							transferNameMap.put(code, name);
						}

						break;
					}
					eventType = parser.next();
				}

				if (transferNameMap.containsKey(transferCode)) {
					return transferNameMap.get(transferCode);
				} else {
					return "未知";
				}

			} catch (IOException e) {
				e.printStackTrace();
				return "未知";
			} catch (XmlPullParserException e) {
				e.printStackTrace();
				return "未知";
			}
		}
	}

	public static HashMap<String, String> getReversalMap() {
		if (null != reversalMap && reversalMap.size() > 0) {
			return reversalMap;

		} else {
			try {
				InputStream stream = AssetsUtil.getInputStreamFromPhone("reversalmap.xml");
				KXmlParser parser = new KXmlParser();
				parser.setInput(stream, "utf-8");
				int eventType = parser.getEventType();
				while (eventType != XmlPullParser.END_DOCUMENT) {
					switch (eventType) {
					case XmlPullParser.START_TAG:
						if ("item".equalsIgnoreCase(parser.getName())) {
							String key = parser.getAttributeValue(null, "key");
							String value = parser.getAttributeValue(null, "value");
							reversalMap.put(key, value);
						}

						break;
					}
					eventType = parser.next();// 进入下一个元素并触发相应事件
				}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			}

			return reversalMap;
		}
	}

}
