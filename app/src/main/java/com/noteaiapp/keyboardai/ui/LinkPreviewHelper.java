package com.noteaiapp.keyboardai.ui;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to detect links in an EditText and dynamically fetch and display a rich preview.
 */
public class LinkPreviewHelper {

    private static final String TAG = "LinkPreviewHelper";
    private final Context context;
    private final EditText sourceEditText;
    private final LinearLayout mainContainerLayout;
    private final ImageView linkImage;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();

    // Regex pattern to detect URLs
    private static final Pattern URL_PATTERN = Pattern.compile(
            "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            Pattern.CASE_INSENSITIVE);

    private String lastDetectedLink = null;
    private View currentPreviewView = null;
    private Runnable debounceRunnable;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());

    /**
     * Data structure to hold parsed link metadata.
     */
    private static class LinkMetadata {
        String url;
        String title;
        String description;
        String imageUrl;

        public LinkMetadata(String url, String title, String description, String imageUrl) {
            this.url = url;
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
        }
    }

    /**
     * Constructor for LinkPreviewHelper.
     * @param context The application context (usually the Activity).
     * @param sourceEditText The EditText to watch for link input.
     * @param mainContainerLayout The RelativeLayout where the preview card should be added.
     */
    public LinkPreviewHelper(Context context, EditText sourceEditText, LinearLayout mainContainerLayout, ImageView linkImage) {
        this.context = context;
        this.sourceEditText = sourceEditText;
        this.mainContainerLayout = mainContainerLayout;
        this.linkImage = linkImage;
    }

    /**
     * Sets up the TextWatcher to start monitoring the EditText for URLs.
     * This should be called once in the Activity's onCreate.
     */
    public void setupLinkPreviewWatcher() {
        sourceEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Cancel previous debounced task
                if (debounceRunnable != null) {
                    debounceHandler.removeCallbacks(debounceRunnable);
                }

                // Check for a complete URL
                Matcher matcher = URL_PATTERN.matcher(s.toString());
                String detectedUrl = null;
                while (matcher.find()) {
                    detectedUrl = matcher.group(0);
                    // Use the last complete URL found
                }

                if (detectedUrl != null && !detectedUrl.equals(lastDetectedLink)) {
                    final String finalUrl = detectedUrl;
                    lastDetectedLink = finalUrl;

                    // Debounce the network request to wait for typing to stop
                    debounceRunnable = () -> fetchLinkMetadata(finalUrl);
                    debounceHandler.postDelayed(debounceRunnable, 1000); // 1-second delay
                } else if (detectedUrl == null && currentPreviewView != null) {
                    // If no URL is present anymore, remove the preview
                    removePreview();
                    lastDetectedLink = null;
                }
            }
        });
    }

    private void fetchLinkMetadata(String urlString) {
        networkExecutor.execute(() -> {
            LinkMetadata metadata = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(urlString);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    // Read up to 128KB of HTML to find the metadata tags quickly
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder html = new StringBuilder();
                    char[] buffer = new char[1024];
                    int charsRead;
                    int totalRead = 0;
                    while ((charsRead = reader.read(buffer)) != -1 && totalRead < 128 * 1024) {
                        html.append(buffer, 0, charsRead);
                        totalRead += charsRead;
                    }
                    reader.close();
                    metadata = parseHtmlForMetadata(urlString, html.toString());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching link metadata for: " + urlString, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            // Post result back to the main UI thread
            final LinkMetadata finalMetadata = metadata;
            uiHandler.post(() -> {
                if (finalMetadata != null) {
                    displayLinkPreview(finalMetadata);
                } else {
                    removePreview();
                }
            });
        });
    }

    private LinkMetadata parseHtmlForMetadata(String url, String html) {
        String title = extractTagContent(html, "og:title");
        String description = extractTagContent(html, "og:description");
        String imageUrl = extractTagContent(html, "og:image");

        if (title == null || title.isEmpty()) {
            title = extractTitleTag(html);
        }
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = extractTagContent(html, "twitter:image");
        }
        if (title == null || title.isEmpty()) {
            title = url;
        }

        return new LinkMetadata(url, title, description, imageUrl);
    }

    private String extractTagContent(String html, String propertyName) {
        String patternString = "<meta[^>]+(property|name)\\s*=\\s*['\"]" + Pattern.quote(propertyName) + "['\"][^>]*?content\\s*=\\s*['\"]([^'\"]*)['\"][^>]*>";
        Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        return null;
    }

    private String extractTitleTag(String html) {
        Pattern pattern = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private void displayLinkPreview(LinkMetadata metadata) {
        removePreview(); // Clear existing preview

        // Create the main card view container
        CardView cardView = new CardView(context);
        RelativeLayout.LayoutParams cardParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        cardParams.addRule(RelativeLayout.BELOW, sourceEditText.getId()); // Place it below the source EditText

        final int marginDp = 12;
        final float scale = context.getResources().getDisplayMetrics().density;
        int marginPx = (int) (marginDp * scale);

        cardParams.setMargins(0, marginPx, 0, marginPx);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(24f);
        cardView.setCardElevation(8f);
        cardView.setClickable(true);
        cardView.setFocusable(true);
        cardView.setOnClickListener(v -> {
            // Open the link when the card is clicked
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(metadata.url));
            context.startActivity(browserIntent);
        });

        // Content Layout (Vertical Stack)
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        contentLayout.setPadding(marginPx, marginPx, marginPx, marginPx);

        // 1. ImageView (Thumbnail)
        ImageView thumbnailView = new ImageView(context);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (200 * scale));
        imageParams.bottomMargin = marginPx / 2;
        thumbnailView.setLayoutParams(imageParams);
        thumbnailView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnailView.setBackgroundColor(Color.parseColor("#E0E0E0"));
        thumbnailView.setVisibility(View.GONE);

        // 2. Title TextView
        TextView titleTextView = new TextView(context);
        titleTextView.setText(metadata.title);
        titleTextView.setTextSize(18);
        titleTextView.setTypeface(null, Typeface.BOLD);
        titleTextView.setTextColor(Color.BLACK);

        // 3. Description TextView
        TextView descTextView = new TextView(context);
        if (metadata.description != null && !metadata.description.isEmpty()) {
            descTextView.setText(metadata.description);
            descTextView.setTextSize(14);
            descTextView.setTextColor(Color.DKGRAY);
            descTextView.setMaxLines(3);
        } else {
            descTextView.setVisibility(View.GONE);
        }

        // 4. URL TextView (Source)
        TextView urlTextView = new TextView(context);
        urlTextView.setText(Uri.parse(metadata.url).getHost()); // Show only the host name
        urlTextView.setTextSize(12);
        urlTextView.setTextColor(Color.GRAY);
        urlTextView.setMovementMethod(LinkMovementMethod.getInstance());


        // Add views to content layout
        contentLayout.addView(thumbnailView);
        contentLayout.addView(titleTextView);
        contentLayout.addView(descTextView);
        contentLayout.addView(urlTextView);

        cardView.addView(contentLayout);

        // Add card to main layout
        mainContainerLayout.addView(cardView);
        currentPreviewView = cardView;
        Log.d("com.noteaiapp.keyboardai","metdata;"+metadata.title+":url:"+metadata.imageUrl+":title:"+metadata.title);
        // Start loading the image if a URL is found
        if (metadata.imageUrl != null && !metadata.imageUrl.isEmpty()) {
            loadImageFromUrl(metadata.imageUrl, thumbnailView);

        }
    }

    private void loadImageFromUrl(String imageUrl, ImageView imageView) {
        networkExecutor.execute(() -> {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(imageUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                Log.e(TAG, "Error loading image: " + imageUrl, e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            final Bitmap finalBitmap = bitmap;
            uiHandler.post(() -> {
                if (finalBitmap != null) {
                     imageView.setImageBitmap(finalBitmap);
                    imageView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void removePreview() {
        if (currentPreviewView != null) {
            mainContainerLayout.removeView(currentPreviewView);
            currentPreviewView = null;
        }
    }

    /**
     * Should be called in the Activity's onDestroy to clean up resources.
     */
    public void shutdown() {
        networkExecutor.shutdownNow();
        debounceHandler.removeCallbacksAndMessages(null);
        removePreview();
        Log.d(TAG, "LinkPreviewHelper shutdown complete.");
    }
}