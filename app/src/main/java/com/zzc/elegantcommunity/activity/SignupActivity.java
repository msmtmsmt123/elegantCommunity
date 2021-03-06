package com.zzc.elegantcommunity.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.zzc.elegantcommunity.MainActivity;
import com.zzc.elegantcommunity.R;
import com.zzc.elegantcommunity.constant.ErrorCode;
import com.zzc.elegantcommunity.model.Header;
import com.zzc.elegantcommunity.model.RegisterModel;
import com.zzc.elegantcommunity.model.User;
import com.zzc.elegantcommunity.retrofit.MyRetrofit;
import com.zzc.elegantcommunity.retrofit.UserService;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


/**
 * Created by Administrator on 2017/7/23.
 */

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";

    @OnClick(R.id.btn_back)
    public void back() {
        finish();
    }
    @OnClick(R.id.link_login)
    public  void temperary(){

    }


    @OnClick(R.id.btn_signup)
    public void finsihSignup() {
        //判断输入是否正确
        int legal = isLegal();
        switch (legal) {
            case ErrorCode.EMPTY_NAME:
                Toast.makeText(this, "账户不能为空", Toast.LENGTH_SHORT).show();
                break;
            case ErrorCode.EMPTY_PASSWARD:
                Toast.makeText(this, "密码不能为空", Toast.LENGTH_SHORT).show();
                break;
            case ErrorCode.NOT_SAME_PASSWORD:
                Toast.makeText(this, "两次输入密码不一致", Toast.LENGTH_SHORT).show();
                break;
        }
        //输入正确
         if(legal==0){
        RegisterModel registerModel = new RegisterModel();
        User user=new User();
        user.setUserAcc(input_name.getText().toString());
        user.setPwd(input_password.getText().toString());
        user.setPhoneNumber(input_mobile.getText().toString());
        user.setUserName(null);
        user.setMale(0);
        user.setEmail(input_email.getText().toString());
        user.setNickName(input_nick_name.getText().toString());
        user.setUserGrade(0);
        user.setVerifyType(0);
        registerModel.setHeader(new Header());
        registerModel.setUser(user);

        Retrofit retrofit = MyRetrofit.getGsonRetrofitInstance();
        UserService userService = retrofit.create(UserService.class);

        Call<ResponseBody> responseBodyCall = userService.signUp(registerModel);
        responseBodyCall.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                try {
                   String respond= response.body().string();
                    Log.i(TAG, "注册返回数据:"+respond);
                    JsonElement je = new JsonParser().parse(respond);
                    String resultCode = je.getAsJsonObject().get("resultCode").toString();
                    Log.i(TAG, "resultCode:"+resultCode);
                    if(resultCode.contains("0000")){
                        Toast.makeText(SignupActivity.this,"注册成功",Toast.LENGTH_LONG).show();
                        Intent intent=new Intent(SignupActivity.this,MainActivity.class);
                        startActivity(intent);
                        finish();
                    }else {
                        //注册失败
                        String resultMessage = je.getAsJsonObject().get("resultMessage").toString();
                        Toast.makeText(SignupActivity.this, "注册失败,"+resultMessage, Toast.LENGTH_LONG).show();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG,"注册失败");
            }
        });
         }
    }
    @BindView(R.id.input_name)
     EditText input_name;
    @BindView(R.id.input_email)
     EditText input_email;
    @BindView(R.id.input_mobile)
     EditText input_mobile;
    @BindView(R.id.input_password)
     EditText input_password;
    @BindView(R.id.input_reEnterPassword)
     EditText input_reEnterPassword;
    @BindView(R.id.input_nick_name)
     EditText input_nick_name;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);

    }

    private int isLegal() {
        if (input_name.getText().toString().equals("")) {
            return ErrorCode.EMPTY_NAME;
        } else if (input_password.getText().toString().equals("")) {
            return ErrorCode.EMPTY_PASSWARD;
        } else if (!input_password.getText().toString().equals(input_reEnterPassword.getText().toString())) {
            return ErrorCode.NOT_SAME_PASSWORD;
        } else {
            return 0;
        }
    }

}
