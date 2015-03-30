package com.bebeeru.wifi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.bebeeru.wifi.WiFiUtils.IpAssignment;
import com.bebeeru.wifi.WiFiUtils.NetworkInfo;

/**
 * 配置WiFi信息
 * 
 * @author BlueTel
 */
public class MainActivity extends Activity {

	private WifiManager mWifiManager;
	// Input
	private EditText 	mIpAddressView;
	private EditText 	mGatewayView;
	private EditText 	mDns1View;
	private EditText 	mDns2View;
	// Output
	private TextView 	mInfoView;
	
	private IpAssignment mIpAssignment = IpAssignment.STATIC;;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		initViews();
	}
	
	private void initViews() {
		mIpAddressView = (EditText) findViewById(R.id.et_ip);
		mGatewayView = (EditText) findViewById(R.id.et_gateway);
		mDns1View = (EditText) findViewById(R.id.et_dns);
		mDns2View = (EditText) findViewById(R.id.et_dns2);
		mInfoView = (TextView) findViewById(R.id.tv);
		
		findViewById(R.id.btn_update).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String ipAddress 	= mIpAddressView.getText().toString().trim();
				String gateway 		= mGatewayView.getText().toString().trim();
				String dns1 		= mDns1View.getText().toString().trim();
				String dns2 		= mDns2View.getText().toString().trim();

				boolean succeed = setIpAddress(mIpAssignment, ipAddress, gateway, dns1, dns2);
				Toast.makeText(MainActivity.this, succeed ? "Ok" : "Failed", Toast.LENGTH_SHORT).show();
			}
		});
		
		findViewById(R.id.btn_show).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mInfoView.setText(WiFiUtils.getNetworkInfo(MainActivity.this).toString());	
			}
		});
		
		RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
				case R.id.rd_static:
					mIpAssignment = IpAssignment.STATIC;
					break;
				case R.id.rd_dhcp:
					mIpAssignment = IpAssignment.DHCP;
					break;
				}
				Toast.makeText(MainActivity.this, mIpAssignment.stringValue(), Toast.LENGTH_SHORT).show();
			}
		});
		
		setDefautValues();
	}
	
	private void setDefautValues() {
		NetworkInfo networkInfo = WiFiUtils.getNetworkInfo(this);
		if (networkInfo != null) {
			setValue(mIpAddressView, networkInfo.ipAddress);
			setValue(mGatewayView, networkInfo.gateway);
			setValue(mDns1View, networkInfo.dns1);
			setValue(mDns1View, networkInfo.dns1);
			setValue(mInfoView, networkInfo.toString());
		}
	}
	
	private void setValue(TextView view, String text) {
		view.setText(text == null ? "" : text);
	}
	
	/*
	 * 配置信息
	 * 
	 * 1, wifi名称
	 * 2, 静态(STATIC) 还是动态(DHCP)
	 * 3, 静态ip地址, 若是dhcp就随意了
	 * 4, 网关
	 * 5, dns
	 * 
	 * 
	 * 专门设置wifi的app
	 * # 重置
	 * # 所有wifi相同配置
	 * # 配置单一wifi
	 * 
	 * 1, 启动dump所有保存的wifi的信息
	 * 2, 判断操作 类型
	 * 3, 进行配置
	 * 4, 重启wifi
	 * 
	 * 
	 * 连接某一个wifi
	 * 
	 * mWifiManager.disconnect()
	 * mWifiManager.enableNetwork()
	 * mWifiManager.reconnect()
	 * 
	 */
	
	boolean setIpAddress(IpAssignment type, String ip, String gateway, String dns1, String dns2) {
		boolean updateIpSuccess = false;
		
		WifiConfiguration wifiConf = null;
		
		WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();        
        for (WifiConfiguration conf : configuredNetworks){
        	String ssid = conf.SSID;		// "test2"
        	int priority = conf.priority;	// 优先级
        	String key = conf.preSharedKey;
            if (conf.networkId == connectionInfo.getNetworkId()){
                wifiConf = conf;
                break;              
            }
        }
        
		// 没有wifi还扯什么
		if (wifiConf == null) {
			return false;
		}
		
		// mWifiManager.reconnect();
		mWifiManager.disconnect();
		try{
	        WiFiUtils.setIpAssignment(type, wifiConf); // or "DHCP" for dynamic setting
	        
	        if(IpAssignment.STATIC == type) {
	        	WiFiUtils.setIpAddress(InetAddress.getByName(ip), 24, wifiConf);
	        	WiFiUtils.setGateway(InetAddress.getByName(gateway), wifiConf);
	        	WiFiUtils.setDNS(InetAddress.getByName(dns1), InetAddress.getByName(dns2), wifiConf);
	        }
	        
			int networkId = mWifiManager.updateNetwork(wifiConf);		// apply the setting
			if(networkId != -1) {
				updateIpSuccess = mWifiManager.saveConfiguration(); 	//Save it
			}
			
			mWifiManager.enableNetwork(networkId, true);
			mWifiManager.reconnect();
	    }catch(Exception e){
	        e.printStackTrace();
	    }
		return updateIpSuccess;
	}
}