package com.cpe.browser;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {

    static final int ACC = 0xFF3BA4FF, BG = 0xFF061327, CARD = 0xFF0F2342;
    static final String UA_DESK = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    static final String START = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
            + "<body style='background:#061327;color:#eaf4ff;font-family:sans-serif;text-align:center;padding:40px 20px'>"
            + "<h1 style='color:#3BA4FF'>Cpe Browser</h1><p>Pega un enlace o escribe para buscar en la barra de arriba.</p>"
            + "<p><a style='color:#8fd0ff' href='https://www.google.com'>Google</a> &nbsp; "
            + "<a style='color:#8fd0ff' href='https://es.wikipedia.org'>Wikipedia</a> &nbsp; "
            + "<a style='color:#8fd0ff' href='https://www.pexels.com'>Pexels</a></p>"
            + "<p style='color:#8fb0d8;font-size:14px'>Mantén pulsada una imagen para descargarla, o toca ⬇ para ver los archivos de la página.</p></body></html>";
    static final String SCAN = "(function(){var o=[];function a(u,t){if(!u)return;try{u=new URL(u,location.href).href;if(/^https?:/.test(u))o.push([t,u])}catch(e){}}"
            + "document.querySelectorAll('video').forEach(function(v){a(v.currentSrc||v.src,'v');v.querySelectorAll('source').forEach(function(s){a(s.src,'v')})});"
            + "document.querySelectorAll('img').forEach(function(i){if((i.naturalWidth||0)>=120)a(i.currentSrc||i.src,'i')});"
            + "document.querySelectorAll('a[href]').forEach(function(l){var h=l.href;if(/\\.(mp4|webm|mkv|mov|m4v|mp3|m4a)(\\?|$)/i.test(h))a(h,'v');else if(/\\.(jpe?g|png|gif|webp|pdf|zip)(\\?|$)/i.test(h))a(h,'i')});"
            + "return JSON.stringify(o)})()";

    WebView web;
    EditText url;
    ProgressBar bar;
    Button dl;
    FrameLayout root;
    LinearLayout main;
    View custom;
    WebChromeClient.CustomViewCallback cvCb;
    ValueCallback<Uri[]> fileCb;
    String mobileUa;
    boolean desktop = false;
    final Set<String> sniffed = Collections.synchronizedSet(new LinkedHashSet<String>());

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }

    Button btn(String t, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(t);
        b.setTextColor(Color.WHITE);
        b.setTextSize(18);
        b.setAllCaps(false);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setOnClickListener(l);
        return b;
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    void copy(String s) {
        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("url", s));
        toast("Enlace copiado");
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        List<String> need = new ArrayList<>();
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            need.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED)
            need.add("android.permission.POST_NOTIFICATIONS");
        if (!need.isEmpty()) requestPermissions(need.toArray(new String[0]), 1);

        root = new FrameLayout(this);
        main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(BG);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8), dp(6), dp(4), dp(6));
        url = new EditText(this);
        url.setSingleLine(true);
        url.setHint("Pega un enlace o busca…");
        url.setHintTextColor(0xFF8FB0D8);
        url.setTextColor(Color.WHITE);
        url.setTextSize(15);
        url.setSelectAllOnFocus(true);
        url.setImeOptions(EditorInfo.IME_ACTION_GO);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        GradientDrawable g = new GradientDrawable();
        g.setColor(CARD);
        g.setCornerRadius(dp(22));
        url.setBackground(g);
        url.setPadding(dp(16), dp(8), dp(16), dp(8));
        url.setOnEditorActionListener((tv, a, ev) -> { go(url.getText().toString()); return true; });
        top.addView(url, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(btn("➜", v -> go(url.getText().toString())));
        final Button menuBtn = btn("⋮", null);
        menuBtn.setOnClickListener(v -> showMenu(menuBtn));
        top.addView(menuBtn);
        main.addView(top);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.getProgressDrawable().setColorFilter(ACC, PorterDuff.Mode.SRC_IN);
        main.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        web = new WebView(this);
        main.addView(web, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setBackgroundColor(CARD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1);
        nav.addView(btn("◀", v -> { if (web.canGoBack()) web.goBack(); }), lp);
        nav.addView(btn("▶", v -> { if (web.canGoForward()) web.goForward(); }), lp);
        nav.addView(btn("⟳", v -> web.reload()), lp);
        nav.addView(btn("🏠", v -> home()), lp);
        dl = btn("⬇", v -> showDownloads());
        dl.setTextColor(ACC);
        nav.addView(dl, lp);
        main.addView(nav);

        root.addView(main, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        setupWeb();

        Uri d = getIntent() != null ? getIntent().getData() : null;
        if (d != null) go(d.toString()); else home();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        if (i.getData() != null) go(i.getData().toString());
    }

    void home() { web.loadDataWithBaseURL("https://inicio.cpe/", START, "text/html", "utf-8", null); url.setText(""); }

    void setupWeb() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        mobileUa = WebSettings.getDefaultUserAgent(this).replace("; wv", "");
        s.setUserAgentString(mobileUa);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String u = r.getUrl().toString();
                if (u.startsWith("http://") || u.startsWith("https://")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception e) { }
                return true;
            }

            @Override
            public void onPageStarted(WebView v, String pu, Bitmap f) {
                if (!url.hasFocus() && !pu.contains("inicio.cpe")) url.setText(pu);
                sniffed.clear();
                badge();
            }

            @Override
            public void onPageFinished(WebView v, String pu) {
                if (!url.hasFocus() && !pu.contains("inicio.cpe")) url.setText(pu);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                String u = r.getUrl().toString();
                String l = u.toLowerCase();
                int q = l.indexOf('?');
                String p = q > 0 ? l.substring(0, q) : l;
                if (p.matches(".*\\.(mp4|webm|mkv|mov|m4v|mp3|m4a)$") && sniffed.add(u)) runOnUiThread(() -> badge());
                return null;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                bar.setProgress(p);
                bar.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onShowCustomView(View v, CustomViewCallback cb) {
                if (custom != null) { cb.onCustomViewHidden(); return; }
                custom = v;
                cvCb = cb;
                main.setVisibility(View.GONE);
                root.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }

            @Override
            public void onHideCustomView() { hideCustom(); }

            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCb != null) fileCb.onReceiveValue(null);
                fileCb = cb;
                try { startActivityForResult(p.createIntent(), 2); }
                catch (Exception e) { fileCb = null; return false; }
                return true;
            }
        });

        web.setDownloadListener((u, ua, cd, mime, len) -> download(u, cd, mime));

        web.setOnLongClickListener(v -> {
            WebView.HitTestResult h = web.getHitTestResult();
            final String ex = h.getExtra();
            int t = h.getType();
            if (ex == null) return false;
            if (t == WebView.HitTestResult.IMAGE_TYPE || t == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                new AlertDialog.Builder(this).setTitle("Imagen")
                        .setItems(new String[]{"⬇ Descargar imagen", "📋 Copiar enlace de la imagen"}, (d, i) -> {
                            if (i == 0) download(ex, null, null); else copy(ex);
                        }).show();
                return true;
            }
            if (t == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                new AlertDialog.Builder(this).setTitle("Enlace")
                        .setItems(new String[]{"Abrir", "⬇ Descargar archivo", "📋 Copiar enlace"}, (d, i) -> {
                            if (i == 0) web.loadUrl(ex); else if (i == 1) download(ex, null, null); else copy(ex);
                        }).show();
                return true;
            }
            return false;
        });
    }

    void badge() { dl.setText(sniffed.isEmpty() ? "⬇" : "⬇ " + sniffed.size()); }

    void go(String in) {
        in = in.trim();
        if (in.isEmpty()) return;
        String u;
        if (in.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) u = in;
        else if (!in.contains(" ") && in.contains(".") && !in.endsWith(".")) u = "https://" + in;
        else u = "https://www.google.com/search?q=" + Uri.encode(in);
        web.loadUrl(u);
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(url.getWindowToken(), 0);
        url.clearFocus();
        web.requestFocus();
    }

    void download(String u, String cd, String mime) {
        try {
            if (u == null || !u.startsWith("http")) { toast("Este enlace no se puede descargar directamente"); return; }
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(u));
            String name = URLUtil.guessFileName(u, cd, mime);
            String ck = CookieManager.getInstance().getCookie(u);
            if (ck != null) r.addRequestHeader("Cookie", ck);
            r.addRequestHeader("User-Agent", web.getSettings().getUserAgentString());
            if (web.getUrl() != null) r.addRequestHeader("Referer", web.getUrl());
            r.setTitle(name);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "CpeBrowser/" + name);
            if (mime != null && !mime.isEmpty()) r.setMimeType(mime);
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(r);
            toast("Descargando: " + name + "\nSe guarda en Descargas/CpeBrowser");
        } catch (Exception e) {
            toast("No se pudo descargar: " + e.getMessage());
        }
    }

    String nameOf(String u) {
        String n = Uri.parse(u).getLastPathSegment();
        if (n == null || n.isEmpty()) n = u;
        return n.length() > 40 ? n.substring(0, 40) + "…" : n;
    }

    void showDownloads() {
        web.evaluateJavascript(SCAN, res -> {
            LinkedHashMap<String, String> m = new LinkedHashMap<>();
            synchronized (sniffed) { for (String s : sniffed) m.put(s, "v"); }
            try {
                if (res != null && !res.equals("null")) {
                    JSONArray a = new JSONArray(String.valueOf(new JSONTokener(res).nextValue()));
                    for (int i = 0; i < a.length(); i++) {
                        JSONArray p = a.getJSONArray(i);
                        if (!m.containsKey(p.getString(1))) m.put(p.getString(1), p.getString(0));
                    }
                }
            } catch (Exception ex) { }
            final List<String> urls = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (Map.Entry<String, String> en : m.entrySet()) {
                String u = en.getKey();
                if (u.toLowerCase().contains(".m3u8")) continue;
                urls.add(u);
                labels.add((en.getValue().equals("v") ? "🎬 " : "🖼️ ") + nameOf(u));
            }
            if (urls.isEmpty()) {
                toast("No encontré archivos descargables directos en esta página. Los videos con transmisión protegida (como YouTube o Netflix) no se pueden descargar.");
                return;
            }
            final boolean[] chk = new boolean[urls.size()];
            final AlertDialog dlg = new AlertDialog.Builder(this).setTitle("Descargar de esta página")
                    .setMultiChoiceItems(labels.toArray(new String[0]), chk, (d, i, c) -> chk[i] = c)
                    .setPositiveButton("Descargar", (d, w) -> {
                        int n = 0;
                        for (int i = 0; i < chk.length; i++) if (chk[i]) { download(urls.get(i), null, null); n++; }
                        if (n == 0) toast("No seleccionaste nada");
                    })
                    .setNegativeButton("Cancelar", null)
                    .setNeutralButton("Todo", null).create();
            dlg.show();
            dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(x -> {
                ListView lv = dlg.getListView();
                for (int i = 0; i < chk.length; i++) { chk[i] = true; lv.setItemChecked(i, true); }
            });
        });
    }

    void showMenu(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add(0, 1, 0, desktop ? "Modo móvil" : "Modo escritorio");
        pm.getMenu().add(0, 2, 1, "Copiar enlace");
        pm.getMenu().add(0, 3, 2, "Compartir enlace");
        pm.getMenu().add(0, 4, 3, "Abrir en otro navegador");
        pm.getMenu().add(0, 5, 4, "Ver descargas");
        pm.getMenu().add(0, 6, 5, "Borrar datos de navegación");
        pm.setOnMenuItemClickListener(it -> {
            String cur = web.getUrl() == null ? "" : web.getUrl();
            switch (it.getItemId()) {
                case 1:
                    desktop = !desktop;
                    web.getSettings().setUserAgentString(desktop ? UA_DESK : mobileUa);
                    web.reload();
                    break;
                case 2: copy(cur); break;
                case 3:
                    Intent s = new Intent(Intent.ACTION_SEND);
                    s.setType("text/plain");
                    s.putExtra(Intent.EXTRA_TEXT, cur);
                    startActivity(Intent.createChooser(s, "Compartir enlace"));
                    break;
                case 4:
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(cur))); } catch (Exception e) { toast("No hay otro navegador"); }
                    break;
                case 5: startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)); break;
                case 6:
                    new AlertDialog.Builder(this).setTitle("¿Borrar datos?")
                            .setMessage("Se borrarán cookies, caché e historial. Cerrarás tus sesiones en los sitios.")
                            .setPositiveButton("Borrar", (d, w) -> {
                                web.clearCache(true);
                                web.clearHistory();
                                CookieManager.getInstance().removeAllCookies(null);
                                WebStorage.getInstance().deleteAllData();
                                toast("Datos borrados");
                            }).setNegativeButton("Cancelar", null).show();
                    break;
            }
            return true;
        });
        pm.show();
    }

    void hideCustom() {
        if (custom == null) return;
        root.removeView(custom);
        custom = null;
        main.setVisibility(View.VISIBLE);
        getWindow().getDecorView().setSystemUiVisibility(0);
        if (cvCb != null) cvCb.onCustomViewHidden();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 2 && fileCb != null) {
            fileCb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            fileCb = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (custom != null) hideCustom();
        else if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onPause() { super.onPause(); web.onPause(); }

    @Override
    protected void onResume() { super.onResume(); web.onResume(); }

    @Override
    protected void onDestroy() { web.destroy(); super.onDestroy(); }
}
