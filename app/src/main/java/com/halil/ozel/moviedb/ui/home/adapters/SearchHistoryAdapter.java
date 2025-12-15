package com.halil.ozel.moviedb.ui.home.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.halil.ozel.moviedb.R;
import java.util.List;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.HistoryViewHolder> {

    private  List<String> historyList;
    private final OnHistoryItemClickListener listener;
    // Interface untuk menangani klik
    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(String query);
    }

    public SearchHistoryAdapter(List<String> historyList, OnHistoryItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }
    public void updateData(List<String> newHistory) {
        this.historyList = newHistory;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        String query = historyList.get(position);
        holder.tvQuery.setText(query);
        holder.itemView.setOnClickListener(v -> listener.onHistoryItemClick(query));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuery;

        HistoryViewHolder(View itemView) {
            super(itemView);
            tvQuery = itemView.findViewById(R.id.tvHistoryQuery);
        }
    }
}