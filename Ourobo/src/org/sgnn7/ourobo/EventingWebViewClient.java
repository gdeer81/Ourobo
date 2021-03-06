package org.sgnn7.ourobo;

import java.util.ArrayList;
import java.util.List;

import org.sgnn7.ourobo.eventing.IChangeEventListener;
import org.sgnn7.ourobo.util.LogMe;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class EventingWebViewClient extends WebViewClient {
	private static final String WWW_PREFIX = "www.";
	private static final String VIDEO_QUERY_PARAMETER = "v";
	private static final String HTTP_PROTOCOL_PREFIX = "http://";
	private static final String MOBILE_YOUTUBE_SITE = "m.youtube.com/";
	private static final String MAIN_YOUTUBE_SITE = "youtube.com/";

	private static final String YOUTUBE_LINK_PREFIX = "vnd.youtube:";
	private static final String NO_MATCHING_INTENT = "Cannot find an app to handle this kind of link";

	private final Activity activity;
	private final List<IChangeEventListener> listeners = new ArrayList<IChangeEventListener>();

	public EventingWebViewClient(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		notifyAllListeners();
		super.onPageFinished(view, url);
	}

	private void notifyAllListeners() {
		for (IChangeEventListener listener : listeners) {
			listener.handle();
		}
	}

	public void addPageLoadedListener(IChangeEventListener listener) {
		listeners.add(listener);
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		boolean overrideUrlLoading = false;

		LogMe.d("Loading: " + url);

		if (url.startsWith(YOUTUBE_LINK_PREFIX)) {
			overrideUrlLoading = openYoutubeApp(url);
		} else if (isYoutubeLink(url) && extractVideoId(url) != null) {
			String modifiedUrl = YOUTUBE_LINK_PREFIX + extractVideoId(url);
			LogMe.i("Modified URL: " + modifiedUrl);
			overrideUrlLoading = openYoutubeApp(modifiedUrl);
		}

		return overrideUrlLoading;
	}

	private String extractVideoId(String url) {
		String videoId = Uri.parse(url).getQueryParameter(VIDEO_QUERY_PARAMETER);
		LogMe.d("Video ID: " + videoId);
		return videoId;
	}

	private boolean isYoutubeLink(String url) {
		boolean isYoutubeLink = false;
		if (isMobileYoutubeSite(url) || isMainYoutubeSite(url)) {
			isYoutubeLink = true;
		}
		LogMe.e("Is youtube link: " + isYoutubeLink);
		return isYoutubeLink;
	}

	private boolean isMobileYoutubeSite(String url) {
		return url.startsWith(HTTP_PROTOCOL_PREFIX + MOBILE_YOUTUBE_SITE) || url.startsWith(MOBILE_YOUTUBE_SITE);
	}

	private boolean isMainYoutubeSite(String url) {
		return url.startsWith(HTTP_PROTOCOL_PREFIX + MAIN_YOUTUBE_SITE)
				|| url.startsWith(HTTP_PROTOCOL_PREFIX + WWW_PREFIX + MAIN_YOUTUBE_SITE)
				|| url.startsWith(MAIN_YOUTUBE_SITE);
	}

	private boolean openYoutubeApp(String url) {
		boolean overrideUrlLoading = false;
		LogMe.e("YouTube link: " + url);
		Intent urlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		List<ResolveInfo> list = activity.getPackageManager().queryIntentActivities(urlIntent,
				PackageManager.MATCH_DEFAULT_ONLY);

		if (list.size() != 0) {
			try {
				Intent videoIntent = new Intent(Intent.ACTION_VIEW);
				videoIntent.setData(Uri.parse(url));

				activity.startActivity(videoIntent);

				overrideUrlLoading = true;
			} catch (ActivityNotFoundException anfe) {
				displayAndLogIntentError(anfe);
			}
		} else {
			displayAndLogIntentError(new ActivityNotFoundException("YouTube intent search failed"));
		}

		return overrideUrlLoading;
	}

	private void displayAndLogIntentError(ActivityNotFoundException anfe) {
		LogMe.e(anfe);
		LogMe.e(NO_MATCHING_INTENT);
		Toast.makeText(activity, NO_MATCHING_INTENT, Toast.LENGTH_LONG).show();
	}
}
