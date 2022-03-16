package dev.truewinter.framed.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Set;

import dev.truewinter.framed.R;

public class ScenesAdapter extends RecyclerView.Adapter<ScenesAdapter.ViewHolder> {
    //private List<String> mData;
    private Set<String> sceneSet;
    private String currentScene;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    public ScenesAdapter(Context context, Set<String> sceneSet) {
        this.mInflater = LayoutInflater.from(context);
        this.sceneSet = sceneSet;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.scene_list_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        //String hostname = mData.get(position);
        String name = sceneSet.toArray()[position].toString();
        holder.sceneView.setText(name);
        holder.currentScene.setVisibility(View.GONE);

        if (name.equals(currentScene)) {
            holder.currentScene.setVisibility(View.VISIBLE);
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return sceneSet.size();
        //return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener/*, View.OnLongClickListener*/ {
        TextView sceneView;
        TextView currentScene;

        ViewHolder(View itemView) {
            super(itemView);
            sceneView = itemView.findViewById(R.id.sceneView);
            currentScene = itemView.findViewById(R.id.currentScene);
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
        return sceneSet.toArray()[index].toString();
    }

    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public void setCurrentScene(String currentScene) {
        this.currentScene = currentScene;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
        //void onLongClick(View view, int position);
    }
}
