package com.example.casthelloworld;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class DojoSenderActivity extends ActionBarActivity {
	
	private static final String TAG = DojoSenderActivity.class.getSimpleName();


	private MediaRouter mMediaRouter;
	private MediaRouteSelector mMediaRouteSelector;
	private MediaRouter.Callback mMediaRouterCallback;

	private CastDevice mSelectedDevice;
	private GoogleApiClient mApiClient;
	
	private Cast.Listener mCastListener;
	private ConnectionCallbacks mConnectionCallbacks;
	private ConnectionFailedListener mConnectionFailedListener;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dojo_sender);
		
		mMediaRouter = MediaRouter.getInstance(getApplicationContext());
		mMediaRouteSelector = new MediaRouteSelector.Builder()
				.addControlCategory(
						CastMediaControlIntent.categoryForCast(getResources()
								.getString(R.string.app_id))).build();
		mMediaRouterCallback = new MyMediaRouterCallback();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main, menu);
		MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
		MediaRouteActionProvider mediaRouteActionProvider = (MediaRouteActionProvider) MenuItemCompat
				.getActionProvider(mediaRouteMenuItem);
		// Set the MediaRouteActionProvider selector for device discovery.
		mediaRouteActionProvider.setRouteSelector(mMediaRouteSelector);
		return true;
	}
	@Override
	protected void onResume() {
		super.onResume();
		// Start media router discovery
		mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback,
				MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
	}

	@Override
	protected void onPause() {
		if (isFinishing()) {
			// End media router discovery
			mMediaRouter.removeCallback(mMediaRouterCallback);
		}
		super.onPause();
	}
	

	private void launchReceiver() {
		try {
			mCastListener = new Cast.Listener() {

				@Override
				public void onApplicationDisconnected(int errorCode) {
					Log.d(TAG, "application has stopped");
//					teardown();
				}

			};
			// Connect to Google Play services
			mConnectionCallbacks = new ConnectionCallbacks();
			mConnectionFailedListener = new ConnectionFailedListener();
			Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
					.builder(mSelectedDevice, mCastListener);
			mApiClient = new GoogleApiClient.Builder(this)
					.addApi(Cast.API, apiOptionsBuilder.build())
					.addConnectionCallbacks(mConnectionCallbacks)
					.addOnConnectionFailedListener(mConnectionFailedListener)
					.build();

			mApiClient.connect();
		} catch (Exception e) {
			Log.e(TAG, "Failed launchReceiver", e);
		}
	}
	
	/**
	 * Google Play services callbacks
	 */
	private class ConnectionCallbacks implements
			GoogleApiClient.ConnectionCallbacks {
		@Override
		public void onConnected(Bundle connectionHint) {
			Log.d(TAG, "onConnected");

			if (mApiClient == null) {
				// We got disconnected while this runnable was pending
				// execution.
				return;
			}

			try {
//				if (mWaitingForReconnect) {
//					mWaitingForReconnect = false;
//
//					// Check if the receiver app is still running
//					if ((connectionHint != null)
//							&& connectionHint
//									.getBoolean(Cast.EXTRA_APP_NO_LONGER_RUNNING)) {
//						Log.d(TAG, "App  is no longer running");
//						teardown();
//					} else {
//						// Re-create the custom message channel
//						try {
//							Cast.CastApi.setMessageReceivedCallbacks(
//									mApiClient,
//									mHelloWorldChannel.getNamespace(),
//									mHelloWorldChannel);
//						} catch (IOException e) {
//							Log.e(TAG, "Exception while creating channel", e);
//						}
//					}
//				} else {
					// Launch the receiver app
					Cast.CastApi
							.launchApplication(mApiClient,
									getString(R.string.app_id), false)
									.setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
										@Override
										public void onResult(
												ApplicationConnectionResult result) {
											// TODO Auto-generated method stub
											sendMessage("{\"type\":\"setAngle\", \"x\": 2, \"y\": 2}");	
										}										
									});
//				}
			} catch (Exception e) {
				Log.e(TAG, "Failed to launch application", e);
			}
		}

		@Override
		public void onConnectionSuspended(int cause) {
			Log.d(TAG, "onConnectionSuspended");
//			mWaitingForReconnect = true;
		}
	}
	
	/**
	 * Google Play services callbacks
	 */
	private class ConnectionFailedListener implements
			GoogleApiClient.OnConnectionFailedListener {
		@Override
		public void onConnectionFailed(ConnectionResult result) {
			Log.e(TAG, "onConnectionFailed ");

//			teardown();
		}
	}
	
	/**
	 * Callback for MediaRouter events
	 */
	private class MyMediaRouterCallback extends MediaRouter.Callback {

		@Override
		public void onRouteSelected(MediaRouter router, RouteInfo info) {
			Log.d(TAG, "onRouteSelected");
			// Handle the user route selection.
			mSelectedDevice = CastDevice.getFromBundle(info.getExtras());

			launchReceiver();
		}

		@Override
		public void onRouteUnselected(MediaRouter router, RouteInfo info) {
			Log.d(TAG, "onRouteUnselected: info=" + info);
//			teardown();
			mSelectedDevice = null;
		}
	}
	
	/**
	 * Send a text message to the receiver
	 * 
	 * @param message
	 */
	private void sendMessage(String message) {
		if (mApiClient != null) {
			try {
				Cast.CastApi.sendMessage(mApiClient,
						"urn:x-cast:fr.francetv.player.dojo-controller", message)
						.setResultCallback(new ResultCallback<Status>() {
							@Override
							public void onResult(Status result) {
								if (!result.isSuccess()) {
									Log.e(TAG, "Sending message failed");
								}
							}
						});
			} catch (Exception e) {
				Log.e(TAG, "Exception while sending message", e);
			}
		} else {
			Toast.makeText(this, message, Toast.LENGTH_SHORT)
					.show();
		}
	}

}
