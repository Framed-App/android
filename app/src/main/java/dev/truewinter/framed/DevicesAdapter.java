package dev.truewinter.framed;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {
    //private List<String> mData;
    private Map<String, JSONObject> deviceMap;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    DevicesAdapter(Context context, Map<String, JSONObject> deviceMap) {
        this.mInflater = LayoutInflater.from(context);
        //this.mData = data;
        this.deviceMap = deviceMap;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.device_list_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        //String hostname = mData.get(position);
        try {
            JSONObject thisObj = deviceMap.get(deviceMap.keySet().toArray()[position]);
            String hostname = thisObj.getString("hostname");
            String version = thisObj.getString("version");
            holder.textView.setText(String.format("%s (v%s)", hostname, version));
            holder.compatView.setVisibility(View.GONE);

            if (!thisObj.getBoolean("_compatible")) {
                holder.compatView.setVisibility(View.VISIBLE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return deviceMap.size();
        //return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        TextView textView;
        TextView compatView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.deviceView);
            compatView = itemView.findViewById(R.id.deviceIncompat);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View view) {
            if (mClickListener != null) mClickListener.onLongClick(view, getAdapterPosition());
            return true;
        }
    }

    // convenience method for getting data at click position
    String getIdFromIndex(int index) {
        return deviceMap.keySet().toArray()[index].toString();
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
        void onLongClick(View view, int position);
    }
}