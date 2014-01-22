package org.apache.cordova.facebook;

import java.util.Iterator;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.Session;
import com.facebook.SessionDefaultAudience;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;
import com.sromku.simple.fb.Permissions;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.SimpleFacebook.OnLoginListener;
import com.sromku.simple.fb.SimpleFacebook.OnLogoutListener;
import com.sromku.simple.fb.SimpleFacebookConfiguration;

public class ConnectPlugin extends CordovaPlugin {

    public static final String SINGLE_SIGN_ON_DISABLED = "service_disabled";
    private final String TAG = "ConnectPlugin";

    private SimpleFacebook facebook;
    private String userId;
    public CallbackContext callbackContext;
    private Bundle paramBundle;
    private boolean showShareDialogAfterLogin = false;
    private String method;
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException{
        PluginResult pr = new PluginResult(PluginResult.Status.NO_RESULT);
        pr.setKeepCallback(true);
        this.callbackContext = callbackContext;
        if (action.equals("init")) {
            try {
                
                  String appId = args.getString(0);
                  if(appId != null && appId.contains("{")){
                    JSONObject jsonObject = new JSONObject(appId);
                    appId = jsonObject.getString("appId");
                  }
                Permissions[] permissions = new Permissions[]
                        {
                            Permissions.BASIC_INFO,
                            Permissions.EMAIL,
                            Permissions.USER_PHOTOS,
                            Permissions.USER_BIRTHDAY,
                            Permissions.USER_LOCATION,
                            Permissions.PUBLISH_ACTION,
                            Permissions.PUBLISH_STREAM
                        };
                    
                        SimpleFacebookConfiguration configuration = new SimpleFacebookConfiguration.Builder()
                            .setAppId(appId)
                            .setNamespace("rundavoodevelopment")
                            .setPermissions(permissions)
                            .setDefaultAudience(SessionDefaultAudience.FRIENDS)
                            .build();
                        
                        SimpleFacebook.setConfiguration(configuration);
                
                facebook = SimpleFacebook.getInstance(this.cordova.getActivity());

                Log.d(TAG, "init: Initializing plugin."+appId);
                if(facebook.isLogin()){
                    Log.d(TAG, "init: Initializing login.");
                       pr = new PluginResult(PluginResult.Status.OK, getResponse());
                       callbackContext.sendPluginResult(pr);
                       return true;
                }
                
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.NO_RESULT));
            } catch (JSONException e) {
               
                e.printStackTrace();
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Invalid JSON args used. expected a string as the first arg."));
                return true;
            }
        }

        else if (action.equals("login")) {
            if (facebook != null) {
                cordova.setActivityResultCallback(this);
                if(!facebook.isLogin()){
                    Runnable runnable = new Runnable() {
                        public void run() {
                        facebook.login(mOnLoginListener);
                        };
                    };
                    cordova.getActivity().runOnUiThread(runnable);
                }else{
                    pr = new PluginResult(PluginResult.Status.OK, getResponse());
                    callbackContext.sendPluginResult(pr);
                    return true;
                }
            } else {
                pr = new PluginResult(PluginResult.Status.ERROR, "Must call init before login.");
            }
        }

        else if (action.equals("logout")) {
            if (facebook != null) {
           
                    facebook.logout(mOnLogoutListener);
                   return true;
            } else {
                pr = new PluginResult(PluginResult.Status.ERROR, "Must call init before logout.");
            }
        }

        else if (action.equals("getLoginStatus")) {
            if (facebook != null) {
                pr = new PluginResult(PluginResult.Status.OK, getResponse());
            } else {
                pr = new PluginResult(PluginResult.Status.ERROR, "Must call init before getLoginStatus.");
            }
        }
        
        else if (action.equals("showDialog")) {
            
            if (facebook != null) {
                 Bundle collect = new Bundle();
                JSONObject params = null;
                try {
                    params = args.getJSONObject(0);
                } catch (JSONException e) {
                    params = new JSONObject();
                }
                
                Iterator<?> iter = params.keys();
                while (iter.hasNext()) {
                    String key = (String) iter.next();
                    if (key.equals("method")) {
                        try {
                            this.method = params.getString(key);
                        } catch (JSONException e) {
                            Log.w(TAG, "Nonstring method parameter provided to dialog");
                        }
                    } else {
                        try {
                            collect.putString(key, params.getString(key));
                        } catch (JSONException e) {
                            // Need to handle JSON parameters
                            Log.w(TAG, "Nonstring parameter provided to dialog discarded");
                        }
                    }
                }
                this.paramBundle =  new Bundle(collect);
                if(facebook.isLogin()){
                    this.showShareDialog();
                }else{
                    this.showShareDialogAfterLogin = true;
                    facebook.login(mOnLoginListener);
                }
                

            } else {
                pr = new PluginResult(PluginResult.Status.ERROR, "Must call init before showDialog.");
            }
            
        }
        callbackContext.sendPluginResult(pr);
        return true;
    }
    private void showShareDialog() {
        Log.d(TAG, method);
            Runnable runnable = new Runnable() {
                public void run() {
                     WebDialog feedDialog = (
                            new WebDialog.FeedDialogBuilder(cordova.getActivity(),
                                SimpleFacebook.getOpenSession(),
                                paramBundle))
                            .setOnCompleteListener(new OnCompleteListener(){

                                @Override
                                public void onComplete(Bundle values,
                                        FacebookException error) {
                                    if(values!= null){
                                        if(values.containsKey("post_id") && values.get("post_id") != null){
                                           callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
                                        }else{
                                           callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
                                        }
                                    }else{
                                        if(error != null){
                                          callbackContext.error("error: " + error.getMessage());
                                        }
                                    }
                                    
                                }})
                            .build();
                       feedDialog.show();
                };
            };
            cordova.getActivity().runOnUiThread(runnable);
    }
    private void toast(String message)
    {
        Toast.makeText(this.cordova.getActivity(), message, Toast.LENGTH_SHORT).show();
    }
    
 // Login listener
    private OnLoginListener mOnLoginListener = new OnLoginListener()
    {

        @Override
        public void onFail(String reason)
        {
             callbackContext.error("error: " +  "Failed to login");
            Log.w(TAG, "Failed to login");
        }

        @Override
        public void onException(Throwable throwable)
        {
            callbackContext.error("error: " + "Bad thing happened");
            Log.e(TAG, "Bad thing happened", throwable);
        }

        @Override
        public void onThinking()
        {
            // show progress bar or something to the user while login is happening
            
        }

        @Override
        public void onLogin()
        {
            
              SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
              prefs.edit().putLong("access_expires", SimpleFacebook.getOpenSession().getExpirationDate().getTime()).commit();
              prefs.edit().putString("access_token", facebook.getAccessToken()).commit();

              if(!showShareDialogAfterLogin){
                  PluginResult pr = new PluginResult(PluginResult.Status.OK, getResponse());
                  callbackContext.sendPluginResult(pr);
              }else{
                  showShareDialog() ;
              }
        }

        @Override
        public void onNotAcceptingPermissions()
        {
            toast("You didn't accept read permissions");
        }
    };

    // Logout listener
    private OnLogoutListener mOnLogoutListener = new OnLogoutListener()
    {


        @Override
        public void onFail(String reason)
        {
            PluginResult pr = new PluginResult(PluginResult.Status.JSON_EXCEPTION, "Error logging out.");
            callbackContext.sendPluginResult(pr);
        }

        @Override
        public void onException(Throwable throwable)
        {
            PluginResult pr = new PluginResult(PluginResult.Status.ERROR, "Error logging out.");
            callbackContext.sendPluginResult(pr);
        }

        @Override
        public void onThinking()
        {
        
        }

        @Override
        public void onLogout()
        {
             SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity());
             prefs.edit().putLong("access_expires", -1).commit();
             prefs.edit().putString("access_token", null).commit();

             
             PluginResult pr = new PluginResult(PluginResult.Status.OK, getResponse());
             callbackContext.sendPluginResult(pr);
        }

    };

    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        facebook.onActivityResult(this.cordova.getActivity(), requestCode, resultCode, data);
        
    }

    public JSONObject getResponse() {
        String response;
        
        Session session = SimpleFacebook.getOpenSession();
        
        if (session!= null && session.isOpened()) {
            long expiresTimeInterval =session.getExpirationDate().getTime() - System.currentTimeMillis();
            long expiresIn = (expiresTimeInterval > 0) ? expiresTimeInterval : 0;
            response = "{"+
            "\"status\": \"connected\","+
            "\"authResponse\": {"+
              "\"accessToken\": \""+facebook.getAccessToken()+"\","+
              "\"expiresIn\": \""+expiresIn+"\","+
              "\"session_key\": true,"+
              "\"sig\": \"...\","+
              "\"userId\": \""+this.userId+"\""+
            "}"+
          "}";
        } else {
            response = "{"+
            "\"status\": \"unknown\""+
          "}";
        }

        try {
            return new JSONObject(response);
        } catch (JSONException e) {
           
            e.printStackTrace();
        }
        return new JSONObject();
    }
    
    
}
