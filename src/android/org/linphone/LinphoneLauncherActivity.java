package org.linphone;

/*
LinphoneLauncherActivity.java
Copyright (C) 2017  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import org.linphone.assistant.AssistantActivity;
import org.linphone.assistant.RemoteProvisioningActivity;
import org.linphone.database.DbContext;
import org.linphone.mediastream.Version;
import org.linphone.myactivity.LoginActivity;
import org.linphone.network.NetContext;
import org.linphone.network.Service;
import org.linphone.network.UrlNetContext;
import org.linphone.network.models.DSCongTyResponse;
import org.linphone.network.models.VoidRespon;
import org.linphone.tutorials.TutorialLauncherActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Intent.ACTION_MAIN;

/**
 * Launch Linphone main activity when Service is ready.
 */
public class LinphoneLauncherActivity extends Activity {

	private final String ACTION_CALL_LINPHONE  = "org.linphone.intent.action.CallLaunched";
	public static boolean isLinphoneActivity = false;
	private Handler mHandler;
	private ServiceWaitThread mServiceThread;
	private String addressToCall;
	private Uri uriToResolve;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Hack to avoid to draw twice LinphoneActivity on tablets
		if (getResources().getBoolean(R.bool.orientation_portrait_only)) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}
		setContentView(R.layout.launch_screen);

		mHandler = new Handler();

		Intent intent = getIntent();
		if (intent != null) {
			String action = intent.getAction();
			if (Intent.ACTION_CALL.equals(action)) {
				if (intent.getData() != null) {
					addressToCall = intent.getData().toString();
					addressToCall = addressToCall.replace("%40", "@");
					addressToCall = addressToCall.replace("%3A", ":");
					if (addressToCall.startsWith("sip:")) {
						addressToCall = addressToCall.substring("sip:".length());
					}
				}
			} else if (Intent.ACTION_VIEW.equals(action)) {
				if (LinphoneService.isReady()) {
					addressToCall = ContactsManager.getInstance().getAddressOrNumberForAndroidContact(getContentResolver(), intent.getData());
				} else {
					uriToResolve = intent.getData();
				}
			}
		}

		UrlNetContext.getInstance().init(this);
		if (LinphoneService.isReady()) {

			Service service = UrlNetContext.getInstance().create(Service.class);
			service.getDsCongTy("/AppRouter.aspx?loaicongty=2").enqueue(new Callback<DSCongTyResponse>() {
				@Override
				public void onResponse(Call<DSCongTyResponse> call, Response<DSCongTyResponse> response) {

					DSCongTyResponse dsCongTyResponse = response.body();
					boolean dsCongTyResponseStatus = dsCongTyResponse.isStatus();
					if (!dsCongTyResponseStatus) {
						Toast.makeText(LinphoneLauncherActivity.this, dsCongTyResponse.getMsg(), Toast.LENGTH_SHORT).show();
					} else {
						try {
							DbContext.getInstance().setDsCongty(dsCongTyResponse,LinphoneLauncherActivity.this);
							Log.d("LinphoneLaunched", "onResponse: "+DbContext.getInstance().getDsCongTy(LinphoneLauncherActivity.this).toString());
							NetContext.getInstance().setBASE_URL(dsCongTyResponse.getDscongty().get(0).getBaseURL());
							onServiceReady();
						} catch (Exception e) {
							android.util.Log.d("LinphoneLaunched", "onResponse: "+e.toString());
						}
					}
				}

				@Override
				public void onFailure(Call<DSCongTyResponse> call, Throwable t) {
					Toast.makeText(LinphoneLauncherActivity.this,
							"Không có kết nối internet,vui lòng bật wifi hoặc 3g",
							Toast.LENGTH_SHORT).show();

				}
			});

		} else {
			// start linphone as background
			Service service = UrlNetContext.getInstance().create(Service.class);
			service.getDsCongTy("/AppRouter.aspx?loaicongty=2").enqueue(new Callback<DSCongTyResponse>() {
				@Override
				public void onResponse(Call<DSCongTyResponse> call, Response<DSCongTyResponse> response) {

					DSCongTyResponse dsCongTyResponse = response.body();
					Log.d("234234", "onResponse: "+dsCongTyResponse.toString());
					boolean dsCongTyResponseStatus = dsCongTyResponse.isStatus();
					if (!dsCongTyResponseStatus) {
						Toast.makeText(LinphoneLauncherActivity.this, dsCongTyResponse.getMsg(), Toast.LENGTH_SHORT).show();
					} else {
						try {
							DbContext.getInstance().setDsCongty(dsCongTyResponse,LinphoneLauncherActivity.this);
							NetContext.getInstance().setBASE_URL(dsCongTyResponse.getDscongty().get(0).getBaseURL());
							startService(new Intent(ACTION_MAIN).setClass(LinphoneLauncherActivity.this, LinphoneService.class));
//            Intent startService = new Intent("com.example.helloandroid.alarms");
//            sendBroadcast(startService);
							mServiceThread = new ServiceWaitThread();
							mServiceThread.start();
						} catch (Exception e) {
							android.util.Log.d("LinphoneLaunched", "onResponse: "+e.toString());
						}
					}
				}

				@Override
				public void onFailure(Call<DSCongTyResponse> call, Throwable t) {
					Toast.makeText(LinphoneLauncherActivity.this,
							"Không có kết nối internet,vui lòng bật wifi hoặc 3g",
							Toast.LENGTH_SHORT).show();

				}
			});

		}
	}

	@Override
	protected void onResume(){
		super.onResume();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
	}

	protected void onServiceReady() {
		final Class<? extends Activity> classToStart;
		if (getResources().getBoolean(R.bool.show_tutorials_instead_of_app)) {
			classToStart = TutorialLauncherActivity.class;
		} else if (getResources().getBoolean(R.bool.display_sms_remote_provisioning_activity) && LinphonePreferences.instance().isFirstRemoteProvisioning()) {
			classToStart = RemoteProvisioningActivity.class;
		} else {
			classToStart = LoginActivity.class;
		}

		// We need LinphoneService to start bluetoothManager
		if (Version.sdkAboveOrEqual(Version.API11_HONEYCOMB_30)) {
			BluetoothManager.getInstance().initBluetooth();
		}

		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				Intent newIntent = new Intent(LinphoneLauncherActivity.this, classToStart);
				Intent intent = getIntent();
                String stringFileShared = null;
				String stringUriFileShared = null;
				Uri fileUri = null;
				if (intent != null) {
					String action = intent.getAction();
					String type = intent.getType();
					newIntent.setData(intent.getData());
					if (Intent.ACTION_SEND.equals(action) && type != null) {
						if (type.contains("text/")){
							if(("text/plain").equals(type) && (String)intent.getStringExtra(Intent.EXTRA_TEXT)!= null) {
								stringFileShared = intent.getStringExtra(Intent.EXTRA_TEXT);
								newIntent.putExtra("msgShared", stringFileShared);
							} else if(((Uri) intent.getExtras().get(Intent.EXTRA_STREAM)) != null){
								stringFileShared = (LinphoneUtils.createCvsFromString(LinphoneUtils.processContactUri(getApplicationContext(), (Uri)intent.getExtras().get(Intent.EXTRA_STREAM)))).toString();
								newIntent.putExtra("fileShared", stringFileShared);
							}
						}else {
							if(((String) intent.getStringExtra(Intent.EXTRA_STREAM)) != null){
								stringUriFileShared = intent.getStringExtra(Intent.EXTRA_STREAM);
							}else {
								fileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
								stringUriFileShared = LinphoneUtils.getRealPathFromURI(getBaseContext(), fileUri);
								if(stringUriFileShared == null)
									if(fileUri.getPath().contains("/0/1/mediakey:/local") || fileUri.getPath().contains("/ORIGINAL/NONE/")) {
										stringUriFileShared = LinphoneUtils.getFilePath(getBaseContext(), fileUri);
									}else
										stringUriFileShared = fileUri.getPath();
							}
							newIntent.putExtra("fileShared", stringUriFileShared);
						}
					}else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
						if (type.startsWith("image/")) {
							//TODO : Manage multiple files sharing
						}
					}else if( ACTION_CALL_LINPHONE.equals(action) && (intent.getStringExtra("NumberToCall") != null)) {
						String numberToCall = intent.getStringExtra("NumberToCall");
						if (CallActivity.isInstanciated()) {
							CallActivity.instance().startIncomingCallActivity();
						} else {
							LinphoneManager.getInstance().newOutgoingCall(numberToCall, null);
						}
					}
				}
				if (uriToResolve != null) {
					addressToCall = ContactsManager.getInstance().getAddressOrNumberForAndroidContact(getContentResolver(), uriToResolve);
					Log.i("LinphoneLauncher", "Intent has uri to resolve : " + uriToResolve.toString());
					uriToResolve = null;
				}
				if (addressToCall != null) {
					newIntent.putExtra("SipUriOrNumber", addressToCall);
					Log.i("LinphoneLauncher", "Intent has address to call : " + addressToCall);
					addressToCall = null;
				}
				startActivity(newIntent);
                if (classToStart == LinphoneActivity.class && LinphoneActivity.isInstanciated() && (stringFileShared != null || fileUri != null)) {
					if(stringFileShared != null) {
						LinphoneActivity.instance().displayChat(null, stringFileShared, null);
					}
					else if(fileUri != null) {
						LinphoneActivity.instance().displayChat(null, null, stringUriFileShared);
					}
                }
				finish();
			}
		}, 1000);
	}



	private class ServiceWaitThread extends Thread {
		public void run() {
			while (!LinphoneService.isReady()) {
				try {
					sleep(30);
				} catch (InterruptedException e) {
					throw new RuntimeException("waiting thread sleep() has been interrupted");
				}
			}
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					onServiceReady();
				}
			});
			mServiceThread = null;
		}
	}

}


