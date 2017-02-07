package kh.android.donotforgot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by liangyuteng0927 on 17-1-6.
 * Email: liangyuteng12345@gmail.com
 */

public class ContextCls {
    private static class Item {
        private String mTitle;
        private boolean mEnable;

        String getTitle() {
            return mTitle;
        }

        void setTitle(String mTitle) {
            this.mTitle = mTitle;
        }

        boolean isEnable() {
            return mEnable;
        }

        void setEnable(boolean mEnable) {
            this.mEnable = mEnable;
        }
        @Override
        public String toString () {
            return "[Title=" + mTitle + "][Enable=" + mEnable + "]";
        }
        static ArrayList<Item> getList (Context context) {
            SharedPreferences preferences = context.getApplicationContext()
                    .getSharedPreferences("items", Context.MODE_PRIVATE);
            Set<String> titles = preferences.getStringSet("list", null);
            Set<String> enables = preferences.getStringSet("enable", null);
            if (titles == null || enables == null)
                return new ArrayList<>();
            ArrayList<Item> items = new ArrayList<>();
            ArrayList<String> titleList = new ArrayList<>();
            titleList.addAll(titles);
            ArrayList<String> enableList = new ArrayList<>();
            enableList.addAll(enables);
            for (String s : titleList) {
                Item item = new Item();
                item.mTitle = s;
                for (String str : enableList) {
                    if (str.equals(s)) {
                        item.mEnable = true;
                        break;
                    }
                }
                items.add(item);
            }
            return items;
        }
        static void updateList (Context context, ArrayList<Item> items) {
            Set<String> titles = new HashSet<>();
            Set<String> enables = new HashSet<>();
            for (Item item : items) {
                titles.add(item.mTitle);
                if (item.mEnable) {
                    enables.add(item.mTitle);
                }
            }
            SharedPreferences preferences = context.getApplicationContext()
                    .getSharedPreferences("items", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putStringSet("list", titles);
            editor.putStringSet("enable", enables);
            editor.apply();
        }
    }
    public static class ManageActivity extends Activity {
        private ListView mListView;
        private ArrayList<Item> mList;
        private Adapter mAdapter;
        private NotificationManager mNotificationManager;
        private static final int ID = 0;
        @Override
        protected void onCreate( Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            LinearLayout linearLayout = new LinearLayout(this);
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
            mListView = new ListView(this);
            mList = Item.getList(this);
            mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            updateNotification();
            mAdapter = new Adapter();
            mListView.setAdapter(mAdapter);
            Button button_add = new Button(this);
            button_add.setText("添加");
            button_add.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final EditText editText = new EditText(ManageActivity.this);
                    editText.setSingleLine(true);
                    new AlertDialog.Builder(ManageActivity.this)
                            .setTitle("添加")
                            .setView(editText).setCancelable(false)
                            .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (!editText.getText().toString().isEmpty()) {
                                        Item item = new Item();
                                        item.setTitle(editText.getText().toString());
                                        item.setEnable(false);
                                        mList.add(item);
                                        mAdapter.notifyDataSetChanged();
                                    }
                                }
                            })
                            .setPositiveButton(android.R.string.cancel, null).show();
                }
            });
            Button button_ok = new Button(this);
            button_ok.setText(android.R.string.ok);
            button_ok.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Item.updateList(ManageActivity.this, mList);
                    updateNotification();
                    finish();
                }
            });
            Button button_about = new Button(this);
            button_about.setText("关于");
            button_about.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://coolapk.com/u/543424")));
                    } catch (Exception e) {
                        e.printStackTrace();
                        // nothing
                    }
                }
            });
            linearLayout.setGravity(Gravity.CENTER);
            linearLayout.addView(button_add);
            linearLayout.addView(button_about);
            linearLayout.addView(button_ok);
            LinearLayout view = new LinearLayout(this);
            view.setOrientation(LinearLayout.VERTICAL);
            view.addView(linearLayout);
            view.addView(mListView);
            setContentView(view);
        }
        private void updateNotification () {
            Notification.Builder builder = new Notification.Builder(this);
            builder.setOngoing(true);
            builder.setSmallIcon(R.mipmap.ic_launcher);
            if (Build.VERSION.SDK_INT >= 21) {
                builder.setVisibility(Notification.VISIBILITY_SECRET);
            }
            builder.setPriority(Notification.PRIORITY_MIN);
            String text = "";
            int enable = 0;
            for (int i = 0; i < mList.size(); i ++) {
                Item item = mList.get(i);
                if (item.isEnable()) {
                    enable++;
                    text += item.getTitle() + "\n";
                }
            }
            if (text.equals("")) {
                mNotificationManager.cancel(ID);
                return;
            }
            builder.addAction(0, "管理", PendingIntent.
                    getActivity(this, 0, new Intent(this, ManageActivity.class), 0));
            builder.setContentTitle(String.format("%1$s个在做任务", String.valueOf(enable)));
            builder.setSubText(String.format("共%1$s个任务", String.valueOf(mList.size())));
            builder.setContentText(text);
            builder.setStyle(new Notification.BigTextStyle()
                    .bigText(text));
            Notification notification = builder.build();
            mNotificationManager.notify(ID, notification);
        }
        private class Adapter extends ArrayAdapter<Item> {
            Adapter () {
                super(ManageActivity.this, 0, mList);
            }
            @Override
            public
            View getView(final int position,
                         View convertView,
                          ViewGroup parent) {
                LinearLayout layout = new LinearLayout(ManageActivity.this);
                layout.setOrientation(LinearLayout.HORIZONTAL);
                final Item item = mList.get(position);
                CheckBox compatCheckBox = new CheckBox(ManageActivity.this);
                compatCheckBox.setText(item.getTitle());
                compatCheckBox.setChecked(item.isEnable());
                compatCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        item.setEnable(isChecked);
                    }
                });
                Button button_edit = new Button(ManageActivity.this);
                button_edit.setText("改");
                button_edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final EditText editText = new EditText(ManageActivity.this);
                        editText.setText(item.getTitle());
                        editText.setSingleLine(true);
                        new AlertDialog.Builder(ManageActivity.this)
                                .setTitle("编辑")
                                .setView(editText).setCancelable(false)
                                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (!editText.getText().toString().isEmpty()) {
                                            item.setTitle(editText.getText().toString());
                                            notifyDataSetChanged();
                                        }
                                    }
                                })
                                .setNeutralButton("删除", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new AlertDialog.Builder(ManageActivity.this)
                                                .setTitle("删除")
                                                .setMessage(String.format("删掉 %1$s ?", item.getTitle()))
                                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        mList.remove(position);
                                                        mAdapter.notifyDataSetChanged();
                                                    }
                                                }).setNegativeButton(android.R.string.cancel, null).setCancelable(false)
                                                .show();
                                    }
                                })
                                .setPositiveButton(android.R.string.cancel, null).show();
                    }
                });

                layout.addView(button_edit);
                layout.addView(compatCheckBox);
                return layout;
            }
        }
    }
}
