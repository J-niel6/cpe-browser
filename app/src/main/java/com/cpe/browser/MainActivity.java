package com.cpe.browser;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONTokener;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    static final int ACC = 0xFF3BA4FF, BG = 0xFF061327, CARD = 0xFF0F2342;
    static final String UA_DESK = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    static final String START = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
            + "<body style='background:#061327;color:#eaf4ff;font-family:sans-serif;text-align:center;padding:40px 20px'>"
            + "<h1 style='color:#3BA4FF'>Cpe Browser</h1><p>Pega un enlace o escribe para buscar en la barra de arriba.</p>"
            + "<p><a style='color:#8fd0ff' href='https://www.google.com'>Google</a> &nbsp; "
            + "<a style='color:#8fd0ff' href='https://es.wikipedia.org'>Wikipedia</a> &nbsp; "
            + "<a style='color:#8fd0ff' href='https://www.pexels.com'>Pexels</a></p>"
            + "<p><a style='display:inline-block;background:#3BA4FF;color:#fff;padding:12px 20px;border-radius:24px;text-decoration:none' href='cpe://link'>⬇ Descargar desde un enlace</a></p>"
            + "<p style='color:#8fb0d8;font-size:14px'>Mantén pulsada cualquier foto o video para descargarlo, o pega el enlace directo de un archivo en la barra.</p></body></html>";
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
    static final String HOOK = "(function(){if(window.__cpeh)return;window.__cpeh=1;document.addEventListener('play',function(e){var v=e.target;if(!v||v.tagName!=='VIDEO')return;CpeApp.onPlay(v.currentSrc||v.src||'')},true);var t=null,sx=0,sy=0;function pick(x,y){var els=document.elementsFromPoint(x,y);for(var i=0;i<els.length;i++){var el=els[i],u='',k='';if(el.tagName==='VIDEO'){u=el.currentSrc||el.src||'';if(!u){var s=el.querySelector('source');u=s?s.src:''}k='v'}else if(el.tagName==='IMG'){u=el.currentSrc||el.src||'';k='i'}else{var bg=getComputedStyle(el).backgroundImage||'';var a=bg.indexOf('url(');if(a>=0){var b=bg.indexOf(')',a);u=bg.substring(a+4,b).split(String.fromCharCode(34)).join('').split(String.fromCharCode(39)).join('');k='i'}}if(u){try{u=new URL(u,location.href).href}catch(e){}CpeApp.onPress(k,u);return}}}document.addEventListener('touchstart',function(e){var p=e.touches[0];sx=p.clientX;sy=p.clientY;clearTimeout(t);t=setTimeout(function(){pick(sx,sy)},600)},{passive:true,capture:true});document.addEventListener('touchmove',function(e){var p=e.touches[0];if(Math.abs(p.clientX-sx)>10||Math.abs(p.clientY-sy)>10)clearTimeout(t)},{passive:true,capture:true});document.addEventListener('touchend',function(){clearTimeout(t)},{passive:true,capture:true});document.addEventListener('touchcancel',function(){clearTimeout(t)},{passive:true,capture:true});})()";
    static final String DETECT = "(function(){var c=[];function a(u){if(!u)return;try{u=new URL(u,location.href).href}catch(e){return}if(/^https?:/.test(u))c.push(u)}['meta[property=\"og:video\"]','meta[property=\"og:video:url\"]','meta[property=\"og:video:secure_url\"]','meta[name=\"twitter:player:stream\"]'].forEach(function(q){var m=document.querySelector(q);if(m)a(m.content)});document.querySelectorAll('video').forEach(function(v){a(v.currentSrc||v.src);v.querySelectorAll('source').forEach(function(s){a(s.src)})});return JSON.stringify(c)})()";
    final ExecutorService pool = Executors.newFixedThreadPool(3);
    final Map<Long, String> audioJobs = new ConcurrentHashMap<>();
    TextView offer;
    String playing;
    String lastLink = "";
    long lastLinkT = 0;
    boolean warned = false;
    BroadcastReceiver doneRx;
    final Runnable hideOffer = new Runnable() { public void run() { offer.setVisibility(View.GONE); } };

    static class Item {
        String url, type, title, info = "Cargando información…", ext = "";
        Bitmap thumb;
        boolean checked, audio, isAudioFile;
        int w, h;
        long dur, size = -1;
        Map<String, String> hd;
        int q;
        String mime;
    }

    class Js {
        @JavascriptInterface
        public void onPress(final String kind, final String u) { runOnUiThread(new Runnable() { public void run() { handlePress(kind, u); } }); }

        @JavascriptInterface
        public void onPlay(final String src) { runOnUiThread(new Runnable() { public void run() { handlePlay(src); } }); }
    }

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
        dl = btn("⬇", v -> showDownloads(null));
        dl.setTextColor(ACC);
        nav.addView(dl, lp);
        main.addView(nav);

        root.addView(main, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        offer = new TextView(this);
        offer.setText("⬇  Descargar este video");
        offer.setTextColor(Color.WHITE);
        offer.setTextSize(16);
        offer.setTypeface(null, android.graphics.Typeface.BOLD);
        offer.setPadding(dp(20), dp(12), dp(20), dp(12));
        GradientDrawable og = new GradientDrawable();
        og.setColor(ACC);
        og.setCornerRadius(dp(26));
        offer.setBackground(og);
        offer.setElevation(dp(8));
        offer.setVisibility(View.GONE);
        offer.setOnClickListener(v -> { offer.setVisibility(View.GONE); showDownloads(playing); });
        FrameLayout.LayoutParams olp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        olp.bottomMargin = dp(70);
        root.addView(offer, olp);
        setContentView(root);
        doneRx = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                final long id = i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                final String nm = audioJobs.remove(id);
                if (nm != null) pool.execute(new Runnable() { public void run() { extract(id, nm); } });
            }
        };
        IntentFilter fl = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(doneRx, fl, Context.RECEIVER_EXPORTED);
        else registerReceiver(doneRx, fl);
        setupWeb();

        if (!handleIntent(getIntent())) home();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        handleIntent(i);
    }

    boolean skipClip = false;
    boolean clipAsking = false;

    String firstUrl(String t) {
        if (t == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("https?://[^\\s]+").matcher(t);
        if (!m.find()) return null;
        String u = m.group();
        while (u.length() > 0 && ".,;:!?)\"'".indexOf(u.charAt(u.length() - 1)) >= 0) u = u.substring(0, u.length() - 1);
        return u;
    }

    boolean handleIntent(Intent i) {
        if (i == null) return false;
        if (Intent.ACTION_SEND.equals(i.getAction())) {
            String u = firstUrl(i.getStringExtra(Intent.EXTRA_TEXT));
            if (u == null) { toast("No encontré un enlace en lo que compartiste"); return false; }
            skipClip = true;
            go(u);
            return true;
        }
        if (i.getData() != null) { skipClip = true; go(i.getData().toString()); return true; }
        return false;
    }

    void checkClipboard() {
        if (skipClip) { skipClip = false; return; }
        if (clipAsking) return;
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (!cm.hasPrimaryClip() || cm.getPrimaryClip().getItemCount() == 0) return;
            CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
            final String u = firstUrl(t == null ? null : t.toString());
            if (u == null) return;
            android.content.SharedPreferences sp = getSharedPreferences("cpe", MODE_PRIVATE);
            if (u.equals(sp.getString("lastClip", ""))) return;
            sp.edit().putString("lastClip", u).apply();
            clipAsking = true;
            String shown = u.length() > 90 ? u.substring(0, 90) + "…" : u;
            new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setTitle("Enlace copiado")
                    .setMessage(shown)
                    .setPositiveButton("Abrir", (d, w) -> go(u))
                    .setNeutralButton("Descargar archivo", (d, w) -> showLink(u, null))
                    .setNegativeButton("Ignorar", null)
                    .setOnDismissListener(d -> clipAsking = false)
                    .show();
        } catch (Exception e) { }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && web != null) checkClipboard();
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
                if (u.startsWith("cpe://link")) { askLink(); return true; }
                if (u.startsWith("http://") || u.startsWith("https://")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception e) { }
                return true;
            }

            @Override
            public void onPageStarted(WebView v, String pu, Bitmap f) {
                if (!url.hasFocus() && !pu.contains("inicio.cpe")) url.setText(pu);
                sniffed.clear();
                warned = false;
                offer.setVisibility(View.GONE);
                badge();
            }

            @Override
            public void onPageFinished(WebView v, String pu) {
                v.evaluateJavascript(HOOK, null);
                v.evaluateJavascript(DETECT, res -> detect(res));
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
                offer.bringToFront();
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

        web.addJavascriptInterface(new Js(), "CpeApp");
        web.setDownloadListener((u, ua, cd, mime, len) -> download(u, cd, mime));

        web.setOnLongClickListener(v -> {
            WebView.HitTestResult h = web.getHitTestResult();
            final String ex = h.getExtra();
            int t = h.getType();
            if (ex == null) return false;
            if (t == WebView.HitTestResult.IMAGE_TYPE || t == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                showLink(ex, "i");
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
        if (u.startsWith("http") && isMediaExt(extOf(u))) { showLink(u, null); return; }
        if (u.startsWith("https://www.google.com/search?q=")) web.loadUrl(u); else probeThenOpen(u);
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(url.getWindowToken(), 0);
        url.clearFocus();
        web.requestFocus();
    }

    long download(String u, String cd, String mime) {
        try {
            if (u == null || !(u.startsWith("https://") || u.startsWith("http://"))) {
                toast("El enlace no es una dirección HTTP/HTTPS válida");
                return -1;
            }
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(u));
            String cleanMime = mime == null ? "" : mime.split(";")[0].trim().toLowerCase(Locale.ROOT);
            String name = URLUtil.guessFileName(u, cd, cleanMime);
            if (name == null || name.trim().isEmpty()) name = "descarga_" + System.currentTimeMillis();
            // Evita rutas y caracteres que pueden provocar nombres inválidos en Android.
            name = name.replaceAll("[/\\\\:*?\"<>|\\r\\n]", "_").trim();
            while (name.startsWith(".")) name = name.substring(1);
            if (name.isEmpty()) name = "descarga_" + System.currentTimeMillis();

            String ext = extOf(name);
            if (!cleanMime.isEmpty()) {
                String wanted = MimeTypeMap.getSingleton().getExtensionFromMimeType(cleanMime);
                if (wanted != null && !wanted.isEmpty() && !wanted.equalsIgnoreCase(ext)) {
                    int dot = name.lastIndexOf('.');
                    name = (dot > 0 ? name.substring(0, dot) : name) + "." + wanted;
                }
            }
            if (cleanMime.isEmpty()) {
                String guessed = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(Locale.ROOT));
                if (guessed != null) cleanMime = guessed;
            }

            String ck = CookieManager.getInstance().getCookie(u);
            if (ck != null && !ck.isEmpty()) r.addRequestHeader("Cookie", ck);
            r.addRequestHeader("User-Agent", web.getSettings().getUserAgentString());
            if (web.getUrl() != null && web.getUrl().startsWith("http")) r.addRequestHeader("Referer", web.getUrl());
            r.setTitle(name);
            r.setDescription("Cpe Browser · Descargas");
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "CpeBrowser/" + name);
            if (!cleanMime.isEmpty()) r.setMimeType(cleanMime);
            long id = ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(r);
            toast("Descargando: " + name + "\nSe guarda en Descargas/CpeBrowser");
            return id;
        } catch (Exception e) {
            toast("No se pudo iniciar la descarga: " + e.getMessage());
            return -1;
        }
    }

    String nameOf(String u) {
        String n = Uri.parse(u).getLastPathSegment();
        if (n == null || n.isEmpty()) n = u;
        return n.length() > 40 ? n.substring(0, 40) + "…" : n;
    }

    String probeType(String u, Map<String, String> hd) {
        // Algunos servidores no implementan HEAD; en ese caso se consulta solo el primer byte.
        String[] methods = new String[]{"HEAD", "GET"};
        for (int attempt = 0; attempt < methods.length; attempt++) {
            HttpURLConnection c = null;
            try {
                c = open(u, hd, methods[attempt]);
                if (attempt == 1) c.setRequestProperty("Range", "bytes=0-0");
                c.setInstanceFollowRedirects(true);
                c.setConnectTimeout(7000);
                c.setReadTimeout(7000);
                int code = c.getResponseCode();
                String ct = c.getContentType();
                if (code >= 200 && code < 400 && ct != null && !ct.trim().isEmpty()) return ct.toLowerCase(Locale.ROOT);
            } catch (Exception e) {
                // Se intenta el método alternativo; no se interpreta el fallo como un video descargable.
            } finally {
                if (c != null) c.disconnect();
            }
        }
        return "";
    }

    void probeThenOpen(final String u) {
        final Map<String, String> hd = headersFor(u);
        url.setText(u);
        bar.setVisibility(View.VISIBLE);
        bar.setProgress(15);
        pool.execute(new Runnable() {
            public void run() {
                final String ct = probeType(u, hd);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (ct.startsWith("video/") || ct.startsWith("audio/") || ct.startsWith("image/")) {
                            bar.setVisibility(View.GONE);
                            showLink(u, null);
                        } else if (ct.contains("mpegurl")) {
                            bar.setVisibility(View.GONE);
                            toast("Este enlace es una transmisión por fragmentos (HLS); no se puede descargar como un solo archivo.");
                        } else web.loadUrl(u);
                    }
                });
            }
        });
    }

    void found(String u) {
        playing = u;
        offer.setVisibility(View.VISIBLE);
        offer.bringToFront();
        offer.removeCallbacks(hideOffer);
        offer.postDelayed(hideOffer, 10000);
    }

    void detect(String res) {
        try {
            if (res == null || res.equals("null")) return;
            JSONArray a = new JSONArray(String.valueOf(new JSONTokener(res).nextValue()));
            int n = Math.min(a.length(), 3);
            for (int i = 0; i < n; i++) {
                final String u = a.getString(i);
                if (u.toLowerCase().contains(".m3u8")) continue;
                if (guessType(extOf(u)).equals("v")) { found(u); return; }
                final Map<String, String> hd = headersFor(u);
                pool.execute(new Runnable() {
                    public void run() {
                        String ct = probeType(u, hd);
                        if (ct.startsWith("video/")) runOnUiThread(new Runnable() { public void run() { found(u); } });
                    }
                });
            }
        } catch (Exception e) { }
    }

    void handlePlay(String src) {
        String cand = null;
        if (src != null && src.startsWith("http") && !src.toLowerCase().contains(".m3u8")) cand = src;
        else synchronized (sniffed) { for (String x : sniffed) cand = x; }
        if (cand == null) {
            if (!warned) { warned = true; toast("Este video usa transmisión protegida o por fragmentos; no se puede descargar."); }
            return;
        }
        playing = cand;
        offer.setVisibility(View.VISIBLE);
        offer.bringToFront();
        offer.removeCallbacks(hideOffer);
        offer.postDelayed(hideOffer, 10000);
    }

    String extOf(String u) {
        String p = u.toLowerCase();
        int q = p.indexOf('?');
        if (q > 0) p = p.substring(0, q);
        int d = p.lastIndexOf('.');
        return d >= 0 && p.length() - d <= 5 ? p.substring(d + 1) : "";
    }

    Map<String, String> headersFor(String u) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("User-Agent", web.getSettings().getUserAgentString());
        if (web.getUrl() != null) h.put("Referer", web.getUrl());
        String ck = CookieManager.getInstance().getCookie(u);
        if (ck != null) h.put("Cookie", ck);
        return h;
    }

    long pl(String s) { try { return Long.parseLong(s); } catch (Exception e) { return 0; } }

    String qual(int h) { return h >= 2160 ? "4K" : h >= 1440 ? "2K" : h >= 1080 ? "1080p" : h >= 720 ? "720p" : h >= 480 ? "480p" : h >= 360 ? "360p" : h + "p"; }

    String dur(long ms) {
        long s = ms / 1000;
        return s >= 3600 ? String.format(Locale.US, "%d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60)
                : String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }

    String sz(long b) {
        return b >= (1L << 30) ? String.format(Locale.US, "%.2f GB", b / 1073741824.0)
                : b >= (1L << 20) ? String.format(Locale.US, "%.1f MB", b / 1048576.0) : (b / 1024) + " KB";
    }

    String describe(Item it) {
        if (it.type.equals("page")) return "🌐 Esto es una página web, no un archivo de video o imagen. Toca «Abrir página», reproduce el video y usa el botón ⬇ de la app.";
        StringBuilder s = new StringBuilder();
        if (it.type.equals("i")) {
            s.append("🖼️ ").append(it.ext.isEmpty() ? "IMG" : it.ext.toUpperCase());
            if (it.w > 0) s.append(" · ").append(it.w).append("×").append(it.h);
        } else if (it.isAudioFile) {
            s.append("🎵 ").append(it.ext.toUpperCase());
        } else if (it.audio) {
            s.append("🎧 Solo audio (M4A)");
        } else {
            s.append("🎬 ").append(it.ext.isEmpty() ? "VIDEO" : it.ext.toUpperCase());
            if (it.h > 0) s.append(" · ").append(qual(it.h)).append(" ").append(it.w).append("×").append(it.h);
        }
        if (it.dur > 0) s.append(" · ").append(dur(it.dur));
        if (it.size > 0) s.append(" · ").append(sz(it.size));
        if (it.audio && !it.isAudioFile) s.append(" (baja el video y extrae el audio)");
        return s.toString();
    }

    HttpURLConnection open(String u, Map<String, String> hd, String m) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setRequestMethod(m);
        for (Map.Entry<String, String> e : hd.entrySet()) c.setRequestProperty(e.getKey(), e.getValue());
        c.setConnectTimeout(8000);
        c.setReadTimeout(10000);
        return c;
    }

    void loadMeta(final Item it, final Runnable refresh, final boolean[] dead) {
        pool.execute(new Runnable() {
            public void run() {
                try {
                    try {
                        HttpURLConnection c = open(it.url, it.hd, "HEAD");
                        int status = c.getResponseCode();
                        it.size = c.getContentLengthLong();
                        String ct = c.getContentType();
                        c.disconnect();
                        if ((status >= 400 || ct == null || ct.trim().isEmpty()) && !dead[0]) {
                            c = open(it.url, it.hd, "GET");
                            c.setRequestProperty("Range", "bytes=0-0");
                            status = c.getResponseCode();
                            if (it.size <= 0) {
                                String cr = c.getHeaderField("Content-Range");
                                if (cr != null && cr.contains("/")) it.size = pl(cr.substring(cr.lastIndexOf('/') + 1));
                                else it.size = c.getContentLengthLong();
                            }
                            ct = c.getContentType();
                            c.disconnect();
                        }
                        if (ct != null) {
                            ct = ct.toLowerCase();
                            it.mime = ct;
                            if (ct.startsWith("text/html") || ct.startsWith("application/xhtml")) it.type = "page";
                            else if (ct.startsWith("image/")) it.type = "i";
                            else if (ct.startsWith("video/") || ct.startsWith("audio/")) { it.type = "v"; it.isAudioFile = ct.startsWith("audio/"); }
                            if (it.ext.isEmpty() && ct.contains("/")) {
                                String sub = ct.substring(ct.indexOf('/') + 1);
                                int sc = sub.indexOf(';');
                                if (sc > 0) sub = sub.substring(0, sc);
                                it.ext = sub.equals("jpeg") ? "jpg" : sub.equals("mpeg") ? "mp3" : sub;
                            }
                        }
                    } catch (Exception e) { }
                    if (dead[0]) return;
                    if (it.type.equals("v")) {
                        MediaMetadataRetriever m = new MediaMetadataRetriever();
                        try {
                            m.setDataSource(it.url, it.hd);
                            it.dur = pl(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                            it.w = (int) pl(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                            it.h = (int) pl(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                            if (!it.isAudioFile && !dead[0]) {
                                Bitmap f = m.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                                if (f != null) it.thumb = Bitmap.createScaledBitmap(f, 480, Math.max(1, f.getHeight() * 480 / f.getWidth()), true);
                            }
                        } catch (Exception e) { }
                        finally { try { m.release(); } catch (Exception e) { } }
                    } else if (it.type.equals("i")) {
                        HttpURLConnection c = open(it.url, it.hd, "GET");
                        InputStream in = c.getInputStream();
                        BitmapFactory.Options o = new BitmapFactory.Options();
                        o.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(in, null, o);
                        in.close();
                        c.disconnect();
                        it.w = o.outWidth;
                        it.h = o.outHeight;
                        if (it.w > 0 && it.size < 20000000 && !dead[0]) {
                            c = open(it.url, it.hd, "GET");
                            in = c.getInputStream();
                            BitmapFactory.Options o2 = new BitmapFactory.Options();
                            o2.inSampleSize = Math.max(1, it.w / 480);
                            it.thumb = BitmapFactory.decodeStream(in, null, o2);
                            in.close();
                            c.disconnect();
                        }
                    }
                } catch (Exception e) { }
                it.info = describe(it);
                if (!dead[0]) runOnUiThread(refresh);
            }
        });
    }

    View row(final Item it, final BaseAdapter ad) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(8), dp(8), dp(8), dp(8));
        final CheckBox cb = new CheckBox(this);
        cb.setChecked(it.checked);
        cb.setOnClickListener(v -> it.checked = cb.isChecked());
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackgroundColor(CARD);
        if (it.thumb != null) iv.setImageBitmap(it.thumb);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(88), dp(62));
        ip.setMargins(dp(4), 0, dp(10), 0);
        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setText(it.title);
        t.setTextColor(Color.WHITE);
        t.setTextSize(14);
        t.setMaxLines(2);
        TextView n = new TextView(this);
        n.setText(it.info);
        n.setTextColor(0xFF8FD0FF);
        n.setTextSize(12);
        tx.addView(t);
        tx.addView(n);
        r.addView(cb);
        r.addView(iv, ip);
        r.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (it.type.equals("v") && !it.isAudioFile) {
            TextView f = new TextView(this);
            f.setText(it.audio ? "🎧" : "🎬");
            f.setTextSize(24);
            f.setPadding(dp(10), dp(4), dp(4), dp(4));
            f.setOnClickListener(v -> { it.audio = !it.audio; it.info = describe(it); ad.notifyDataSetChanged(); });
            r.addView(f);
        }
        r.setOnClickListener(v -> { it.checked = !it.checked; ad.notifyDataSetChanged(); });
        return r;
    }

    void showDownloads(final String preferred) {
        web.evaluateJavascript(SCAN, res -> {
            LinkedHashMap<String, String> m = new LinkedHashMap<>();
            if (preferred != null) m.put(preferred, "v");
            synchronized (sniffed) { for (String s : sniffed) if (!m.containsKey(s)) m.put(s, "v"); }
            try {
                if (res != null && !res.equals("null")) {
                    JSONArray a = new JSONArray(String.valueOf(new JSONTokener(res).nextValue()));
                    for (int i = 0; i < a.length(); i++) {
                        JSONArray p = a.getJSONArray(i);
                        if (!m.containsKey(p.getString(1))) m.put(p.getString(1), p.getString(0));
                    }
                }
            } catch (Exception ex) { }
            final List<Item> items = new ArrayList<>();
            for (Map.Entry<String, String> en : m.entrySet()) {
                String u = en.getKey();
                if (u.toLowerCase().contains(".m3u8")) continue;
                Item it = new Item();
                it.url = u;
                it.type = en.getValue();
                it.title = nameOf(u);
                it.ext = extOf(u);
                it.isAudioFile = it.ext.equals("mp3") || it.ext.equals("m4a");
                it.hd = headersFor(u);
                it.checked = preferred != null && u.equals(preferred);
                items.add(it);
            }
            if (items.isEmpty()) {
                toast("No encontré archivos descargables directos en esta página. Los videos con transmisión protegida (como YouTube o Netflix) no se pueden descargar.");
                return;
            }
            final boolean[] dead = {false};
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            TextView hint = new TextView(this);
            hint.setText("Toca 🎬 / 🎧 de cada video para elegir MP4 o solo audio (M4A).");
            hint.setTextColor(0xFF8FB0D8);
            hint.setTextSize(12);
            hint.setPadding(dp(20), dp(4), dp(20), dp(8));
            ListView lv = new ListView(this);
            box.addView(hint);
            box.addView(lv);
            final BaseAdapter ad = new BaseAdapter() {
                public int getCount() { return items.size(); }
                public Object getItem(int i) { return items.get(i); }
                public long getItemId(int i) { return i; }
                public View getView(int i, View cv, ViewGroup p) { return row(items.get(i), this); }
            };
            lv.setAdapter(ad);
            final AlertDialog dlg = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setTitle("Descargar de esta página").setView(box)
                    .setPositiveButton("Descargar", (d, w) -> {
                        int n = 0;
                        for (Item it : items) if (it.checked) { startItem(it); n++; }
                        if (n == 0) toast("No seleccionaste nada");
                    })
                    .setNegativeButton("Cancelar", null)
                    .setNeutralButton("Todo", null).create();
            dlg.setOnDismissListener(d -> dead[0] = true);
            dlg.show();
            dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(x -> {
                for (Item it : items) it.checked = true;
                ad.notifyDataSetChanged();
            });
            final Runnable refresh = () -> ad.notifyDataSetChanged();
            for (Item it : items) loadMeta(it, refresh, dead);
        });
    }

    boolean isMediaExt(String e) {
        return e.equals("jpg") || e.equals("jpeg") || e.equals("png") || e.equals("gif") || e.equals("webp") || e.equals("bmp")
                || e.equals("mp4") || e.equals("webm") || e.equals("mkv") || e.equals("mov") || e.equals("m4v") || e.equals("mp3") || e.equals("m4a");
    }

    String guessType(String e) {
        if (e.equals("jpg") || e.equals("jpeg") || e.equals("png") || e.equals("gif") || e.equals("webp") || e.equals("bmp")) return "i";
        return isMediaExt(e) ? "v" : "f";
    }

    void handlePress(String kind, String u) {
        if (u == null) return;
        if (u.startsWith("http") && !u.toLowerCase().contains(".m3u8")) { showLink(u, kind); return; }
        if ("v".equals(kind)) {
            String cand = null;
            synchronized (sniffed) { for (String x : sniffed) cand = x; }
            if (cand != null) showLink(cand, "v");
            else toast("Este video usa transmisión protegida o por fragmentos; no se puede descargar.");
        } else toast("Esta imagen no se puede descargar directamente.");
    }

    void askLink() {
        final EditText et = new EditText(this);
        et.setHint("https://…/foto.jpg  o  video.mp4");
        et.setHintTextColor(0xFF8FB0D8);
        et.setTextColor(Color.WHITE);
        et.setSingleLine(true);
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (cm.hasPrimaryClip()) {
                CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
                if (t != null && t.toString().trim().startsWith("http")) et.setText(t.toString().trim());
            }
        } catch (Exception e) { }
        new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK).setTitle("Descargar desde un enlace")
                .setMessage("Pega el enlace directo de una imagen, foto o video.").setView(et)
                .setPositiveButton("Continuar", (d, w) -> showLink(et.getText().toString().trim(), null))
                .setNegativeButton("Cancelar", null).show();
    }

    void buildOpts(RadioGroup rg, final Item it) {
        rg.setOnCheckedChangeListener(null);
        rg.removeAllViews();
        String[] o = it.type.equals("i")
                ? new String[]{"Original (sin cambios)", "JPG calidad alta (hasta 2048 px)", "JPG calidad media (1280 px)", "JPG calidad baja (720 px)"}
                : it.type.equals("v")
                ? (it.isAudioFile ? new String[]{"Audio original"} : new String[]{"🎬 Video MP4 (calidad original)", "🎧 Solo audio (M4A)"})
                : it.type.equals("page") ? new String[]{"No hay un archivo que descargar en este enlace"}
                : new String[]{"Archivo original"};
        for (int i = 0; i < o.length; i++) {
            RadioButton b = new RadioButton(this);
            b.setId(100 + i);
            b.setText(o[i]);
            b.setTextColor(Color.WHITE);
            rg.addView(b);
        }
        if (it.q >= o.length) it.q = 0;
        rg.check(100 + it.q);
        it.audio = it.type.equals("v") && !it.isAudioFile && it.q == 1;
        rg.setOnCheckedChangeListener((g, id) -> {
            it.q = id - 100;
            it.audio = it.type.equals("v") && !it.isAudioFile && it.q == 1;
        });
    }

    void showLink(final String u, String kind) {
        if (u == null || !u.startsWith("http")) { toast("Escribe un enlace que empiece con http o https"); return; }
        long now = SystemClock.elapsedRealtime();
        if (u.equals(lastLink) && now - lastLinkT < 2500) return;
        lastLink = u;
        lastLinkT = now;
        final Item it = new Item();
        it.url = u;
        it.title = nameOf(u);
        it.ext = extOf(u);
        it.hd = headersFor(u);
        it.isAudioFile = it.ext.equals("mp3") || it.ext.equals("m4a");
        it.type = kind != null && !kind.isEmpty() ? kind : guessType(it.ext);
        final boolean[] dead = {false};
        final String[] built = {""};
        final AlertDialog[] dref = {null};
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), dp(8));
        final ImageView pv = new ImageView(this);
        pv.setBackgroundColor(CARD);
        pv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        box.addView(pv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(190)));
        final TextView tv = new TextView(this);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(13);
        tv.setPadding(0, dp(10), 0, dp(6));
        box.addView(tv);
        final RadioGroup rg = new RadioGroup(this);
        box.addView(rg);
        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        final Runnable refresh = () -> {
            if (it.thumb != null) pv.setImageBitmap(it.thumb);
            tv.setText(it.title + "\n" + it.info);
            if (dref[0] != null && dref[0].getButton(AlertDialog.BUTTON_POSITIVE) != null)
                dref[0].getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!it.type.equals("page"));
            String key = it.type + it.isAudioFile;
            if (!built[0].equals(key)) { built[0] = key; buildOpts(rg, it); }
        };
        final AlertDialog dlg = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Descargar").setView(sv)
                .setPositiveButton("Descargar", (d, w) -> {
                    if (it.type.equals("i") && it.q > 0) saveScaled(it); else startItem(it);
                })
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Abrir página", (d, w) -> web.loadUrl(u)).create();
        dref[0] = dlg;
        dlg.setOnDismissListener(d -> dead[0] = true);
        dlg.show();
        refresh.run();
        loadMeta(it, refresh, dead);
    }

    java.io.OutputStream openOut(String name, String mime) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            cv.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/CpeBrowser");
            Uri o = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            return getContentResolver().openOutputStream(o);
        }
        File d = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CpeBrowser");
        d.mkdirs();
        return new java.io.FileOutputStream(new File(d, name));
    }

    void saveScaled(final Item it) {
        toast("Procesando imagen…");
        pool.execute(() -> {
            try {
                final int mx = it.q == 1 ? 2048 : it.q == 2 ? 1280 : 720;
                final int ql = it.q == 1 ? 92 : it.q == 2 ? 85 : 75;
                HttpURLConnection c = open(it.url, it.hd, "GET");
                InputStream in = c.getInputStream();
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) {
                    bo.write(buf, 0, n);
                    if (bo.size() > 40000000) throw new Exception("la imagen es demasiado grande");
                }
                in.close();
                c.disconnect();
                byte[] data = bo.toByteArray();
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, o);
                if (o.outWidth <= 0) throw new Exception("no es una imagen válida");
                int ss = 1;
                while (Math.max(o.outWidth, o.outHeight) / ss > mx * 2) ss *= 2;
                BitmapFactory.Options o2 = new BitmapFactory.Options();
                o2.inSampleSize = ss;
                Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, o2);
                if (b == null) throw new Exception("no se pudo leer la imagen");
                float k = Math.min(1f, (float) mx / Math.max(b.getWidth(), b.getHeight()));
                if (k < 1f) b = Bitmap.createScaledBitmap(b, Math.round(b.getWidth() * k), Math.round(b.getHeight() * k), true);
                Bitmap r = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas cvs = new Canvas(r);
                cvs.drawColor(Color.WHITE);
                cvs.drawBitmap(b, 0, 0, null);
                String nm = URLUtil.guessFileName(it.url, null, null);
                int dd = nm.lastIndexOf('.');
                if (dd > 0) nm = nm.substring(0, dd);
                final String name = nm + (it.q == 1 ? "_alta" : it.q == 2 ? "_media" : "_baja") + ".jpg";
                java.io.OutputStream os = openOut(name, "image/jpeg");
                r.compress(Bitmap.CompressFormat.JPEG, ql, os);
                os.close();
                runOnUiThread(() -> toast("🖼️ Imagen guardada: " + name + "\nEn Descargas/CpeBrowser"));
            } catch (final Exception e) {
                runOnUiThread(() -> toast("No se pudo guardar la imagen: " + e.getMessage()));
            }
        });
    }

    void startItem(Item it) {
        if (it.type.equals("page")) { toast("Esto es una página web, no un archivo. Ábrela y usa el botón ⬇ al reproducir el video."); return; }
        String name = URLUtil.guessFileName(it.url, null, null);
        if (it.audio && !it.isAudioFile) {
            long id = download(it.url, null, null);
            if (id > 0) {
                int d = name.lastIndexOf('.');
                audioJobs.put(id, d > 0 ? name.substring(0, d) : name);
                toast("Descargando el video para extraer el audio…");
            }
        } else download(it.url, null, it.mime);
    }

    void extract(long id, String base) {
        try {
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            Cursor c = dm.query(new DownloadManager.Query().setFilterById(id));
            boolean ok = false;
            if (c != null) {
                if (c.moveToFirst()) ok = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL;
                c.close();
            }
            if (!ok) return;
            Uri src = dm.getUriForDownloadedFile(id);
            if (src == null) throw new Exception("no se encontró el archivo");
            MediaExtractor ex = new MediaExtractor();
            ex.setDataSource(this, src, null);
            int ti = -1;
            MediaFormat fmt = null;
            for (int i = 0; i < ex.getTrackCount(); i++) {
                MediaFormat f = ex.getTrackFormat(i);
                String mm = f.getString(MediaFormat.KEY_MIME);
                if (mm != null && mm.startsWith("audio/")) { ti = i; fmt = f; break; }
            }
            if (ti < 0) { ex.release(); throw new Exception("el video no tiene pista de audio"); }
            ex.selectTrack(ti);
            final String name = base + ".m4a";
            MediaMuxer mux;
            ParcelFileDescriptor pfd = null;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues cv = new ContentValues();
                cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                cv.put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4");
                cv.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/CpeBrowser");
                Uri out = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                pfd = getContentResolver().openFileDescriptor(out, "rw");
                mux = new MediaMuxer(pfd.getFileDescriptor(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            } else {
                File d = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CpeBrowser");
                d.mkdirs();
                mux = new MediaMuxer(new File(d, name).getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            }
            int o = mux.addTrack(fmt);
            mux.start();
            ByteBuffer buf = ByteBuffer.allocate(1 << 20);
            MediaCodec.BufferInfo bi = new MediaCodec.BufferInfo();
            while (true) {
                buf.clear();
                int n = ex.readSampleData(buf, 0);
                if (n < 0) break;
                bi.set(0, n, ex.getSampleTime(), (ex.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0 ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0);
                mux.writeSampleData(o, buf, bi);
                ex.advance();
            }
            mux.stop();
            mux.release();
            ex.release();
            if (pfd != null) pfd.close();
            dm.remove(id);
            runOnUiThread(() -> toast("🎧 Audio guardado: " + name + "\nEn Descargas/CpeBrowser"));
        } catch (final Exception e) {
            runOnUiThread(() -> toast("No se pudo extraer el audio (" + e.getMessage() + "). El video quedó descargado."));
        }
    }

    void showMenu(View anchor) {
        PopupMenu pm = new PopupMenu(this, anchor);
        pm.getMenu().add(0, 7, 0, "⬇ Descargar desde un enlace…");
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
                case 7: askLink(); break;
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
    protected void onDestroy() {
        try { unregisterReceiver(doneRx); } catch (Exception e) { }
        pool.shutdownNow();
        web.destroy();
        super.onDestroy();
    }
}
