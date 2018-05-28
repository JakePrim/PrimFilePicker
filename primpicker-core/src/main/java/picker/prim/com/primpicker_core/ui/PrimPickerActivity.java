package picker.prim.com.primpicker_core.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import picker.prim.com.primpicker_core.R;
import picker.prim.com.primpicker_core.cursors.FileLoaderCallback;
import picker.prim.com.primpicker_core.cursors.FileLoaderHelper;
import picker.prim.com.primpicker_core.entity.Directory;
import picker.prim.com.primpicker_core.entity.MediaItem;
import picker.prim.com.primpicker_core.entity.SelectItemCollection;
import picker.prim.com.primpicker_core.entity.SelectSpec;
import picker.prim.com.primpicker_core.ui.adapter.DirectoryAdapter;
import picker.prim.com.primpicker_core.ui.adapter.SelectAdapter;
import picker.prim.com.primpicker_core.ui.view.DirectorySpinner;

/**
 * ================================================
 * 作    者：linksus
 * 版    本：1.0
 * 创建日期：5/24 0024
 * 描    述：选择文件的activity 这里参考了部分知乎的开源项目代码
 * 修订历史：
 * ================================================
 */
public class PrimPickerActivity extends AppCompatActivity implements FileLoaderCallback.LoaderCallback,
        View.OnClickListener,
        PrimSelectFragment.OnSelectFragmentListener,
        SelectAdapter.OnSelectItemListener,
        DirectorySpinner.OnDirsItemSelectedListener {

    private ImageView iv_picker_back;

    private TextView tv_picker_type;

    private TextView btn_next;

    private CheckBox cb_compress;

    private static final String TAG = "PrimPickerActivity";

    private FrameLayout container, layout_empty;

    private SelectItemCollection selectItemCollection;

    private Directory directory;

    private RelativeLayout layout_bottom;

    private DirectorySpinner directorySpinner;

    private DirectoryAdapter directoryAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);
        selectItemCollection = new SelectItemCollection(this);
        iv_picker_back = (ImageView) findViewById(R.id.iv_picker_back);
        layout_bottom = (RelativeLayout) findViewById(R.id.layout_bottom);
        tv_picker_type = (TextView) findViewById(R.id.tv_picker_type);
        btn_next = (TextView) findViewById(R.id.btn_next);
        cb_compress = (CheckBox) findViewById(R.id.cb_compress);
        container = (FrameLayout) findViewById(R.id.container);
        layout_empty = (FrameLayout) findViewById(R.id.layout_empty);
        FileLoaderHelper.getInstance().onCreate(this, this);
        FileLoaderHelper.getInstance().onRestoreInstanceState(savedInstanceState);
        FileLoaderHelper.getInstance().getLoadDirs();
        directoryAdapter = new DirectoryAdapter(this, null, false);
        directorySpinner = new DirectorySpinner(this);
        directorySpinner.setOnDirsItemSelectedListener(this);
        directorySpinner.setSelectTextView(tv_picker_type);
        directorySpinner.setPopupAnchorView(findViewById(R.id.layout_top));
        directorySpinner.setAdapter(directoryAdapter);
        iv_picker_back.setOnClickListener(this);
        btn_next.setOnClickListener(this);
        cb_compress.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isComprss = isChecked;
            }
        });
        isComprss = SelectSpec.getInstance().compress;
        cb_compress.setChecked(isComprss);
        if (SelectSpec.getInstance().onlyShowVideos()) {
            layout_bottom.setVisibility(View.VISIBLE);
        } else {
            layout_bottom.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        FileLoaderHelper.getInstance().onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileLoaderHelper.getInstance().onDestory();
    }

    @Override
    public void loadFinish(final Cursor data) {
        directoryAdapter.swapCursor(data);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                data.moveToPosition(0);
                directory = Directory.valueOf(data);
                if (directory.isAll() && SelectSpec.getInstance().capture) {
                    directory.addCaptureCount();
                }
                setData(directory);

            }
        });
    }

    private void setData(Directory directory) {
        tv_picker_type.setText(directory.getDisplayName(this));
        if (directory.isAll() && directory.isEmpty()) {
            container.setVisibility(View.GONE);
            layout_empty.setVisibility(View.VISIBLE);
        } else {
            container.setVisibility(View.VISIBLE);
            layout_empty.setVisibility(View.GONE);
            PrimSelectFragment fragment = PrimSelectFragment.newInstance(directory);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment, PrimSelectFragment.class.getSimpleName())
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void loadReset() {
        directoryAdapter.swapCursor(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public static final String EXTRA_RESULT_SELECTION = "extra_result_selection";
    public static final String EXTRA_RESULT_SELECTION_PATH = "extra_result_selection_path";
    public static final String EXTRA_RESULT_COMPRESS = "extra_result_compress";
    public boolean isComprss;

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.iv_picker_back) {
            finish();
        } else if (i == R.id.btn_next) {
            Intent result = new Intent();
            ArrayList<Uri> selectedUris = (ArrayList<Uri>) selectItemCollection.asListOfUri();
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
            ArrayList<String> selectedPaths = (ArrayList<String>) selectItemCollection.asListOfString();
            result.putStringArrayListExtra(EXTRA_RESULT_SELECTION_PATH, selectedPaths);
            result.putExtra(EXTRA_RESULT_COMPRESS, isComprss);
            setResult(RESULT_OK, result);
            finish();
        }
    }

    @Override
    public SelectItemCollection getSelectItemCollction() {
        return selectItemCollection;
    }

    public static final int REQUEST_CODE_PREVIEW = 601;

    @Override
    public void itemClick(View view, MediaItem item, int position) {
        PerviewActivity.newInstance(this, directory, item);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onUpdate() {
        if (selectItemCollection.isEmpty()) {
            btn_next.setTextColor(getResources().getColor(R.color.color_666666));
            btn_next.setEnabled(false);
            btn_next.setText(getResources().getString(R.string.str_next_text));
        } else {
            btn_next.setEnabled(true);
            btn_next.setTextColor(getResources().getColor(R.color.color_ffffff));
            btn_next.setText(getResources().getString(R.string.str_next_text) + "(" + selectItemCollection.count() + ")");
        }
    }

    @Override
    public void onDirItemSelected(AdapterView<?> parent, View view, int position, long id) {
        directoryAdapter.getCursor().moveToPosition(position);
        Directory directory = Directory.valueOf(directoryAdapter.getCursor());
        if (directory.isAll() && SelectSpec.getInstance().capture) {
            directory.addCaptureCount();
        }
        setData(directory);
    }
}