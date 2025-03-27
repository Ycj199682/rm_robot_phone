package com.reeman.phone.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reeman.phone.R;

import java.util.List;

public class LogFilesAdapter extends ArrayAdapter<String> {

    private Context context;
    private List<String> fileNames;
    private LogFileClickListener clickListener;

    public LogFilesAdapter(Context context, List<String> fileNames, LogFileClickListener clickListener) {
        super(context, R.layout.dialog_log_files_item, fileNames);
        this.context = context;
        this.fileNames = fileNames;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.dialog_log_files_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.fileNameTextView = convertView.findViewById(R.id.file_name);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String fileName = fileNames.get(position);
        viewHolder.fileNameTextView.setText(fileName);

        viewHolder.fileNameTextView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onLogFileClick(position);
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView fileNameTextView;
    }

    public interface LogFileClickListener {
        void onLogFileClick(int position);
    }
}
