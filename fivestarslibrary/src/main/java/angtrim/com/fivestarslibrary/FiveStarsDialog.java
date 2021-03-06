package angtrim.com.fivestarslibrary;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;


/**
 * Created by angtrim on 12/09/15.
 *
 */
public class FiveStarsDialog  implements DialogInterface.OnClickListener{

    protected final static String DEFAULT_TITLE = "Rate this app";
    protected final static String DEFAULT_TEXT = "How much do you love our app?";
    protected final static String DEFAULT_POSITIVE = "Ok";
    protected final static String DEFAULT_NEGATIVE = "Not Now";
    protected final static String DEFAULT_NEVER = "Never";
    protected final static String SP_NUM_OF_ACCESS = "numOfAccess";
    protected static final String SP_DISABLED = "disabled";
    protected static final String TAG = FiveStarsDialog.class.getSimpleName();
    protected final Context context;
    protected boolean isForceMode = false;
    SharedPreferences sharedPrefs;
    protected String supportEmail;
    protected TextView contentTextView;
    protected RatingBar ratingBar;
    protected String title = null;
    protected String rateText = null;
    protected AlertDialog alertDialog;
    protected View dialogView;
    protected int upperBound = 4;
    protected NegativeReviewListener negativeReviewListener;
    protected ReviewListener reviewListener;


    public FiveStarsDialog(Context context,String supportEmail){
        this.context = context;
        sharedPrefs = context.getSharedPreferences(context.getPackageName(),Context.MODE_PRIVATE);
        this.supportEmail = supportEmail;
    }

    protected void build(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        dialogView = inflater.inflate(R.layout.stars, null);
        String titleToAdd = (title == null) ? DEFAULT_TITLE : title;
        String textToAdd = (rateText == null) ? DEFAULT_TEXT : rateText;
        contentTextView = (TextView)dialogView.findViewById(R.id.text_content);
        contentTextView.setText(textToAdd);
        ratingBar = (RatingBar) dialogView.findViewById(R.id.ratingBar);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                Log.d(TAG, "Rating changed : " + v);
                if (isForceMode && v >= upperBound) {
                    openMarket();
                    if(reviewListener != null)
                        reviewListener.onReview((int)ratingBar.getRating());
                }
            }
        });
        alertDialog = builder.setTitle(titleToAdd)
                .setView(dialogView)
                .setNegativeButton(DEFAULT_NEGATIVE,this)
                .setPositiveButton(DEFAULT_POSITIVE,this)
                .setNeutralButton(DEFAULT_NEVER,this)
                .create();
    }



    protected void disable() {
        SharedPreferences shared = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putBoolean(SP_DISABLED, true);
        editor.apply();
    }

    protected void openMarket() {
        final String appPackageName = context.getPackageName();
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }


    private void sendEmail() {
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, supportEmail);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "App Report ("+context.getPackageName()+")");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        context.startActivity(Intent.createChooser(emailIntent, "Send mail..."));
    }




    private void show() {
        boolean disabled  = sharedPrefs.getBoolean(SP_DISABLED, false);
        if(!disabled){
            build();
            alertDialog.show();
        }
    }

    public void showAfter(int numberOfAccess){
        build();
        SharedPreferences.Editor editor = sharedPrefs.edit();
        int numOfAccess = sharedPrefs.getInt(SP_NUM_OF_ACCESS, 0);
        editor.putInt(SP_NUM_OF_ACCESS, numOfAccess + 1);
        editor.apply();
        if(numOfAccess + 1 >= numberOfAccess){
            show();
        }
    }


    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if(i == DialogInterface.BUTTON_POSITIVE){
            if(ratingBar.getRating() < upperBound){
                if(negativeReviewListener == null){
                    sendEmail();
                }else{
                    negativeReviewListener.onNegativeReview((int)ratingBar.getRating());
                }

            }else if(!isForceMode){
                openMarket();
            }
            disable();
            if(reviewListener != null)
                reviewListener.onReview((int)ratingBar.getRating());
        }
        if(i == DialogInterface.BUTTON_NEUTRAL){
            disable();
        }
        if(i == DialogInterface.BUTTON_NEGATIVE){
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putInt(SP_NUM_OF_ACCESS, 0);
            editor.apply();
        }
        alertDialog.hide();
    }

    public FiveStarsDialog setTitle(String title) {
        this.title = title;
        return this;

    }

    public FiveStarsDialog setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
        return this;
    }

    public FiveStarsDialog setRateText(String rateText){
        this.rateText = rateText;
        return this;
    }

    /**
     * Set to true if you want to send the user directly to the market
     * @param isForceMode
     * @return
     */
    public FiveStarsDialog setForceMode(boolean isForceMode){
        this.isForceMode = isForceMode;
        return this;
    }

    /**
     * Set the upper bound for the rating.
     * If the rating is >= of the bound, the market is opened.
     * @param bound the upper bound
     * @return the dialog
     */
    public FiveStarsDialog setUpperBound(int bound){
        this.upperBound = bound;
        return this;
    }

    /**
     * Set a custom listener if you want to OVERRIDE the default "send email" action when the user gives a negative review
     * @param listener
     * @return
     */
    public FiveStarsDialog setNegativeReviewListener(NegativeReviewListener listener){
        this.negativeReviewListener = listener;
        return  this;
    }

    /**
     * Set a listener to get notified when a review (positive or negative) is issued, for example for tracking purposes
     * @param listener
     * @return
     */
    public FiveStarsDialog setReviewListener(ReviewListener listener){
        this.reviewListener = listener;
        return this;
    }

}
