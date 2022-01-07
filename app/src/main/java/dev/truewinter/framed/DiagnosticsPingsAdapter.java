package dev.truewinter.framed;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DiagnosticsPingsAdapter extends RecyclerView.Adapter<DiagnosticsPingsAdapter.ViewHolder> {
    private List<JSONObject> pingList;
    private LayoutInflater mInflater;
    private DiagnosticsPingsAdapter.ItemClickListener mClickListener;

    // data is passed into the constructor
    DiagnosticsPingsAdapter(Context context, List<JSONObject> pingList) {
        this.mInflater = LayoutInflater.from(context);
        //this.mData = data;
        this.pingList = pingList;
    }

    // inflates the row layout from xml when needed
    @Override
    public DiagnosticsPingsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.diag_ping_list_row, parent, false);
        return new DiagnosticsPingsAdapter.ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(DiagnosticsPingsAdapter.ViewHolder holder, int position) {
        //String hostname = mData.get(position);
        try {
            JSONObject thisObj = pingList.get(position);

            holder.textView.setText(thisObj.getString("name"));
            holder.pingView.setText(String.format(Locale.ENGLISH, "%.2fms", thisObj.getDouble("ping")));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return pingList.size();
        //return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener/*, View.OnLongClickListener*/ {
        TextView textView;
        TextView pingView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.diagPingsView);
            pingView = itemView.findViewById(R.id.diagPingsTime);
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

    // allows clicks events to be caught
    void setClickListener(DiagnosticsPingsAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
        //void onLongClick(View view, int position);
    }
}
