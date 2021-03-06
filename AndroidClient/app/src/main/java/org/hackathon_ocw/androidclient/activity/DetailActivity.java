package org.hackathon_ocw.androidclient.activity;


import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
//import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import org.hackathon_ocw.androidclient.util.Constants;
import org.hackathon_ocw.androidclient.util.CustomApplication;
import org.hackathon_ocw.androidclient.widget.FullscreenVideoLayout;
import org.hackathon_ocw.androidclient.adapter.PageFragmentAdapter;
import org.hackathon_ocw.androidclient.R;
import org.hackathon_ocw.androidclient.util.SystemBarTintManager;
import org.hackathon_ocw.androidclient.domain.UserProfile;
import org.hackathon_ocw.androidclient.util.Utils;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


public class DetailActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private IWXAPI api;
    private MediaController mediaController;

    private FullscreenVideoLayout videoLayout;

    private Uri uri;
    private ViewPager viewPager;
    private PopupWindow popWindow;
    private InputMethodManager imm;
    private EditText editText;
    private Bitmap videoImage;

    private String courseId;
    private String description;
    private String title;

    private Tracker mTracker;

    private NestedScrollingChildHelper mChildHelper;
    private final DisplayMetrics metrics = new DisplayMetrics();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CustomApplication application = (CustomApplication) getApplication();
        mTracker = application.getDefaultTracker();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_detail);

        api = WXAPIFactory.createWXAPI(this, Constants.APP_ID, true);

        Intent intent = getIntent();
        title = intent.getStringExtra("title");
        description = intent.getStringExtra("description");
        courseId = intent.getStringExtra("id");
        uri = Uri.parse(intent.getStringExtra("videoUrl"));
        UserProfile.getInstance().setNickname(intent.getStringExtra("nickname"));
        UserProfile.getInstance().setHeadimgurl(intent.getStringExtra("headimgurl"));
        UserProfile.getInstance().setUserId(intent.getStringExtra("userid"));
        String videoUrl = intent.getStringExtra("videoImg");

        detailToolBarInit();

        TextView titleDetail = (TextView) findViewById(R.id.titleDetail);
        titleDetail.setText(title);

        RelativeLayout videoLayout = (RelativeLayout) findViewById(R.id.videoLayout);
        if(Utils.isTablet(this.getApplicationContext())) {
            videoLayout.getLayoutParams().height = 1000;
        }
        else{
            videoLayout.getLayoutParams().height = 460;
        }

        getVideoImage(videoUrl);

        videoInit();
        viewPagerInit();
        addListenerOnBackButton();
        addListenerOnShareButton();

        addListenerOnCommentButton();
        addListenerOnViewCommentButton();

        //Google Analytics tracker
        sendScreenImageName();
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Get current timeline
        int position = videoLayout.getCurrentPosition();
        UserProfile.getInstance().setPosition(Long.valueOf(courseId), position);
    }

    @Override
    protected void onResume() {
        super.onResume();

        int position = UserProfile.getInstance().getPosition(Long.valueOf(courseId));
        videoLayout.seekTo(position);
    }


    private void viewPagerInit(){
        viewPager = (ViewPager) findViewById(R.id.detailPager);
        viewPager.setAdapter(new PageFragmentAdapter(getSupportFragmentManager(),
                DetailActivity.this));

        // Give the TabLayout the ViewPager
        TabLayout tabLayout = (TabLayout) findViewById(R.id.detailTabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
    }

    @SuppressWarnings("ConstantConditions")
    private void detailToolBarInit(){
        Toolbar detailToolbar = (Toolbar) findViewById(R.id.detailToolbar);
        setSupportActionBar(detailToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        detailToolbar.setPadding(0, getStatusBarHeight(), 0, 0);
        SystemBarTintManager tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintResource(R.color.colorPrimaryDark);

        TextView titleToolBar = (TextView) findViewById(R.id.titleToolBar);
        titleToolBar.setText("学啥");
    }

    private void videoInit(){
        videoLayout = (FullscreenVideoLayout)findViewById(R.id.videoView);
        videoLayout.setActivity(this);
        videoLayout.setShouldAutoplay(true);
        try{
            videoLayout.setVideoURI(uri);
        }catch (IOException e)
        {
            e.printStackTrace();
            Log.e("videoLayout", e.toString());
        }
    }

    private void getVideoImage(String url) {

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        ImageRequest request = new ImageRequest(url, new Response.Listener<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                //给imageView设置图片
                videoImage = response;
            }
        }, 0, 0, Bitmap.Config.RGB_565, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        requestQueue.add(request);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        videoLayout.resize();
    }

    // A method to find height of the status bar
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    private void addListenerOnBackButton() {
        Button backBtn = (Button) findViewById(R.id.backBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void addListenerOnShareButton() {
        //Share to Wechat
        Button shareBtn = (Button) findViewById(R.id.shareBtn);
        shareBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                PopupMenu popupMenu = new PopupMenu(DetailActivity.this, v);
                //Use reflect to solve the issue that icon can't show in android 3.0+
                try {
                    Field[] fields = popupMenu.getClass().getDeclaredFields();
                    for (Field field : fields) {
                        if ("mPopup".equals(field.getName())) {
                            field.setAccessible(true);
                            Object menuPopupHelper = field.get(popupMenu);
                            Class<?> classPopupHelper = Class.forName(menuPopupHelper
                                    .getClass().getName());
                            Method setForceIcons = classPopupHelper.getMethod(
                                    "setForceShowIcon", boolean.class);
                            setForceIcons.invoke(menuPopupHelper, true);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                popupMenu.setOnMenuItemClickListener(DetailActivity.this);
                popupMenu.inflate(R.menu.detail);
                popupMenu.show();

            }
        });
    }

    public boolean onMenuItemClick(MenuItem item){
        switch (item.getItemId()){
            case R.id.shareWXSession:
                WXShare(true);
                return true;
            case R.id.shareWXTimeline:
                WXShare(false);
                return true;
            default:
                return false;
        }
    }

    private void WXShare(boolean isTimelineCb){
        //WXVideoObject videoObject = new WXVideoObject();
        //videoObject.videoUrl = uri.toString();

        WXWebpageObject webpageObject = new WXWebpageObject();
        webpageObject.webpageUrl = uri.toString();

        //WXMediaMessage msg = new WXMediaMessage(videoObject);
        WXMediaMessage msg = new WXMediaMessage(webpageObject);
        msg.title = title;
        msg.description = description;
        if (videoImage != null) {
            videoImage.getHeight();
            Bitmap thumb = Bitmap.createScaledBitmap(videoImage, 150, 120, true);
            //videoImage.recycle();
            msg.thumbData = Utils.bmpToByteArray(thumb);
        }

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = buildTransaction("webpage");
        req.message = msg;
        req.scene = isTimelineCb ? SendMessageToWX.Req.WXSceneSession : SendMessageToWX.Req.WXSceneTimeline;
        api.sendReq(req);

    }

    private void addListenerOnViewCommentButton(){
        //Change view
        Button viewComment = (Button)findViewById(R.id.ViewCommentBtn);
        viewComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(1, true);
            }
        });
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }

    private void addListenerOnCommentButton(){
        editText = (EditText)findViewById(R.id.EditComment);
        if(Utils.isTablet(this.getApplicationContext())) {
            editText.getLayoutParams().width = metrics.widthPixels - 200;
        }
        editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    commentShowPopup(v);
                    editText.clearFocus();
                    popUpInputMethodWindow();
                }
            }
        });
    }

    @Override
    public void onBackPressed(){
        if(popWindow != null && popWindow.isShowing()) {
            popWindow.dismiss();
        }
        else {
            super.onBackPressed();
        }
    }

    private void commentShowPopup(final View parent){

        if(popWindow == null)
        {
            LayoutInflater layoutInflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.comment_popwindow, null);
            popWindow = new PopupWindow(view, LinearLayout.LayoutParams.FILL_PARENT, 80);
        }
        else
        {
            popWindow.update();
        }
        //popWindow.setAnimationStyle(R.style.pop);
        EditText writeCommentPopWin = (EditText) popWindow.getContentView().findViewById(R.id.WriteCommentPopWin);
        if(Utils.isTablet(this.getApplicationContext())) {
            writeCommentPopWin.getLayoutParams().width = metrics.widthPixels - 200;
        }
        popWindow.setBackgroundDrawable(new ShapeDrawable());
        popWindow.setFocusable(true);
        popWindow.setOutsideTouchable(true);
        popWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        popWindow.showAtLocation(parent, Gravity.BOTTOM, 0, 0);
        addListenerOnSendCommentButton();

        popWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                popDownInputMethodWindow(parent);
            }
        });

        popWindow.setTouchInterceptor(new View.OnTouchListener() {
            public boolean onTouch(View view, MotionEvent event) {
                return false;
            }
        });
    }

    private void popUpInputMethodWindow(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                imm = (InputMethodManager) editText.getContext().getSystemService(Service.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }, 0);
    }

    private void popDownInputMethodWindow(final View view){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                imm = (InputMethodManager) editText.getContext().getSystemService(Service.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }, 0);
    }

    private void addListenerOnSendCommentButton(){
        ImageButton sendCommentBtn = (ImageButton)popWindow.getContentView().findViewById(R.id.SendBtn);
        sendCommentBtn.setColorFilter(Color.parseColor("#64B5F6"));
        sendCommentBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Get item_id
                int item_id = Integer.valueOf(courseId);

                //Get author_id
                int author_id = Integer.valueOf(UserProfile.getInstance().getUserId());
                String author_name;
                if(UserProfile.getInstance().getNickname() != null) {
                    author_name = UserProfile.getInstance().getNickname();
                }
                else {
                    author_name = "匿名用户";
                }
                int like = 0;

                //Get post time
                Calendar currentTime = Calendar.getInstance();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.CHINA);
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                String currentTimeStr = simpleDateFormat.format(currentTime.getTime());

                //Get text
                EditText editText = (EditText)popWindow.getContentView().findViewById(R.id.WriteCommentPopWin);
                String comment = editText.getText().toString();

                //Get current timeline
                int timeline = videoLayout.getCurrentPosition() / 1000;

                //Get Url
                String httpurl = "http://jieko.cc/item/" + courseId + "/Comments";

                //Send a POST message
                RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

                JSONObject jsonObject = new JSONObject();
                try
                {
                    jsonObject.put("item_id", item_id);
                    jsonObject.put("author_id", author_id);
                    jsonObject.put("author_name", author_name);
                    jsonObject.put("posted", currentTimeStr);
                    jsonObject.put("text", comment);
                    jsonObject.put("timeline", timeline);
                    jsonObject.put("like", like);
                }catch (Exception e)
                {
                    Log.e("Json Error",e.toString());
                }
                JsonRequest<JSONObject> jsonRequest = new JsonObjectRequest(Request.Method.POST,httpurl, jsonObject,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                Log.d("Response", response.toString());

                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Error.Response", error.toString());
                    }
                })
                {
                    @Override
                    public Map<String, String> getHeaders() {
                        HashMap<String, String> headers = new HashMap<>();
                        headers.put("Accept", "application/json");
                        headers.put("Content-Type", "application/json; charset=UTF-8");

                        return headers;
                    }
                };
                requestQueue.add(jsonRequest);

                //Update the comment list

                HashMap<String, String>map = new HashMap<String, String>();
                map.put("commentId", String.valueOf(item_id));
                map.put("userName", author_name);
                map.put("commentTime", currentTimeStr);
                map.put("comment", comment);
                map.put("timeline", String.valueOf(timeline));
                map.put("like", String.valueOf(like));
                if(UserProfile.getInstance().getHeadimgurl() != null)
                {
                    map.put("headimgurl", UserProfile.getInstance().getHeadimgurl());
                }

                PageFragmentAdapter pageFragmentAdapter = (PageFragmentAdapter)viewPager.getAdapter();
                pageFragmentAdapter.getTabComment().mCommentAdapter.AddComments(map);

                popWindow.dismiss();
            }
        });

    }


    //Google Analytics
    private void sendScreenImageName() {
        String name = title;
        // [START screen_view_hit]
        //Log.i(TAG, "Setting screen name: " + name);
        mTracker.setScreenName("Screen~" + name);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        // [END screen_view_hit]
    }
}