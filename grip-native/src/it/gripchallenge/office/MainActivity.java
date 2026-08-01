package it.gripchallenge.office;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends Activity {

    private static final String PREFS = "grip_challenge_preferences";
    private static final String RESULTS_KEY = "results";

    private static final int COLOR_BG = Color.rgb(10, 15, 30);
    private static final int COLOR_CARD = Color.rgb(23, 31, 52);
    private static final int COLOR_CARD_ALT = Color.rgb(31, 41, 68);
    private static final int COLOR_TEXT = Color.WHITE;
    private static final int COLOR_MUTED = Color.rgb(166, 177, 202);
    private static final int COLOR_YELLOW = Color.rgb(255, 183, 3);
    private static final int COLOR_GREEN = Color.rgb(61, 220, 132);
    private static final int COLOR_RED = Color.rgb(255, 93, 115);
    private static final int COLOR_BLUE = Color.rgb(78, 168, 222);

    private final Handler handler = new Handler(Looper.getMainLooper());

    private EditText nameInput;
    private TextView timerText;
    private TextView statusText;
    private TextView statsText;
    private LinearLayout podiumContainer;
    private LinearLayout leaderboardContainer;
    private LinearLayout historyContainer;

    private Button startButton;
    private Button stopButton;
    private Button resetButton;
    private Button saveButton;

    private boolean running = false;
    private boolean countdownActive = false;
    private long startElapsedRealtime = 0L;
    private long elapsedMs = 0L;
    private int sessionToken = 0;

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            elapsedMs = SystemClock.elapsedRealtime() - startElapsedRealtime;
            timerText.setText(formatTime(elapsedMs));
            handler.postDelayed(this, 10L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(COLOR_BG);
        getWindow().setNavigationBarColor(COLOR_BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        setContentView(buildScreen());
        renderAllResults();
        updateButtons();
    }

    @Override
    protected void onDestroy() {
        sessionToken++;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private View buildScreen() {
        final FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(COLOR_BG);

        final ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        root.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(24), dp(18), dp(42));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView badge = text("OFFICE CHALLENGE  •  40 KG", 13, COLOR_YELLOW, true);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(14), dp(8), dp(14), dp(8));
        badge.setBackground(rounded(COLOR_CARD_ALT, 50));
        LinearLayout.LayoutParams badgeParams = wrap();
        badgeParams.gravity = Gravity.CENTER_HORIZONTAL;
        badgeParams.bottomMargin = dp(14);
        content.addView(badge, badgeParams);

        TextView title = text("Grip Challenge", 36, COLOR_TEXT, true);
        title.setGravity(Gravity.CENTER);
        content.addView(title, matchWrap());

        TextView subtitle = text("Chi tiene l'hand grip chiuso più a lungo?", 15, COLOR_MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = matchWrap();
        subtitleParams.topMargin = dp(6);
        subtitleParams.bottomMargin = dp(18);
        content.addView(subtitle, subtitleParams);

        statsText = text("0 partecipanti  •  Record --", 14, COLOR_MUTED, true);
        statsText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statsParams = matchWrap();
        statsParams.bottomMargin = dp(18);
        content.addView(statsText, statsParams);

        LinearLayout challengeCard = card();
        content.addView(challengeCard);
        challengeCard.addView(sectionTitle("Nuovo tentativo"));

        nameInput = new EditText(this);
        nameInput.setHint("Nome partecipante");
        nameInput.setHintTextColor(Color.rgb(120, 134, 164));
        nameInput.setTextColor(COLOR_TEXT);
        nameInput.setTextSize(17);
        nameInput.setSingleLine(true);
        nameInput.setPadding(dp(16), 0, dp(16), 0);
        nameInput.setBackground(rounded(COLOR_CARD_ALT, 16));
        LinearLayout.LayoutParams inputParams = match(dp(56));
        inputParams.topMargin = dp(14);
        challengeCard.addView(nameInput, inputParams);

        timerText = text("00:00.00", 52, COLOR_TEXT, true);
        timerText.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        timerText.setGravity(Gravity.CENTER);
        timerText.setPadding(dp(8), dp(24), dp(8), dp(8));
        challengeCard.addView(timerText, matchWrap());

        statusText = text("Inserisci il nome e premi AVVIA", 14, COLOR_MUTED, true);
        statusText.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.bottomMargin = dp(18);
        challengeCard.addView(statusText, statusParams);

        LinearLayout firstButtonRow = horizontalRow();
        challengeCard.addView(firstButtonRow, matchWrap());

        startButton = actionButton("AVVIA", COLOR_GREEN, Color.rgb(7, 48, 31));
        stopButton = actionButton("STOP", COLOR_RED, Color.WHITE);
        firstButtonRow.addView(startButton, weightedButton(1f, 0));
        firstButtonRow.addView(stopButton, weightedButton(1f, dp(10)));

        LinearLayout secondButtonRow = horizontalRow();
        LinearLayout.LayoutParams secondRowParams = matchWrap();
        secondRowParams.topMargin = dp(10);
        challengeCard.addView(secondButtonRow, secondRowParams);

        resetButton = actionButton("RESET", COLOR_CARD_ALT, COLOR_TEXT);
        saveButton = actionButton("SALVA TEMPO", COLOR_YELLOW, Color.rgb(45, 31, 0));
        secondButtonRow.addView(resetButton, weightedButton(1f, 0));
        secondButtonRow.addView(saveButton, weightedButton(1f, dp(10)));

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startChallenge();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopChallenge();
            }
        });
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetTimer();
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCurrentResult();
            }
        });

        LinearLayout podiumCard = card();
        content.addView(podiumCard);
        podiumCard.addView(sectionTitle("Podio"));
        podiumContainer = verticalContainer();
        LinearLayout.LayoutParams podiumParams = matchWrap();
        podiumParams.topMargin = dp(12);
        podiumCard.addView(podiumContainer, podiumParams);

        LinearLayout leaderboardCard = card();
        content.addView(leaderboardCard);
        leaderboardCard.addView(sectionTitle("Leaderboard"));
        leaderboardContainer = verticalContainer();
        LinearLayout.LayoutParams leaderboardParams = matchWrap();
        leaderboardParams.topMargin = dp(12);
        leaderboardCard.addView(leaderboardContainer, leaderboardParams);

        LinearLayout historyCard = card();
        content.addView(historyCard);
        historyCard.addView(sectionTitle("Tutti i tentativi"));
        historyContainer = verticalContainer();
        LinearLayout.LayoutParams historyParams = matchWrap();
        historyParams.topMargin = dp(12);
        historyCard.addView(historyContainer, historyParams);

        Button clearButton = actionButton("AZZERA TUTTA LA CHALLENGE", COLOR_RED, Color.WHITE);
        LinearLayout.LayoutParams clearParams = match(dp(54));
        clearParams.topMargin = dp(2);
        content.addView(clearButton, clearParams);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmClearAll();
            }
        });

        TextView footer = text("I risultati restano salvati su questo telefono.", 12, COLOR_MUTED, false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = matchWrap();
        footerParams.topMargin = dp(16);
        content.addView(footer, footerParams);

        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                int top;
                int bottom;
                int left;
                int right;
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                    left = bars.left;
                    top = bars.top;
                    right = bars.right;
                    bottom = bars.bottom;
                } else {
                    left = insets.getSystemWindowInsetLeft();
                    top = insets.getSystemWindowInsetTop();
                    right = insets.getSystemWindowInsetRight();
                    bottom = insets.getSystemWindowInsetBottom();
                }
                root.setPadding(left, top, right, bottom);
                return insets;
            }
        });

        return root;
    }

    private void startChallenge() {
        String name = cleanName();
        if (name.length() == 0) {
            nameInput.requestFocus();
            Toast.makeText(this, "Inserisci il nome del partecipante", Toast.LENGTH_SHORT).show();
            return;
        }
        if (running || countdownActive) {
            return;
        }

        resetTimerInternal(false);
        final int token = ++sessionToken;
        countdownActive = true;
        statusText.setText("Preparati e impugna l'hand grip");
        updateButtons();

        final int[] remaining = new int[]{3};
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (token != sessionToken || !countdownActive) {
                    return;
                }
                if (remaining[0] > 0) {
                    timerText.setText(String.valueOf(remaining[0]));
                    statusText.setText("Stringi al VIA");
                    remaining[0]--;
                    handler.postDelayed(this, 1000L);
                    return;
                }

                countdownActive = false;
                running = true;
                elapsedMs = 0L;
                startElapsedRealtime = SystemClock.elapsedRealtime();
                timerText.setText("00:00.00");
                statusText.setText("VIA! Tieni chiuso");
                updateButtons();
                handler.post(timerRunnable);
            }
        });
    }

    private void stopChallenge() {
        if (!running) {
            return;
        }
        elapsedMs = SystemClock.elapsedRealtime() - startElapsedRealtime;
        running = false;
        handler.removeCallbacks(timerRunnable);
        timerText.setText(formatTime(elapsedMs));
        statusText.setText("Tempo fermato: salva il risultato");
        updateButtons();
    }

    private void resetTimer() {
        resetTimerInternal(true);
    }

    private void resetTimerInternal(boolean showMessage) {
        sessionToken++;
        running = false;
        countdownActive = false;
        handler.removeCallbacks(timerRunnable);
        elapsedMs = 0L;
        timerText.setText("00:00.00");
        statusText.setText(showMessage ? "Timer azzerato" : "Preparati");
        updateButtons();
    }

    private void saveCurrentResult() {
        String name = cleanName();
        if (name.length() == 0) {
            Toast.makeText(this, "Inserisci il nome del partecipante", Toast.LENGTH_SHORT).show();
            return;
        }
        if (running || countdownActive) {
            Toast.makeText(this, "Ferma prima il timer", Toast.LENGTH_SHORT).show();
            return;
        }
        if (elapsedMs <= 0L) {
            Toast.makeText(this, "Non c'è ancora un tempo da salvare", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONArray results = loadResults();
        JSONObject result = new JSONObject();
        try {
            result.put("id", UUID.randomUUID().toString());
            result.put("name", name);
            result.put("timeMs", elapsedMs);
            result.put("createdAt", System.currentTimeMillis());
            results.put(result);
            saveResults(results);
        } catch (JSONException exception) {
            Toast.makeText(this, "Errore durante il salvataggio", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Tempo salvato: " + formatTime(elapsedMs), Toast.LENGTH_SHORT).show();
        renderAllResults();
        resetTimerInternal(false);
        statusText.setText("Risultato salvato");
    }

    private String cleanName() {
        return nameInput == null ? "" : nameInput.getText().toString().trim().replaceAll("\\s+", " ");
    }

    private void updateButtons() {
        setButtonEnabled(startButton, !running && !countdownActive);
        setButtonEnabled(stopButton, running);
        setButtonEnabled(resetButton, running || countdownActive || elapsedMs > 0L);
        setButtonEnabled(saveButton, !running && !countdownActive && elapsedMs > 0L);
        if (nameInput != null) {
            nameInput.setEnabled(!running && !countdownActive);
            nameInput.setAlpha(nameInput.isEnabled() ? 1f : 0.65f);
        }
    }

    private void setButtonEnabled(Button button, boolean enabled) {
        if (button == null) {
            return;
        }
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.38f);
    }

    private JSONArray loadResults() {
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(RESULTS_KEY, "[]");
        try {
            return new JSONArray(raw == null ? "[]" : raw);
        } catch (JSONException exception) {
            return new JSONArray();
        }
    }

    private void saveResults(JSONArray results) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(RESULTS_KEY, results.toString())
                .apply();
    }

    private void renderAllResults() {
        JSONArray results = loadResults();
        List<Score> leaderboard = buildLeaderboard(results);
        renderStats(results, leaderboard);
        renderPodium(leaderboard);
        renderLeaderboard(leaderboard);
        renderHistory(results);
    }

    private List<Score> buildLeaderboard(JSONArray results) {
        Map<String, Score> bestByName = new HashMap<String, Score>();
        for (int index = 0; index < results.length(); index++) {
            JSONObject item = results.optJSONObject(index);
            if (item == null) {
                continue;
            }
            String displayName = item.optString("name", "Partecipante").trim();
            long timeMs = item.optLong("timeMs", 0L);
            if (displayName.length() == 0 || timeMs <= 0L) {
                continue;
            }
            String key = displayName.toLowerCase(Locale.ITALY);
            Score current = bestByName.get(key);
            if (current == null || timeMs > current.timeMs) {
                bestByName.put(key, new Score(displayName, timeMs));
            }
        }

        List<Score> scores = new ArrayList<Score>(bestByName.values());
        Collections.sort(scores, new Comparator<Score>() {
            @Override
            public int compare(Score first, Score second) {
                int timeComparison = Long.compare(second.timeMs, first.timeMs);
                if (timeComparison != 0) {
                    return timeComparison;
                }
                return first.name.compareToIgnoreCase(second.name);
            }
        });
        return scores;
    }

    private void renderStats(JSONArray results, List<Score> leaderboard) {
        int participantCount = leaderboard.size();
        String participantLabel = participantCount == 1 ? "partecipante" : "partecipanti";
        String record = leaderboard.isEmpty() ? "--" : formatTime(leaderboard.get(0).timeMs);
        statsText.setText(participantCount + " " + participantLabel + "  •  " + results.length()
                + " tentativi  •  Record " + record);
    }

    private void renderPodium(List<Score> scores) {
        podiumContainer.removeAllViews();
        if (scores.isEmpty()) {
            podiumContainer.addView(emptyMessage("Il podio apparirà dopo il primo tentativo."));
            return;
        }

        String[] medals = new String[]{"🥇", "🥈", "🥉"};
        int[] colors = new int[]{COLOR_YELLOW, Color.rgb(202, 211, 226), Color.rgb(205, 127, 50)};
        int count = Math.min(3, scores.size());
        for (int index = 0; index < count; index++) {
            Score score = scores.get(index);
            podiumContainer.addView(resultRow(
                    medals[index],
                    score.name,
                    formatTime(score.timeMs),
                    colors[index],
                    index > 0
            ));
        }
    }

    private void renderLeaderboard(List<Score> scores) {
        leaderboardContainer.removeAllViews();
        if (scores.isEmpty()) {
            leaderboardContainer.addView(emptyMessage("Nessun risultato salvato."));
            return;
        }

        for (int index = 0; index < scores.size(); index++) {
            Score score = scores.get(index);
            leaderboardContainer.addView(resultRow(
                    String.valueOf(index + 1),
                    score.name,
                    formatTime(score.timeMs),
                    index < 3 ? COLOR_YELLOW : COLOR_BLUE,
                    index > 0
            ));
        }
    }

    private void renderHistory(JSONArray results) {
        historyContainer.removeAllViews();
        List<JSONObject> items = new ArrayList<JSONObject>();
        for (int index = 0; index < results.length(); index++) {
            JSONObject item = results.optJSONObject(index);
            if (item != null) {
                items.add(item);
            }
        }

        Collections.sort(items, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject first, JSONObject second) {
                return Long.compare(second.optLong("createdAt", 0L), first.optLong("createdAt", 0L));
            }
        });

        if (items.isEmpty()) {
            historyContainer.addView(emptyMessage("La cronologia è vuota."));
            return;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY);
        for (int index = 0; index < items.size(); index++) {
            final JSONObject item = items.get(index);
            final String id = item.optString("id", "");

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(12), dp(8), dp(12));
            row.setBackground(rounded(COLOR_CARD_ALT, 14));
            LinearLayout.LayoutParams rowParams = matchWrap();
            if (index > 0) {
                rowParams.topMargin = dp(8);
            }
            row.setLayoutParams(rowParams);

            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView main = text(item.optString("name", "Partecipante") + "  •  "
                    + formatTime(item.optLong("timeMs", 0L)), 15, COLOR_TEXT, true);
            info.addView(main, matchWrap());

            String date = formatter.format(new Date(item.optLong("createdAt", 0L)));
            TextView secondary = text(date, 12, COLOR_MUTED, false);
            LinearLayout.LayoutParams secondaryParams = matchWrap();
            secondaryParams.topMargin = dp(3);
            info.addView(secondary, secondaryParams);

            Button delete = actionButton("×", Color.TRANSPARENT, COLOR_RED);
            delete.setTextSize(24);
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(dp(46), dp(46));
            deleteParams.leftMargin = dp(8);
            row.addView(delete, deleteParams);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    confirmDeleteResult(id);
                }
            });

            historyContainer.addView(row);
        }
    }

    private View resultRow(String rank, String name, String value, int accentColor, boolean addTopMargin) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        row.setBackground(rounded(COLOR_CARD_ALT, 14));
        LinearLayout.LayoutParams rowParams = matchWrap();
        if (addTopMargin) {
            rowParams.topMargin = dp(8);
        }
        row.setLayoutParams(rowParams);

        TextView rankView = text(rank, rank.length() > 1 ? 15 : 21, accentColor, true);
        rankView.setGravity(Gravity.CENTER);
        row.addView(rankView, new LinearLayout.LayoutParams(dp(48), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView nameView = text(name, 16, COLOR_TEXT, true);
        nameView.setSingleLine(false);
        row.addView(nameView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView valueView = text(value, 16, accentColor, true);
        valueView.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        valueView.setGravity(Gravity.END);
        row.addView(valueView, wrap());

        return row;
    }

    private void confirmDeleteResult(final String resultId) {
        if (resultId.length() == 0) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Eliminare il tentativo?")
                .setMessage("Il risultato verrà rimosso dalla classifica.")
                .setNegativeButton("Annulla", null)
                .setPositiveButton("Elimina", (dialog, which) -> deleteResult(resultId))
                .show();
    }

    private void deleteResult(String resultId) {
        JSONArray current = loadResults();
        JSONArray updated = new JSONArray();
        for (int index = 0; index < current.length(); index++) {
            JSONObject item = current.optJSONObject(index);
            if (item == null || resultId.equals(item.optString("id", ""))) {
                continue;
            }
            updated.put(item);
        }
        saveResults(updated);
        renderAllResults();
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(this)
                .setTitle("Azzerare la challenge?")
                .setMessage("Verranno eliminati podio, leaderboard e cronologia.")
                .setNegativeButton("Annulla", null)
                .setPositiveButton("Azzera", (dialog, which) -> {
                    saveResults(new JSONArray());
                    renderAllResults();
                    resetTimerInternal(false);
                    statusText.setText("Challenge azzerata");
                })
                .show();
    }

    private String formatTime(long milliseconds) {
        long safeMs = Math.max(0L, milliseconds);
        long totalSeconds = safeMs / 1000L;
        long centiseconds = (safeMs % 1000L) / 10L;
        long seconds = totalSeconds % 60L;
        long minutes = (totalSeconds / 60L) % 60L;
        long hours = totalSeconds / 3600L;
        if (hours > 0L) {
            return String.format(Locale.ITALY, "%d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds);
        }
        return String.format(Locale.ITALY, "%02d:%02d.%02d", minutes, seconds, centiseconds);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(rounded(COLOR_CARD, 22));
        LinearLayout.LayoutParams params = matchWrap();
        params.bottomMargin = dp(16);
        card.setLayoutParams(params);
        return card;
    }

    private LinearLayout horizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private LinearLayout verticalContainer() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        return container;
    }

    private TextView sectionTitle(String value) {
        return text(value, 20, COLOR_TEXT, true);
    }

    private TextView emptyMessage(String value) {
        TextView view = text(value, 14, COLOR_MUTED, false);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(8), dp(18), dp(8), dp(18));
        return view;
    }

    private TextView text(String value, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        view.setLineSpacing(0f, 1.08f);
        return view;
    }

    private Button actionButton(String value, int backgroundColor, int textColor) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextSize(14);
        button.setTextColor(textColor);
        button.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setBackground(rounded(backgroundColor, 16));
        return button;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams match(int heightDp) {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(heightDp)
        );
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams weightedButton(float weight, int leftMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(56), weight);
        params.leftMargin = leftMarginDp;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class Score {
        final String name;
        final long timeMs;

        Score(String name, long timeMs) {
            this.name = name;
            this.timeMs = timeMs;
        }
    }
}
