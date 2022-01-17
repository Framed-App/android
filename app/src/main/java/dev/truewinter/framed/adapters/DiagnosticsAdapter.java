package dev.truewinter.framed.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import dev.truewinter.framed.R;

public class DiagnosticsAdapter extends RecyclerView.Adapter<DiagnosticsAdapter.ViewHolder> {
    private Map<String, JSONObject> diagMap;
    private LayoutInflater mInflater;
    private DiagnosticsAdapter.ItemClickListener mClickListener;

    // data is passed into the constructor
    public DiagnosticsAdapter(Context context, Map<String, JSONObject> deviceMap) {
        this.mInflater = LayoutInflater.from(context);
        //this.mData = data;
        this.diagMap = deviceMap;
    }

    // inflates the row layout from xml when needed
    @Override
    public DiagnosticsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.diag_list_row, parent, false);
        return new DiagnosticsAdapter.ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(DiagnosticsAdapter.ViewHolder holder, int position) {
        //String hostname = mData.get(position);
        try {
            JSONObject thisObj = diagMap.get(diagMap.keySet().toArray()[position]);
            Date date = new Date(Long.parseLong(diagMap.keySet().toArray()[position].toString()));
            String dateTime = new SimpleDateFormat("D MMM YYYY h:mm:ss a").format(date);
            int droppedFrames = thisObj.getInt("frames");
            holder.textView.setText(dateTime);
            holder.droppedFramesView.setText(String.format(Locale.ENGLISH, "%d", droppedFrames));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return diagMap.size();
        //return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener/*, View.OnLongClickListener*/ {
        TextView textView;
        TextView droppedFramesView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.diagView);
            droppedFramesView = itemView.findViewById(R.id.diagDroppedFramesView);
            itemView.setOnClickListener(this);
            //itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }

        /*@Override
        public boolean onLongClick(View view) {
            if (mClickListener != null) mClickListener.onLongClick(view, getAdapterPosition());
            return true;
        }*/
    }

    // convenience method for getting data at click position
    public String getIdFromIndex(int index) {
        return diagMap.keySet().toArray()[index].toString();
    }

    // allows clicks events to be caught
    public void setClickListener(DiagnosticsAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
        //void onLongClick(View view, int position);
    }
}
