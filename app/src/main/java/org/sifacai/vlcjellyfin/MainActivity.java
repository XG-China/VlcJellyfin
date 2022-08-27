package org.sifacai.vlcjellyfin;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

public class MainActivity extends BaseActivity {
    private String TAG = "JMainActivity:";
    private Activity mActivity = null;
    private TextView tvLoginOut;
    private LinearLayout tvContiner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.jellyfin_home);

        disableActiveBarBack();

        mActivity = this;
        tvLoginOut = findViewById(R.id.tvLoginOut);
        tvContiner = findViewById(R.id.tvItems);
        getConfigFromSP();
    }

    @Override
    protected void onResume() {
        initView();
        super.onResume();
    }

    private void initView() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (Utils.AccessToken.equals("") || Utils.UserId.equals("")) {
                    login();
                } else {
                    initData();
                }
            }
        }).start();
    }

    private void initData() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showLoadingDialog("正在加载首页，请稍候…………");
                showLogoutBtn();
                tvContiner.removeAllViews();
            }
        });

        String viewsUrl = "/Users/" + Utils.UserId + "/Views";
        String resumeUrl = "/Users/" + Utils.UserId + "/Items/Resume?";
        resumeUrl += "Limit=12&Recursive=true&Fields=PrimaryImageAspectRatio,BasicSyncInfo";
        resumeUrl += "&ImageTypeLimit=1&EnableImageTypes=Primary,Backdrop,Thumb";
        resumeUrl += "&EnableTotalRecordCount=false&MediaTypes=Video";

        String viewsJson = Utils.okhttpSend(viewsUrl);
        if (viewsJson.length() > 0) {
            JsonObject viewsObj = new Gson().fromJson(viewsJson, JsonObject.class);
            JsonArray views = viewsObj.getAsJsonArray("Items").getAsJsonArray();
            if (views.size() > 0) {
                addRowTvRecyclerView("我的媒体", views, true);
                String resumeJson = Utils.okhttpSend(resumeUrl);
                if (resumeJson.length() > 0) {
                    JsonObject resumeObj = new Gson().fromJson(resumeJson, JsonObject.class);
                    JsonArray resumes = resumeObj.getAsJsonArray("Items").getAsJsonArray();
                    addRowTvRecyclerView("最近播放", resumes, false);
                }
                for (int i = 0; i < views.size(); i++) {
                    JsonObject item = views.get(i).getAsJsonObject();
                    String itemid = item.get("Id").getAsString();
                    String itemname = item.get("Name").getAsString();
                    String lastestUrl = "/Users/" + Utils.UserId + "/Items/Latest?";
                    lastestUrl += "Limit=16&Fields=PrimaryImageAspectRatio%2CBasicSyncInfo%2CPath";
                    lastestUrl += "&ImageTypeLimit=1&EnableImageTypes=Primary,Backdrop,Thumb";
                    lastestUrl += "&ParentId=" + itemid;
                    String lastestJson = Utils.okhttpSend(lastestUrl);
                    JsonArray lastes = new Gson().fromJson(lastestJson, JsonArray.class);
                    addRowTvRecyclerView("新的 " + itemname, lastes, false);
                    //hideLoading();
                    dismissLoadingDialog();
                }
            }
        }
    }

    /**
     * 添加类别行
     */
    private void addRowTvRecyclerView(String title, JsonArray data, boolean horizon) {
        Log.d(TAG, "addRowTvRecyclerView: " + data.toString());
        JRecyclerView tvRecyclerView = (JRecyclerView) LayoutInflater.from(this)
                                                        .inflate(R.layout.home_horizon_tvrecycler,null);
        ((V7LinearLayoutManager)tvRecyclerView.getLayoutManager()).setOrientation(V7LinearLayoutManager.HORIZONTAL);

        JAdapter jAdapter = new JAdapter(data, horizon);
        jAdapter.setOnItemClickListener(new JAdapter.OnItemClickListener() {
            @Override
            public void onClick(JsonObject jo) {
                String type = Utils.getJsonString(jo,"Type").getAsString();
                String itemId = jo.get("Id").getAsString();
                Intent intent = null;
                if(type.equals("Folder") || type.equals("CollectionFolder")){
                    intent = new Intent(mActivity,CollectionActivity.class);
                }else{
                    intent = new Intent(mActivity,DetailActivity.class);
                }
                intent.putExtra("itemId",itemId);
                mActivity.startActivity(intent);
            }
        });
        tvRecyclerView.setAdapter(jAdapter);

        TextView titleView = new TextView(tvRecyclerView.getContext());
        titleView.setText(title);
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvContiner.addView(titleView);
                tvContiner.addView(tvRecyclerView);
            }
        });
    }

    private void showMessage(String msg) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mActivity, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void login() {
        boolean notL = true;
        if (ValidUrl(Utils.JellyfinUrl)) {
            Log.d(TAG, "initView: Url有效");
            if (authenticateByName(Utils.UserName, Utils.PassWord)) {
                Log.d(TAG, "initView: 用户名密码有效");
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLogoutBtn();
                    }
                });
                notL = false;
            }
        }
        if (notL) {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    loginDialog();
                }
            });
        } else {
            initData();
        }
    }

    private void showLogoutBtn() {
        tvLoginOut.setVisibility(View.VISIBLE);
        tvLoginOut.setText("注 销");
        tvLoginOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logout();
            }
        });
    }

    /**
     * 登 出
     */
    private void logout() {
        SharedPreferences sp = this.getSharedPreferences("Jellyfin", this.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.commit();
        Utils.UserId = "";
        Utils.AccessToken = "";
        finish();
    }

    /**
     * 读取配置
     */
    private void getConfigFromSP() {
        SharedPreferences sp = this.getSharedPreferences("Jellyfin", this.MODE_PRIVATE);
        Utils.JellyfinUrl = sp.getString("url", "");
        Utils.UserName = sp.getString("username", "");
        Utils.PassWord = sp.getString("password", "");
    }

    /**
     * 保存配置
     *
     * @param url
     * @param username
     * @param password
     */
    private void saveConfigToSP(String url, String username, String password) {
        SharedPreferences sp = this.getSharedPreferences("Jellyfin", this.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("url", url);
        editor.putString("username", username);
        editor.putString("password", password);
        editor.commit();
    }

    /**
     * 登录框
     */
    private void loginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.create();
        dialog.setTitle("");
        dialog.setMessage("请输入服务器地址、用户名、和密码！");
        dialog.setCanceledOnTouchOutside(false);

        LinearLayout ll = new LinearLayout(builder.getContext());
        ll.setOrientation(LinearLayout.VERTICAL);
        int pd = Utils.getPixelsFromDp(mActivity,mActivity.getResources().getDimensionPixelSize(R.dimen.padding_border));
        ll.setPadding(pd,pd,pd,pd);
        EditText urlInput = new EditText(ll.getContext());
        urlInput.setHint("服务器地址");
        urlInput.setText(Utils.JellyfinUrl);
        EditText unInput = new EditText(ll.getContext());
        unInput.setHint("用户名");
        EditText pwInput = new EditText(ll.getContext());
        pwInput.setHint("密码");
        Button commitBtn = new Button(ll.getContext());
        commitBtn.setText("确定");
        commitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = urlInput.getText().toString();
                String un = unInput.getText().toString();
                String pw = pwInput.getText().toString();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (ValidUrl(url)) {
                            if (authenticateByName(un, pw)) {
                                saveConfigToSP(url, un, pw);
                                dialog.dismiss();
                                initData(); //刷新首页
                            } else {
                                showMessage("用户名或密码无效，请重新输入！");
                            }
                        } else {
                            showMessage("服务器地址无效，请重新输入！");
                        }
                    }
                }).start();
            }
        });
        Button canelBtn = new Button(ll.getContext());
        canelBtn.setText("取消");
        canelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                mActivity.finish();
            }
        });
        ll.addView(urlInput);
        ll.addView(unInput);
        ll.addView(pwInput);
        ll.addView(commitBtn);
        ll.addView(canelBtn);
        dialog.setView(ll);
        dialog.show();
    }

    /**
     * 验证服务器url
     *
     * @param url
     * @return
     */
    private boolean ValidUrl(String url) {
        boolean valid = false;
        if (url.length() > 0) {
            if (url.startsWith("http://") || url.startsWith("https://")) {
                valid = true;
            }
        }
        if (valid) {
            String publicUrl = url + "/system/info/public";
            String publicInfo = Utils.okhttpSend(publicUrl);
            JsonObject serverInfo = Utils.JsonToObj(publicInfo,JsonObject.class);
            if (serverInfo != null) {
                String ServerId = serverInfo.get("Id").getAsString();
                if (ServerId == null || ServerId.length() == 0) {
                    return false;
                } else {
                    Utils.JellyfinUrl = url;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 验证用户名密码
     *
     * @param username
     * @param password
     * @return 用户信息
     */
    private boolean authenticateByName(String username, String password) {
        String url = "/Users/authenticatebyname";
        String reqjson = "{\"Username\":\"" + username + "\",\"Pw\":\"" + password + "\"}";
        String userinfo = Utils.okhttpSend(url, reqjson);
        Log.d(TAG, "authenticateByName: userinf:" + userinfo);
        JsonObject userObj = Utils.JsonToObj(userinfo,JsonObject.class);
        if (userObj != null) {
            String userId = Utils.getJsonString(userObj,"User").getAsJsonObject().get("Id").getAsString();
            String Token = userObj.get("AccessToken").getAsString();
            if (Token != null) {
                Utils.UserId = userId;
                Utils.AccessToken = Token;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private long exitTime = 0;
    public void exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(getApplicationContext(), "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
            System.exit(0);
        }
    }
}