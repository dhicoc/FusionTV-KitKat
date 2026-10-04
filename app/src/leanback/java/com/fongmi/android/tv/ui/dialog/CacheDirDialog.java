package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;

import com.fongmi.android.tv.Setting;
import com.fongmi.android.tv.databinding.DialogCacheDirBinding;
import com.fongmi.android.tv.impl.CacheDirCallback;
import com.fongmi.android.tv.ui.adapter.CacheDirAdapter;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CacheDirDialog implements CacheDirAdapter.OnClickListener {
    private final DialogCacheDirBinding binding;
    private final CacheDirAdapter adapter;
    private final AlertDialog dialog;
    private final CacheDirCallback callback;
    private List<String> mItems;
    private int position;
    public static CacheDirDialog create(Activity activity) {
        return new CacheDirDialog(activity);
    }

    public CacheDirDialog(Activity activity) {
        mItems = new ArrayList<>();
        mItems.add(activity.getCacheDir().getAbsolutePath());
        for(File dir : activity.getExternalCacheDirs()) mItems.add(dir.getAbsolutePath());
        String cacheDir = Setting.getThunderCacheDir();
        position = 0;
        for(int i=0; i<mItems.size(); i++) {
            if (mItems.get(i).equals(cacheDir)) position = i;
        }
        this.adapter = new CacheDirAdapter(this, mItems);
        this.callback = (CacheDirCallback) activity;
        this.binding = DialogCacheDirBinding.inflate(LayoutInflater.from(activity));
        this.dialog = new MaterialAlertDialogBuilder(activity).setView(binding.getRoot()).create();
    }

    public void show() {
        initView();
    }

    private int getCount() {
        return 1;
    }

    private float getWidth() {
        return 0.7f + (getCount() - 1) * 0.2f;
    }

    private void initView() {
        setRecyclerView();
        setDialog();
    }

    private void setRecyclerView() {
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setItemAnimator(null);
        binding.recycler.addItemDecoration(new SpaceItemDecoration(getCount(), 16));
        binding.recycler.setLayoutManager(new GridLayoutManager(dialog.getContext(), getCount()));
        binding.recycler.post(() -> binding.recycler.scrollToPosition(position));

    }

    private void setDialog() {
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (ResUtil.getScreenWidth() * getWidth());
        dialog.getWindow().setAttributes(params);
        dialog.getWindow().setDimAmount(0);
        dialog.show();
    }

    @Override
    public void onItemClick(String path) {
        callback.setCacheDir(path);
        dialog.dismiss();
    }

}
