package tv.aura.searchlab;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A test bench for the search part of a TV launcher. For every app that publishes search suggestions it shows
 * the raw answer (every column and value), tries to show the cover it names, lists who the system says can open
 * the result, and offers several ways of opening it, each with its own button, so that one photo shows which way
 * works. It only uses public Android APIs (SearchManager, ContentResolver, PackageManager).
 */
public class LabActivity extends Activity {
    private static final int PER_APP = 6;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private LinearLayout results;
    private EditText input;
    private ScrollView scroll;
    private int generation;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF10162A);
        root.setPadding(dp(32), dp(16), dp(32), dp(8));
        TextView title = new TextView(this);
        title.setText("AURA Search Lab: type words, press Enter");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20);
        root.addView(title);
        input = new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0xFF8890A8);
        input.setHint("search words");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        input.setOnEditorActionListener((v, action, e) -> { go(); return true; });
        input.setOnKeyListener((v, code, e) -> {
            if (e.getAction() == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER) { go(); return true; }
            return false;
        });
        root.addView(input);
        scroll = new ScrollView(this);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(results);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        String q = getIntent().getStringExtra("q");
        if (q != null) { input.setText(q); go(); }
    }

    @Override
    protected void onDestroy() {
        generation++;
        pool.shutdownNow();
        super.onDestroy();
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int color, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setPadding(0, dp(2), 0, dp(2));
        return t;
    }

    private void go() {
        final String q = input.getText().toString().trim();
        if (q.length() < 2) return;
        final int mine = ++generation;
        results.removeAllViews();
        PackageManager pm = getPackageManager();
        List<ResolveInfo> searchers = pm.queryIntentActivities(new Intent(Intent.ACTION_SEARCH), 0);
        results.addView(text("Apps with a search screen: " + searchers.size(), 0xFFA0B4FF, 14));
        List<String> done = new ArrayList<>();
        final SearchManager sm = (SearchManager) getSystemService(SEARCH_SERVICE);
        for (ResolveInfo ri : searchers) {
            final String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || done.contains(pkg)) continue;
            done.add(pkg);
            final String label = String.valueOf(ri.loadLabel(pm));
            final ComponentName searchable = new ComponentName(pkg, ri.activityInfo.name);
            pool.submit(() -> {
                final List<Object> views = ask(sm, label, pkg, searchable, q);
                ui.post(() -> {
                    if (mine != generation) return;
                    for (Object o : views) results.addView(o instanceof Snap ? view((Snap) o) : text(((String[]) o)[0], Integer.parseInt(((String[]) o)[1]), 14));
                });
            });
        }
    }

    /** Everything one app answers, as views. Runs off the main thread, so the views are made on it too (no attach). */
    private List<Object> ask(SearchManager sm, String label, String pkg, ComponentName searchable, String q) {
        final List<Object> out = new ArrayList<>();
        final String head = label + "  (" + pkg + ")";
        try {
            SearchableInfo info = sm.getSearchableInfo(searchable);
            if (info == null || info.getSuggestAuthority() == null) {
                out.add(new String[]{head + "\n  no suggestions published", String.valueOf(0xFF8890A8)});
                return out;
            }
            Uri.Builder ub = new Uri.Builder().scheme("content").authority(info.getSuggestAuthority());
            if (info.getSuggestPath() != null) ub.appendEncodedPath(info.getSuggestPath());
            ub.appendPath(SearchManager.SUGGEST_URI_PATH_QUERY);
            String selection = info.getSuggestSelection();
            String[] args = null;
            if (selection != null) args = new String[]{q};
            else ub.appendPath(q);
            Uri uri = ub.build();
            String meta = "\n  authority " + info.getSuggestAuthority() + "  action " + info.getSuggestIntentAction()
                    + "  data " + info.getSuggestIntentData() + "  selection " + selection;
            Cursor c;
            try {
                c = getContentResolver().query(uri, null, selection, args, null);
            } catch (SecurityException e) {
                out.add(new String[]{head + meta + "\n  no access: " + e.getMessage(), String.valueOf(0xFFFF9090)});
                return out;
            } catch (RuntimeException e) {
                out.add(new String[]{head + meta + "\n  error: " + e, String.valueOf(0xFFFF9090)});
                return out;
            }
            if (c == null) { out.add(new String[]{head + meta + "\n  provider returned nothing", String.valueOf(0xFFFF9090)}); return out; }
            try {
                out.add(new String[]{head + meta + "\n  rows: " + c.getCount() + "  columns: " + String.join(",", c.getColumnNames()), String.valueOf(0xFF7CF0B0)});
                int n = 0;
                while (c.moveToNext() && n++ < PER_APP) out.add(snap(c, info, pkg, q));
            } finally {
                c.close();
            }
        } catch (RuntimeException e) {
            out.add(new String[]{head + "\n  error: " + e, String.valueOf(0xFFFF9090)});
        }
        return out;
    }

    
    private String col(Cursor c, String name) {
        int i = c.getColumnIndex(name);
        if (i < 0) return null;
        try { return c.getString(i); } catch (RuntimeException e) { return null; }
    }

    /** What one result says, read on the worker thread; the views are made from it on the main thread. */
    private static final class Snap {
        String pkg, title, sub, raw, ref, refFrom, act, dat, extra, userQ;
    }

    private Snap snap(Cursor c, SearchableInfo info, String pkg, String q) {
        Snap s = new Snap();
        s.pkg = pkg;
        s.userQ = q;
        s.title = col(c, SearchManager.SUGGEST_COLUMN_TEXT_1);
        s.sub = col(c, SearchManager.SUGGEST_COLUMN_TEXT_2);
        String action = col(c, SearchManager.SUGGEST_COLUMN_INTENT_ACTION);
        if (action == null) action = info.getSuggestIntentAction();
        if (action == null) action = Intent.ACTION_VIEW;
        String data = col(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA);
        if (data == null) {
            String id = col(c, SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
            if (info.getSuggestIntentData() != null) data = id == null ? info.getSuggestIntentData() : info.getSuggestIntentData() + "/" + Uri.encode(id);
        }
        s.act = action;
        s.dat = data;
        s.extra = col(c, SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA);
        StringBuilder raw = new StringBuilder();
        for (int i = 0; i < c.getColumnCount(); i++) {
            String v;
            try { v = c.getString(i); } catch (RuntimeException e) { v = "<blob>"; }
            if (v == null) continue;
            if (v.length() > 140) v = v.substring(0, 140) + "...";
            raw.append(c.getColumnName(i)).append(" = ").append(v).append('\n');
        }
        s.raw = raw.toString();
        // The cover: the Android TV column first, then the icons.
        s.ref = col(c, "suggest_result_card_image");
        s.refFrom = "suggest_result_card_image";
        if (s.ref == null || s.ref.isEmpty()) { s.ref = col(c, SearchManager.SUGGEST_COLUMN_ICON_1); s.refFrom = "suggest_icon_1"; }
        if (s.ref == null || s.ref.isEmpty()) { s.ref = col(c, SearchManager.SUGGEST_COLUMN_ICON_2); s.refFrom = "suggest_icon_2"; }
        return s;
    }

    private View view(final Snap s) {
        final String pkg = s.pkg, act = s.act, dat = s.dat, extra = s.extra, title = s.title, userQ = s.userQ;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(12), dp(8), dp(12), dp(8));
        card.setBackgroundColor(0xFF1B2444);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(6), 0, dp(6));
        card.setLayoutParams(lp);

        card.addView(text(title + "   " + s.sub, 0xFFFFFFFF, 17));
        card.addView(text(s.raw, 0xFFB8C4E8, 12));
        final boolean hasRef = s.ref != null && !s.ref.isEmpty();
        final TextView coverNote = text("cover: " + (hasRef ? s.refFrom + " = " + s.ref : "none given"), 0xFFFFD27C, 12);
        card.addView(coverNote);
        final ImageView cover = new ImageView(this);
        cover.setAdjustViewBounds(true);
        cover.setMaxHeight(dp(160));
        cover.setMaxWidth(dp(160));
        cover.setVisibility(View.GONE);
        card.addView(cover);
        if (hasRef) loadCover(s.ref, pkg, cover, coverNote);

        // Who can open it, according to the system.
        Intent probe = new Intent(act);
        if (dat != null) probe.setData(Uri.parse(dat));
        List<ResolveInfo> all = getPackageManager().queryIntentActivities(probe, PackageManager.MATCH_ALL);
        StringBuilder who = new StringBuilder("who takes action+address: " + all.size());
        int shown = 0;
        for (ResolveInfo ri : all) {
            if (shown++ >= 4) break;
            who.append("\n   ").append(ri.activityInfo.packageName).append('/').append(ri.activityInfo.name);
        }
        card.addView(text(who.toString(), 0xFFB8C4E8, 12));

        // The ways of opening it.
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.START);
        final String[] names = {"A own", "B no pkg", "C explicit", "D view", "E search", "F app"};
        for (int k = 0; k < names.length; k++) {
            final int way = k;
            Button btn = new Button(this);
            btn.setText(names[k]);
            btn.setAllCaps(false);
            btn.setOnClickListener(v -> tryWay(way, pkg, act, dat, extra, title, userQ));
            row.addView(btn);
        }
        card.addView(row);
        return card;
    }

    private Intent base(String pkg, String act, String dat, String extra, String title, String userQ) {
        Intent i = new Intent(act);
        if (dat != null) i.setData(Uri.parse(dat));
        if (pkg != null) i.setPackage(pkg);
        if (extra != null) i.putExtra(SearchManager.EXTRA_DATA_KEY, extra);
        i.putExtra(SearchManager.USER_QUERY, userQ);
        i.putExtra(SearchManager.QUERY, title);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    private void tryWay(int way, String pkg, String act, String dat, String extra, String title, String userQ) {
        String name = "ABCDEF".substring(way, way + 1);
        try {
            Intent i;
            switch (way) {
                case 0: i = base(pkg, act, dat, extra, title, userQ); break;
                case 1: i = base(null, act, dat, extra, title, userQ); break;
                case 2: {
                    i = base(null, act, dat, extra, title, userQ);
                    ResolveInfo hit = null;
                    for (ResolveInfo ri : getPackageManager().queryIntentActivities(new Intent(i).setPackage(pkg), PackageManager.MATCH_ALL)) { hit = ri; break; }
                    if (hit == null) { toast(name + ": no screen of " + pkg + " takes this action and address"); return; }
                    i.setComponent(new ComponentName(hit.activityInfo.packageName, hit.activityInfo.name));
                    break;
                }
                case 3:
                    if (dat == null) { toast(name + ": no address"); return; }
                    i = new Intent(Intent.ACTION_VIEW, Uri.parse(dat)).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    break;
                case 4:
                    i = new Intent(Intent.ACTION_SEARCH).setPackage(pkg).putExtra(SearchManager.QUERY, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    break;
                default:
                    i = getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
                    if (i == null) i = getPackageManager().getLaunchIntentForPackage(pkg);
                    if (i == null) { toast(name + ": no launcher entry"); return; }
            }
            startActivity(i);
            toast(name + ": started");
        } catch (ActivityNotFoundException e) {
            toast(name + ": no activity found for " + act + " " + dat);
        } catch (SecurityException e) {
            toast(name + ": refused: " + e.getMessage());
        } catch (RuntimeException e) {
            toast(name + ": " + e);
        }
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    // ------------------------------------------------------------------ covers

    private void loadCover(final String ref, final String pkg, final ImageView into, final TextView note) {
        pool.submit(() -> {
            String result;
            Bitmap bmp = null;
            try {
                if (ref.matches("\\d+")) {
                    Resources r = getPackageManager().getResourcesForApplication(pkg);
                    android.graphics.drawable.Drawable d = r.getDrawable(Integer.parseInt(ref), null);
                    bmp = Bitmap.createBitmap(Math.max(1, d.getIntrinsicWidth()), Math.max(1, d.getIntrinsicHeight()), Bitmap.Config.ARGB_8888);
                    android.graphics.Canvas cv = new android.graphics.Canvas(bmp);
                    d.setBounds(0, 0, cv.getWidth(), cv.getHeight());
                    d.draw(cv);
                    result = "decoded from the app's resource";
                } else if (ref.startsWith("http://") || ref.startsWith("https://")) {
                    HttpURLConnection con = (HttpURLConnection) new URL(ref).openConnection();
                    con.setConnectTimeout(4000);
                    con.setReadTimeout(4000);
                    try (InputStream in = con.getInputStream()) {
                        BitmapFactory.Options o = new BitmapFactory.Options();
                        o.inSampleSize = 2;
                        bmp = BitmapFactory.decodeStream(in, null, o);
                    }
                    result = bmp == null ? "web link, not an image" : "downloaded from the web link";
                } else {
                    try (InputStream in = getContentResolver().openInputStream(Uri.parse(ref))) {
                        BitmapFactory.Options o = new BitmapFactory.Options();
                        o.inSampleSize = 2;
                        bmp = BitmapFactory.decodeStream(in, null, o);
                    }
                    result = bmp == null ? "opened, not an image" : "opened through the app's provider";
                }
            } catch (Exception e) {
                result = "could not open: " + e.getClass().getSimpleName() + " " + e.getMessage();
            }
            final Bitmap shown = bmp;
            final String res = result;
            ui.post(() -> {
                note.setText(note.getText() + "\n   -> " + res);
                if (shown != null) { into.setImageBitmap(shown); into.setVisibility(View.VISIBLE); }
            });
        });
    }
}
