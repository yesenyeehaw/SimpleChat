package com.codepath.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class ChatActivity extends AppCompatActivity {

    static final String USER_ID_KEY = "userId";
    static final String BODY_KEY = "body";
    static final String TAG = "ChatActivity";
    RecyclerView rvChat;
    EditText etMessage;
    ImageButton ibSend;
    boolean mFirstLoad;
    ArrayList mMessages;
    ChatAdapter mAdapter;
    static final int MAX_CHAT_MESSAGES_TO_SHOW = 50;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ParseObject.registerSubclass(Message.class);

        setContentView(R.layout.activity_chat);

// building a client to communicate with the server
        // Use for monitoring Parse network traffic
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        // Can be Level.BASIC, Level.HEADERS, or Level.BODY
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        // any network interceptors must be added with the Configuration Builder given this syntax
        builder.networkInterceptors().add(httpLoggingInterceptor);

        // Set applicationId and server based on the values in the Back4App settings.
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("BgMeRhXN4Mfsr4iu7cg2PJPITPJgn9T4meopyWPG") // ⚠️ TYPE IN A VALID APPLICATION ID HERE
                .clientKey("r6fZMD0BL8dZx0pVlYemVJbHwqaBtIRqO19SX40c") // ⚠️ TYPE IN A VALID CLIENT KEY HERE
                .clientBuilder(builder)
                .server("https://parseapi.back4app.com/").build());  // ⚠️ TYPE IN A VALID SERVER URL HERE
        login();
    }
    void startWithCurrentUser(){
        setupMessagePosting();
    }

    void login(){
        ParseAnonymousUtils.logIn(new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Anonymous login failed: ", e);
                } else {
                    startWithCurrentUser();
                }
            }
        });
    }

    // Set up button event handler which posts the entered message to Parse
    void setupMessagePosting() {
        // Find the text field and button
        etMessage = (EditText) findViewById(R.id.etMessage);
        ibSend = (ImageButton) findViewById(R.id.ibSend);
        rvChat = (RecyclerView) findViewById(R.id.rvChat);
        mMessages = new ArrayList<>();
        mFirstLoad = true;
        final String userId = ParseUser.getCurrentUser().getObjectId();
        mAdapter = new ChatAdapter(ChatActivity.this, userId, mMessages);
        rvChat.setAdapter(mAdapter);

        // associate the LayoutManager with the RecylcerView
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ChatActivity.this);
        linearLayoutManager.setReverseLayout(true);
        //tell the recycler view how to display
        rvChat.setLayoutManager(linearLayoutManager);

        // When send button is clicked, create message object on Parse creates and sends to database
        ibSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = etMessage.getText().toString();
                //ParseObject message = ParseObject.create("Message");
                //message.put(USER_ID_KEY, ParseUser.getCurrentUser().getObjectId());
                //message.put(BODY_KEY, data);

                // Using new `Message.java` Parse-backed model now
                Message message = new Message();
                message.setUserId(ParseUser.getCurrentUser().getObjectId());
                message.setBody(data);
                message.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e == null) {
                            Toast.makeText(ChatActivity.this, "Successfully created message on Parse",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Failed to save message", e);
                        }
                    }
                });
                etMessage.setText(null);
            }
        });
    }
    // Query messages from Parse so we can load them into the chat adapter
    void refreshMessages() {
        // Construct query to execute
        ParseQuery<Message> query = ParseQuery.getQuery(Message.class);
        // Configure limit and sort order
        query.setLimit(MAX_CHAT_MESSAGES_TO_SHOW);

        // get the latest 50 messages, order will show up newest to oldest of this group
        query.orderByDescending("createdAt");
        // Execute query to fetch all messages from Parse asynchronously
        // This is equivalent to a SELECT query with SQL
        query.findInBackground(new FindCallback<Message>() {
            public void done(List<Message> messages, ParseException e) {
                if (e == null) {
                    mMessages.clear();
                    mMessages.addAll(messages);
                    mAdapter.notifyDataSetChanged(); // update adapter
                    // Scroll to the bottom of the list on initial load
                    if (mFirstLoad) {
                        rvChat.scrollToPosition(0);
                        mFirstLoad = false;
                    }
                } else {
                    Log.e("message", "Error Loading Messages" + e);
                }
            }
        });
    }
    // Create a handler which can run code periodically
    static final long POLL_INTERVAL = TimeUnit.SECONDS.toMillis(3);
    Handler myHandler = new android.os.Handler();
    Runnable mRefreshMessagesRunnable = new Runnable() {
        @Override
        public void run() {
            refreshMessages();
            myHandler.postDelayed(this, POLL_INTERVAL);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        // Only start checking for new messages when the app becomes active in foreground
        myHandler.postDelayed(mRefreshMessagesRunnable, POLL_INTERVAL);
    }

    @Override
    protected void onPause() {
        // Stop background task from refreshing messages, to avoid unnecessary traffic & battery drain
        myHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

}