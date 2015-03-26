package com.bebeeru.wifi;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * 通过反射更新WiFi配置信息
 * 
 * @author BlueTel
 */
public class WiFiUtils {
	
	private WiFiUtils() {}

	public static void setIpAssignment(IpAssignment ipAssignmentType, WifiConfiguration wifiConf) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		setEnumField(wifiConf, ipAssignmentType.stringValue(), "ipAssignment");
	}

	public static void setIpAddress(InetAddress addr, int prefixLength, WifiConfiguration wifiConf) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, ClassNotFoundException, InstantiationException, InvocationTargetException {
		Object linkProperties = getField(wifiConf, "linkProperties");
		if (linkProperties == null)
			return;
		
		Class laClass = Class.forName("android.net.LinkAddress");
		Constructor laConstructor = laClass.getConstructor(new Class[] {InetAddress.class, int.class });
		Object linkAddress = laConstructor.newInstance(addr, prefixLength);

		ArrayList mLinkAddresses = (ArrayList) getDeclaredField(linkProperties, "mLinkAddresses");
		mLinkAddresses.clear();
		mLinkAddresses.add(linkAddress);
	}

	public static void setGateway(InetAddress gateway, WifiConfiguration wifiConf) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException, InstantiationException, InvocationTargetException {
		
		Object linkProperties = getField(wifiConf, "linkProperties");
		if (linkProperties == null)
			return;
		
		Class routeInfoClass = Class.forName("android.net.RouteInfo");
		Constructor routeInfoConstructor = routeInfoClass.getConstructor(new Class[] { InetAddress.class });
		Object routeInfo = routeInfoConstructor.newInstance(gateway);

		ArrayList mRoutes = (ArrayList) getDeclaredField(linkProperties, "mRoutes");
		mRoutes.clear();
		mRoutes.add(routeInfo);
	}

	public static void setDNS(InetAddress dns1, InetAddress dns2, WifiConfiguration wifiConf) throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
		Object linkProperties = getField(wifiConf, "linkProperties");
		if (linkProperties == null)
			return;

		ArrayList<InetAddress> mDnses = (ArrayList<InetAddress>) getDeclaredField(linkProperties, "mDnses");
		mDnses.clear(); // or add a new dns address , here I just want to replace DNS1
		if (dns1 != null)
			mDnses.add(dns1);
		if (dns2 != null)
			mDnses.add(dns2);
	}
	
	public static NetworkInfo getNetworkInfo(Context context) {
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		DhcpInfo dhcpInfo = wm.getDhcpInfo();
		
		NetworkInfo info = new NetworkInfo();
		info.ipAddress = NetworkUtils.intToInetAddress(dhcpInfo.ipAddress).getHostAddress();
		info.gateway = NetworkUtils.intToInetAddress(dhcpInfo.gateway).getHostAddress();
		
		WifiInfo connectionInfo = wm.getConnectionInfo();
		List<WifiConfiguration> configuredNetworks = wm.getConfiguredNetworks();
		for(WifiConfiguration config: configuredNetworks) {
			if(connectionInfo.getNetworkId() == config.networkId) {
				// DNS
				ArrayList<InetAddress> dnses = getDnses(config);
				if(dnses != null) {
					info.dns1 = getDns(dnses, 0);
					info.dns2 = getDns(dnses, 1);
				}
				break;
			}
		}
		return info;
	}
	
	private static String getDns(ArrayList<InetAddress> dnses, int index) {
		String dns = null;
		try {
			dns = dnses.get(index).getHostAddress();
		}catch(Exception e) {
			// 数组越界异常
		}
		return dns == null ? "" : dns;
	}
	
	private static ArrayList<InetAddress> getDnses(WifiConfiguration wifiConf) {
		try {
			Object linkProperties = getField(wifiConf, "linkProperties");

			if (linkProperties == null)
				return null;

			return (ArrayList<InetAddress>) getDeclaredField(linkProperties, "mDnses");
		} catch (Exception e) {
			// 反射出错
		}
		return null;
	}
	
	
	// ============================
	// 反射
	// ============================

	private static Object getField(Object obj, String name) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException { 
		
		Field f = obj.getClass().getField(name);
		Object out = f.get(obj);
		return out;
	}

	private static Object getDeclaredField(Object obj, String name) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field f = obj.getClass().getDeclaredField(name);
		f.setAccessible(true);
		Object out = f.get(obj);
		return out;
	}

	private static void setEnumField(Object obj, String value, String name) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field f = obj.getClass().getField(name);
		f.set(obj, Enum.valueOf((Class<Enum>) f.getType(), value));
	}
	
	public static class NetworkInfo {
		
		String ipAddress;
		String gateway;
		String dns1;
		String dns2;
		String subnetMask = "255.255.255.0";
		IpAssignment ipAssignment;
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(ipAddress == null ? "" : ipAddress);
			sb.append("\n"); sb.append(gateway == null ? "" : gateway);
			sb.append("\n"); sb.append(dns1 == null ? "" : dns1);
			sb.append("\n"); sb.append(dns2 == null ? "" : dns2);
			sb.append("\n"); sb.append(subnetMask == null ? "" : subnetMask);
			sb.append("\n"); sb.append(ipAssignment == null ? "" : ipAssignment.stringValue());
			return sb.toString();
		}
	}
	
	public static enum IpAssignment {
		
		STATIC("STATIC"), DHCP("DHCP");

		private final String value;

		IpAssignment(String value) {
			this.value = value;
		}

		String stringValue() {
			return this.value;
		}
	}
}
