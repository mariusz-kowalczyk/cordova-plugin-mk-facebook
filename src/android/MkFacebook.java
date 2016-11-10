package pl.kowalczyk.mariusz.cordova;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.net.Uri;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookDialogException;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.FacebookServiceException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.GameRequestContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphObject;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.GameRequestDialog;
import com.facebook.share.widget.MessageDialog;
import com.facebook.share.widget.ShareDialog;
import com.facebook.share.widget.AppInviteDialog;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;

/**
 * This class echoes a string called from JavaScript.
 */
public class MkFacebook extends CordovaPlugin {
    
    private final String TAG = "MkFacebook";
    
    private static final int INVALID_ERROR_CODE = -2; //-1 is FacebookRequestError.INVALID_ERROR_CODE

    private ShareDialog shareDialog;
    
    private CallbackManager callbackManager;
    
    private AppEventsLogger logger;
    
    private CallbackContext showDialogContext = null;
    
    private JSONObject params;
    
    private ShareDialog.Mode mode;

    @Override
    protected void pluginInitialize() {
        FacebookSdk.sdkInitialize(cordova.getActivity().getApplicationContext());

        // create callbackManager
        callbackManager = CallbackManager.Factory.create();

        // create AppEventsLogger
        logger = AppEventsLogger.newLogger(cordova.getActivity().getApplicationContext());

        // Set up the activity result callback to this class
        cordova.setActivityResultCallback(this);

        shareDialog = new ShareDialog(cordova.getActivity());
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                if (showDialogContext != null) {
                    showDialogContext.success(result.getPostId());
                    showDialogContext = null;
                }
            }

            @Override
            public void onCancel() {
                FacebookOperationCanceledException e = new FacebookOperationCanceledException();
                handleError(e, showDialogContext);
            }

            @Override
            public void onError(FacebookException e) {
                Log.e("Activity", String.format("Error: %s", e.toString()));
                handleError(e, showDialogContext);
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("shareScreen")) {
            if (!ShareDialog.canShow(ShareLinkContent.class)) {
                callbackContext.error("Cannot show dialog");
                return true;
            }
            showDialogContext = callbackContext;
            
            this.shareScreen(args, callbackContext);
            return true;
        }else if (action.equals("shareLinkDialog")) {
            if (!ShareDialog.canShow(ShareLinkContent.class)) {
                callbackContext.error("Cannot show dialog");
                return true;
            }
            showDialogContext = callbackContext;
            params = args.getJSONObject(0);
            mode = parseMode(args.getString(1));
            
            this.shareLinkDialog();
            return true;
        }
        return false;
    }

    private void shareLinkDialog() {
        
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ShareLinkContent linkcontent = new ShareLinkContent.Builder();
                if(!params.isNull("title")) {
                    linkcontent.setContentTitle(params.getString("title"));
                }
                if(!params.isNull("description")) {
                    linkcontent.setContentDescription(params.getString("description"));
                }
                if(!params.isNull("image")) {
                    linkcontent.setImageUrl(Uri.parse(params.getString("image")));
                }
                if(!params.isNull("url")) {
                    linkcontent.setContentUrl(Uri.parse(params.getString("url")));
                }
                
                shareDialog.show(linkcontent, ShareDialog.Mode.FEED);
            }
        });
    }
    
    private ShareDialog.Mode parseMode(String mode) {
        ShareDialog.Mode _mode = ShareDialog.Mode.FEED;
        switch(mode.toUpperCase()) {
            case "FEED":
                _mode = ShareDialog.Mode.FEED;
                break;
            case "NATIVE":
                _mode = ShareDialog.Mode.NATIVE;
                break;
            case "AUTOMATIC":
                _mode = ShareDialog.Mode.AUTOMATIC;
                break;
            case "WEB":
                _mode = ShareDialog.Mode.WEB;
                break;
        }
        return _mode;
    }
    
    private void shareScreen(JSONArray args, CallbackContext callbackContext) {

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = getBitmap();
                if (bitmap != null) {
                    SharePhoto photo = new SharePhoto.Builder()
                            .setBitmap(bitmap)
                            .build();
                    SharePhotoContent shareContent = new SharePhotoContent.Builder()
                            .addPhoto(photo)
                            .build();

                    shareDialog.show(shareContent);
                } else {
                    //callbackContext.error("Null bitmap.");
                }
            }
        });
        /*
        if (true) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
         */
    }

    private Bitmap getBitmap() {
        Bitmap bitmap = null;

        boolean isCrosswalk = false;
        try {
            Class.forName("org.crosswalk.engine.XWalkWebViewEngine");
            isCrosswalk = true;
        } catch (Exception e) {
        }

        if (isCrosswalk) {
            webView.getPluginManager().postMessage("captureXWalkBitmap", this);
        } else {
            View view = webView.getView();//.getRootView();
            view.setDrawingCacheEnabled(true);
            bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);
        }

        return bitmap;
    }

    private void handleError(FacebookException exception, CallbackContext context) {
        if (exception.getMessage() != null) {
            Log.e(TAG, exception.toString());
        }
        String errMsg = "Facebook error: " + exception.getMessage();
        int errorCode = 1000;
        // User clicked "x"
        if (exception instanceof FacebookOperationCanceledException) {
            errMsg = "User cancelled dialog";
            errorCode = 4201;
        } else if (exception instanceof FacebookDialogException) {
            // Dialog error
            errMsg = "Dialog error: " + exception.getMessage();
        }

        if (context != null) {
            context.error(getErrorResponse(exception, errMsg, errorCode));
        } else {
            Log.e(TAG, "Error already sent so no context, msg: " + errMsg + ", code: " + errorCode);
        }
    }
    
    public JSONObject getFacebookRequestErrorResponse(FacebookRequestError error) {

        String response = "{"
            + "\"errorCode\": \"" + error.getErrorCode() + "\","
            + "\"errorType\": \"" + error.getErrorType() + "\","
            + "\"errorMessage\": \"" + error.getErrorMessage() + "\"";

        if (error.getErrorUserMessage() != null) {
            response += ",\"errorUserMessage\": \"" + error.getErrorUserMessage() + "\"";
        }

        if (error.getErrorUserTitle() != null) {
            response += ",\"errorUserTitle\": \"" + error.getErrorUserTitle() + "\"";
        }

        response += "}";

        try {
            return new JSONObject(response);
        } catch (JSONException e) {

            e.printStackTrace();
        }
        return new JSONObject();
    }
    
    public JSONObject getErrorResponse(Exception error, String message, int errorCode) {
        if (error instanceof FacebookServiceException) {
            return getFacebookRequestErrorResponse(((FacebookServiceException) error).getRequestError());
        }

        String response = "{";

        if (error instanceof FacebookDialogException) {
            errorCode = ((FacebookDialogException) error).getErrorCode();
        }

        if (errorCode != INVALID_ERROR_CODE) {
            response += "\"errorCode\": \"" + errorCode + "\",";
        }

        if (message == null) {
            message = error.getMessage();
        }

        response += "\"errorMessage\": \"" + message + "\"}";

        try {
            return new JSONObject(response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
    }

}
