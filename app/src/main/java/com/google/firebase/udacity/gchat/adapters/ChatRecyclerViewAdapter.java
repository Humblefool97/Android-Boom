package com.google.firebase.udacity.gchat.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.udacity.gchat.model.FriendlyMessage;
import com.google.firebase.udacity.gchat.R;

import java.util.List;

/**
 * Created by Rajeev on 2/26/2017.
 */

public class ChatRecyclerViewAdapter extends RecyclerView.Adapter<ChatRecyclerViewAdapter.PrimaryItemViewHolder> {

    private Context mContext;
    private List<FriendlyMessage> mDataSrc;
    private FirebaseAuth mFirebaseAuth;
    private static final int VIEW_TYPE_PRIMARY = 1;
    private static final int VIEW_TYPE_SECONDARY = 2;


    public ChatRecyclerViewAdapter(Context context,List<FriendlyMessage> data,FirebaseAuth firebaseAuth){
        mContext = context;
        mDataSrc = data;
        mFirebaseAuth = firebaseAuth;
    }


    public void setData(FriendlyMessage data){
        mDataSrc.add(data);
        notifyDataSetChanged();
    }

    public void clear(){
        if(mDataSrc!=null && ! mDataSrc.isEmpty()){
            mDataSrc.clear();
            notifyDataSetChanged();
        }
    }

    @Override
    public PrimaryItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view =((Activity)mContext).getLayoutInflater().inflate(R.layout.item_message,parent,false);
            return new PrimaryItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PrimaryItemViewHolder holder, int position) {

        if(mDataSrc != null && !mDataSrc.isEmpty()){
            FriendlyMessage message = mDataSrc.get(position);
            if(message.getName().equals(mFirebaseAuth.getCurrentUser().getDisplayName())){
                holder.mSecondaryView.setVisibility(View.GONE);
                holder.mPrimaryView.setVisibility(View.VISIBLE);
                boolean isPhoto = message.getPhotoUrl() != null;
                if (isPhoto) {
                    holder.messageTextView.setVisibility(View.GONE);
                    holder.imageView.setVisibility(View.VISIBLE);
                    Glide.with(holder.imageView.getContext())
                            .load(message.getPhotoUrl())
                            .crossFade()
                            .into(holder.imageView);
                } else {
                    holder.messageTextView.setVisibility(View.VISIBLE);
                    holder.imageView.setVisibility(View.GONE);
                    holder.messageTextView.setText(message.getText());
                }
                holder.authorTextView.setText(message.getName());
            }else{
                holder.mPrimaryView.setVisibility(View.GONE);
                holder.mSecondaryView.setVisibility(View.VISIBLE);
                boolean isPhoto = message.getPhotoUrl() != null;
                if (isPhoto) {
                    holder.secondaryMessageTextView.setVisibility(View.GONE);
                    holder.secondaryImageView.setVisibility(View.VISIBLE);
                    Glide.with(holder.secondaryImageView.getContext())
                            .load(message.getPhotoUrl())
                            .crossFade()
                            .into(holder.secondaryImageView);
                } else {
                    holder.secondaryMessageTextView.setVisibility(View.VISIBLE);
                    holder.secondaryImageView.setVisibility(View.GONE);
                    holder.secondaryMessageTextView.setText(message.getText());
                }
                holder.secondaryAuthorTextView.setText(message.getName());
            }

        }

    }


    @Override
    public int getItemCount() {
        if(mDataSrc!=null && !mDataSrc.isEmpty()){
            return  mDataSrc.size();
        }

        return 0;
    }

       public class PrimaryItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView messageTextView;
        TextView authorTextView;
        ImageView secondaryImageView;
        TextView secondaryMessageTextView;
        TextView secondaryAuthorTextView;
        View mPrimaryView;
        View mSecondaryView;


        public PrimaryItemViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.photoImageView);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            authorTextView = (TextView) itemView.findViewById(R.id.nameTextView);

            secondaryImageView = (ImageView) itemView.findViewById(R.id.AnotherPhotoImageView);
            secondaryMessageTextView = (TextView) itemView.findViewById(R.id.AnotherMessageTextView);
            secondaryAuthorTextView= (TextView) itemView.findViewById(R.id.AnotherNameTextView);

            mPrimaryView = itemView.findViewById(R.id.primaryChatView);
            mSecondaryView = itemView.findViewById(R.id.secondaryChatView);

        }
    }
}
