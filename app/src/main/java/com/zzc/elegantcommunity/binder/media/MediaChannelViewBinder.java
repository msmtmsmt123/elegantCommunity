package com.zzc.elegantcommunity.binder.media;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.zzc.elegantcommunity.ErrorAction;
import com.zzc.elegantcommunity.R;
import com.zzc.elegantcommunity.bean.media.MediaChannelBean;
import com.zzc.elegantcommunity.interfaces.IOnItemLongClickListener;
import com.zzc.elegantcommunity.module.media.home.MediaHomeActivity;
import com.zzc.elegantcommunity.util.ImageLoader;
import com.zzc.elegantcommunity.widget.CircleImageView;

import java.util.concurrent.TimeUnit;

import io.reactivex.functions.Consumer;
import me.drakeet.multitype.ItemViewBinder;

/**
 * Created by Meiji on 2017/6/12.
 */

public class MediaChannelViewBinder extends ItemViewBinder<MediaChannelBean, MediaChannelViewBinder.ViewHolder> {

    private IOnItemLongClickListener listener;

    public MediaChannelViewBinder(IOnItemLongClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    protected MediaChannelViewBinder.ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        View view = inflater.inflate(R.layout.item_media_channel, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    protected void onBindViewHolder(@NonNull final ViewHolder holder, @NonNull final MediaChannelBean item) {
        try {
            final Context context = holder.itemView.getContext();
            String url = item.getAvatar();
            ImageLoader.loadCenterCrop(context, url, holder.cv_avatar, R.color.viewBackground);
            holder.tv_mediaName.setText(item.getName());
            holder.tv_descText.setText(item.getDescText());

            RxView.clicks(holder.itemView)
                    .throttleFirst(1, TimeUnit.SECONDS)
                    .subscribe(new Consumer<Object>() {
                        @Override
                        public void accept(@io.reactivex.annotations.NonNull Object o) throws Exception {
                            MediaHomeActivity.launch(item.getId());
                        }
                    });
        } catch (Exception e) {
            ErrorAction.print(e);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        private CircleImageView cv_avatar;
        private TextView tv_mediaName;
        private TextView tv_followCount;
        private TextView tv_descText;
        private IOnItemLongClickListener listener;

        public ViewHolder(View itemView, IOnItemLongClickListener listener) {
            super(itemView);
            this.cv_avatar = itemView.findViewById(R.id.cv_avatar);
            this.tv_mediaName = itemView.findViewById(R.id.tv_mediaName);
            this.tv_followCount = itemView.findViewById(R.id.tv_followCount);
            this.tv_descText = itemView.findViewById(R.id.tv_descText);
            this.listener = listener;
            itemView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            if (listener != null) {
                listener.onLongClick(v, getLayoutPosition());
                return true;
            }
            return false;
        }
    }
}
