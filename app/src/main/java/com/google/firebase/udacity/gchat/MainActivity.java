/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.udacity.gchat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER =  2;


    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;

    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private String mUsername;

    //Firebase Database fields
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mFirebaseDatabaseRef;
    private ChildEventListener mChildEventListener;

    //Firebase Authentication fields
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mFirebaseAuthStateListener;

    //Firebase storage Fields
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mFirebaseStorageReference;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;

        //Initialize firebase DB
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseDatabaseRef= mFirebaseDatabase.getReference().child(GChatUtils.FIREBASE_ROOT_MESSAGES);

        //Initialize firebase auth
        mFirebaseAuth=FirebaseAuth.getInstance();

        //Initialize firebase storage
        mFirebaseStorage = FirebaseStorage.getInstance();
        mFirebaseStorageReference = mFirebaseStorage.getReference().child(GChatUtils.FIREBASE_ROOT_STORAGE);




        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(this);
        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPhotoButtonClicked();
            }


        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });



        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        mFirebaseAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user!=null){
                    onSignedInInitialize(user.getDisplayName());
                }else{
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).
                                            build(),
                                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

    }

    private void onPhotoButtonClicked() {
        Intent pickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
        //Any type of image
        pickerIntent.setType("image/*");

        //No need download from a remote service like (drive)
        pickerIntent.putExtra(Intent.EXTRA_LOCAL_ONLY,true);

        startActivityForResult(Intent.createChooser(pickerIntent,"Choose pics from the below sources"),RC_PHOTO_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case RC_SIGN_IN:
                if(resultCode == RESULT_CANCELED){
                    finish();
                }
                break;
            case RC_PHOTO_PICKER:
                if(resultCode == RESULT_OK){
                    Uri imageUri = data.getData();
                    StorageReference storageReference = mFirebaseStorageReference.child(imageUri.getLastPathSegment());
                    storageReference.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                Uri imageUrl = taskSnapshot.getDownloadUrl();
                                FriendlyMessage friendlyMessage = new FriendlyMessage(null,mUsername,imageUrl.toString());
                                mFirebaseDatabaseRef.push().setValue(friendlyMessage);

                        }
                    });
                }
                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.sendButton:
                handleSendMessage();
                break;


        }
    }

    private void handleSendMessage() {
        String message = mMessageEditText.getText().toString();

        FriendlyMessage friendlyMessage = new FriendlyMessage(message,
                mUsername,null);
        mFirebaseDatabaseRef.push().setValue(friendlyMessage);
        // Clear input box
        mMessageEditText.setText("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mFirebaseAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mFirebaseAuthStateListener !=null) {
            mFirebaseAuth.removeAuthStateListener(mFirebaseAuthStateListener);
        }
        cleanupReadDatabaseListener();
        mMessageAdapter.clear();

    }

    private void onSignedInInitialize(String displayName) {
        mUsername = displayName;
        initializeReadDatabase();
    }

    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
        cleanupReadDatabaseListener();
        mMessageAdapter.clear();

    }


    private void initializeReadDatabase(){

        if(mChildEventListener == null){

            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    FriendlyMessage friendlyMessage = dataSnapshot.getValue(FriendlyMessage.class);
                    mMessageAdapter.add(friendlyMessage);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            mFirebaseDatabaseRef.addChildEventListener(mChildEventListener);
        }
    }

    private void cleanupReadDatabaseListener(){
        if(mChildEventListener!=null){
            mFirebaseDatabaseRef.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

}
